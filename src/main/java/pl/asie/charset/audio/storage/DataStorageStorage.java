package pl.asie.charset.audio.storage;

import java.io.IOException;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

import net.minecraftforge.common.capabilities.Capability;

import pl.asie.charset.api.audio.IDataStorage;
import pl.asie.charset.audio.ModCharsetAudio;

public class DataStorageStorage implements Capability.IStorage<IDataStorage> {
	@Override
	public NBTBase writeNBT(Capability<IDataStorage> capability, IDataStorage instance, EnumFacing side) {
		System.out.println("write");
		//new Throwable().printStackTrace();
		if (instance.getUniqueId() != null) {
			System.out.println("write2");
			NBTTagCompound compound = new NBTTagCompound();

			compound.setInteger("size", instance.getSize());
			compound.setString("uid", instance.getUniqueId());

			try {
				instance.onUnload();
			} catch (IOException e) {
				ModCharsetAudio.logger.error("Could not save a DataStorage! (ID: " + instance.getUniqueId() + ")");
				e.printStackTrace();
			}

			return compound;
		} else {
			return null;
		}
	}

	@Override
	public void readNBT(Capability<IDataStorage> capability, IDataStorage instance, EnumFacing side, NBTBase nbt) {
		System.out.println("read");
		//new Throwable().printStackTrace();
		if (nbt instanceof NBTTagCompound) {
			System.out.println("read2");
			NBTTagCompound compound = (NBTTagCompound) nbt;
			if (compound.hasKey("uid")) {
				instance.initialize(compound.getString("uid"), 0, compound.getInteger("size"));
			}
		}
	}
}
