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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tessellator;
import io.github.axolotlclient.AxolotlClientConfig.api.util.Colors;
import io.github.axolotlclient.AxolotlClientConfig.impl.util.DrawUtil;
import io.github.axolotlclient.waypoints.AxolotlClientWaypoints;
import io.github.axolotlclient.waypoints.mixin.GameRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiElement;
import net.minecraft.client.render.Window;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector4f;

public class WaypointRenderer {

	private static final double CUTOFF_DIST = 5;
	private final Minecraft minecraft = Minecraft.getInstance();
	private final Matrix4f view = new Matrix4f();
	private final Vector4f viewProj = new Vector4f();
	private final Set<Waypoint> worldRendererWaypoints = new HashSet<>();

	public void render(float f) {
		if (!AxolotlClientWaypoints.renderWaypoints.get()) return;
		if (!AxolotlClientWaypoints.renderWaypointsInWorld.get()) return;
		if (minecraft.world == null) return;
		var profiler = Minecraft.getInstance().profiler;
		profiler.swap("waypoints");

		var cam = minecraft.getCamera();

		GlStateManager.color3f(1, 1, 1);
		GlStateManager.pushMatrix();

		var camPos = cam.getEyePosition(f);
		var cpos = camPos.subtract(0, cam.getEyeHeight(), 0);
		//var camPos = new Vector3d(cam.x, cam.y, cam.z);
		//var prevCamPos = new Vector3d(cam.prevTickX, cam.prevTickY, cam.prevTickZ);
		//camPos.sub(prevCamPos).mul(f).add(prevCamPos);

		GlStateManager.depthMask(false);
		GlStateManager.disableDepthTest();
		float fov = ((GameRendererAccessor) minecraft.gameRenderer).invokeGetFov(f, true);
		var win = new Window(minecraft);
		worldRendererWaypoints.clear();

		for (Waypoint waypoint : AxolotlClientWaypoints.getCurrentWaypoints()) {
			profiler.push(waypoint.name());
			renderWaypoint(waypoint, camPos, cpos, fov, win.getWidth(), win.getHeight());
			profiler.pop();
		}

		GlStateManager.depthMask(true);
		GlStateManager.enableDepthTest();
		GlStateManager.popMatrix();
	}

	private void renderWaypoint(Waypoint waypoint, Vec3d camPos, Vec3d camEyePos, float fov, int guiWidth, int guiHeight) {
		int textWidth = minecraft.textRenderer.getWidth(waypoint.display());
		int width = textWidth + Waypoint.displayXOffset() * 2;
		int textHeight = minecraft.textRenderer.fontHeight;
		int height = textHeight + Waypoint.displayYOffset() * 2;
		var displayStart = projectToScreen(guiWidth, guiHeight, camPos, fov, width, height, waypoint.x(), waypoint.y(), waypoint.z(), new Vector2f(-(width / 2f * 0.04f), (height / 2f * 0.04f)));
		if (displayStart == null) return;
		var displayEnd = projectToScreen(guiWidth, guiHeight, camPos, fov, width, height, waypoint.x(), waypoint.y(), waypoint.z(), new Vector2f(width / 2f * 0.04f, -(height / 2f * 0.04f)));
		if (displayEnd == null) return;
		float projWidth = Math.abs(displayEnd.x() - displayStart.x());
		float projHeight = Math.abs(displayEnd.y() - displayStart.y());
		if (projWidth < width && projHeight < height) {
			return;
		}
		worldRendererWaypoints.add(waypoint);

		GlStateManager.pushMatrix();
		GlStateManager.translated(waypoint.x() - camEyePos.x, waypoint.y() - camEyePos.y, waypoint.z() - camEyePos.z);
		var dispatcher = minecraft.getEntityRenderDispatcher();
		GlStateManager.rotatef(-dispatcher.cameraYaw, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotatef(dispatcher.cameraPitch, 1.0F, 0.0F, 0.0F);
		float scale = 0.04F;
		GlStateManager.scalef(-scale, -scale, scale);
		GlStateManager.enableBlend();
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
		var waypoints = AxolotlClientWaypoints.getCurrentWaypoints();
		var positionDrawers = new ArrayList<Runnable>(waypoints.size());
		for (Waypoint waypoint : waypoints) {
			GlStateManager.pushMatrix();
			renderWaypoint(waypoint, tick, cam, positionDrawers, win.getWidth(), win.getHeight());
			GlStateManager.popMatrix();
		}
		if (!positionDrawers.isEmpty()) {
			positionDrawers.forEach(Runnable::run);
		}
		GlStateManager.popMatrix();
		profiler.pop();
	}

	private Matrix4f getProjectionMatrix(float fov) {
		Matrix4f matrix4f = new Matrix4f();
		return matrix4f.perspective(fov * ((float) Math.PI / 180F), (float) this.minecraft.width / (float) this.minecraft.height, 0.05F, minecraft.options.viewDistance * 4);
	}

	private void renderWaypoint(Waypoint waypoint, float tick, Entity camera, List<Runnable> positionDrawn, int guiWidth, int guiHeight) {
		var fov = ((GameRendererAccessor) minecraft.gameRenderer).invokeGetFov(tick, false);

		var textWidth = minecraft.textRenderer.getWidth(waypoint.display());
		int width = textWidth + Waypoint.displayXOffset() * 2;
		int textHeight = minecraft.textRenderer.fontHeight;
		int height = textHeight + Waypoint.displayYOffset() * 2;
		var camPos = camera.getEyePosition(tick);

		var displayStart = projectToScreen(guiWidth, guiHeight, camPos, fov, width, height, waypoint.x(), waypoint.y(), waypoint.z(), new Vector2f(-(width / 2f * 0.04f), (height / 2f * 0.04f)));
		var displayEnd = projectToScreen(guiWidth, guiHeight, camPos, fov, width, height, waypoint.x(), waypoint.y(), waypoint.z(), new Vector2f((width / 2f * 0.04f), -(height / 2f * 0.04f)));
		Result result = projectToScreen(guiWidth, guiHeight, camPos, fov, width, height, waypoint.x(), waypoint.y(), waypoint.z(), null);
		if (result == null) return;
		float projWidth;
		float projHeight;
		if (displayStart != null && displayEnd != null) {
			projWidth = Math.abs(displayEnd.x() - displayStart.x());
			projHeight = Math.abs(displayEnd.y() - displayStart.y());
		} else {
			projWidth = 0;
			projHeight = 0;
		}

		GlStateManager.translatef(result.x(), result.y(), 0);
		boolean outOfView = result.x() < -width / 2f || result.x() > guiWidth + width / 2f || result.y() < -height / 2f || result.y() > guiHeight + height / 2f;
		if (!AxolotlClientWaypoints.renderOutOfViewWaypointsOnScreenEdge.get() && outOfView) {
			return;
		}

		boolean _3dOnScreen;
		if (displayEnd != null && displayStart != null) {
			float minX = displayStart.x();
			float minY = displayStart.y();
			float maxX = displayEnd.x();
			float maxY = displayEnd.y();
			_3dOnScreen = minX > 0 && minY > 0 && minX < guiWidth && minY < guiHeight ||
				minX > 0 && maxY > 0 && minX < guiWidth && maxY < guiHeight ||
				maxX > 0 && maxY > 0 && maxX < guiWidth && maxY < guiHeight ||
				maxX > 0 && minY > 0 && maxX < guiWidth && minY < guiHeight ||
				minX < guiWidth && maxX > 0 && minY < guiHeight && maxY > 0;
		} else {
			_3dOnScreen = false;
		}
		boolean displayX = Math.abs(result.x() - guiWidth / 2f) < (_3dOnScreen ? Math.max(projWidth, width) : width) / 2f + guiWidth / 4f;
		boolean displayY = Math.abs(result.y() - guiHeight / 2f) < (_3dOnScreen ? Math.max(height, projHeight) : height) / 2f + guiHeight / 4f;
		if (displayX && displayY) {
			positionDrawn.add(() -> {
				var line1 = waypoint.name();
				GlStateManager.pushMatrix();
				GlStateManager.translatef(result.x(), result.y(), 0);
				GlStateManager.translatef(0, Math.max(height, projHeight + 4) / 2f + 4, 0);
				if ((projWidth >= width || projHeight >= height) && _3dOnScreen) {
					float y = result.y() + Math.max(height, projHeight + 4) / 2f + 4;
					var y2 = Math.min(y, displayEnd.y() + 6);
					GlStateManager.translatef(0, y2 - y, 0);
				}
				int line1W = minecraft.textRenderer.getWidth(line1);
				GuiElement.fill(-line1W / 2 - 2, -2, line1W / 2 + 2, minecraft.textRenderer.fontHeight + 2, Colors.GRAY.withAlpha(100).toInt());
				DrawUtil.outlineRect(-line1W / 2 - 2, -2, line1W + 4, minecraft.textRenderer.fontHeight + 4, Colors.GRAY.toInt());
				minecraft.textRenderer.draw(line1, -line1W / 2f, 0, -1, true);
				if (!waypoint.closerToThan(camPos.x, camPos.y, camPos.z, CUTOFF_DIST)) {
					GlStateManager.translatef(0, minecraft.textRenderer.fontHeight + 4, 0);
					var line2 = AxolotlClientWaypoints.tr("distance", "%.2f".formatted(waypoint.distTo(camPos.x, camPos.y, camPos.z)));
					minecraft.textRenderer.draw(line2, -minecraft.textRenderer.getWidth(line2) / 2f, 0, -1, false);
				}
				GlStateManager.popMatrix();
			});
		}

		if ((projWidth >= width || projHeight >= height) && _3dOnScreen && worldRendererWaypoints.contains(waypoint)) {
			return;
		}

		GuiElement.fill(-width / 2, -height / 2, width / 2, height / 2, waypoint.color().toInt());
		minecraft.textRenderer.draw(waypoint.display(), -textWidth / 2f, -textHeight / 2f, -1, false);
	}

	private @Nullable Result projectToScreen(int guiWidth, int guiHeight, Vec3d camPos, float fov, int width, int height, double x, double y, double z, Vector2f orthoOffset) {
		viewProj.set(x, y, z, 1);
		var dispatcher = minecraft.getEntityRenderDispatcher();
		if (orthoOffset != null) {
			var vec = new Matrix4f();
			var camRot = new Quaternionf()
				.rotationYXZ(-dispatcher.cameraYaw * (float) (Math.PI / 180.0), dispatcher.cameraPitch * (float) (Math.PI / 180.0), 0.0F)
				.rotateY((float) -(Math.PI));
			vec.rotate(camRot.invert(new Quaternionf()));
			vec.translate(orthoOffset.x(), orthoOffset.y(), 0);
			vec.rotate(camRot);
			vec.transform(viewProj);
		}

		view.rotation(new Quaternionf()
			.rotationYXZ(-dispatcher.cameraYaw * (float) (Math.PI / 180.0), dispatcher.cameraPitch * (float) (Math.PI / 180.0), 0.0F)
			.rotateY((float) -(Math.PI)).invert()).translate((float) -camPos.x, (float) -camPos.y, (float) -camPos.z);

		Matrix4f projection = getProjectionMatrix(fov);
		projection.mul(view);
		viewProj.mul(projection);

		if (orthoOffset == null && AxolotlClientWaypoints.renderOutOfViewWaypointsOnScreenEdge.get()) {
			viewProj.w = Math.max(Math.abs(viewProj.x()), Math.max(Math.abs(viewProj.y()), viewProj.w()));
		}

		if (viewProj.w() <= 0) {
			return null;
		}
		viewProj.div(viewProj.w());

		float projX = viewProj.x();
		float projY = viewProj.y();

		//float x = (graphics.guiWidth()/2f) + ((graphics.guiWidth() - width) * (viewProj.x() / 2f));
		float resultX = 0.5f * (guiWidth * (projX + 1) - width * projX);
		//float y = graphics.guiHeight() - (graphics.guiHeight()/2f + (graphics.guiHeight()-height) * (viewProj.y() / 2f));
		float resultY = guiHeight * (0.5f - projY / 2) + (height * projY) / 2f;
		return new Result(resultX, resultY);
	}

	private record Result(float x, float y) {
	}
}
