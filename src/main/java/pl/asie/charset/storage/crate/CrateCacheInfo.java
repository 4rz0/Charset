package pl.asie.charset.storage.crate;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import pl.asie.charset.lib.render.IRenderComparable;
import pl.asie.charset.lib.utils.RenderUtils;

public class CrateCacheInfo implements IRenderComparable<CrateCacheInfo> {
    public final ItemStack plank;
    public final int connectionMap;
    private final int hash;

    public CrateCacheInfo(ItemStack plank, int connectionMap) {
        this.plank = plank;
        this.connectionMap = connectionMap;

        int hash = 0;
        if (plank != null) {
            hash = (hash * 3) + (Item.getIdFromItem(plank.getItem()) * 7) + plank.getMetadata();
        }
        hash = (hash * 3) + connectionMap;
        this.hash = hash;
    }

    public boolean isConnected(EnumFacing side) {
        return (connectionMap & (1 << side.ordinal())) != 0;
    }

    public boolean isCorner(int x, int y, int z) {
        return (connectionMap & (1 << (6 + (x * 4) + (y * 2) + z))) != 0;
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof CrateCacheInfo) ? renderEquals((CrateCacheInfo) other) : false;
    }

    @Override
    public boolean renderEquals(CrateCacheInfo other) {
        return other.connectionMap == connectionMap
                && RenderUtils.getSprite(plank) == RenderUtils.getSprite(other.plank);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public int renderHashCode() {
        return hash;
    }
}
