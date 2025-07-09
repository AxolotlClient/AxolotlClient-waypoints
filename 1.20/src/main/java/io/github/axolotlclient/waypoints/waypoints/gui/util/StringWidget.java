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
