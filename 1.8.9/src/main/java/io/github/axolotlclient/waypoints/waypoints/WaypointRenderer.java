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

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.Tessellator;
import io.github.axolotlclient.waypoints.AxolotlClientWaypoints;
import io.github.axolotlclient.waypoints.mixin.GameRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiElement;
import net.minecraft.client.render.Window;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

public class WaypointRenderer {

	private static final double CUTOFF_DIST = 12;
	private final Minecraft minecraft = Minecraft.getInstance();
	private final Matrix4f view = new Matrix4f();
	private final Vector4f viewProj = new Vector4f();


	public void render() {
		if (!AxolotlClientWaypoints.renderWaypoints.get()) return;
		if (!AxolotlClientWaypoints.renderWaypointsInWorld.get()) return;
		if (minecraft.world == null) return;
		var profiler = Minecraft.getInstance().profiler;
		profiler.swap("waypoints");

		var cam = minecraft.getCamera();

		GlStateManager.pushMatrix();
		var camPos = cam.getSourcePos();
		var guiScale = new Window(minecraft).getScale();

		for (Waypoint waypoint : AxolotlClientWaypoints.getCurrentWaypoints()) {
			if (waypoint.closerToThan(camPos.x, camPos.y, camPos.z, CUTOFF_DIST / guiScale)) {
				profiler.push(waypoint.name());
				renderWaypoint(waypoint, camPos, cam);
				profiler.pop();
			}
		}

		GlStateManager.popMatrix();
	}

	private void renderWaypoint(Waypoint waypoint, Vec3d camPos, Entity cam) {
		GlStateManager.pushMatrix();
		GlStateManager.translated(waypoint.x() - camPos.x, waypoint.y() - camPos.y, waypoint.z() - camPos.z);
		var dispatcher = minecraft.getEntityRenderDispatcher();
		GlStateManager.rotatef(-dispatcher.cameraYaw, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotatef(dispatcher.cameraPitch, 1.0F, 0.0F, 0.0F);
		float scale = 0.04F;
		GlStateManager.scalef(scale, -scale, scale);
		int textWidth = minecraft.textRenderer.getWidth(waypoint.display());
		int width = textWidth + Waypoint.displayXOffset() * 2;
		int textHeight = minecraft.textRenderer.fontHeight;
		int height = textHeight + Waypoint.displayYOffset() * 2;
		drawFontBatch(waypoint.display(), -textWidth / 2f, -textHeight / 2f);
		fillRect(-width / 2f, -height / 2f, -0.1f, width / 2f, height / 2f, waypoint.color().toInt());
		GlStateManager.popMatrix();
	}

	private void fillRect(float x, float y, float z, float x2, float y2, int color) {
		var buf = Tessellator.getInstance().getBuilder();
		buf.vertex(x, y, z).nextVertex();
		buf.vertex(x, y2, z).nextVertex();
		buf.vertex(x2, y2, z).nextVertex();
		buf.vertex(x2, y, z).nextVertex();
		buf.setQuadColor(color);
		Tessellator.getInstance().end();
	}

	private void drawFontBatch(String text, float x, float y) {
		minecraft.textRenderer.draw(text, x, y, -1, false);
	}

	public void renderWaypoints(float tick) {
		if (!AxolotlClientWaypoints.renderWaypoints.get()) return;
		if (!AxolotlClientWaypoints.renderWaypointsInWorld.get()) return;
		var profiler = Minecraft.getInstance().profiler;
		var cam = minecraft.getCamera();
		profiler.push("waypoints");
		var win = new Window(minecraft);

		GlStateManager.pushMatrix();
		var positionDrawn = new AtomicBoolean();
		for (Waypoint waypoint : AxolotlClientWaypoints.getCurrentWaypoints()) {
			GlStateManager.pushMatrix();
			renderWaypoint(waypoint, tick, cam, positionDrawn, win.getWidth(), win.getHeight(), win.getScale());
			GlStateManager.popMatrix();
		}
		GlStateManager.popMatrix();
		profiler.pop();
	}

	private Matrix4f getProjectionMatrix(float fov) {
		Matrix4f matrix4f = new Matrix4f();
		return matrix4f.perspective(fov * ((float) Math.PI / 180F), (float) this.minecraft.width / (float) this.minecraft.height, 0.05F, minecraft.options.viewDistance * 4);
	}

	private void renderWaypoint(Waypoint waypoint, float tick, Entity camera, AtomicBoolean positionDrawn, int guiWidth, int guiHeight, int guiScale) {
		var fov = ((GameRendererAccessor) minecraft.gameRenderer).invokeGetFov(tick, true);

		var textWidth = minecraft.textRenderer.getWidth(waypoint.display());
		int width = textWidth + Waypoint.displayXOffset() * 2;
		int textHeight = minecraft.textRenderer.fontHeight;
		int height = textHeight + Waypoint.displayYOffset() * 2;

		viewProj.set(waypoint.x(), waypoint.y(), waypoint.z(), 1);
		view.rotation(new Quaternionf().rotationYXZ(-camera.getHeadYaw() * (float) (Math.PI / 180.0), camera.pitch * (float) (Math.PI / 180.0), 0.0F)).translate((float) -camera.x, (float) -camera.y, (float) -camera.z);

		Matrix4f projection = getProjectionMatrix(fov);
		projection.mul(view);
		viewProj.mul(projection);

		var camPos = camera.getSourcePos();

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
		float x = 0.5f * (guiWidth * (projX + 1) - width * projX);
		//float y = graphics.guiHeight() - (graphics.guiHeight()/2f + (graphics.guiHeight()-height) * (viewProj.y() / 2f));
		float y = guiHeight * (0.5f - projY / 2) + (height * projY) / 2f;

		GlStateManager.translatef(x, y, 0);

		if (!AxolotlClientWaypoints.renderOutOfViewWaypointsOnScreenEdge.get() && (x < -width / 2f || x > guiWidth + width / 2f || y < -height / 2f || y > guiHeight + height / 2f)) {
			return;
		}
		if (waypoint.closerToThan(camPos.x, camPos.y, camPos.z, CUTOFF_DIST / guiScale)) {
			return;
		}

		if (!positionDrawn.get() && Math.abs(x - guiWidth / 2f) < 8 && Math.abs(y - guiHeight / 2f) < 8) {
			positionDrawn.set(true);
			GlStateManager.pushMatrix();
			GlStateManager.translatef(0, height / 2f + 2, 0);
			var line1 = waypoint.name();
			minecraft.textRenderer.draw(line1, -minecraft.textRenderer.getWidth(line1) / 2f, 0, -1, false);
			GlStateManager.translatef(0, minecraft.textRenderer.fontHeight + 2, 0);
			var line2 = AxolotlClientWaypoints.tr("distance", "%.2f".formatted(waypoint.distTo(camPos.x, camPos.y, camPos.z)));
			minecraft.textRenderer.draw(line2, -minecraft.textRenderer.getWidth(line2) / 2f, 0, -1, false);
			GlStateManager.popMatrix();
		}

		GuiElement.fill(-width / 2, -height / 2, width / 2, height / 2, waypoint.color().toInt());
		minecraft.textRenderer.draw(waypoint.display(), -textWidth / 2f, -textHeight / 2f, -1, false);
	}
}
