/*
 * Copyright (c) 2015-2016 Adrian Siekierka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pl.asie.charset.pipes.pipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import net.minecraft.util.math.Vec3d;
import net.minecraftforge.items.IItemHandler;

import mcmultipart.multipart.IMultipart;
import mcmultipart.multipart.IMultipartContainer;
import mcmultipart.multipart.PartSlot;
import pl.asie.charset.api.lib.IItemInjectable;
import pl.asie.charset.api.pipes.IShifter;
import pl.asie.charset.lib.inventory.InventoryUtils;
import pl.asie.charset.lib.utils.DirectionUtils;
import pl.asie.charset.lib.utils.ItemUtils;
import pl.asie.charset.pipes.ModCharsetPipes;
import pl.asie.charset.pipes.PipeUtils;

import javax.annotation.Nullable;

public class PipeItem {
	public static final int MAX_PROGRESS = 128;
	public static final int CENTER_PROGRESS = MAX_PROGRESS / 2;
	public static final int SPEED = MAX_PROGRESS / 16;
	private static short nextId;

	public final short id;
	byte blocksSinceSync;

	protected EnumFacing input, output;
	protected boolean reachedCenter;
	protected ItemStack stack;
	protected int progress;

	private int activeShifterDistance;
	private PartPipe owner;
	private boolean stuck;

	public PipeItem(PartPipe owner, ItemStack stack, EnumFacing side) {
		this.id = nextId++;
		this.owner = owner;
		this.stack = stack;
		initializeFromEntrySide(side);
	}

	public PipeItem(PartPipe owner, NBTTagCompound nbt) {
		this.id = nextId++;
		this.owner = owner;
		readFromNBT(nbt);
	}

	protected PipeItem(PartPipe tile, short id) {
		this.owner = tile;
		this.id = id;
	}

	public boolean isStuck() {
		return stuck;
	}

	public boolean isValid() {
		return stack != null && stack.getItem() != null && input != null;
	}

	private float getTranslatedCoord(int offset) {
		if (progress >= CENTER_PROGRESS) {
			return 0.5F + (float) offset * (progress - CENTER_PROGRESS) / MAX_PROGRESS;
		} else {
			switch (offset) {
				case -1:
					return 1.0F + (float) offset * progress / MAX_PROGRESS;
				case 0:
				default:
					return 0.5F;
				case 1:
					return (float) offset * progress / MAX_PROGRESS;
			}
		}
	}

	public float getX() {
		return getDirection() != null ? getTranslatedCoord(getDirection().getFrontOffsetX()) : 0.5F;
	}

	public float getY() {
		return getDirection() != null ? getTranslatedCoord(getDirection().getFrontOffsetY()) : 0.5F;
	}

	public float getZ() {
		return getDirection() != null ? getTranslatedCoord(getDirection().getFrontOffsetZ()) : 0.5F;
	}

	public ItemStack getStack() {
		return stack;
	}

	public @Nullable EnumFacing getDirection() {
		return reachedCenter ? output : (input != null ? input.getOpposite() : null);
	}

	private boolean isCentered() {
		return progress == CENTER_PROGRESS;
	}

	// This version takes priority into account (filtered shifters are
	// prioritized over unfiltered shifters at the same distance).
	private int getInternalShifterStrength(IShifter shifter, EnumFacing dir) {
		if (shifter == null) {
			return 0;
		} else {
			return owner.getShifterStrength(dir) * 2 + (shifter.hasFilter() ? 0 : 1);
		}
	}

	private void updateStuckFlag() {
		if (progress <= CENTER_PROGRESS) {
			boolean needsRecalculation = false;

			if (!isValidDirection(output)) {
				needsRecalculation = true;
			} else if (stuck && isCentered()) {
				// Detect changes in shifter air stream.
				boolean foundShifter = false;
				int minimumShifterDistance = Integer.MAX_VALUE;

				// Find the closest shifter affecting the item.
				for (EnumFacing dir : EnumFacing.VALUES) {
					IShifter p = owner.getNearestShifter(dir);
					int ps = getInternalShifterStrength(p, dir);
					if (ps > 0 && ps < minimumShifterDistance
							&& isShifterPushing(p, output)) {
						minimumShifterDistance = ps;
						foundShifter = true;
					}
				}

				if (
						(!foundShifter && activeShifterDistance > 0)
								|| (foundShifter && activeShifterDistance != minimumShifterDistance)
								|| (foundShifter && activeShifterDistance != getInternalShifterStrength(owner.getNearestShifter(output), output))
						) {
					TileEntity shifterTile = owner.getWorld().getTileEntity(owner.getPos().offset(output.getOpposite(), activeShifterDistance));

					if (shifterTile == null
							|| !shifterTile.hasCapability(ModCharsetPipes.CAP_SHIFTER, output)
							|| !isShifterPushing(shifterTile.getCapability(ModCharsetPipes.CAP_SHIFTER, output), output)) {
						needsRecalculation = true;
					}
				}
			}

			if (needsRecalculation) {
				calculateOutputDirection();
			}
		} else {
			if (!isValidDirection(output)) {
				output = null;
			}
		}

		if (output == null) {
			// Never stuck when UNKNOWN, because the item will drop anyway.
			stuck = false;
		} else {
			if (isCentered() && activeShifterDistance > 0
					&& owner.getShifterStrength(output.getOpposite()) == owner.getShifterStrength(output)
					&& isShifterPushing(owner.getNearestShifter(output.getOpposite()), output.getOpposite())) {
				// Handle the "equal-distance opposite shifters" scenario for stopping items.
				// This does not take filtering into account!
				stuck = true;
			} else {
				stuck = !canMoveDirection(output, false);
			}
		}
	}

	PacketItemUpdate getSyncPacket(boolean syncStack) {
		return new PacketItemUpdate(owner, this, syncStack);
	}

	protected void sendPacket(boolean syncStack) {
		if (owner.getWorld() != null && !owner.getWorld().isRemote) {
			ModCharsetPipes.packet.sendToAllAround(getSyncPacket(syncStack), owner, ModCharsetPipes.PIPE_TESR_DISTANCE);
		}
	}

	public boolean move() {
		if (!reachedCenter) {
			boolean atCenter = (progress + SPEED) >= CENTER_PROGRESS;

			if (atCenter) {
				onReachedCenter();
			} else if (!stuck) {
				progress += SPEED;
			}
		} else {
			if (owner.getWorld().isRemote) {
				if (!stuck) {
					progress += SPEED;
				}

				if (progress >= MAX_PROGRESS) {
					onItemEnd();
					return false;
				}
			} else {
				EnumFacing oldOutput = output;
				boolean oldStuck = stuck;

				updateStuckFlag();

				if (!stuck) {
					progress += SPEED;
				}

				if (progress >= MAX_PROGRESS) {
					onItemEnd();
					return false;
				}

				if (oldStuck != stuck || oldOutput != output) {
					sendPacket(false);
				}
			}
		}

		return true;
	}

	private void onItemEnd() {
		PartPipe pipe = output != null ? PipeUtils.getPipe(owner.getWorld(), owner.getPos().offset(output), output.getOpposite()) : null;

		if (owner.getWorld().isRemote) {
			if (blocksSinceSync < 2) {
				if (passToPipe(pipe, output, false)) {
					blocksSinceSync++;
				} else {
					stack = null;
				}
			} else {
				stack = null;
			}
		} else {
			if (output != null) {
				if (passToPipe(pipe, output, false)) {
					// Pipe passing does not take into account stack size
					// subtraction, as it re-uses the same object instance.
					// Therefore, we need to quit here.
					return;
				} else {
					TileEntity tile = owner.getNeighbourTile(output);

					if (!passToInjectable(tile, output, false)) {
						addToItemHandler(tile, output, false);
					}
				}
			}

			if (stack != null && stack.stackSize > 0) {
				dropItem(true);
			}
		}
	}

	private boolean isValidDirection(EnumFacing dir) {
		if (dir == null || !owner.connects(dir)) {
			return false;
		}

		PartPipe pipe = PipeUtils.getPipe(owner.getWorld(), owner.getPos().offset(dir), dir.getOpposite());

		if (pipe != null) {
			return pipe.canInjectItems(dir.getOpposite());
		}

		TileEntity tile = owner.getNeighbourTile(dir);

		if (tile != null) {
			if (tile instanceof IItemInjectable) {
				return ((IItemInjectable) tile).canInjectItems(dir.getOpposite());
			} else {
				IItemHandler handler = InventoryUtils.getItemHandler(tile, dir.getOpposite());
				if (handler != null && handler.getSlots() > 0) {
					return true;
				}
			}
		}

		return false;
	}

	private boolean canMoveDirection(EnumFacing dir, boolean isPickingDirection) {
		if (dir == null) {
			return activeShifterDistance == 0;
		}

		PartPipe pipe = PipeUtils.getPipe(owner.getWorld(), owner.getPos().offset(dir), dir.getOpposite());

		/* if (isPickingDirection) {
			// If we're picking the direction, only check for pipe *connection*,
			// so that clogging mechanics (pipes which can't take in items) work
			// as intended.
			if (tile instanceof TilePipe) {
				if (((TilePipe) tile).connects(dir.getOpposite())) {
					return true;
				}
			}
		} else { */
		if (passToPipe(pipe, dir, true)) {
			return true;
		}
		// }

		TileEntity tile = owner.getNeighbourTile(dir);

		if (passToInjectable(tile, dir, true)) {
			return true;
		}

		if (addToItemHandler(tile, dir, true)) {
			return true;
		}

		return false;
	}

	private boolean isShifterPushing(IShifter p, EnumFacing direction) {
		return p != null
				&& p.getDirection() == direction
				&& p.isShifting()
				&& p.matches(stack);
	}

	private void calculateOutputDirection() {
		if (owner.getWorld() == null || owner.getWorld().isRemote) {
			return;
		}

		List<EnumFacing> directionList = new ArrayList<EnumFacing>();
		List<EnumFacing> pressureList = new ArrayList<EnumFacing>();

		activeShifterDistance = 0;

		// Step 1: Make a list of all valid directions, as well as all shifters.
		for (EnumFacing direction : EnumFacing.VALUES) {
			if (isValidDirection(direction)) {
				directionList.add(direction);
			}

			IShifter p = owner.getNearestShifter(direction);

			if (p != null && isShifterPushing(p, direction)) {
				pressureList.add(direction);
			}
		}

		// Step 2: Sort the shifter list.
		Collections.sort(pressureList, new Comparator<EnumFacing>() {
			@Override
			public int compare(EnumFacing o1, EnumFacing o2) {
				return getInternalShifterStrength(owner.getNearestShifter(o1), o1) - getInternalShifterStrength(owner.getNearestShifter(o2), o2);
			}
		});


		EnumFacing firstOutput = null;

		// Step 3: Pick the next path.
		for (EnumFacing dir : pressureList) {
			if (canMoveDirection(dir, true)) {
				// Pressurizer prioritizing valid direction, move.
				this.output = dir;
				activeShifterDistance = getInternalShifterStrength(owner.getNearestShifter(dir), dir);
				return;
			} else if (pressureList.contains(dir.getOpposite())
					&& owner.getShifterStrength(dir) == owner.getShifterStrength(dir.getOpposite())) {
				// Pressurizers freezing an item in place.
				this.output = dir;
				activeShifterDistance = getInternalShifterStrength(owner.getNearestShifter(dir), dir);
				return;
			} else if (directionList.contains(dir) && firstOutput == null) {
				firstOutput = dir;
			}
		}

		directionList.removeAll(pressureList);
		directionList.remove(input);

		if (directionList.size() > 0) {
			EnumFacing dir;
			int i = 0;

			// Step 3b: Pick the first "unforced" direction to scan.
			// A direction opposite to input (aka. "straight line")
			// takes priority.
			if (directionList.contains(input.getOpposite())) {
				dir = input.getOpposite();
				directionList.remove(input.getOpposite());
			} else if (directionList.size() > 1) {
				Collections.shuffle(directionList);
				dir = directionList.get(0);
				i = 1;
			} else if (directionList.size() == 1) {
				this.output = directionList.get(0);
				return;
			} else {
				this.output = null;
				return;
			}

			if (firstOutput == null) {
				firstOutput = dir;
			}

			while (!canMoveDirection(dir, true) && i < directionList.size()) {
				dir = directionList.get(i);
				i++;
			}

			// Step 3c: If a valid, free direction has been found, use that.
			// Otherwise, set it to the first output direction selected to
			// prioritize that, for some reason.
			if (canMoveDirection(dir, true)) {
				this.output = dir;
			} else {
				this.output = firstOutput;
			}
		} else {
			this.output = firstOutput;
		}
	}

	private void onReachedCenter() {
		progress = CENTER_PROGRESS;
		this.reachedCenter = true;

		if (owner.getWorld().isRemote) {
			return;
		}

		calculateOutputDirection();
		updateStuckFlag();
		sendPacket(false);
	}

	protected void reset(PartPipe owner, EnumFacing input) {
		this.owner = owner;
		initializeFromEntrySide(input);

		// Do an early calculation to aid the server side.
		// Won't always be right, might be sometimes right.
		calculateOutputDirection();
	}

	private boolean passToInjectable(TileEntity tile, EnumFacing dir, boolean simulate) {
		if (tile instanceof IMultipartContainer) {
			IMultipart part = ((IMultipartContainer) tile).getPartInSlot(PartSlot.CENTER);
			if (part instanceof PartPipe) {
				return false;
			}
		}

		if (tile instanceof IItemInjectable) {
			int added = ((IItemInjectable) tile).injectItem(stack, dir.getOpposite(), simulate);
			if (added > 0) {
				if (!simulate) {
					stack.stackSize -= added;
				}
				return true;
			}
		}

		return false;
	}

	private boolean passToPipe(PartPipe pipe, EnumFacing dir, boolean simulate) {
		if (pipe != null) {
			if (pipe.injectItemInternal(this, dir.getOpposite(), simulate)) {
				return true;
			}
		}

		return false;
	}

	private boolean addToItemHandler(TileEntity tile, EnumFacing dir, boolean simulate) {
		if (tile != null) {
			int stackSize = stack.stackSize;
			IItemHandler handler = InventoryUtils.getItemHandler(tile, dir.getOpposite());
			if (handler != null) {
				if (owner.getWorld().isRemote) {
					return true;
				}

				for (int i = 0; i < handler.getSlots(); i++) {
					ItemStack remain = handler.insertItem(i, stack, simulate);
					int added = stack.stackSize;
					if (remain != null) {
						added -= remain.stackSize;
					}
					stackSize -= added;
					if (stackSize == 0) {
						if (!simulate) {
							stack = null;
						}
						return true;
					} else if (!simulate) {
						stack = stack.copy();
						stack.stackSize = stackSize;
					}
				}

				if (stackSize > 0 && !simulate) {
					stack = stack.copy();
					stack.stackSize = stackSize;
					return true;
				}
			}
		}

		return false;
	}

	protected void dropItem(boolean useOutputDirection) {
		EnumFacing dir = null;

		if (useOutputDirection) {
			// Decide output direction
			int directions = 0;
			for (EnumFacing d : EnumFacing.VALUES) {
				if (owner.connects(d)) {
					directions++;
					dir = d.getOpposite();
					if (directions >= 2) {
						break;
					}
				}
			}

			if (directions >= 2) {
				dir = null;
			}
		}

		ItemUtils.spawnItemEntity(owner.getWorld(), new Vec3d(
				(double) owner.getPos().getX() + 0.5 + (dir != null ? dir.getFrontOffsetX() : 0) * 0.75,
				(double) owner.getPos().getY() + 0.5 + (dir != null ? dir.getFrontOffsetY() : 0) * 0.75,
				(double) owner.getPos().getZ() + 0.5 + (dir != null ? dir.getFrontOffsetZ() : 0) * 0.75
		), stack, 0, 0, 0, 0);

		stack = null;
	}

	private void initializeFromEntrySide(EnumFacing side) {
		this.input = side;
		this.output = null;
		this.reachedCenter = false;
		this.stuck = false;
		this.progress = 0;

		if (owner.getWorld().isRemote) {

		}
	}

	public void readFromNBT(NBTTagCompound nbt) {
		stack = ItemStack.loadItemStackFromNBT(nbt);
		progress = nbt.getShort("p");
		input = DirectionUtils.get(nbt.getByte("in"));
		output = DirectionUtils.get(nbt.getByte("out"));
		reachedCenter = nbt.getBoolean("reachedCenter");
		if (nbt.hasKey("stuck")) {
			stuck = nbt.getBoolean("stuck");
		}
		if (nbt.hasKey("activePD")) {
			activeShifterDistance = nbt.getInteger("activePD");
		}
	}

	public void writeToNBT(NBTTagCompound nbt) {
		stack.writeToNBT(nbt);
		nbt.setShort("p", (short) progress);
		nbt.setByte("in", (byte) DirectionUtils.ordinal(input));
		nbt.setByte("out", (byte) DirectionUtils.ordinal(output));
		nbt.setBoolean("reachedCenter", reachedCenter);
		if (stuck) {
			nbt.setBoolean("stuck", stuck);
		}
		if (activeShifterDistance > 0) {
			nbt.setInteger("activePD", activeShifterDistance);
		}
	}

	public boolean hasReachedCenter() {
		return reachedCenter;
	}

	public void setStuckFlagClient(boolean stuck) {
		if (owner.getWorld().isRemote) {
			this.stuck = stuck;
		}
	}

	public PartPipe getOwner() {
		return owner;
	}

	public float getProgress() {
		return (float) progress / MAX_PROGRESS;
	}
}
