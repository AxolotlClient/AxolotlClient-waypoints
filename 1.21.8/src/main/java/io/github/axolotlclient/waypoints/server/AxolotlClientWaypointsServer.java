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

package io.github.axolotlclient.waypoints.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.axolotlclient.waypoints.AxolotlClientWaypointsCommon;
import io.github.axolotlclient.waypoints.network.Payload;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;

@Slf4j
public class AxolotlClientWaypointsServer implements DedicatedServerModInitializer {
	private static final Path OPTIONS_PATH = AxolotlClientWaypointsCommon.OPTIONS_PATH.resolveSibling("server_options.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private Payload options;
	private static final int PERMISSION_LEVEL = 3;

	@Override
	public void onInitializeServer() {
		load();
		PayloadTypeRegistry.configurationS2C().register(Payload.TYPE, Payload.CODEC);
		PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
		CommandRegistrationCallback.EVENT.register((commandDispatcher, commandBuildContext, commandSelection) -> {
			var modifyConfig = Commands.literal("modify_config")
				.requires(Commands.hasPermission(PERMISSION_LEVEL));

			for (var m : Payload.class.getMethods()) {
				if (m.getParameterCount() != 1) continue;
				if (m.getParameterTypes()[0] != boolean.class) continue;
				if (Modifier.isStatic(m.getModifiers())) continue;
				for (Field f : Payload.class.getDeclaredFields()) {
					if (m.getName().contains(Character.toUpperCase(f.getName().charAt(0)) + f.getName().substring(1))) {
						modifyConfig.then(Commands.literal(f.getName()).then(Commands.argument(f.getName(), BoolArgumentType.bool()).executes(c -> {
							if (options == null) {
								options = new Payload();
							}
							try {
								options = (Payload) m.invoke(options, BoolArgumentType.getBool(c, f.getName()));
							} catch (IllegalAccessException | InvocationTargetException e) {
								throw new RuntimeException(e);
							}

							save();
							reconfigure(c);
							return 0;
						})));
					}
				}
			}

			commandDispatcher.register(Commands.literal(AxolotlClientWaypointsCommon.MODID)
				.then(Commands.literal("reload")
					.requires(Commands.hasPermission(PERMISSION_LEVEL)).executes(c -> {
						load();
						c.getSource().sendSuccess(() -> tr("reload_complete"), true);
						reconfigure(c);
						return 0;
					}))
				.then(Commands.literal("show_config")
					.executes(c -> {
						if (options != null) {
							c.getSource().sendSuccess(() -> tr("current_config", GSON.toJson(options)), true);
						} else {
							c.getSource().sendFailure(tr("current_config_unavailable"));
						}
						return 0;
					}))
				.then(modifyConfig));
		});
		if (options == null) {
			log.warn("Not sending options because we failed to read or instantiate them");
		}
		ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
			if (options != null) {
				handler.send(new ClientboundCustomPayloadPacket(options));
			}
		});
	}

	private void reconfigure(CommandContext<CommandSourceStack> c) {
		c.getSource().getServer().getPlayerList().getPlayers().forEach(p ->
			p.connection.send(new ClientboundCustomPayloadPacket(options)));
	}

	private void load() {
		try {
			readOrGenerateConfig();
		} catch (IOException e) {
			log.warn("Failed to read config! Will not register or send packet to clients!", e);
		}
	}

	private void save() {
		try {
			try (BufferedWriter writer = Files.newBufferedWriter(OPTIONS_PATH)) {
				GSON.toJson(options, writer);
			}
		} catch (IOException e) {
			log.warn("Failed to save config!", e);
		}
	}

	private void readOrGenerateConfig() throws IOException {
		var path = OPTIONS_PATH;
		if (Files.exists(path)) {
			try (BufferedReader reader = Files.newBufferedReader(path)) {
				options = GSON.fromJson(reader, Payload.class);
			}
		} else {
			options = new Payload();
			save();
		}
	}

	private static Component tr(String key, Object... args) {
		return Component.translatable(AxolotlClientWaypointsCommon.MODID + "." + key, args);
	}
}
