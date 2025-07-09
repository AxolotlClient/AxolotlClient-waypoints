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
import io.github.axolotlclient.waypoints.AxolotlClientWaypoints;
import net.minecraft.client.Minecraft;
import net.minecraft.resource.Identifier;

public class DropdownButton extends ButtonWidget {
	private static final Identifier ARROW_UP = AxolotlClientWaypoints.rl("textures/gui/sprites/dropdown/up.png"),
		ARROW_UP_HIGHLIGHTED = AxolotlClientWaypoints.rl("textures/gui/sprites/dropdown/up_highlighted.png"),
		ARROW_DOWN = AxolotlClientWaypoints.rl("textures/gui/sprites/dropdown/down.png"),
		ARROW_DOWN_HIGHLIGHTED = AxolotlClientWaypoints.rl("textures/gui/sprites/dropdown/down_highlighted.png");

	private boolean state;

	public DropdownButton(int x, int y, int width, int height, String message, PressConsumer onPress) {
		super(x, y, width, height, message, btn -> {
			var dropdown = (DropdownButton) btn;
			dropdown.state = !dropdown.state;
			onPress.pressed(btn, dropdown.state);
		});
	}

	@Override
	protected void drawWidget(int mouseX, int mouseY, float partialTick) {
		if (state) {
			Minecraft.getInstance().getTextureManager().bind(isHovered() ? ARROW_UP_HIGHLIGHTED : ARROW_UP);
			drawTexture(getX(), getY(), 0, 0, getWidth(), getHeight(), getWidth(), getHeight());
		} else {
			Minecraft.getInstance().getTextureManager().bind(isHovered() ? ARROW_DOWN_HIGHLIGHTED : ARROW_DOWN);
			drawTexture(getX(), getY(), 0, 0, getWidth(), getHeight(), getWidth(), getHeight());
		}
	}

	public interface PressConsumer {
		void pressed(ButtonWidget button, boolean state);
	}
}
