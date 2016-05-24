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

package pl.asie.charset.lib.multipart;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import net.minecraftforge.fmp.item.ItemMultipart;
import net.minecraftforge.fmp.multipart.IMultipart;
import net.minecraftforge.fmp.multipart.IMultipartContainer;
import net.minecraftforge.fmp.multipart.MultipartHelper;
import net.minecraftforge.fmp.multipart.OcclusionHelper;
import net.minecraftforge.fmp.multipart.PartSlot;
import pl.asie.charset.lib.ModCharsetLib;

public abstract class ItemPartSlab extends ItemMultipart {
	public ItemPartSlab() {
		super();
		setCreativeTab(ModCharsetLib.CREATIVE_TAB);
	}


	public abstract PartSlab createPartSlab(World world, BlockPos blockPos, EnumFacing facing, Vec3d vec3, ItemStack stack, EntityPlayer player);

	@Override
	public IMultipart createPart(World world, BlockPos pos, EnumFacing facing, Vec3d hit, ItemStack stack, EntityPlayer player) {
		PartSlab slab = createPartSlab(world, pos, facing, hit, stack, player);
		slab.isTop = facing == EnumFacing.UP || (hit.yCoord >= 0.5 && facing != EnumFacing.DOWN);

		IMultipartContainer container = MultipartHelper.getPartContainer(world, pos);
		if (container != null) {
			boolean occupiedDown = false;
			if (container.getPartInSlot(PartSlot.DOWN) != null || !OcclusionHelper.occlusionTest(OcclusionHelper.boxes(PartSlab.BOXES[0]), container.getParts())) {
				slab.isTop = true;
				occupiedDown = true;
			}
			if (slab.isTop && (container.getPartInSlot(PartSlot.UP) != null || !OcclusionHelper.occlusionTest(OcclusionHelper.boxes(PartSlab.BOXES[1]), container.getParts()))) {
				if (occupiedDown) {
					return null;
				} else {
					slab.isTop = false;
				}
			}
		}

		return slab;
	}
}
