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
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tessellator;
import io.github.axolotlclient.waypoints.AxolotlClientWaypoints;
import io.github.axolotlclient.waypoints.mixin.GameRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiElement;
import net.minecraft.client.render.Window;
import net.minecraft.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector4f;

public class WaypointRenderer {

	private static final double CUTOFF_DIST = 12;
	private final Minecraft minecraft = Minecraft.getInstance();
	private final Matrix4f view = new Matrix4f();
	private final Vector4f viewProj = new Vector4f();

	public void render(float f) {
		if (!AxolotlClientWaypoints.renderWaypoints.get()) return;
		if (!AxolotlClientWaypoints.renderWaypointsInWorld.get()) return;
		if (minecraft.world == null) return;
		var profiler = Minecraft.getInstance().profiler;
		profiler.swap("waypoints");

		var cam = minecraft.getCamera();

		GlStateManager.color3f(1, 1, 1);
		GlStateManager.pushMatrix();

		var camPos = new Vector3d(cam.x, cam.y, cam.z);
		var prevCamPos = new Vector3d(cam.prevTickX, cam.prevTickY, cam.prevTickZ);
		camPos.sub(prevCamPos).mul(f).add(prevCamPos);

		GlStateManager.depthMask(false);
		GlStateManager.disableDepthTest();
		var guiScale = new Window(minecraft).getScale();

		for (Waypoint waypoint : AxolotlClientWaypoints.getCurrentWaypoints()) {
			if (waypoint.closerToThan(camPos.x, camPos.y + cam.getEyeHeight(), camPos.z, CUTOFF_DIST / guiScale)) {
				profiler.push(waypoint.name());
				renderWaypoint(waypoint, camPos);
				profiler.pop();
			}
		}

		GlStateManager.depthMask(true);
		GlStateManager.enableDepthTest();
		GlStateManager.popMatrix();
	}

	private void renderWaypoint(Waypoint waypoint, Vector3d camPos) {
		GlStateManager.pushMatrix();
		GlStateManager.translated(waypoint.x() - camPos.x, waypoint.y() - (camPos.y), waypoint.z() - camPos.z);
		var dispatcher = minecraft.getEntityRenderDispatcher();
		GlStateManager.rotatef(-dispatcher.cameraYaw, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotatef(dispatcher.cameraPitch, 1.0F, 0.0F, 0.0F);
		float scale = 0.04F;
		GlStateManager.scalef(-scale, -scale, scale);
		GlStateManager.enableBlend();
		int textWidth = minecraft.textRenderer.getWidth(waypoint.display());
		int width = textWidth + Waypoint.displayXOffset() * 2;
		int textHeight = minecraft.textRenderer.fontHeight;
		int height = textHeight + Waypoint.displayYOffset() * 2;
		fillRect(-width / 2f, -height / 2f, -0.1f, width / 2f, height / 2f, waypoint.color().toInt());
		drawFontBatch(waypoint.display(), -textWidth / 2f, -textHeight / 2f);
		GlStateManager.popMatrix();
	}

	private void fillRect(float x, float y, float z, float x2, float y2, int color) {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuilder();
		GlStateManager.enableBlend();
		GlStateManager.disableTexture();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);
		GlStateManager.color4f((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, (color >> 24 & 0xFF) / 255.0F);
		bufferBuilder.begin(7, DefaultVertexFormat.POSITION);
		bufferBuilder.vertex(x, y2, z).nextVertex();
		bufferBuilder.vertex(x2, y2, z).nextVertex();
		bufferBuilder.vertex(x2, y, z).nextVertex();
		bufferBuilder.vertex(x, y, z).nextVertex();
		tessellator.end();
		GlStateManager.enableTexture();
		GlStateManager.color3f(1, 1, 1);
		GlStateManager.disableBlend();
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
		var dispatcher = minecraft.getEntityRenderDispatcher();
		var camPos = camera.getEyePosition(tick);
		view.rotation(new Quaternionf()
			.rotationYXZ(-dispatcher.cameraYaw * (float) (Math.PI / 180.0), dispatcher.cameraPitch * (float) (Math.PI / 180.0), 0.0F)
			.rotateY((float) -(Math.PI)).invert()).translate((float) -camPos.x, (float) -camPos.y, (float) -camPos.z);

		Matrix4f projection = getProjectionMatrix(fov);
		projection.mul(view);
		viewProj.mul(projection);

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
