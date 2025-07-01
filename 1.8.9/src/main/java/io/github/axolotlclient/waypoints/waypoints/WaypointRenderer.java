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

package io.github.axolotlclient.waypoints.waypoints;

import java.util.concurrent.atomic.AtomicBoolean;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.axolotlclient.waypoints.AxolotlClientWaypoints;
import io.github.axolotlclient.waypoints.mixin.GameRendererAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

public class WaypointRenderer {

	private static final double CUTOFF_DIST = 12;
	private final Minecraft minecraft = Minecraft.getInstance();
	private final Matrix4f view = new Matrix4f();
	private final Vector4f viewProj = new Vector4f();


	public void render(DeltaTracker deltaTracker) {
		if (!AxolotlClientWaypoints.renderWaypoints.get()) return;
		if (!AxolotlClientWaypoints.renderWaypointsInWorld.get()) return;
		if (minecraft.level == null) return;
		var profiler = Minecraft.getInstance().getProfiler();
		profiler.popPush("waypoints");

		MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
		var stack = new PoseStack();
		var cam = minecraft.gameRenderer.getMainCamera();

		stack.pushPose();
		stack.mulPose(cam.rotation().invert());
		var camPos = minecraft.gameRenderer.getMainCamera().getPosition();
		var _x = deltaTracker;

		for (Waypoint waypoint : AxolotlClientWaypoints.getCurrentWaypoints()) {
			if (waypoint.closerToThan(camPos.x(), camPos.y(), camPos.z(), CUTOFF_DIST / minecraft.getWindow().getGuiScale())) {
				profiler.push(waypoint.name());
				renderWaypoint(waypoint, stack, camPos, cam, bufferSource);
				profiler.pop();
			}
		}

		stack.popPose();
		bufferSource.endLastBatch();
		if (!stack.clear()) {
			throw new IllegalStateException("Pose stack not empty");
		}
	}

	private void renderWaypoint(Waypoint waypoint, PoseStack stack, Vec3 camPos, Camera cam, MultiBufferSource.BufferSource bufferSource) {
		stack.pushPose();
		stack.translate(waypoint.x() - camPos.x(), waypoint.y() - camPos.y(), waypoint.z() - camPos.z());
		stack.mulPose(cam.rotation().invert(new Quaternionf()));
		float scale = 0.04F;
		stack.scale(scale, -scale, scale);
		int textWidth = minecraft.font.width(waypoint.display());
		int width = textWidth + Waypoint.displayXOffset() * 2;
		int textHeight = minecraft.font.lineHeight;
		int height = textHeight + Waypoint.displayYOffset() * 2;
		drawFontBatch(waypoint.display(), -textWidth / 2f, -textHeight / 2f, stack.last().pose(), bufferSource);
		fillRect(stack, bufferSource, -width / 2f, -height / 2f, -0.1f, width / 2f, height / 2f, waypoint.color().toInt());
		stack.popPose();
	}

	private void fillRect(PoseStack stack, MultiBufferSource.BufferSource source, float x, float y, float z, float x2, float y2, int color) {
		var buf = source.getBuffer(RenderType.gui());
		var matrix = stack.last().pose();
		buf.addVertex(matrix, x, y, z).setColor(color);
		buf.addVertex(matrix, x, y2, z).setColor(color);
		buf.addVertex(matrix, x2, y2, z).setColor(color);
		buf.addVertex(matrix, x2, y, z).setColor(color);
	}

	private void drawFontBatch(String text, float x, float y, Matrix4f matrix, MultiBufferSource bufferSource) {
		minecraft.font.drawInBatch(text, x, y, -1, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
	}

	public void renderWaypoints(GuiGraphics graphics, DeltaTracker deltaTracker) {
		if (!AxolotlClientWaypoints.renderWaypoints.get()) return;
		if (!AxolotlClientWaypoints.renderWaypointsInWorld.get()) return;
		var profiler = Minecraft.getInstance().getProfiler();
		var cam = minecraft.gameRenderer.getMainCamera();
		profiler.push("waypoints");

		graphics.pose().pushPose();
		var positionDrawn = new AtomicBoolean();
		for (Waypoint waypoint : AxolotlClientWaypoints.getCurrentWaypoints()) {
			graphics.pose().pushPose();
			renderWaypoint(waypoint, graphics, deltaTracker, cam, positionDrawn);
			graphics.pose().popPose();
		}
		graphics.pose().popPose();
		profiler.pop();
	}

	private void renderWaypoint(Waypoint waypoint, GuiGraphics graphics, DeltaTracker tracker, Camera camera, AtomicBoolean positionDrawn) {
		var tick = tracker.getGameTimeDeltaPartialTick(true);
		var fov = ((GameRendererAccessor) minecraft.gameRenderer).invokeGetFov(camera, tick, true);
		var pose = graphics.pose();

		var textWidth = minecraft.font.width(waypoint.display());
		int width = textWidth + Waypoint.displayXOffset() * 2;
		int textHeight = minecraft.font.lineHeight;
		int height = textHeight + Waypoint.displayYOffset() * 2;

		viewProj.set(waypoint.x(), waypoint.y(), waypoint.z(), 1);
		view.rotation(camera.rotation()).translate(camera.getPosition().toVector3f().negate());
		graphics.drawString(minecraft.font, ""+camera.rotation(), 20, 20, -1);

		Matrix4f projection = minecraft.gameRenderer.getProjectionMatrix(fov);
		projection.mul(view);
		viewProj.mul(projection);

		var camPos = camera.getPosition();

		if (AxolotlClientWaypoints.renderOutOfViewWaypointsOnScreenEdge.get()) {
			viewProj.w = Math.max(Math.abs(viewProj.x()), Math.max(Math.abs(viewProj.y()), viewProj.w()));
		}

		if (viewProj.w() <= 0) {
			return;
		}
		viewProj.div(viewProj.w());

		float projX = viewProj.x();
		float projY = viewProj.y();

		//float x = (graphics.guiWidth()/2f) + ((graphics.guiWidth() - width) * (viewProj.x() / 2f));
		float x = 0.5f * (graphics.guiWidth() * (projX + 1) - width * projX);
		//float y = graphics.guiHeight() - (graphics.guiHeight()/2f + (graphics.guiHeight()-height) * (viewProj.y() / 2f));
		float y = graphics.guiHeight() * (0.5f - projY / 2) + (height * projY) / 2f;

		pose.translate(x, y, 0);

		if (!AxolotlClientWaypoints.renderOutOfViewWaypointsOnScreenEdge.get() && (x < -width / 2f || x > graphics.guiWidth() + width / 2f || y < -height / 2f || y > graphics.guiHeight() + height / 2f)) {
			return;
		}
		if (waypoint.closerToThan(camPos.x(), camPos.y(), camPos.z(), CUTOFF_DIST / minecraft.getWindow().getGuiScale())) {
			return;
		}

		if (!positionDrawn.get() && Math.abs(x - graphics.guiWidth() / 2f) < 8 && Math.abs(y - graphics.guiHeight() / 2f) < 8) {
			positionDrawn.set(true);
			pose.pushPose();
			pose.translate(0, height / 2f + 2, 0);
			var line1 = waypoint.name();
			graphics.drawString(minecraft.font, line1, -minecraft.font.width(line1) / 2, 0, -1, false);
			pose.translate(0, minecraft.font.lineHeight + 2, 0);
			var line2 = AxolotlClientWaypoints.tr("distance", "%.2f".formatted(waypoint.distTo(camPos.x(), camPos.y(), camPos.z())));
			graphics.drawString(minecraft.font, line2, -minecraft.font.width(line2) / 2, 0, -1, false);
			pose.popPose();
		}

		graphics.fill(-width / 2, -height / 2, width / 2, height / 2, waypoint.color().toInt());
		graphics.drawString(minecraft.font, waypoint.display(), -textWidth / 2, -textHeight / 2, -1, false);
	}
}
