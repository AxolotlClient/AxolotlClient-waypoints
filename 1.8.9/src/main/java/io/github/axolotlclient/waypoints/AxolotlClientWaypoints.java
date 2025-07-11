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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

import io.github.axolotlclient.AxolotlClientConfig.api.AxolotlClientConfig;
import io.github.axolotlclient.AxolotlClientConfig.api.manager.ConfigManager;
import io.github.axolotlclient.AxolotlClientConfig.api.options.OptionCategory;
import io.github.axolotlclient.AxolotlClientConfig.impl.managers.VersionedJsonConfigManager;
import io.github.axolotlclient.AxolotlClientConfig.impl.options.BooleanOption;
import io.github.axolotlclient.AxolotlClientConfig.impl.util.ConfigStyles;
import io.github.axolotlclient.waypoints.map.Minimap;
import io.github.axolotlclient.waypoints.map.WorldMapScreen;
import io.github.axolotlclient.waypoints.waypoints.Waypoint;
import io.github.axolotlclient.waypoints.waypoints.WaypointRenderer;
import io.github.axolotlclient.waypoints.waypoints.WaypointStorage;
import io.github.axolotlclient.waypoints.waypoints.gui.CreateWaypointScreen;
import io.github.axolotlclient.waypoints.waypoints.gui.WaypointsScreen;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.locale.I18n;
import net.minecraft.resource.Identifier;
import net.ornithemc.osl.keybinds.api.KeyBindingEvents;
import net.ornithemc.osl.lifecycle.api.client.MinecraftClientEvents;
import org.lwjgl.input.Keyboard;

@Slf4j
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

	private final KeyBinding map = new KeyBinding(MODID + ".world_map", Keyboard.KEY_M, MODID);
	private final KeyBinding manageWaypoints = new KeyBinding(MODID + ".waypoints_menu", Keyboard.KEY_K, MODID);
	private final KeyBinding newWaypoint = new KeyBinding(MODID + ".create_waypoint", Keyboard.KEY_N, MODID);

	@Override
	public void onInitializeClient() {
		MINIMAP.init();

		category.add(waypoints);
		waypoints.add(renderWaypoints, renderWaypointsInWorld, renderOutOfViewWaypointsOnScreenEdge);

		try {
			Files.createDirectories(FabricLoader.getInstance().getConfigDir().resolve(MODID));
		} catch (IOException e) {
			log.warn("Failed to create config dir, options may not save correctly!", e);
		}
		ConfigManager configManager;
		AxolotlClientConfig.getInstance().register(configManager = new VersionedJsonConfigManager(OPTIONS_PATH, category, 1,
			(oldVersion, newVersion, rootCategory, json) -> json));
		configManager.load();
		configManager.save();

		KeyBindingEvents.REGISTER_KEYBINDS.register(reg -> {
			reg.register(map);
			reg.register(manageWaypoints);
			reg.register(newWaypoint);
		});
		MinecraftClientEvents.TICK_END.register(mc -> {
			if (map.consumeClick()) {
				mc.openScreen(new WorldMapScreen());
			} else if (manageWaypoints.consumeClick()) {
				mc.openScreen(new WaypointsScreen(mc.screen));
			} else if (newWaypoint.consumeClick()) {
				mc.openScreen(new CreateWaypointScreen(mc.screen));
			}
		});
	}

	public static Screen createOptionsScreen(Screen parent) {
		return ConfigStyles.createScreen(parent, category);
	}

	public static Identifier rl(String path) {
		return new Identifier(MODID, path);
	}

	public static String tr(String key, Object... args) {
		return I18n.translate(MODID + "." + key, args);
	}

	public static String tra(String key) {
		return I18n.translate(MODID + "." + key);
	}

	public static List<Waypoint> getCurrentWaypoints() {
		return getCurrentWaypoints(true);
	}

	public static List<Waypoint> getCurrentWaypoints(boolean dimension) {
		var mc = Minecraft.getInstance();
		var player = mc.player;
		return WAYPOINT_STORAGE.getCurrentlyAvailableWaypoints(dimension ? String.valueOf(mc.world.dimension.getName()) : null).stream()
			.sorted(Comparator.comparingDouble(w -> w.squaredDistTo(player.x, player.y, player.z)))
			.toList();
	}

	private static String getB64(String s) {
		return new String(Base64.getUrlEncoder().encode(s.getBytes(StandardCharsets.UTF_8)));
	}

	public static Path getCurrentStorageDir() {
		var mc = Minecraft.getInstance();
		return getCurrentWorldStorageDir()
			.resolve(getB64(mc.world.dimension.getName()));
	}

	public static Path getCurrentWorldStorageDir() {
		var mc = Minecraft.getInstance();
		String str;
		if (mc.getServer() == null) {
			str = mc.getCurrentServerEntry().address;
		} else {
			str = mc.getServer().getWorldSaveName();
		}
		return AxolotlClientWaypoints.MOD_STORAGE_DIR.resolve(getB64(str));
	}

	public static boolean playerHasOp() {
		return Minecraft.getInstance().getServer().getPlayerManager().isOp(Minecraft.getInstance().player.getGameProfile());
	}
}
