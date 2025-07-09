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

package io.github.axolotlclient.waypoints;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.axolotlclient.AxolotlClientConfig.api.AxolotlClientConfig;
import io.github.axolotlclient.AxolotlClientConfig.api.manager.ConfigManager;
import io.github.axolotlclient.AxolotlClientConfig.api.options.OptionCategory;
import io.github.axolotlclient.AxolotlClientConfig.impl.managers.VersionedJsonConfigManager;
import io.github.axolotlclient.AxolotlClientConfig.impl.options.BooleanOption;
import io.github.axolotlclient.AxolotlClientConfig.impl.util.ConfigStyles;
import io.github.axolotlclient.waypoints.map.Minimap;
import io.github.axolotlclient.waypoints.map.WorldMapScreen;
import io.github.axolotlclient.waypoints.mixin.MinecraftServerAccessor;
import io.github.axolotlclient.waypoints.waypoints.Waypoint;
import io.github.axolotlclient.waypoints.waypoints.WaypointRenderer;
import io.github.axolotlclient.waypoints.waypoints.WaypointStorage;
import io.github.axolotlclient.waypoints.waypoints.gui.CreateWaypointScreen;
import io.github.axolotlclient.waypoints.waypoints.gui.WaypointsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("DataFlowIssue")
public class AxolotlClientWaypoints implements ClientModInitializer {

	public static final String MODID = "axolotlclient_waypoints";
	public static final Path OPTIONS_PATH = FabricLoader.getInstance().getConfigDir().resolve(MODID).resolve("options.json");
	public static final boolean AXOLOTLCLIENT_PRESENT = FabricLoader.getInstance().isModLoaded("axolotlclient");
	private static final Path MOD_STORAGE_DIR = FabricLoader.getInstance().getGameDir().resolve("." + MODID);
	public static final Minimap MINIMAP = new Minimap();
	public static final WaypointStorage WAYPOINT_STORAGE = new WaypointStorage();
	public static final WaypointRenderer WAYPOINT_RENDERER = new WaypointRenderer();

	public static OptionCategory category = OptionCategory.create(MODID);
	private final OptionCategory waypoints = OptionCategory.create("waypoints");
	public static BooleanOption renderWaypoints = new BooleanOption("render_waypoints", true);
	public static BooleanOption renderWaypointsInWorld = new BooleanOption("render_waypoints_in_world", true);
	public static BooleanOption renderOutOfViewWaypointsOnScreenEdge = new BooleanOption("render_out_of_view_waypoints", true);

	private final KeyMapping map = new KeyMapping(MODID + ".world_map", InputConstants.KEY_M, MODID);
	private final KeyMapping manageWaypoints = new KeyMapping(MODID + ".waypoints_menu", InputConstants.KEY_K, MODID);
	private final KeyMapping newWaypoint = new KeyMapping(MODID + ".create_waypoint", InputConstants.KEY_N, MODID);

	@Override
	public void onInitializeClient() {
		MINIMAP.init();

		category.add(waypoints);
		waypoints.add(renderWaypoints, renderWaypointsInWorld, renderOutOfViewWaypointsOnScreenEdge);

		ConfigManager configManager;
		AxolotlClientConfig.getInstance().register(configManager = new VersionedJsonConfigManager(OPTIONS_PATH, category, 1,
			(oldVersion, newVersion, rootCategory, json) -> json));
		configManager.load();
		configManager.save();

		KeyBindingHelper.registerKeyBinding(map);
		KeyBindingHelper.registerKeyBinding(manageWaypoints);
		KeyBindingHelper.registerKeyBinding(newWaypoint);
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (map.consumeClick()) {
				mc.setScreen(new WorldMapScreen());
			} else if (manageWaypoints.consumeClick()) {
				mc.setScreen(new WaypointsScreen(mc.screen));
			} else if (newWaypoint.consumeClick()) {
				mc.setScreen(new CreateWaypointScreen(mc.screen));
			}
		});
	}

	public static Screen createOptionsScreen(Screen parent) {
		return ConfigStyles.createScreen(parent, category);
	}

	public static ResourceLocation rl(String path) {
		return ResourceLocation.fromNamespaceAndPath(MODID, path);
	}

	public static Component tr(String key, Object... args) {
		return Component.translatable(MODID + "." + key, args);
	}

	public static String tra(String key) {
		return I18n.get(MODID + "." + key);
	}

	public static List<Waypoint> getCurrentWaypoints() {
		return getCurrentWaypoints(true, true);
	}

	public static List<Waypoint> getCurrentWaypoints(boolean world, boolean dimension) {
		var mc = Minecraft.getInstance();
		String str;
		if (mc.getCurrentServer() != null) {
			str = mc.getCurrentServer().ip;
		} else if (mc.getSingleplayerServer() != null) {
			str = ((MinecraftServerAccessor) mc.getSingleplayerServer()).getStorageSource().getLevelId();
		} else {
			return Collections.emptyList();
		}
		var pos = mc.player.position().toVector3f();
		return WAYPOINT_STORAGE.getCurrentlyAvailableWaypoints(str, mc.level.dimension().location().toString()).stream()
			.sorted(Comparator.comparingDouble(w -> w.squaredDistTo(pos)))
			.toList();
	}

	private static String getB64(String s) {
		return new String(Base64.getUrlEncoder().encode(s.getBytes(StandardCharsets.UTF_8)));
	}

	public static Path getCurrentStorageDir() {
		var mc = Minecraft.getInstance();
		return getCurrentWorldStorageDir()
			.resolve(getB64(mc.level.dimension().location().toString()));
	}

	public static Path getCurrentWorldStorageDir() {
		var mc = Minecraft.getInstance();
		String str;
		if (mc.getSingleplayerServer() == null) {
			str = mc.getCurrentServer().ip;
		} else {
			str = ((MinecraftServerAccessor) mc.getSingleplayerServer()).getStorageSource().getLevelId();
		}
		return AxolotlClientWaypoints.MOD_STORAGE_DIR.resolve(getB64(str));
	}

	public static boolean playerHasOp() {
		return Minecraft.getInstance().player.hasPermissions(4);
	}
}
