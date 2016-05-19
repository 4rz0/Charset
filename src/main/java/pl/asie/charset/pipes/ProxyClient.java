package pl.asie.charset.pipes;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;

import mcmultipart.client.multipart.MultipartRegistryClient;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pl.asie.charset.lib.render.ModelFactory;
import pl.asie.charset.lib.render.ModelPipeLike;
import pl.asie.charset.lib.render.SpritesheetFactory;
import pl.asie.charset.lib.utils.RenderUtils;

public class ProxyClient extends ProxyCommon {
	private SpecialRendererPipe rendererPipe;
	private ModelPipeLike rendererPipeStatic;

	@Override
	public void registerRenderers() {
		MultipartRegistryClient.bindMultipartSpecialRenderer(PartPipe.class, rendererPipe = new SpecialRendererPipe());
		ClientRegistry.bindTileEntitySpecialRenderer(TileShifter.class, new SpecialRendererShifter());
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onPostBake(ModelBakeEvent event) {
		rendererPipeStatic = new ModelPipe();

		event.getModelRegistry().putObject(new ModelResourceLocation("charsetpipes:pipe", "multipart"), rendererPipeStatic);
		event.getModelRegistry().putObject(new ModelResourceLocation("charsetpipes:pipe", "inventory"), rendererPipeStatic);
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onTextureStitch(TextureStitchEvent.Pre event) {
		ModelPipe.sprites = SpritesheetFactory.register(event.getMap(), ModelPipe.PIPE_TEXTURE_LOC, 4, 4);
		rendererPipe.clearCache();
	}

	@Override
	public boolean stopsRenderFast(World world, ItemStack stack) {
		return RenderUtils.isDynamicItemRenderer(world, stack);
	}
}
