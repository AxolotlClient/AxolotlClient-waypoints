package io.github.axolotlclient.waypoints.waypoints.gui.util;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public class StringWidget extends net.minecraft.client.gui.components.StringWidget {
	public StringWidget(Component message, Font font) {
		super(message, font);
	}

	public StringWidget(int width, int height, Component message, Font font) {
		super(width, height, message, font);
	}

	public StringWidget(int x, int y, int width, int height, Component message, Font font) {
		super(x, y, width, height, message, font);
	}

	public void setHeight(int height) {
		this.height = height;
	}
}
