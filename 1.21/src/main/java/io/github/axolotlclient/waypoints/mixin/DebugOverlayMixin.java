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

package io.github.axolotlclient.waypoints.mixin;

import java.util.List;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.axolotlclient.waypoints.AxolotlClientWaypoints;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DebugScreenOverlay.class)
public class DebugOverlayMixin {

	@Inject(method = "getGameInformation", at = @At("TAIL"))
	private void addMinimapDebugInformation(CallbackInfoReturnable<List<String>> cir, @Local List<String> lines) {
		if (AxolotlClientWaypoints.MINIMAP.isEnabled()) {
			lines.add("Minimap Update Time: %.2f ms".formatted(AxolotlClientWaypoints.MINIMAP.updateDuration / 1000_000f));
		}
		lines.add("Waypoint counts: Total: %d Current: %d".formatted(AxolotlClientWaypoints.WAYPOINT_STORAGE.getWaypointCount(), AxolotlClientWaypoints.getCurrentWaypoints().size()));
	}
}
