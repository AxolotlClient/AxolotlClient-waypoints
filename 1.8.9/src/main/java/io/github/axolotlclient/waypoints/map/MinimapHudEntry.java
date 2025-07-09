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
import io.github.axolotlclient.modules.hud.gui.entry.BoxHudEntry;
import io.github.axolotlclient.waypoints.AxolotlClientWaypoints;
import net.minecraft.resource.Identifier;

public class MinimapHudEntry extends BoxHudEntry {

	public static final Identifier ID = AxolotlClientWaypoints.rl("minimap_hud");

	private final Minimap minimap;

	public MinimapHudEntry(Minimap minimap) {
		super(AxolotlClientWaypoints.MINIMAP.size+2, AxolotlClientWaypoints.MINIMAP.size+2, true);
		this.minimap = minimap;
		outline = minimap.minimapOutline;
		outlineColor = minimap.outlineColor;
	}

	@Override
	public void renderComponent(float v) {
		int x = (int) (getX()/getScale()+1);
		if (minimap.getX() != x) {
			minimap.setX(x);
		}
		int y = (int) (getY()/getScale()+1);
		if (minimap.getY() != y) {
			minimap.setY(y);
		}
		minimap.renderMap();
	}

	@Override
	public void renderPlaceholderComponent(float v) {

	}

	@Override
	public Identifier getId() {
		return ID;
	}

	@Override
	public void setX(int x) {
		super.setX(x);
		minimap.setX((int) (x/getScale())+1);
	}

	@Override
	public void setY(int y) {
		super.setY(y);
		minimap.setY((int) (y/getScale())+1);
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
