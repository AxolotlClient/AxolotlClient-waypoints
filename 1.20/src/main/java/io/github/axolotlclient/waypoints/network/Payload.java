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
