package io.github.axolotlclient.waypoints.mixin;

import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(World.class)
public interface LevelAccessor {

	@Invoker("isChunkLoadedAt")
	boolean invokeChunkLoadedAt(int x, int z, boolean force);
}
