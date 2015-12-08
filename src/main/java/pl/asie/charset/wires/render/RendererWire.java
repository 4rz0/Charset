package pl.asie.charset.wires.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

import net.minecraftforge.common.property.IExtendedBlockState;

import pl.asie.charset.wires.ItemWire;
import pl.asie.charset.wires.TileWireContainer;
import pl.asie.charset.wires.WireType;
import pl.asie.charset.wires.internal.WireLocation;

public class RendererWire extends RendererWireBase {
	private final List<RendererWireBase> renderers = new ArrayList<RendererWireBase>();

	public RendererWire() {
		renderers.add(new RendererWireNormal("wire", 2, 2));
		renderers.add(new RendererWireInsulated("insulated_wire", 4, 3));
		renderers.add(new RendererWireBundled("bundled_wire", 6, 4));
	}

	@Override
	public IBakedModel handleBlockState(IBlockState state) {
		super.handleBlockState(state);
		return this;
	}

	@Override
	public IBakedModel handleItemState(ItemStack stack) {
		super.handleItemState(stack);
		return this;
	}

	private int getRendererId(WireLocation side) {
		if (state != null) {
			TileWireContainer wire = null;
			if (state instanceof IExtendedBlockState) {
				wire = ((IExtendedBlockState) state).getValue(TileWireContainer.PROPERTY);
			}

			if (wire != null) {
				return wire.getWireType(side).type().ordinal();
			}
		} else if (stack != null) {
			return WireType.VALUES[stack.getItemDamage() >> 1].type().ordinal();
		}

		return 0;
	}

	@Override
	public List<BakedQuad> getGeneralQuads() {
		List<BakedQuad> quads = new ArrayList<BakedQuad>();
		TileWireContainer wire = null;
		if (state instanceof IExtendedBlockState) {
			wire = ((IExtendedBlockState) state).getValue(TileWireContainer.PROPERTY);
		}

		if (wire != null) {
			for (WireLocation side : WireLocation.VALUES) {
				if (wire.hasWire(side)) {
					renderers.get(getRendererId(side)).handleBlockState(state);
					renderers.get(getRendererId(side)).addWire(wire, side, wire.getRedstoneLevel(side) > 0, quads);
				}
			}
		} else if (stack != null) {
			if (ItemWire.isFreestanding(stack)) {
				renderers.get(getRendererId(WireLocation.FREESTANDING)).handleItemState(stack);
				renderers.get(getRendererId(WireLocation.FREESTANDING)).addWireFreestanding(null, false, quads);
			} else {
				renderers.get(getRendererId(WireLocation.DOWN)).handleItemState(stack);
				renderers.get(getRendererId(WireLocation.DOWN)).addWire(null, WireLocation.DOWN, false, quads);
			}
		}

		return quads;
	}

	@Override
	public List<BakedQuad> getFaceQuads(EnumFacing face) {
		return Collections.emptyList();
	}

	@Override
	public TextureAtlasSprite getParticleTexture() {
		return renderers.get(getRendererId(WireLocation.FREESTANDING)).getParticleTexture();
	}

	@Override
	public void loadTextures(TextureMap map) {
		for (RendererWireBase renderer : renderers) {
			renderer.loadTextures(map);
		}
	}
}
