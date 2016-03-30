package pl.asie.charset.audio.integration.jei;

import mezz.jei.api.*;

import javax.annotation.Nonnull;

@JEIPlugin
public class JEIPluginCharsetAudio implements IModPlugin {
	@Override
	public void onJeiHelpersAvailable(IJeiHelpers jeiHelpers) {
	}

	@Override
	public void onItemRegistryAvailable(IItemRegistry itemRegistry) {

	}

	@Override
	public void register(IModRegistry registry) {
		registry.addRecipeHandlers(new JEITapeCraftingRecipe.Handler(), new JEITapeReelCraftingRecipe.Handler());
	}

	@Override
	public void onRecipeRegistryAvailable(IRecipeRegistry recipeRegistry) {
	}

	@Override
	public void onRuntimeAvailable(@Nonnull IJeiRuntime jeiRuntime) {

	}
}
