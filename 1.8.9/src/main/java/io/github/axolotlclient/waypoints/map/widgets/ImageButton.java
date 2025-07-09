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

import io.github.axolotlclient.AxolotlClientConfig.impl.ui.ButtonWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.resource.Identifier;

public class ImageButton extends ButtonWidget {

	private final WidgetSprites sprites;

	public ImageButton(int x, int y, int width, int height, WidgetSprites sprites, PressAction onPress) {
		this(x, y, width, height, sprites, onPress, "");
	}

	public ImageButton(int x, int y, int width, int height, WidgetSprites sprites, PressAction onPress, String message) {
		super(x, y, width, height, message, onPress);
		this.sprites = sprites;
	}

	public ImageButton(int width, int height, WidgetSprites sprites, PressAction onPress, String message) {
		this(0, 0, width, height, sprites, onPress, message);
	}

	@Override
	public void drawWidget(int mouseX, int mouseY, float partialTick) {
		Identifier identifier = this.sprites.get(active, isHovered());
		Minecraft.getInstance().getTextureManager().bind(identifier);
		drawTexture(this.getX(), this.getY(), 0, 0, getWidth(), this.getHeight(), getWidth(), getHeight());
	}

	public record WidgetSprites(Identifier enabled, Identifier disabled, Identifier enabledFocused,
								Identifier disabledFocused) {
		public WidgetSprites(Identifier enabled, Identifier disabled) {
			this(enabled, enabled, disabled, disabled);
		}

		public WidgetSprites(Identifier enabled, Identifier disabled, Identifier enabledFocused) {
			this(enabled, disabled, enabledFocused, disabled);
		}

		public Identifier get(boolean enabled, boolean focused) {
			if (enabled) {
				return focused ? this.enabledFocused : this.enabled;
			} else {
				return focused ? this.disabledFocused : this.disabled;
			}
		}
	}
}
