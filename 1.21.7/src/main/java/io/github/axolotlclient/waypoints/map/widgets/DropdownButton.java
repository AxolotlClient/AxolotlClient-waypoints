/*
 * Copyright © 2025 moehreag <moehreag@gmail.com> & Contributors
 *
 * This file is part of AxolotlClient.
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

import io.github.axolotlclient.waypoints.AxolotlClientWaypoints;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class DropdownButton extends Button {
	private static final ResourceLocation ARROW_UP = AxolotlClientWaypoints.rl("dropdown/up"),
		ARROW_UP_HIGHLIGHTED = AxolotlClientWaypoints.rl("dropdown/up_highlighted"),
		ARROW_DOWN = AxolotlClientWaypoints.rl("dropdown/down"),
		ARROW_DOWN_HIGHLIGHTED = AxolotlClientWaypoints.rl("dropdown/down_highlighted");

	private boolean state;

	public DropdownButton(int x, int y, int width, int height, Component message, PressConsumer onPress) {
		super(x, y, width, height, message, btn -> {
			var dropdown = (DropdownButton) btn;
			dropdown.state = !dropdown.state;
			onPress.pressed(btn, dropdown.state);
		}, DEFAULT_NARRATION);
	}

	@Override
	protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		if (state) {
			guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, isHovered() ? ARROW_UP_HIGHLIGHTED : ARROW_UP, getX(), getY(), getWidth(), getHeight());
		} else {
			guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, isHovered() ? ARROW_DOWN_HIGHLIGHTED : ARROW_DOWN, getX(), getY(), getWidth(), getHeight());
		}
	}

	public interface PressConsumer {
		void pressed(Button button, boolean state);
	}
}
