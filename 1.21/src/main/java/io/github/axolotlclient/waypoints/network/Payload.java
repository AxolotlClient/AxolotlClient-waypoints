package io.github.axolotlclient.waypoints.network;

import io.github.axolotlclient.waypoints.AxolotlClientWaypointsCommon;
import lombok.With;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

@With
public record Payload(boolean disableMinimapCaves, boolean disableWorldmapCaves,
					  boolean disableMinimap, boolean allowWorldmapCavesNether) implements CustomPacketPayload {
	public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AxolotlClientWaypointsCommon.MODID, "options"));
	public static final StreamCodec<FriendlyByteBuf, Payload> CODEC = CustomPacketPayload.codec(Payload::write, Payload::new);

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
	public @NotNull Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
