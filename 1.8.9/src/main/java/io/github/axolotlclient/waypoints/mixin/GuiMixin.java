/*
 * Copyright © 2025 moehreag <moehreag@gmail.com> & Contributors
 *
 * This file is part of AxolotlClient.
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

import io.github.axolotlclient.waypoints.AxolotlClientWaypoints;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {


	@Inject(method = "<init>", at = @At("TAIL"))
	private void register(Minecraft minecraft, CallbackInfo ci) {
		AxolotlClientWaypoints.MINIMAP.setup();
	}

	@Inject(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getSelected()Lnet/minecraft/world/item/ItemStack;"))
	private void updateMinimap(CallbackInfo ci) {
		AxolotlClientWaypoints.MINIMAP.updateMapView();
	}

	@Inject(method = "renderCameraOverlays", at = @At("TAIL"))
	private void renderHud(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		AxolotlClientWaypoints.WAYPOINT_RENDERER.renderWaypoints(guiGraphics, deltaTracker);
		AxolotlClientWaypoints.MINIMAP.renderMapOverlay(guiGraphics, deltaTracker);
	}
}
