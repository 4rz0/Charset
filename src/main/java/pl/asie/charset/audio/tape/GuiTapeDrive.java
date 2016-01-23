package pl.asie.charset.audio.tape;

import java.io.IOException;
import java.util.Arrays;

import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import pl.asie.charset.audio.ModCharsetAudio;
import pl.asie.charset.lib.container.GuiContainerCharset;

public class GuiTapeDrive extends GuiContainerCharset {
	private static final ResourceLocation TEXTURE = new ResourceLocation("charsetaudio:textures/gui/tape_drive.png");
	private static final int BUTTON_START_X = 88 - (Button.values().length * 10);
	private static final int BUTTON_START_Y = 58;

	private State state = State.STOPPED;
	public enum Button {
		REWIND,
		PLAY,
		STOP,
		FAST_FORWARD,
		RECORD
	}
	private PartTapeDrive tapeDrive;
	private int counter;
	private Button buttonHovering = null;
	private boolean ctrResetHover = false;
	private TapeRecordThread tapeRecord;
	private Thread tapeRecordThread;

	public boolean isRecording() {
		return tapeRecordThread != null && tapeRecordThread.isAlive();
	}
	
	public GuiTapeDrive(Container container, PartTapeDrive tapeDrive) {
		super(container, 176, 166);
		this.tapeDrive = tapeDrive;
	}
	
	public boolean isButtonPressed(Button button) {
		if (button == buttonHovering) {
			return true;
		}

		if (isRecording() && button == Button.RECORD) {
			return true;
		}

		switch (state) {
			case FORWARDING:
				return button == Button.FAST_FORWARD;
			case PLAYING:
				return button == Button.PLAY;
			case REWINDING:
				return button == Button.REWIND;
			case STOPPED:
			default:
				return false;
		}
	}
	
	public void setState(State state) {
		if (tapeDrive != null) {
			try {
				PacketDriveState packet = new PacketDriveState(tapeDrive, state);
				ModCharsetAudio.packet.sendToServer(packet);
				tapeDrive.setState(state);
			} catch (Exception e) {
				//NO-OP
			}
		}
	}
	
	public void handleButtonPress(Button button) {
		if (isRecording()) {
			return;
		}

		switch(button) {
			case REWIND:
				if(state == State.REWINDING) setState(State.STOPPED);
				else setState(State.REWINDING);
				break;
			case PLAY:
				setState(State.PLAYING);
				break;
			case FAST_FORWARD:
				if(state == State.FORWARDING) setState(State.STOPPED);
				else setState(State.FORWARDING);
				break;
			case STOP:
				setState(State.STOPPED);
				break;
			case RECORD:
				if (tapeDrive.inventory.getStackInSlot(0) != null) {
					setState(State.STOPPED);
					JFileChooser chooser = new JFileChooser();
					chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					chooser.setDialogType(JFileChooser.OPEN_DIALOG);
					chooser.setFileFilter(new FileNameExtensionFilter("Audio file " + Arrays.toString(TapeRecordThread.getSupportedExtensions()), TapeRecordThread.getSupportedExtensions()));
					if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
						if (chooser.getSelectedFile() != null) {
							tapeRecord = new TapeRecordThread(chooser.getSelectedFile(), tapeDrive);
							tapeRecordThread = new Thread(tapeRecord);
							tapeRecordThread.start();
						}
					}
				}
				break;
		}
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		this.state = tapeDrive.getState();
		this.counter = tapeDrive.state.counter;
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (isRecording()) {
			if (keyCode == 1 || keyCode == this.mc.gameSettings.keyBindInventory.getKeyCode()) {
				return;
			}
		}

		super.keyTyped(typedChar, keyCode);
	}

	@Override
	public void mouseClicked(int x, int y, int mb) throws IOException {
		if (isRecording()) {
			return;
		}

		super.mouseClicked(x, y, mb);
		if(mb == 0) {
			for(Button button: Button.values()) {
				int button_x = this.xCenter + BUTTON_START_X + (button.ordinal() * 20);
				int button_y = this.yCenter + BUTTON_START_Y;
				if(x >= button_x && x < (button_x + 20) && y >= button_y && y < (button_y + 15)) {
					if(!isButtonPressed(button)) {
						buttonHovering = button;
						return;
					}
				}
			}

			int reset_x = this.xCenter + 122;
			int reset_y = this.yCenter + 39;
			if (x >= reset_x && x <= (reset_x + 5) && y >= reset_y && y <= (reset_y + 6)) {
				ctrResetHover = true;
				return;
			}
		}
	}
	
	@Override
	public void mouseReleased(int x, int y, int which) {
		if (isRecording()) {
			return;
		}

		super.mouseReleased(x, y, which);
		if(which >= 0 && buttonHovering != null) {
			this.mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
			handleButtonPress(buttonHovering);
			buttonHovering = null;
		}

		if (which >= 0 && ctrResetHover) {
			this.mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
			ModCharsetAudio.packet.sendToServer(new PacketDriveCounter(tapeDrive, 0));
			ctrResetHover = false;
		}
	}
	
	// Uses NBT data.
	private String getLabel() {
		ItemStack stack = this.inventorySlots.getInventory().get(0);
		if (stack != null && stack.getItem() instanceof ItemTape) {
			String label = StatCollector.translateToLocal("tooltip.charset.tape.unnamed");
			if (stack.hasDisplayName()) {
				label = stack.getDisplayName();
			}
			return label;
		} else return null;
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
		super.drawGuiContainerBackgroundLayer(f, i, j);

		this.mc.getTextureManager().bindTexture(TEXTURE);
		this.drawTexturedModalRect(this.xCenter, this.yCenter, 0, 0, this.xSize, this.ySize);

		// Draw buttons
		for(Button button: Button.values()) {
			int button_ty = 170 + (button.ordinal() * 15);
			int button_tx = isButtonPressed(button) ? 20 : 0;
			int button_x = BUTTON_START_X + (button.ordinal() * 20);
			this.drawTexturedModalRect(this.xCenter + button_x, this.yCenter + BUTTON_START_Y, button_tx, button_ty, 20, 15);
		}

		if (ctrResetHover) {
			this.drawTexturedModalRect(this.xCenter + 122, this.yCenter + 39, 121, 38, 5, 6);
		}

		// Draw counter
		float counterReal = (float) counter / 48000.0f;
		if (counterReal < 0) {
			counterReal = (counterReal % 1000f) + 1000.0f;
		}
		float[] counterPos = new float[3];
		counterPos[0] = (counterReal / 100.0f) % 10;
		counterPos[1] = (counterReal / 10.0f) % 10;
		counterPos[2] = (counterReal) % 10;
		if (counterPos[2] < 9f) {
			counterPos[1] = (float) Math.floor(counterPos[1]);
		} else {
			counterPos[1] = (float) Math.floor(counterPos[1]) + counterReal % 1;
		}
		if ((counterReal % 100) < 99f) {
			counterPos[0] = (float) Math.floor(counterPos[0]);
		} else {
			counterPos[0] = (float) Math.floor(counterPos[0]) + counterReal % 1;
		}

		for (int c = 0; c < 3; c++) {
			int cpy = Math.round(counterPos[c] * 10);
			if (cpy < 0) cpy += 100;
			this.drawTexturedModalRect(this.xCenter + 98 + (c * 7), this.yCenter + 38, 248, cpy, 8, 9);
		}

		GlStateManager.enableBlend();
		this.drawTexturedModalRect(this.xCenter + 99, this.yCenter + 34, 98, 34, 21, 16);
		GlStateManager.disableBlend();

		// Draw label
		String label = getLabel();
		int labelColor = 0xFFFFFF;

		if (isRecording()) {
			label = tapeRecord.getStatusBar();
			labelColor = 0x90E0B0;
		} else {
			if (label == null) {
				label = StatCollector.translateToLocal("tooltip.charset.tape.none");
				labelColor = 0xFF3333;
			}
			int width = fontRendererObj.getStringWidth(label);
			if (width > 142) {
				while (width > (144 - 8) && label.length() > 4) {
					label = label.substring(0, label.length() - 1);
					width = fontRendererObj.getStringWidth(label);
				}
				label += "...";
			}
		}
		this.drawCenteredString(this.fontRendererObj, label, this.xCenter + 88, this.yCenter + 15, labelColor);
	}
}
