package pl.asie.charset.pipes;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.*;
import pl.asie.charset.api.pipes.IShifter;

public class PipeFluidContainer implements IFluidHandler, ITickable {
    public class Tank {
        public final EnumFacing location;
        public int amount;
        private boolean dirty;

        public Tank(EnumFacing location) {
            this.location = location;
        }

        public FluidStack getType() {
            return fluidStack;
        }

        public int get() {
            return amount;
        }

        public boolean isDirty() {
            return dirty;
        }

        public boolean removeDirty() {
            if (dirty) {
                dirty = false;
                return true;
            } else {
                return false;
            }
        }

        public int add(int amount, boolean simulate) {
            int targetAmount = Math.min(amount, TANK_SIZE - this.amount);
            if (!simulate && targetAmount > 0) {
                this.amount += targetAmount;
                dirty = true;
            }
            return targetAmount;
        }

        public int remove(int amount, boolean simulate) {
            int targetAmount = Math.min(this.amount, amount);
            if (!simulate && targetAmount > 0) {
                this.amount -= targetAmount;
                dirty = true;
                onRemoval();
            }
            return targetAmount;
        }

        public FluidTankInfo getInfo() {
            FluidStack stack = getType().copy();
            stack.amount = amount;
            return new FluidTankInfo(stack, getCapacity());
        }

        public int getCapacity() {
            return TANK_SIZE;
        }
    }

    public static final int TANK_RATE = 80;
    final Tank[] tanks = new Tank[7];
    FluidStack fluidStack;
    int fluidColor;
    boolean fluidDirty;

    private static final int TANK_SIZE = 250;
    private final PartPipe owner;

    public PipeFluidContainer(PartPipe owner) {
        this.owner = owner;
        for (int i = 0; i < 7; i++) {
            tanks[i] = new Tank(i < 6 ? EnumFacing.getFront(i) : null);
        }
    }

    public void onRemoval() {
        int total = 0;
        for (Tank t : tanks) {
            total += t.amount;
        }
        if (total == 0) {
            fluidStack = null;
            fluidDirty = true;
        }
    }

    public void writeToNBT(NBTTagCompound nbt) {
        if (fluidStack != null) {
            fluidStack.writeToNBT(nbt);
            int[] amt = new int[7];
            for (int i = 0; i <= 6; i++) {
                amt[i] = tanks[i].amount;
            }
            nbt.setIntArray("TankAmts", amt);
        }
    }

    public void readFromNBT(NBTTagCompound nbt) {
        fluidStack = FluidStack.loadFluidStackFromNBT(nbt);
        fluidDirty = true;
        for (int i = 0; i <= 6; i++) {
            tanks[i].amount = 0;
            tanks[i].dirty = true;
        }

        if (fluidStack != null) {
            int[] amt = nbt.getIntArray("TankAmts");
            if (amt != null && amt.length == 7) {
                for (int i = 0; i <= 6; i++) {
                    tanks[i].amount = Math.min(TANK_SIZE, amt[i]);
                }
            }
        }
    }

    @Override
    public void update() {
        if (owner.getWorld() == null || owner.getWorld().isRemote) {
            return;
        }

        if (fluidStack == null) {
            return;
        }

        EnumFacing pushDir = null;
        int shifterDist = Integer.MAX_VALUE;

        for (EnumFacing facing : EnumFacing.VALUES) {
            if (owner.connects(facing)) {
                int sStr = owner.getShifterStrength(facing);
                if (sStr > 0 && sStr < shifterDist) {
                    IShifter s = owner.getNearestShifter(facing);
                    if (s != null && s.isShifting() && s.matches(fluidStack)) {
                        pushDir = facing;
                        shifterDist = sStr;
                    }
                }
            } else {
                tanks[facing.ordinal()].amount = 0;
            }
        }

        if (pushDir != null) {
            pushAll(pushDir);
        } else if (owner.connects(EnumFacing.DOWN)) {
            pushAll(EnumFacing.DOWN);
        } else {
            /* FluidStack baseStack = tanks[6].get();
            if (baseStack == null) {
                List<EnumFacing> dirs = new ArrayList<EnumFacing>(6);
                for (EnumFacing facing : EnumFacing.VALUES) {
                    dirs.add(facing);
                }
                Collections.shuffle(dirs);
                for (EnumFacing facing : dirs) {
                    if (tanks[facing.ordinal()].get() != null) {
                        baseStack = tanks[facing.ordinal()].get();
                        break;
                    }
                }
            }

            if (baseStack != null) {
                float amount = baseStack.amount;
                Set<Tank> tankSet = new HashSet<Tank>();

                for (int i = 0; i <= 6; i++) {
                    if (i == 6 || owner.connects(EnumFacing.getFront(i))) {
                        Tank tank = tanks[i];
                        if (tank.get() != null && tank.get().isFluidEqual(baseStack)) {
                            tankSet.add(tank);
                            amount += tank.get().amount;
                        } else if (tank.get() == null) {
                            tankSet.add(tank);
                        }
                    }
                }

                int tankCount = tankSet.size();

                if (amount > 0) {
                    for (Tank tank : tankSet) {
                        int amt = Math.round(amount / tankCount);
                        if (amt > 0) {
                            if (tank.stack != null && tank.stack.amount != amt) {
                                tank.stack.amount = amt;
                                tank.dirty = true;
                            } else if (tank.stack == null) {
                                tank.stack = baseStack.copy();
                                tank.stack.amount = amt;
                                tank.dirty = true;
                            }
                        } else {
                            if (tank.stack != null) {
                                tank.stack = null;
                                tank.dirty = true;
                            }
                        }
                        amount -= amt;
                        tankCount--;
                    }
                }

                if (amount > 0) {
                    ModCharsetLib.logger.warn(String.format("[PipeFluidContainer->equalize] Accidentally voided %.3f mB at %s!", amount, owner.getPos().toString()));
                }
            } */
        }

        checkPacketUpdate();
    }

    void sendPacket(boolean ignoreDirty) {
        if (owner.getWorld() != null && !owner.getWorld().isRemote) {
            ModCharsetPipes.packet.sendToAllAround(new PacketFluidUpdate(owner, this, ignoreDirty), owner, ModCharsetPipes.PIPE_TESR_DISTANCE);
        }
    }

    private void checkPacketUpdate() {
        if (fluidDirty) {
            if (owner.getWorld() != null && !owner.getWorld().isRemote) {
                sendPacket(false);
            }
            return;
        }

        for (int i = 0; i <= 6; i++) {
            if (tanks[i].isDirty()) {
                if (owner.getWorld() != null && !owner.getWorld().isRemote) {
                    sendPacket(false);
                }
                return;
            }
        }
    }

    private void pushAll(EnumFacing pushDir) {
        push(tanks[pushDir.ordinal()], getTankBlockNeighbor(owner.getPos(), pushDir), pushDir.getOpposite(), TANK_RATE);
        push(tanks[6], tanks[pushDir.ordinal()], TANK_RATE);
        for (EnumFacing facing : EnumFacing.VALUES) {
            if (facing != pushDir && owner.connects(facing)) {
                push(tanks[facing.ordinal()], tanks[6], TANK_RATE);
            }
        }
    }

    private void push(Tank from, Tank to, int maxAmount) {
        if (from.amount == 0 || (to.getType() != null && !to.getType().isFluidEqual(from.getType()))) {
            return;
        }

        int amount = Math.min(from.amount, maxAmount);
        if (amount > 0) {
            int amt = to.add(amount, false);
            from.remove(amt, false);
        }
    }

    private void push(Tank from, IFluidHandler to, EnumFacing toSide, int maxAmount) {
        if (from.amount == 0 || !to.canFill(toSide, from.getType().getFluid())) {
            return;
        }

        FluidStack out = fluidStack.copy();
        out.amount = Math.min(from.amount, maxAmount);
        if (out.amount > 0) {
            int amt = to.fill(toSide, out, true);
            from.remove(amt, false);
        }
    }

    public IFluidHandler getTankBlockNeighbor(BlockPos pos, EnumFacing direction) {
        BlockPos p = pos.offset(direction);
        PartPipe pipe = PipeUtils.getPipe(owner.getWorld(), p, direction.getOpposite());
        if (pipe != null) {
            return pipe.fluid;
        } else {
            TileEntity tile = owner.getWorld().getTileEntity(p);
            if (tile instanceof IFluidHandler) {
                return ((IFluidHandler) tile);
            }
        }

        return null;
    }

    @Override
    public int fill(EnumFacing from, FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount == 0 || !canFill(from, resource.getFluid())) {
            return 0;
        }

        if (fluidStack == null) {
            if (doFill) {
                fluidStack = resource.copy();
                fluidDirty = true;
            }
        } else if (!fluidStack.isFluidEqual(resource)) {
            return 0;
        }

        return tanks[from.ordinal()].add(resource.amount, !doFill);
    }

    @Override
    public boolean canFill(EnumFacing from, Fluid fluid) {
        return owner.connects(from) && (fluidStack == null || fluidStack.getFluid() == fluid);
    }

    @Override
    public FluidTankInfo[] getTankInfo(EnumFacing from) {
        return new FluidTankInfo[]{
                tanks[from == null ? 6 : from.ordinal()].getInfo()
        };
    }

    // Ha! Cannot drain me, for I drain myself just fine!

    @Override
    public FluidStack drain(EnumFacing from, FluidStack resource, boolean doDrain) {
        return null;
    }

    @Override
    public FluidStack drain(EnumFacing from, int maxDrain, boolean doDrain) {
        return null;
    }

    @Override
    public boolean canDrain(EnumFacing from, Fluid fluid) {
        return false;
    }
}
