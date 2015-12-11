package pl.asie.charset.pipes;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;

import net.minecraftforge.fluids.IFluidHandler;

import pl.asie.charset.api.pipes.IPipe;
import pl.asie.charset.api.pipes.IShifter;
import pl.asie.charset.lib.IConnectable;
import pl.asie.charset.lib.TileBase;

public class TilePipe extends TileBase implements IConnectable, IPipe, ITickable {
	protected int[] shifterDistance = new int[6];
	private final Set<PipeItem> itemSet = new HashSet<PipeItem>();

	private byte initClient = 6;
	private boolean neighborBlockChanged;

	public TilePipe() {
	}

	private boolean internalConnects(EnumFacing side) {
		TileEntity tile = getNeighbourTile(side);

		if (tile instanceof IInventory) {
			if (tile instanceof ISidedInventory) {
				int[] slots = ((ISidedInventory) tile).getSlotsForFace(side.getOpposite());
				if (slots == null || slots.length == 0) {
					return false;
				}
			}

			return true;
		}

		if (tile instanceof IFluidHandler || tile instanceof TilePipe) {
			return true;
		}

		if (tile instanceof IShifter && ((IShifter) tile).getMode() == IShifter.Mode.Extract && ((IShifter) tile).getDirection() == side.getOpposite()) {
			return true;
		}

		return false;
	}

	@Override
	public boolean connects(EnumFacing side) {
		if (internalConnects(side)) {
			TileEntity tile = getNeighbourTile(side);
			if (tile instanceof TilePipe && !((TilePipe) tile).internalConnects(side.getOpposite())) {
				return false;
			}

			return true;
		}

		return false;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		readItems(nbt);

		shifterDistance = nbt.getIntArray("shifterDist");
		if (shifterDistance == null || shifterDistance.length != 6) {
			shifterDistance = new int[6];
		}
	}

	private void readItems(NBTTagCompound nbt) {
		NBTTagList list = nbt.getTagList("items", 10);
		synchronized (itemSet) {
			itemSet.clear();

			for (int i = 0; i < list.tagCount(); i++) {
				NBTTagCompound compound = list.getCompoundTagAt(i);
				PipeItem pipeItem = new PipeItem(this, compound);
				if (pipeItem.isValid()) {
					itemSet.add(pipeItem);
				}
			}
		}
	}

	private void writeItems(NBTTagCompound nbt) {
		NBTTagList list = new NBTTagList();

		synchronized (itemSet) {
			for (PipeItem i : itemSet) {
				NBTTagCompound cpd = new NBTTagCompound();
				i.writeToNBT(cpd);
				list.appendTag(cpd);
			}
		}

		nbt.setTag("items", list);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		writeItems(nbt);

		nbt.setIntArray("shifterDist", shifterDistance);
	}

	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		if (worldObj.isRemote) {
			synchronized (itemSet) {
				itemSet.clear();
			}
		}
	}

	@Override
	public void initialize() {
		if (worldObj.isRemote) {
			ModCharsetPipes.packet.sendToServer(new PacketPipeSyncRequest(this));
		}

		updateShifters();
		scheduleRenderUpdate();
	}

	@Override
	public void update() {
		super.update();

		if (neighborBlockChanged) {
			updateShifters();
			scheduleRenderUpdate();
			neighborBlockChanged = false;
		}

		synchronized (itemSet) {
			Iterator<PipeItem> itemIterator = itemSet.iterator();
			while (itemIterator.hasNext()) {
				PipeItem p = itemIterator.next();
				if (!p.move()) {
					itemIterator.remove();
				}
			}
		}
	}

	protected int getShifterStrength(EnumFacing direction) {
		return direction == null ? 0 : shifterDistance[direction.ordinal()];
	}

	private void updateShifterSide(EnumFacing dir) {
		int i = dir.ordinal();
		int oldDistance = shifterDistance[i];

		if (shifterDistance[i] == 1 && getNearestShifterInternal(dir) != null) {
			return;
		}

		BlockPos p = pos;
		int dist = 0;
		TileEntity tile;

		while ((tile = worldObj.getTileEntity(p)) instanceof TilePipe) {
			p = p.offset(dir.getOpposite());
			dist++;

			if (!((TilePipe) tile).connects(dir.getOpposite())) {
				tile = worldObj.getTileEntity(p);
				break;
			}
		}

		if (tile instanceof IShifter && isMatchingShifter((IShifter) tile, dir, dist)) {
			shifterDistance[i] = dist;
		} else {
			shifterDistance[i] = 0;
		}

		if (oldDistance != shifterDistance[i]) {
			TileEntity notifyTile = getNeighbourTile(dir);
			if (notifyTile instanceof TilePipe) {
				((TilePipe) notifyTile).updateShifterSide(dir);
			}
		}
	}

	private void updateShifters() {
		if (!worldObj.isRemote) {
			for (EnumFacing dir : EnumFacing.VALUES) {
				updateShifterSide(dir);
			}
		}
	}

	private boolean isMatchingShifter(IShifter p, EnumFacing dir, int dist) {
		return p.getDirection() == dir && dist <= p.getShiftDistance();
	}

	private IShifter getNearestShifterInternal(EnumFacing dir) {
		TileEntity tile;

		switch (shifterDistance[dir.ordinal()]) {
			case 0:
				return null;
			case 1:
				tile = getNeighbourTile(dir.getOpposite());
				break;
			default:
				tile = worldObj.getTileEntity(pos.offset(dir.getOpposite(), shifterDistance[dir.ordinal()]));
		}

		if (tile instanceof IShifter && isMatchingShifter((IShifter) tile, dir, Integer.MAX_VALUE)) {
			return (IShifter) tile;
		} else {
			return null;
		}
	}

	protected IShifter getNearestShifter(EnumFacing dir) {
		if (dir == null) {
			return null;
		} else if (shifterDistance[dir.ordinal()] == 0) {
			return null;
		} else {
			IShifter p = getNearestShifterInternal(dir);
			if (p == null) {
				updateShifterSide(dir);
				return getNearestShifterInternal(dir);
			} else {
				return p;
			}
		}
	}

	protected void addItemClientSide(PipeItem item) {
		if (!worldObj.isRemote) {
			return;
		}

		synchronized (itemSet) {
			Iterator<PipeItem> itemIterator = itemSet.iterator();
			while (itemIterator.hasNext()) {
				PipeItem p = itemIterator.next();
				if (p.id == item.id) {
					itemIterator.remove();
					break;
				}
			}

			itemSet.add(item);
		}
	}

	protected void removeItemClientSide(PipeItem item) {
		if (worldObj.isRemote) {
			synchronized (itemSet) {
				itemSet.remove(item);
			}
		}
	}

	protected boolean injectItemInternal(PipeItem item, EnumFacing dir, boolean simulate) {
		if (item.isValid()) {
			int stuckItems = 0;

			synchronized (itemSet) {
				for (PipeItem p : itemSet) {
					if (p.isStuck()) {
						stuckItems++;

						if (stuckItems >= 1) {
							return false;
						}
					}
				}

				if (!simulate) {
					itemSet.add(item);
				}
			}

			if (!simulate) {
				item.reset(this, dir);
				item.sendPacket(true);
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean canInjectItems(EnumFacing side) {
		return connects(side);
	}

	@Override
	public int injectItem(ItemStack stack, EnumFacing direction, boolean simulate) {
		if (worldObj.isRemote || !connects(direction)) {
			return 0;
		}

		PipeItem item = new PipeItem(this, stack, direction);

		if (injectItemInternal(item, direction, simulate)) {
			return stack.stackSize;
		} else {
			return 0;
		}
	}

	protected void scheduleRenderUpdate() {
		worldObj.markBlockRangeForRenderUpdate(getPos(), getPos());
	}

	public void onNeighborBlockChange() {
		neighborBlockChanged = true;
	}

	public Collection<PipeItem> getPipeItems() {
		return itemSet;
	}

	protected void onSyncRequest() {
		// TODO: HACK! HACK! HACK! HACK! HACK! HACK! HACK! HACK!
		synchronized (itemSet) {
			for (PipeItem p : itemSet) {
				p.sendPacket(true);
			}
		}
	}

	@Override
	public ItemStack getTravellingStack(EnumFacing side) {
		float targetError = 1000f;
		PipeItem targetItem = null;

		synchronized (itemSet) {
			for (PipeItem p : itemSet) {
				float error;

				if (side == null) {
					error = Math.abs(p.getProgress() - 0.5f);

					if (error > 0.25f) {
						continue;
					}
				} else {
					if (p.getProgress() <= 0.25f && side == p.getDirection().getOpposite()) {
						error = Math.abs(p.getProgress() - 0.125f);
					} else if (p.getProgress() >= 0.75f && side == p.getDirection()) {
						error = Math.abs(p.getProgress() - 0.875f);
					} else {
						continue;
					}

					if (error > 0.125f) {
						continue;
					}
				}

				if (error < targetError) {
					targetError = error;
					targetItem = p;
				}
			}
		}

		return targetItem != null ? targetItem.getStack() : null;
	}
}
