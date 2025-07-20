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
