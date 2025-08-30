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

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.axolotlclient.waypoints.AxolotlClientWaypoints;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlStateManager.class)
public class GlStateManagerMixin {

	@Unique
	private static int currentMatrixMode;

	@Inject(method = "matrixMode", at = @At("HEAD"))
	private static void jomlMatrix$mode(int i, CallbackInfo ci) {
		currentMatrixMode = i;
	}

	@Inject(method = "pushMatrix", at = @At("HEAD"))
	private static void jomlMatrix$push(CallbackInfo ci) {
		if (currentMatrixMode != GL11.GL_MODELVIEW) return;
		AxolotlClientWaypoints.MATRIX_STACK.pushMatrix();
	}

	@Inject(method = "popMatrix", at = @At("HEAD"))
	private static void jomlMatrix$pop(CallbackInfo ci) {
		if (currentMatrixMode != GL11.GL_MODELVIEW) return;
		AxolotlClientWaypoints.MATRIX_STACK.popMatrix();
	}

	@Inject(method = "translatef", at = @At("HEAD"))
	private static void jomlMatrix$translatef(float x, float y, float z, CallbackInfo ci) {
		if (currentMatrixMode != GL11.GL_MODELVIEW) return;
		AxolotlClientWaypoints.MATRIX_STACK.translate(x, y, z);
	}

	@Inject(method = "translated", at = @At("HEAD"))
	private static void jomlMatrix$translated(double x, double y, double z, CallbackInfo ci) {
		if (currentMatrixMode != GL11.GL_MODELVIEW) return;
		AxolotlClientWaypoints.MATRIX_STACK.translate((float) x, (float) y, (float) z);
	}

	@Inject(method = "rotatef", at = @At("HEAD"))
	private static void jomlMatrix$rotatef(float ang, float x, float y, float z, CallbackInfo ci) {
		if (currentMatrixMode != GL11.GL_MODELVIEW) return;
		AxolotlClientWaypoints.MATRIX_STACK.rotate(ang, x, y, z);
	}

	@Inject(method = "scalef", at = @At("HEAD"))
	private static void jomlMatrix$scalef(float x, float y, float z, CallbackInfo ci) {
		if (currentMatrixMode != GL11.GL_MODELVIEW) return;
		AxolotlClientWaypoints.MATRIX_STACK.scale(x, y, z);
	}

	@Inject(method = "scaled", at = @At("HEAD"))
	private static void jomlMatrix$scaled(double x, double y, double z, CallbackInfo ci) {
		if (currentMatrixMode != GL11.GL_MODELVIEW) return;
		AxolotlClientWaypoints.MATRIX_STACK.scale((float) x, (float) y, (float) z);
	}

	@Inject(method = "loadIdentity", at = @At("HEAD"))
	private static void jomlMatrix$identity(CallbackInfo ci) {
		if (currentMatrixMode != GL11.GL_MODELVIEW) return;
		AxolotlClientWaypoints.MATRIX_STACK.identity();
	}
}
