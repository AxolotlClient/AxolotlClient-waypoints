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

package io.github.axolotlclient.waypoints;

import io.github.axolotlclient.AxolotlClientCommon;
import io.github.axolotlclient.AxolotlClientConfig.api.AxolotlClientConfig;
import io.github.axolotlclient.AxolotlClientConfig.impl.managers.JsonConfigManager;
import io.github.axolotlclient.bridge.events.Events;
import io.github.axolotlclient.modules.hud.HudManagerCommon;
import io.github.axolotlclient.waypoints.map.MinimapCommon;
import io.github.axolotlclient.waypoints.map.MinimapHudEntry;

/*
 * Springboard class to not need to reference classes from
 * AxolotlClient directly. This allows this mod to not
 * have to explicitly depend on AxolotlClient but instead leave
 * this dependency optional.
 */
public class HudCreator {

	public static Runnable createHud(MinimapCommon minimap) {
		var hud = new MinimapHudEntry(minimap);
		hud.setEnabled(true);
		var hudConfigManager = new JsonConfigManager(AxolotlClientWaypointsCommon.OPTIONS_PATH.resolveSibling(hud.getId().br$getPath() + ".json"), hud.getAllOptions());
		hudConfigManager.suppressName("x");
		hudConfigManager.suppressName("y");
		hudConfigManager.suppressName(minimap.minimapOutline.getName());
		hudConfigManager.suppressName(minimap.outlineColor.getName());
		AxolotlClientConfig.getInstance().register(hudConfigManager);
		hudConfigManager.load();
		MinimapCommon.minimap.add(hud.getAllOptions(), false);
		Events.CLIENT_READY.register(() -> {
			AxolotlClientCommon.getInstance().getConfig().config.add(AxolotlClientWaypointsCommon.category, false);
			HudManagerCommon.getInstance().addNonConfigured(hud);
		});
		return hudConfigManager::save;
	}
}
