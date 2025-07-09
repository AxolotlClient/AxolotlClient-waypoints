package io.github.axolotlclient.waypoints.map.widgets;

import io.github.axolotlclient.AxolotlClientConfig.api.util.Colors;
import io.github.axolotlclient.AxolotlClientConfig.impl.ui.ClickableWidget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sound.system.SoundManager;
import net.minecraft.resource.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Keyboard;

@Environment(EnvType.CLIENT)
public abstract class AbstractSliderButton extends ClickableWidget {
	private static final Identifier SLIDER_LOCATION = new Identifier("textures/gui/slider.png");
	protected static final int TEXTURE_WIDTH = 200;
	protected static final int TEXTURE_HEIGHT = 20;
	protected static final int TEXTURE_BORDER_X = 20;
	protected static final int TEXTURE_BORDER_Y = 4;
	protected static final int TEXT_MARGIN = 2;
	private static final int HEIGHT = 20;
	private static final int HANDLE_HALF_WIDTH = 4;
	private static final int HANDLE_WIDTH = 8;
	private static final int BACKGROUND = 0;
	private static final int BACKGROUND_FOCUSED = 1;
	private static final int HANDLE = 2;
	private static final int HANDLE_FOCUSED = 3;
	protected double value;
	private boolean canChangeValue;

	public AbstractSliderButton(int x, int y, int width, int height, String message, double value) {
		super(x, y, width, height, message);
		this.value = value;
	}


	@Override
	public void drawWidget(int mouseX, int mouseY, float partialTick) {
		Minecraft minecraft = Minecraft.getInstance();
		//RenderSystem.enableBlend();
		//RenderSystem.defaultBlendFunc();
		//RenderSystem.enableDepthTest();
		minecraft.getTextureManager().bind(WIDGETS_LOCATION);
		this.drawTexture(this.getX(), this.getY(), 0, 46, this.getWidth() / 2, this.getHeight());
		this.drawTexture(this.getX() + this.getWidth() / 2, this.getY(), 200 - this.getWidth() / 2, 46, this.getWidth() / 2, this.getHeight());
		int i = (this.isHovered() ? 2 : 1) * 20;
		this.drawTexture(this.getX() + (int) (this.value * (double) (this.getWidth() - 8)), this.getY(), 0, 46 + i, 4, 20);
		this.drawTexture(this.getX() + (int) (this.value * (double) (this.getWidth() - 8)) + 4, this.getY(), 196, 46 + i, 4, 20);

		drawScrollingText(getMessage(), getX(), getY(), getWidth(), getHeight(), Colors.WHITE);
	}

	@Override
	public void onClick(double mouseX, double mouseY) {
		this.setValueFromMouse(mouseX);
	}

	@Override
	public void setFocused(boolean focused) {
		super.setFocused(focused);
		if (!focused) {
			this.canChangeValue = false;
		} else {
			//InputType inputType = Minecraft.getInstance().getLastInputType();
			//if (inputType == InputType.MOUSE || inputType == InputType.KEYBOARD_TAB) {
			this.canChangeValue = true;
			//}
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == Keyboard.KEY_SPACE || keyCode == Keyboard.KEY_RETURN) {
			this.canChangeValue = !this.canChangeValue;
			return true;
		} else {
			if (this.canChangeValue) {
				boolean bl = keyCode == 263;
				if (bl || keyCode == 262) {
					float f = bl ? -1.0F : 1.0F;
					this.setValue(this.value + f / (this.getWidth() - 8));
					return true;
				}
			}

			return false;
		}
	}

	private void setValueFromMouse(double mouseX) {
		this.setValue((mouseX - (this.getX() + 4)) / (this.getWidth() - 8));
	}

	private void setValue(double value) {
		double d = this.value;
		this.value = MathHelper.clamp(value, 0.0, 1.0);
		if (d != this.value) {
			this.applyValue();
		}

		this.updateMessage();
	}

	@Override
	protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
		this.setValueFromMouse(mouseX);
		super.onDrag(mouseX, mouseY, dragX, dragY);
	}

	@Override
	public void playDownSound(SoundManager soundManager) {

	}

	@Override
	public void onRelease(double mouseX, double mouseY) {
		super.playDownSound(Minecraft.getInstance().getSoundManager());
	}

	protected abstract void updateMessage();

	protected abstract void applyValue();
}
