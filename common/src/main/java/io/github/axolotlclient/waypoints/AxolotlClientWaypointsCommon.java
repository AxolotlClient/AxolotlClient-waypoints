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

import java.nio.file.Path;

import io.github.axolotlclient.AxolotlClientConfig.api.ui.ConfigUI;
import io.github.axolotlclient.bridge.util.AxoIdentifier;
import net.fabricmc.loader.api.FabricLoader;

public class AxolotlClientWaypointsCommon {

	public static final String MODID = "axolotlclient_waypoints";
	static final Path MOD_STORAGE_DIR = FabricLoader.getInstance().getGameDir().resolve("." + MODID);
	public static final Path OPTIONS_PATH = FabricLoader.getInstance().getConfigDir().resolve(MODID).resolve("options.json");
	public static final boolean AXOLOTLCLIENT_PRESENT = FabricLoader.getInstance().isModLoaded("axolotlclient");

	public static Path getCurrentWorldStorageDir() {
		throw new UnsupportedOperationException("Implemented using Mixin");
	}

	public static void init() {
		ConfigUI.getInstance().runWhenLoaded(() -> {
			ConfigUI.getInstance().addWidget("vanilla", MODID+".boolean", "io.github.axolotlclient.waypoints.util.ExtendedBooleanWidget");
			ConfigUI.getInstance().addWidget("rounded", MODID+".boolean", "io.github.axolotlclient.waypoints.util.ExtendedBooleanWidgetRounded");
		});
	}

	public static AxoIdentifier rl(String path) {
		return AxoIdentifier.of(MODID, path);
	}
}
