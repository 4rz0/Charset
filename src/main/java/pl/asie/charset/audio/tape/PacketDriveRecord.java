package pl.asie.charset.audio.tape;

import io.netty.buffer.ByteBuf;

import net.minecraft.server.MinecraftServer;

import mcmultipart.multipart.IMultipart;
import pl.asie.charset.lib.network.PacketPart;

public class PacketDriveRecord extends PacketPart {
	private byte[] data;
	private int totalLength;
	private boolean isLast;

	public PacketDriveRecord() {
		super();
	}

	public PacketDriveRecord(IMultipart part, byte[] data, int totalLength, boolean isLast) {
		super(part);
		this.totalLength = totalLength;
		this.isLast = isLast;
		this.data = data;
	}

	@Override
	public void writeData(ByteBuf buf) {
		super.writeData(buf);

		buf.writeInt(totalLength);
		buf.writeBoolean(isLast);
		buf.writeShort(data.length);
		buf.writeBytes(data);
	}

	@Override
	public void readData(ByteBuf buf) {
		super.readData(buf);

		totalLength = buf.readInt();
		isLast = buf.readBoolean();
		int len = buf.readShort();
		if (len > 0) {
			final byte[] in = new byte[len];
			buf.readBytes(in);

			if (part instanceof PartTapeDrive) {
				if (!MinecraftServer.getServer().isCallingFromMinecraftThread()) {
					MinecraftServer.getServer().addScheduledTask(new Runnable() {
						@Override
						public void run() {
							((PartTapeDrive) part).writeData(in, isLast, totalLength);
						}
					});
				} else {
					((PartTapeDrive) part).writeData(in, isLast, totalLength);
				}
			}
		}
	}
}
