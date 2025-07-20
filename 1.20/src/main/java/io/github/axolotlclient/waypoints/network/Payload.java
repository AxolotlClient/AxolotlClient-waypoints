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

import io.github.axolotlclient.waypoints.AxolotlClientWaypointsCommon;
import lombok.With;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

@With
public record Payload(boolean disableMinimapCaves, boolean disableWorldmapCaves,
					  boolean disableMinimap, boolean allowWorldmapCavesNether) implements FabricPacket {
	public static final ResourceLocation TYPE = new ResourceLocation(AxolotlClientWaypointsCommon.MODID, "options");
	public static final PacketType<Payload> CODEC = PacketType.create(TYPE, Payload::new);

	private Payload(FriendlyByteBuf buf) {
		this(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
	}

	public Payload() {
		this(false, true, true, false);
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeBoolean(disableMinimapCaves);
		buf.writeBoolean(disableWorldmapCaves);
		buf.writeBoolean(disableMinimap);
		buf.writeBoolean(allowWorldmapCavesNether);
	}

	@Override
	public @NotNull PacketType<? extends FabricPacket> getType() {
		return CODEC;
	}
}
