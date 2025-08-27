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

import io.github.axolotlclient.AxolotlClientConfig.api.options.OptionCategory;
import io.github.axolotlclient.AxolotlClientConfig.api.util.Colors;
import io.github.axolotlclient.AxolotlClientConfig.impl.options.ColorOption;
import io.github.axolotlclient.AxolotlClientConfig.impl.options.IntegerOption;
import io.github.axolotlclient.bridge.render.AxoRenderContext;
import io.github.axolotlclient.waypoints.BooleanOption;
import lombok.Getter;
import lombok.Setter;

public abstract class MinimapCommon {
	public final int radius = 64, size = radius * 2;
	@Getter
	@Setter
	protected int x,y;

	public final ColorOption outlineColor = new ColorOption("outline_color", Colors.WHITE);
	public final BooleanOption minimapOutline = new BooleanOption("minimap_outline", true);

	protected final BooleanOption enableBiomeBlending = new BooleanOption("biome_blending", false);
	public final IntegerOption arrowScale = new IntegerOption("arrow_scale", 2, 1, 4);
	protected final BooleanOption lockMapToNorth = new BooleanOption("lock_map_north", true);
	public final BooleanOption enabled = new BooleanOption("enabled", true);
	protected final IntegerOption mapScale = new IntegerOption("map_scale", 1, 1, 5);
	protected final BooleanOption showWaypoints = new BooleanOption("show_waypoints", true);
	protected final BooleanOption showCardinalDirections = new BooleanOption("show_cardinal_directions", true);
	public static final OptionCategory minimap = OptionCategory.create("minimap");

	public abstract void renderMap(AxoRenderContext ctx);

	public abstract boolean isEnabled();
}
