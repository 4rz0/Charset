package pl.asie.charset.tweaks.shard;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;

import pl.asie.charset.lib.ModCharsetLib;
import pl.asie.charset.tweaks.Tweak;

public class TweakGlassShards extends Tweak {
    public static ItemShard shardItem;

    public TweakGlassShards() {
        super("additions", "glassShards", "Adds glass shards which drop from glass in a manner similar to glowstone dust.",
                !Loader.isModLoaded("glass_shards") /* to be nice to ljfa */);
    }

    @Override
    public boolean canTogglePostLoad() {
        return false;
    }

    @Override
    public boolean preInit() {
        shardItem = new ItemShard();
        GameRegistry.registerItem(shardItem, "shard");

        ModCharsetLib.proxy.registerItemModel(shardItem, 0, "charsettweaks:shard");
        for (int i = 1; i <= ItemShard.MAX_SHARD; i++) {
            ModCharsetLib.proxy.registerItemModel(shardItem, i, "charsettweaks:shard#inventory_colored");
        }
        return true;
    }

    @Override
    public boolean init() {
        MinecraftForge.EVENT_BUS.register(this);

        GameRegistry.addShapedRecipe(new ItemStack(Blocks.glass), "gg", "gg", 'g', new ItemStack(shardItem, 1, 0));
        for (int i = 0; i < 16; i++) {
            GameRegistry.addShapedRecipe(new ItemStack(Blocks.stained_glass, 1, i), "gg", "gg", 'g', new ItemStack(shardItem, 1, i + 1));
        }

        OreDictionary.registerOre("shardGlassColorless", new ItemStack(shardItem, 1, 0));
        OreDictionary.registerOre("shardGlass", new ItemStack(shardItem, 1, OreDictionary.WILDCARD_VALUE));
        return true;
    }

    @SubscribeEvent
    public void onBlockHarvest(BlockEvent.HarvestDropsEvent event) {
        Block block = event.state.getBlock();
        boolean isPane = false;
        int md = 0;

        if (block == Blocks.glass) {
        } else if (block == Blocks.stained_glass) {
            md = 1 + block.getMetaFromState(event.state);
        } else if (block == Blocks.glass_pane) {
            isPane = true;
        } else if (block == Blocks.stained_glass_pane) {
            isPane = true;
            md = 1 + block.getMetaFromState(event.state);
        } else {
            return;
        }

        if (event.dropChance == 0) {
            event.dropChance = 1.0f;
        }

        if (isPane) {
            float rand = event.world.rand.nextFloat();
            if (rand >= 0.5f) {
                event.drops.add(new ItemStack(shardItem, 1, md));
            }
        } else {
            int rand = event.world.rand.nextInt(4) + 1;
            event.drops.add(new ItemStack(shardItem, rand, md));
        }
    }
}
