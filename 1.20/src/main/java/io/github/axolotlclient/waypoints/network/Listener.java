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

package io.github.axolotlclient.waypoints.network;

import java.util.ArrayList;
import java.util.List;

import io.github.axolotlclient.waypoints.AxolotlClientWaypoints;
import io.github.axolotlclient.waypoints.map.WorldMapScreen;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

@Slf4j
public class Listener {
	public List<Runnable> postReceive = new ArrayList<>();
	private boolean receivedPayload;

	public void init() {
		ClientPlayConnectionEvents.INIT.register((handler, client) ->
			ClientPlayNetworking.registerGlobalReceiver(Payload.CODEC, (payload, context, s) -> receiveConfig(payload)));
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (receivedPayload) return;
			String msg = message.getString();
			switch (msg) {
				case "§f§a§i§r§x§a§e§r§o" -> disableCaves();
				case "§x§a§e§r§o§w§m§n§e§t§h§e§r§i§s§f§a§i§r" -> WorldMapScreen.allowCavesNether = true;
				case "§n§o§m§i§n§i§m§a§p" -> disableMinimap();
				case "§r§e§s§e§t§x§a§e§r§o" -> reset();
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			reset();
			receivedPayload = false;
		});
	}

	private void receiveConfig(Payload payload) {
		receivedPayload = true;
		if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
			log.info("Received server configuration: {}", payload);
		}
		reset();
		if (payload.disableMinimap()) {
			disableMinimap();
		}
		if (payload.disableMinimapCaves()) {
			AxolotlClientWaypoints.MINIMAP.allowCaves = false;
		}
		if (payload.disableWorldmapCaves()) {
			WorldMapScreen.allowCaves = false;
			if (payload.allowWorldmapCavesNether()) {
				WorldMapScreen.allowCavesNether = true;
			}
		}
		postReceive.forEach(Runnable::run);
	}

	private void disableMinimap() {
		AxolotlClientWaypoints.MINIMAP.enabled.force(true, false);
	}

	private void disableCaves() {
		AxolotlClientWaypoints.MINIMAP.allowCaves = false;
		WorldMapScreen.allowCaves = true;
	}

	private void reset() {
		AxolotlClientWaypoints.MINIMAP.enabled.force(false, false);
		AxolotlClientWaypoints.MINIMAP.allowCaves = true;
		WorldMapScreen.allowCaves = true;
		WorldMapScreen.allowCavesNether = false;
	}

}
