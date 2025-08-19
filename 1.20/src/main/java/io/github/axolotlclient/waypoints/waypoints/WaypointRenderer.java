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

package io.github.axolotlclient.waypoints.waypoints;

import java.util.concurrent.atomic.AtomicReference;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.axolotlclient.AxolotlClientConfig.api.util.Colors;
import io.github.axolotlclient.waypoints.AxolotlClientWaypoints;
import io.github.axolotlclient.waypoints.mixin.GameRendererAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class WaypointRenderer {

	private static final double CUTOFF_DIST = 12;
	private final Minecraft minecraft = Minecraft.getInstance();
	private final Matrix4f view = new Matrix4f();
	private final Vector4f viewProj = new Vector4f();

	public void render(PoseStack stack, MultiBufferSource.BufferSource source, float tick) {
		var profiler = Minecraft.getInstance().getProfiler();
		profiler.popPush("waypoints");
		stack.pushPose();
		var cam = minecraft.gameRenderer.getMainCamera();
		var camPos = cam.getPosition();
		minecraft.gameRenderer.resetProjectionMatrix(minecraft.gameRenderer.getProjectionMatrix(((GameRendererAccessor) minecraft.gameRenderer).invokeGetFov(cam, 0, false)));
		RenderSystem.enableBlend();
		float fov = (float) ((GameRendererAccessor) minecraft.gameRenderer).invokeGetFov(cam, tick, true);

		for (Waypoint waypoint : AxolotlClientWaypoints.getCurrentWaypoints()) {
			if (waypoint.closerToThan(camPos.x(), camPos.y(), camPos.z(), CUTOFF_DIST / minecraft.getWindow().getGuiScale())) {
				profiler.push(waypoint.name());
				renderWaypoint(waypoint, stack, camPos, cam, source, fov);
				profiler.pop();
			}
		}

		stack.popPose();
		if (!stack.clear()) {
			throw new IllegalStateException("Pose stack not empty");
		}
	}

	private void renderWaypoint(Waypoint waypoint, PoseStack stack, Vec3 camPos, Camera cam, MultiBufferSource.BufferSource bufferSource, float fov) {
		int textWidth = minecraft.font.width(waypoint.display());
		int width = textWidth + Waypoint.displayXOffset() * 2;
		int textHeight = minecraft.font.lineHeight;
		int height = textHeight + Waypoint.displayYOffset() * 2;
		var displayStart = projectToScreen(cam, fov, width, height, waypoint.x() - (width / 2f * 0.04), waypoint.y() - (height / 2f * 0.04), waypoint.z());
		if (displayStart == null) return;
		var displayEnd = projectToScreen(cam, fov, width, height, waypoint.x() + (width / 2f * 0.04), waypoint.y() + (height / 2f * 0.04), waypoint.z());
		if (displayEnd == null) return;
		float projWidth = Math.abs(displayEnd.x() - displayStart.x());
		float projHeight = Math.abs(displayEnd.y() - displayStart.y());
		if (projWidth / width <= 1 && projHeight / height <= 1) {
			return;
		}
		stack.pushPose();
		stack.translate(waypoint.x() - camPos.x(), waypoint.y() - camPos.y(), waypoint.z() - camPos.z());
		stack.mulPose(cam.rotation());
		float scale = 0.04F;
		stack.scale(-scale, -scale, scale);
		//fillRect(stack, bufferSource, -width / 2f, -height / 2f, 0f, width / 2f, height / 2f, waypoint.color().toInt());
		drawFontBatch(waypoint.display(), -textWidth / 2f, -textHeight / 2f, stack.last().pose(), bufferSource, waypoint.color().toInt());
		stack.popPose();
	}

	private void fillRect(PoseStack stack, MultiBufferSource.BufferSource source, float x, float y, float z, float x2, float y2, int color) {
		var buf = source.getBuffer(RenderType.textBackgroundSeeThrough());
		var matrix = stack.last().pose();
		buf.vertex(matrix, x, y, z).color(color).uv2(0xF000F0).endVertex();
		buf.vertex(matrix, x, y2, z).color(color).uv2(0xF000F0).endVertex();
		buf.vertex(matrix, x2, y2, z).color(color).uv2(0xF000F0).endVertex();
		buf.vertex(matrix, x2, y, z).color(color).uv2(0xF000F0).endVertex();
	}

	private void drawFontBatch(String text, float x, float y, Matrix4f matrix, MultiBufferSource bufferSource, int bgColor) {
		minecraft.font.drawInBatch(text, x, y, -1, false, matrix, bufferSource, Font.DisplayMode.NORMAL, bgColor, 0xF000F0);
	}

	public void renderWaypoints(GuiGraphics graphics, float delta) {
		if (!AxolotlClientWaypoints.renderWaypoints.get()) return;
		if (!AxolotlClientWaypoints.renderWaypointsInWorld.get()) return;
		var profiler = Minecraft.getInstance().getProfiler();
		var cam = minecraft.gameRenderer.getMainCamera();
		profiler.push("waypoints");

		graphics.pose().pushPose();
		graphics.pose().translate(0.0F, 0.0F, -100.0F);
		var positionDrawer = new AtomicReference<Runnable>();
		for (Waypoint waypoint : AxolotlClientWaypoints.getCurrentWaypoints()) {
			graphics.pose().pushPose();
			renderWaypoint(waypoint, graphics, delta, cam, positionDrawer);
			graphics.pose().popPose();
		}
		if (positionDrawer.get() != null) {
			positionDrawer.get().run();
		}
		graphics.pose().popPose();
		profiler.pop();
	}

	private void renderWaypoint(Waypoint waypoint, GuiGraphics graphics, float tick, Camera camera, AtomicReference<Runnable> positionDrawn) {
		var fov = ((GameRendererAccessor) minecraft.gameRenderer).invokeGetFov(camera, tick, true);
		var pose = graphics.pose();

		var textWidth = minecraft.font.width(waypoint.display());
		int width = textWidth + Waypoint.displayXOffset() * 2;
		int textHeight = minecraft.font.lineHeight;
		int height = textHeight + Waypoint.displayYOffset() * 2;
		var camPos = camera.getPosition();

		var displayStart = projectToScreen(camera, fov, width, height, waypoint.x() - (width / 2f * 0.04), waypoint.y() - (height / 2f * 0.04), waypoint.z());
		if (displayStart == null) return;
		var displayEnd = projectToScreen(camera, fov, width, height, waypoint.x() + (width / 2f * 0.04), waypoint.y() + (height / 2f * 0.04), waypoint.z());
		if (displayEnd == null) return;
		float projWidth = Math.abs(displayEnd.x() - displayStart.x());
		float projHeight = Math.abs(displayEnd.y() - displayStart.y());
		Result result = projectToScreen(camera, fov, width, height, waypoint.x(), waypoint.y(), waypoint.z());
		if (result == null) return;

		pose.translate(result.x(), result.y(), 0);

		if (!AxolotlClientWaypoints.renderOutOfViewWaypointsOnScreenEdge.get() && (result.x() < -width / 2f || result.x() > graphics.guiWidth() + width / 2f || result.y() < -height / 2f || result.y() > graphics.guiHeight() + height / 2f)) {
			return;
		}
		if (projWidth / width > 1 || projHeight / height > 1) {
			return;
		}

		if (positionDrawn.get() == null && Math.abs(result.x() - graphics.guiWidth() / 2f) < width / 2f && Math.abs(result.y() - graphics.guiHeight() / 2f) < height / 2f) {
			pose.pushPose();
			pose.translate(0, height / 2f + 2, 0);
			var pos = pose.last().pose().transformPosition(new Vector3f());
			positionDrawn.set(() -> {
				var line1 = waypoint.name();
				pose.pushPose();
				pose.last().pose().translate(pos);
				int line1W = minecraft.font.width(line1);
				graphics.fill(-line1W / 2 - 2, -2, line1W / 2 + 2, minecraft.font.lineHeight + 2, Colors.GRAY.withAlpha(100).toInt());
				graphics.renderOutline(-line1W / 2 - 2, -2, line1W + 4, minecraft.font.lineHeight + 4, Colors.GRAY.toInt());
				graphics.drawString(minecraft.font, line1, -line1W / 2, 0, -1, true);
				if (!waypoint.closerToThan(camPos.x(), camPos.y(), camPos.z(), CUTOFF_DIST)) {
					pose.translate(0, minecraft.font.lineHeight + 4, 0);
					var line2 = AxolotlClientWaypoints.tr("distance", "%.2f".formatted(waypoint.distTo(camPos.x(), camPos.y(), camPos.z())));
					graphics.drawString(minecraft.font, line2, -minecraft.font.width(line2) / 2, 0, -1, false);
				}
				pose.popPose();
			});
			pose.popPose();
		}

		graphics.fill(-width / 2, -height / 2, width / 2, height / 2, waypoint.color().toInt());
		graphics.drawString(minecraft.font, waypoint.display(), -textWidth / 2, -textHeight / 2, -1, false);
	}

	private @Nullable Result projectToScreen(Camera camera, double fov, int width, int height, double x, double y, double z) {
		viewProj.set(x, y, z, 1);
		view.rotation(camera.rotation().rotateY((float) -(Math.PI), new Quaternionf()).invert()).translate(camera.getPosition().toVector3f().negate());

		Matrix4f projection = minecraft.gameRenderer.getProjectionMatrix(fov);
		projection.mul(view);
		viewProj.mul(projection);


		if (AxolotlClientWaypoints.renderOutOfViewWaypointsOnScreenEdge.get()) {
			viewProj.w = Math.max(Math.abs(viewProj.x()), Math.max(Math.abs(viewProj.y()), viewProj.w()));
		}

		if (viewProj.w() <= 0) {
			return null;
		}
		viewProj.div(viewProj.w());

		float projX = viewProj.x();
		float projY = viewProj.y();

		//float x = (graphics.guiWidth()/2f) + ((graphics.guiWidth() - width) * (viewProj.x() / 2f));
		float resultX = 0.5f * (minecraft.getWindow().getGuiScaledWidth() * (projX + 1) - width * projX);
		//float y = graphics.guiHeight() - (graphics.guiHeight()/2f + (graphics.guiHeight()-height) * (viewProj.y() / 2f));
		float resultY = minecraft.getWindow().getGuiScaledWidth() * (0.5f - projY / 2) + (height * projY) / 2f;
		return new Result(resultX, resultY);
	}

	private record Result(float x, float y) {
	}
}
