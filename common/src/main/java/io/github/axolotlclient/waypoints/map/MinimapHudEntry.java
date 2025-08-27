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

package io.github.axolotlclient.waypoints.map;

import java.util.List;

import io.github.axolotlclient.AxolotlClientConfig.api.options.Option;
import io.github.axolotlclient.bridge.render.AxoRenderContext;
import io.github.axolotlclient.bridge.util.AxoIdentifier;
import io.github.axolotlclient.modules.hud.gui.entry.BoxHudEntry;
import io.github.axolotlclient.waypoints.AxolotlClientWaypointsCommon;

public class MinimapHudEntry extends BoxHudEntry {

	public static final AxoIdentifier ID = AxolotlClientWaypointsCommon.rl("minimap_hud");

	private final MinimapCommon minimap;

	public MinimapHudEntry(MinimapCommon minimap) {
		super(minimap.size + 2, minimap.size + 2, true);
		this.minimap = minimap;
		outline = minimap.minimapOutline;
		outlineColor = minimap.outlineColor;
	}

	@Override
	public void renderComponent(AxoRenderContext guiGraphics, float v) {
		int x = getX() + 1;
		if (minimap.getX() != x) {
			minimap.setX(x);
		}
		int y = getY() + 1;
		if (minimap.getY() != y) {
			minimap.setY(y);
		}
		minimap.renderMap(guiGraphics);
	}

	@Override
	public void renderPlaceholderComponent(AxoRenderContext guiGraphics, float v) {

	}

	@Override
	public AxoIdentifier getId() {
		return ID;
	}

	@Override
	public void setX(int x) {
		super.setX(x);
		minimap.setX((int) (x / getScale()) + 1);
	}

	@Override
	public void setY(int y) {
		super.setY(y);
		minimap.setY((int) (y / getScale()) + 1);
	}

	@Override
	public List<Option<?>> getConfigurationOptions() {
		var options = super.getConfigurationOptions();
		options.remove(enabled);
		options.remove(background);
		options.remove(backgroundColor);
		return options;
	}

	@Override
	public boolean isEnabled() {
		return minimap.isEnabled();
	}

	@Override
	public double getDefaultX() {
		return 0.85;
	}

	@Override
	public double getDefaultY() {
		return 0.05;
	}
}
