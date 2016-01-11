package pl.asie.charset.storage;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import pl.asie.charset.lib.ModCharsetLib;
import pl.asie.charset.lib.refs.Properties;

/**
 * Created by asie on 1/10/16.
 */
public class BlockBackpack extends BlockContainer {
    public static final int DEFAULT_COLOR = 0x805038;

    public BlockBackpack() {
        super(Material.cloth);
        setCreativeTab(ModCharsetLib.CREATIVE_TAB);
        setUnlocalizedName("charset.backpack");
        setBlockBounds(0.1875f, 0.0f, 0.1875f, 0.8125f, 0.75f, 0.8125f);
    }

    @Override
    public float getPlayerRelativeBlockHardness(EntityPlayer player, World world, BlockPos pos) {
        if (player.isSneaking()) {
            return -1.0f;
        } else {
            return super.getPlayerRelativeBlockHardness(player, world, pos);
        }
    }

    @Override
    public void onBlockClicked(World world, BlockPos pos, EntityPlayer player) {
        if (player.isSneaking() && player.getCurrentArmor(2) == null) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileBackpack) {
                ItemStack stack = ((TileBackpack) tile).writeToItemStack();

                world.removeTileEntity(pos);
                world.setBlockToAir(pos);

                player.setCurrentItemOrArmor(3, stack);
            }
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }

        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileBackpack) {
            player.openGui(ModCharsetStorage.instance, 1, world, pos.getX(), pos.getY(), pos.getZ());
            return true;
        }

        return false;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity tile = worldIn.getTileEntity(pos);

        if (tile instanceof IInventory) {
            InventoryHelper.dropInventoryItems(worldIn, pos, (IInventory) tile);
        }

        super.breakBlock(worldIn, pos, state);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public EnumWorldBlockLayer getBlockLayer() {
        return EnumWorldBlockLayer.CUTOUT_MIPPED;
    }

    @Override
    public int getRenderType() {
        return 3;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean isFullCube() {
        return false;
    }

    @Override
    public IBlockState onBlockPlaced(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        return this.getDefaultState().withProperty(Properties.FACING4, placer.getHorizontalFacing());
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, Properties.FACING4);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(Properties.FACING4).ordinal() - 2;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(Properties.FACING4, EnumFacing.getFront((meta & 3) + 2));
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileBackpack();
    }

    @SideOnly(Side.CLIENT)
    public int getBlockColor() {
        return DEFAULT_COLOR;
    }

    @SideOnly(Side.CLIENT)
    public int getRenderColor(IBlockState state) {
        return DEFAULT_COLOR;
    }

    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess worldIn, BlockPos pos, int renderPass) {
        TileEntity tile = worldIn.getTileEntity(pos);
        if (tile instanceof TileBackpack) {
            return ((TileBackpack) tile).getColor();
        } else {
            return DEFAULT_COLOR;
        }
    }
}
