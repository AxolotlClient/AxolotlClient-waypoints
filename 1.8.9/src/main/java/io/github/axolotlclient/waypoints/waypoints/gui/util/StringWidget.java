package io.github.axolotlclient.waypoints.waypoints.gui.util;

import io.github.axolotlclient.AxolotlClientConfig.impl.ui.ClickableWidget;
import lombok.Getter;
import net.minecraft.client.render.TextRenderer;

public class StringWidget extends ClickableWidget {
	private final TextRenderer font;

	public StringWidget(int width, int height, String message, TextRenderer font) {
		this(0, 0, width, height, message, font, 0.5f);
	}

	@Getter
	private float alignX;
	@Getter
	private int color = -1;

	public StringWidget(String message, TextRenderer font) {
		this(0, 0, font.getWidth(message), 9, message, font, 0.5f);
	}

	public StringWidget(int x, int y, int width, int height, String message, TextRenderer font, float alignX) {
		super(x, y, width, height, message);
		this.font = font;
		this.alignX = alignX;
		active = false;
	}

	public StringWidget setColor(int color) {
		this.color = color;
		return this;
	}

	private StringWidget horizontalAlignment(float horizontalAlignment) {
		this.alignX = horizontalAlignment;
		return this;
	}

	public StringWidget alignLeft() {
		return this.horizontalAlignment(0.0F);
	}

	public StringWidget alignCenter() {
		return this.horizontalAlignment(0.5F);
	}

	public StringWidget alignRight() {
		return this.horizontalAlignment(1.0F);
	}

	@Override
	public void render(int mouseX, int mouseY, float partialTick) {
		String component = getMessage();
		int i = this.getX() + Math.round(this.alignX * (this.getWidth() - font.getWidth(component)));
		int j = this.getY() + (this.getHeight() - 9) / 2;
		font.draw(component, i, j, getColor());
	}

	@Override
	public SelectionType getType() {
		return SelectionType.NONE;
	}

	@Override
	public boolean isFocused() {
		return false;
	}

	@Override
	public void setFocused(boolean b) {

	}
}
