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

package pl.asie.charset.wires.logic;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

import net.minecraftforge.fmp.multipart.IMultipart;
import net.minecraftforge.fmp.multipart.IPartFactory;
import net.minecraft.util.ResourceLocation;
import pl.asie.charset.wires.WireKind;

public class PartWireProvider implements IPartFactory.IAdvancedPartFactory {
	public static PartWireBase createPart(int type) {
		PartWireBase part = null;
		WireKind kind = WireKind.VALUES[type];

		switch (kind.type()) {
			case NORMAL:
				part = new PartWireNormal();
				break;
			case INSULATED:
				part = new PartWireInsulated();
				break;
			case BUNDLED:
				part = new PartWireBundled();
				break;
		}

		if (part != null) {
			part.type = kind;
		}

		return part;
	}

	@Override
	public IMultipart createPart(ResourceLocation id, PacketBuffer buf) {
		int type = buf.readByte();
		buf.readerIndex(buf.readerIndex() - 1);
		PartWireBase part = createPart(type);
		part.readUpdatePacket(buf);
		return part;
	}

	@Override
	public IMultipart createPart(ResourceLocation id, NBTTagCompound nbt) {
		PartWireBase part = createPart(nbt.getByte("t"));
		part.readFromNBT(nbt);
		return part;
	}
}
