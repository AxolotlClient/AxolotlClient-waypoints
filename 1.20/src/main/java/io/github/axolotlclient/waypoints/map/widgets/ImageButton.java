package io.github.axolotlclient.waypoints.map.widgets;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public class ImageButton extends Button {
	protected final WidgetSprites sprites;

	public ImageButton(int x, int y, int width, int height, WidgetSprites sprites, Button.OnPress onPress) {
		this(x, y, width, height, sprites, onPress, CommonComponents.EMPTY);
	}

	public ImageButton(int x, int y, int width, int height, WidgetSprites sprites, Button.OnPress onPress, Component message) {
		super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
		this.sprites = sprites;
	}

	public ImageButton(int width, int height, WidgetSprites sprites, Button.OnPress onPress, Component message) {
		this(0, 0, width, height, sprites, onPress, message);
	}

	@Override
	public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		ResourceLocation resourceLocation = this.sprites.get(this.isActive(), this.isHoveredOrFocused());
		guiGraphics.blit(resourceLocation, this.getX(), this.getY(), 0, 0, this.width, this.height, width, height);
	}
}
