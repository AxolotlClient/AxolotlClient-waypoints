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

import java.util.Objects;

import lombok.With;
import net.minecraft.network.PacketByteBuf;
import net.ornithemc.osl.networking.api.CustomPayload;

@With
public final class Payload implements CustomPayload {
	public static final String CHANNEL = "AXOWP|options";
	private boolean disableMinimapCaves;
	private boolean disableWorldmapCaves;
	private boolean disableMinimap;
	private boolean allowWorldmapCavesNether;

	public Payload(boolean disableMinimapCaves, boolean disableWorldmapCaves,
				   boolean disableMinimap, boolean allowWorldmapCavesNether) {
		this.disableMinimapCaves = disableMinimapCaves;
		this.disableWorldmapCaves = disableWorldmapCaves;
		this.disableMinimap = disableMinimap;
		this.allowWorldmapCavesNether = allowWorldmapCavesNether;
	}

	public Payload() {
		this(false, true, true, false);
	}

	public void write(PacketByteBuf buf) {
		buf.writeBoolean(disableMinimapCaves);
		buf.writeBoolean(disableWorldmapCaves);
		buf.writeBoolean(disableMinimap);
		buf.writeBoolean(allowWorldmapCavesNether);
	}

	public void read(PacketByteBuf buf) {
		this.disableMinimapCaves = buf.readBoolean();
		this.disableWorldmapCaves = buf.readBoolean();
		this.disableMinimap = buf.readBoolean();
		this.allowWorldmapCavesNether = buf.readBoolean();
	}

	public boolean disableMinimapCaves() {
		return disableMinimapCaves;
	}

	public boolean disableWorldmapCaves() {
		return disableWorldmapCaves;
	}

	public boolean disableMinimap() {
		return disableMinimap;
	}

	public boolean allowWorldmapCavesNether() {
		return allowWorldmapCavesNether;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (Payload) obj;
		return this.disableMinimapCaves == that.disableMinimapCaves &&
			this.disableWorldmapCaves == that.disableWorldmapCaves &&
			this.disableMinimap == that.disableMinimap &&
			this.allowWorldmapCavesNether == that.allowWorldmapCavesNether;
	}

	@Override
	public int hashCode() {
		return Objects.hash(disableMinimapCaves, disableWorldmapCaves, disableMinimap, allowWorldmapCavesNether);
	}

	@Override
	public String toString() {
		return "Payload[" +
			"disableMinimapCaves=" + disableMinimapCaves + ", " +
			"disableWorldmapCaves=" + disableWorldmapCaves + ", " +
			"disableMinimap=" + disableMinimap + ", " +
			"allowWorldmapCavesNether=" + allowWorldmapCavesNether + ']';
	}

}
