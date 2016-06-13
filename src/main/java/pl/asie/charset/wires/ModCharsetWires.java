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

package pl.asie.charset.wires;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
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
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

import mcmultipart.multipart.MultipartRegistry;
import pl.asie.charset.api.wires.WireType;
import pl.asie.charset.lib.ModCharsetLib;
import pl.asie.charset.lib.network.PacketRegistry;
import pl.asie.charset.lib.recipe.RecipeCharset;
import pl.asie.charset.lib.wires.PartWire;
import pl.asie.charset.lib.wires.RecipeObjectWire;
import pl.asie.charset.lib.wires.RecipeResultWire;
import pl.asie.charset.lib.wires.WireFactory;
import pl.asie.charset.lib.wires.WireManager;
import pl.asie.charset.wires.logic.PartWireBundled;
import pl.asie.charset.wires.logic.PartWireInsulated;
import pl.asie.charset.wires.logic.PartWireNormal;
import pl.asie.charset.wires.logic.PartWireProvider;
import pl.asie.charset.wires.logic.WireSignalFactory;

@Mod(modid = ModCharsetWires.MODID, name = ModCharsetWires.NAME, version = ModCharsetWires.VERSION,
		dependencies = ModCharsetLib.DEP_MCMP, updateJSON = ModCharsetLib.UPDATE_URL)
public class ModCharsetWires {
	public static final String MODID = "CharsetWires";
	public static final String NAME = "+";
	public static final String VERSION = "@VERSION@";

	public static PacketRegistry packet;

	@SidedProxy(clientSide = "pl.asie.charset.wires.ProxyClient", serverSide = "pl.asie.charset.wires.ProxyCommon")
	public static ProxyCommon proxy;

	public static ItemWireOld wire;

	public static WireFactory[] wireFactories = new WireFactory[18];

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		wireFactories[0] = new WireSignalFactory(WireType.NORMAL, -1).setRegistryName(new ResourceLocation("charsetwires:rsWireN"));
		for (int i = 0; i < 16; i++) {
			wireFactories[i + 1] = new WireSignalFactory(WireType.INSULATED, i).setRegistryName(new ResourceLocation("charsetwires:rsWireI." + i));
		}
		wireFactories[17] = new WireSignalFactory(WireType.BUNDLED, -1).setRegistryName(new ResourceLocation("charsetwires:rsWireB"));

		for (int i = 0; i < 18; i++) {
			WireManager.register(wireFactories[i]);
		}

		wire = new ItemWireOld();
		GameRegistry.register(wire.setRegistryName("wire"));

		MinecraftForge.EVENT_BUS.register(proxy);
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		packet = new PacketRegistry(ModCharsetWires.MODID);

		// Temporary recipes
		GameRegistry.addRecipe(RecipeCharset.Builder.create(new RecipeResultWire(wireFactories[0], false, 16))
				.shaped("r r", "rir", "r r", 'r', "dustRedstone", 'i', "ingotIron")
				.build());

		for (int i = 0; i < 16; i++) {
			GameRegistry.addRecipe(RecipeCharset.Builder.create(new RecipeResultWire(wireFactories[i + 1], false, 8))
					.shaped("ddd", "dwd", "ddd", 'd', new RecipeObjectWire(wireFactories[0], false), 'w', new ItemStack(Blocks.WOOL, 1, i))
					.build());
			GameRegistry.addRecipe(RecipeCharset.Builder.create(new RecipeResultWire(wireFactories[i + 1], true, 8))
					.shaped("ddd", "dwd", "ddd", 'd', new RecipeObjectWire(wireFactories[0], true), 'w', new ItemStack(Blocks.WOOL, 1, i))
					.build());
		}

		GameRegistry.addRecipe(RecipeCharset.Builder.create(new RecipeResultWire(wireFactories[17], false, 1))
				.shaped("sws", "www", "sws", 'w', new RecipeObjectSignalWire(WireType.INSULATED, false), 's', Items.STRING)
				.build());
		GameRegistry.addRecipe(RecipeCharset.Builder.create(new RecipeResultWire(wireFactories[17], true, 1))
				.shaped("sws", "www", "sws", 'w', new RecipeObjectSignalWire(WireType.INSULATED, true), 's', Items.STRING)
				.build());

		for (int i = 0; i < 18; i++) {
			GameRegistry.addRecipe(RecipeCharset.Builder.create(new RecipeResultWire(wireFactories[i], false, 1))
					.shapeless(new RecipeObjectWire(wireFactories[i], true)).build());
			GameRegistry.addRecipe(RecipeCharset.Builder.create(new RecipeResultWire(wireFactories[i], true, 1))
					.shapeless(new RecipeObjectWire(wireFactories[i], false)).build());
		}
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
	}
}
