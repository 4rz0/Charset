package pl.asie.charset.storage.crate;

import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import pl.asie.charset.lib.TileBase;
import pl.asie.charset.lib.utils.ItemUtils;
import pl.asie.charset.storage.ModCharsetStorage;

import java.util.EnumSet;
import java.util.Set;

public class TileEntityCrate extends TileBase {
    private ItemStack plank;

    @Override
    public void onPlacedBy(EntityLivingBase placer, ItemStack stack) {
        loadFromStack(stack);
    }

    @Override
    public void readNBTData(NBTTagCompound compound, boolean isClient) {
        plank = ItemStack.loadItemStackFromNBT(compound.getCompoundTag("plank"));

        if (plank == null) {
            plank = new ItemStack(Blocks.PLANKS);
        }
    }

    @Override
    public NBTTagCompound writeNBTData(NBTTagCompound compound, boolean isClient) {
        ItemUtils.writeToNBT(plank, compound, "plank");
        return compound;
    }

    public static ItemStack create(ItemStack plank) {
        TileEntityCrate crate = new TileEntityCrate();
        crate.plank = plank;
        return crate.getDroppedBlock();
    }

    public ItemStack getDroppedBlock() {
        ItemStack stack = new ItemStack(ModCharsetStorage.crateBlock);
        writeNBTData(ItemUtils.getTagCompound(stack, true), true);
        return stack;
    }

    public void loadFromStack(ItemStack stack) {
        NBTTagCompound compound = stack.getTagCompound();
        readNBTData(compound != null ? stack.getTagCompound() : new NBTTagCompound(), true);
    }

    private boolean hasCrate(BlockPos pos) {
        TileEntity oTile = getWorld().getTileEntity(pos);
        if (oTile instanceof TileEntityCrate
                && ItemStack.areItemStacksEqual(((TileEntityCrate) oTile).getMaterial(), getMaterial())) {
            return true;
        }

        return false;
    }

    public CrateCacheInfo getCacheInfo() {
        int connectionMatrix = 0;
        Set<EnumFacing> facingSet = EnumSet.noneOf(EnumFacing.class);

        if (getWorld() != null) {
            for (EnumFacing facing : EnumFacing.VALUES) {
                TileEntity oTile = getNeighbourTile(facing);
                if (oTile instanceof TileEntityCrate
                        && ItemStack.areItemStacksEqual(((TileEntityCrate) oTile).getMaterial(), getMaterial())) {
                    connectionMatrix |= (1 << facing.ordinal());
                    facingSet.add(facing);
                }
            }
        }
        for (int z = 0; z < 2; z++) {
            for (int y = 0; y < 2; y++) {
                for (int x = 0; x < 2; x++) {
                    EnumFacing fx = x == 0 ? EnumFacing.WEST : EnumFacing.EAST;
                    EnumFacing fy = y == 0 ? EnumFacing.DOWN : EnumFacing.UP;
                    EnumFacing fz = z == 0 ? EnumFacing.NORTH : EnumFacing.SOUTH;
                    boolean match = false;

                    if (facingSet.contains(fx) && facingSet.contains(fz)) {
                        if (hasCrate(getPos().offset(fx).offset(fz))) {
                            continue;
                        }
                        match = true;
                    }

                    if (facingSet.contains(fy) && facingSet.contains(fz)) {
                        if (hasCrate(getPos().offset(fy).offset(fz))) {
                            continue;
                        }
                        match = true;
                    }

                    if (facingSet.contains(fx) && facingSet.contains(fy)) {
                        if (hasCrate(getPos().offset(fx).offset(fy))) {
                            continue;
                        }
                        match = true;
                    }

                    if (match) {
                        connectionMatrix |= (1 << (6 + (x * 4) + (y * 2) + z));
                    }
                }
            }
        }
        return new CrateCacheInfo(plank, connectionMatrix);
    }

    public ItemStack getMaterial() {
        return plank;
    }
}
