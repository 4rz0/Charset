package pl.asie.charset.audio.tape;

import io.netty.buffer.ByteBuf;

import net.minecraft.tileentity.TileEntity;

import pl.asie.charset.api.audio.IAudioSource;
import pl.asie.charset.audio.ProxyClient;
import pl.asie.charset.audio.manager.AudioStreamOpenAL;
import pl.asie.charset.audio.manager.IAudioStream;
import pl.asie.charset.lib.network.PacketTile;
import pl.asie.charset.lib.utils.DFPWM;

public class PacketDriveAudio extends PacketTile {
	private static final DFPWM dfpwm = new DFPWM();
	private byte[] packet;

	public PacketDriveAudio() {
		super();
	}

	public PacketDriveAudio(TileEntity tile, byte[] packet) {
		super(tile);
		this.packet = packet;
	}

	@Override
	public void writeData(ByteBuf buf) {
		super.writeData(buf);

		buf.writeMedium(packet.length);
		buf.writeBytes(packet);
	}

	@Override
	public void readData(ByteBuf buf) {
		super.readData(buf);

		int len = buf.readMedium();
		packet = new byte[len];
		buf.readBytes(packet);

		if (tile instanceof IAudioSource) {
			IAudioSource source = (IAudioSource) tile;
			IAudioStream stream = ProxyClient.stream.get(source);
			if (stream == null) {
				stream = new AudioStreamOpenAL(false, false, 8);
				stream.setSampleRate(48000);
				stream.setHearing(32.0F, 1.0F);
				ProxyClient.stream.put(source, stream);
			}

			byte[] out = new byte[packet.length * 8];
			dfpwm.decompress(out, packet, 0, 0, packet.length);
			for (int i = 0; i < out.length; i++) {
				out[i] = (byte) (out[i] ^ 0x80);
			}

			stream.push(out);
			stream.play(tile.getPos().getX(), tile.getPos().getY(), tile.getPos().getZ());
		}
	}
}
