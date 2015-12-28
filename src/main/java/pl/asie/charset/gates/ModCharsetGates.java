package pl.asie.charset.gates;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import mcmultipart.multipart.MultipartRegistry;
import pl.asie.charset.lib.ModCharsetLib;
import pl.asie.charset.lib.network.PacketRegistry;

@Mod(modid = ModCharsetGates.MODID, name = ModCharsetGates.NAME, version = ModCharsetGates.VERSION,
	dependencies = "required-after:CharsetLib@" + ModCharsetGates.VERSION, updateJSON = ModCharsetLib.UPDATE_URL)
public class ModCharsetGates {
	public static final String MODID = "CharsetGates";
	public static final String NAME = "&";
	public static final String VERSION = "@VERSION@";

    @SidedProxy(clientSide = "pl.asie.charset.gates.ProxyClient", serverSide = "pl.asie.charset.gates.ProxyCommon")
    public static ProxyCommon proxy;

	public static PacketRegistry packet;
    public static ItemGate itemGate;

    static final String[] gateMeta = new String[64]; // TODO: why 64 lol
    static final String[] gateUN = new String[64];
    static final Map<String, Integer> metaGate = new HashMap<String, Integer>();
    static final Set<ItemStack> gateStacks = new HashSet<ItemStack>();

    static final Map<String, Class<? extends PartGate>> gateParts = new HashMap<String, Class<? extends PartGate>>();
    static final Map<String, ResourceLocation> gateDefintions = new HashMap<String, ResourceLocation>();
    public static final Map<String, ResourceLocation> gateTextures = new HashMap<String, ResourceLocation>();

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        itemGate = new ItemGate();
        GameRegistry.registerItem(itemGate, "gate");

        registerGate("nand", PartGateNAND.class, 0);
        registerGate("nor", PartGateNOR.class, 1);
        registerGate("xor", PartGateXOR.class, 2);

        MinecraftForge.EVENT_BUS.register(proxy);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        packet = new PacketRegistry(ModCharsetGates.MODID);
        registerGateStack(ItemGate.getStack(new PartGateNOR().setInvertedSides(0b0001)));
        registerGateStack(ItemGate.getStack(new PartGateNAND().setInvertedSides(0b0001)));
        registerGateStack(ItemGate.getStack(new PartGateXOR()));
        registerGateStack(ItemGate.getStack(new PartGateNOR()));
        registerGateStack(ItemGate.getStack(new PartGateNAND()));
        registerGateStack(ItemGate.getStack(new PartGateXOR().setInvertedSides(0b0001)));
    }

    private void registerGateStack(ItemStack stack) {
        gateStacks.add(stack);
    }

    private void registerGate(String name, Class<? extends PartGate> clazz, int meta) {
        registerGate("charsetgates:gate_" + name, clazz, meta, "charsetgates:gatedefs/" + name, "charsetgates:blocks/gate_" + name,
                "part.charset.gate." + name);
    }

    public void registerGate(String name, Class<? extends PartGate> clazz, int meta, String gdLoc, String topLoc, String unl) {
        gateParts.put(name, clazz);
        gateDefintions.put(name, new ResourceLocation(gdLoc + ".json"));
        gateTextures.put(name, new ResourceLocation(topLoc));
        gateMeta[meta] = name;
        gateUN[meta] = unl;
        metaGate.put(name, meta);
        MultipartRegistry.registerPart(clazz, name);
        ModCharsetLib.proxy.registerItemModel(itemGate, meta, name);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
    }
}
