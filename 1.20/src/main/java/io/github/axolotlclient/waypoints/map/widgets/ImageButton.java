/*
 * Copyright © 2025 moehreag <moehreag@gmail.com> & Contributors
 *
 * This file is part of AxolotlClient (Waypoints Mod).
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * For more information, see the LICENSE file.
 */

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
