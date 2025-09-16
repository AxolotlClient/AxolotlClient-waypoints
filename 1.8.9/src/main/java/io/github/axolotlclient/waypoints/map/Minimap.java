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

package io.github.axolotlclient.waypoints.map;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.axolotlclient.AxolotlClientConfig.impl.util.DrawUtil;
import io.github.axolotlclient.bridge.render.AxoRenderContext;
import io.github.axolotlclient.waypoints.AxolotlClientWaypoints;
import io.github.axolotlclient.waypoints.AxolotlClientWaypointsCommon;
import io.github.axolotlclient.waypoints.HudCreator;
import io.github.axolotlclient.waypoints.util.ARGB;
import io.github.axolotlclient.waypoints.waypoints.Waypoint;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiElement;
import net.minecraft.client.render.Window;
import net.minecraft.client.render.texture.DynamicTexture;
import net.minecraft.client.world.color.BiomeColors;
import net.minecraft.resource.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Vector3f;

public class Minimap extends MinimapCommon {

	private static final Identifier texLocation = AxolotlClientWaypoints.rl("minimap");
	public static final Identifier arrowLocation = AxolotlClientWaypoints.rl("textures/gui/sprites/arrow.png");
	private int[] pixels;
	public long updateDuration = -1;
	private DynamicTexture tex;
	private int mapCenterX, mapCenterZ;
	private boolean usingHud;
	public boolean allowCaves = true;

	private final Minecraft minecraft = Minecraft.getInstance();

	public void init() {
		super.init();
		AxolotlClientWaypointsCommon.category.add(Minimap.minimap);
		if (AxolotlClientWaypointsCommon.AXOLOTLCLIENT_PRESENT) {
			usingHud = true;
			HudCreator.createHud(this);
		}
	}

	public boolean isEnabled() {
		return enabled.get();
	}

	public void setup() {
		minecraft.getTextureManager().register(texLocation, tex = new DynamicTexture(viewDistance, viewDistance));
		pixels = tex.getPixels();
		this.x = new Window(minecraft).getWidth() - size - 10;
		this.y = 10;
	}

	public void renderMapOverlay() {
		if (usingHud) {
			return;
		}
		var guiWidth = new Window(minecraft).getWidth();
		this.x = guiWidth - size - 10;
		this.y = 10;
		if (minecraft.isDemo()) {
			this.y += 15;
		}
		/*if (!minecraft.player.getActiveEffects().isEmpty() && (minecraft.screen == null ||
			!(this.minecraft.screen instanceof EffectRenderingInventoryScreen<?> effectRenderingInventoryScreen && effectRenderingInventoryScreen.canSeeEffects()))) {
			if (minecraft.player.getActiveEffects().stream().anyMatch(e -> !e.getEffect().value().isBeneficial())) {
				this.y += 26;
			}
			this.y += 20;
		}*/
		renderMap();
	}

	public void renderMap(AxoRenderContext ctx) {
		renderMap();
	}


	public void renderMap() {
		if (!isEnabled()) {
			return;
		}
		GlStateManager.color4f(1, 1, 1, 1);
		GlStateManager.pushMatrix();
		{
			var vec1 = AxolotlClientWaypoints.MATRIX_STACK.transformPosition(x, y, 0, new Vector3f());
			var vec2 = AxolotlClientWaypoints.MATRIX_STACK.transformPosition(x+size, y+size, 0, new Vector3f());
			DrawUtil.pushScissor(MathHelper.floor(vec1.x), MathHelper.floor(vec1.y), MathHelper.floor(vec2.x-vec1.x), MathHelper.floor(vec2.y-vec1.y));
			GlStateManager.pushMatrix();
			GlStateManager.translatef(x, y, 0);
			GlStateManager.translatef(radius, radius, 0);
			if (!lockMapToNorth.get()) {
				GlStateManager.rotatef(-(minecraft.player.getHeadYaw() + 180), 0, 0, 1);
			}
			GlStateManager.scalef((float) Math.sqrt(2), (float) Math.sqrt(2), 1);
			GlStateManager.scalef(mapScale.get(), mapScale.get(), 1);
			GlStateManager.translatef(-radius, -radius, 0);
			float offX, offZ;
			offX = -(float) (minecraft.player.x - mapCenterX);
			offZ = -(float) (minecraft.player.z - mapCenterZ);
			GlStateManager.translatef(offX, offZ, 0);
			minecraft.getTextureManager().bind(texLocation);
			GuiElement.drawTexture(0, 0, 0, 0, viewDistance, viewDistance, size, size);
			GlStateManager.popMatrix();
			DrawUtil.popScissor();
		}

		if (minimapOutline.get() && !usingHud) {
			DrawUtil.outlineRect(x, y, size, size, outlineColor.get().toInt());
		}
		if (showWaypoints.get()) {
			renderMapWaypoints();
		}
		if (showCardinalDirections.get()) {
			Vector3f pos = new Vector3f();
			var directions = new String[]{"N", "W", "E", "S"};
			for (int i : new int[]{-2, 1, 2, -1}) {
				var label = directions[i < 0 ? i + 2 : i + 1];
				var labelWidth = minecraft.textRenderer.getWidth(label);
				var labelHeight = minecraft.textRenderer.fontHeight;
				AxolotlClientWaypoints.MATRIX_STACK.pushMatrix();
				AxolotlClientWaypoints.MATRIX_STACK.identity();
				AxolotlClientWaypoints.MATRIX_STACK.translate(x + radius, y + radius, 0);
				if (!lockMapToNorth.get()) {
					AxolotlClientWaypoints.MATRIX_STACK.rotate((float) -(((minecraft.player.getHeadYaw() + 180) / 180) * Math.PI), 0, 0, 1);
				}
				AxolotlClientWaypoints.MATRIX_STACK.translate((i % 2) * size, ((int) (i / 2f)) * size, 0);
				pos.zero();
				AxolotlClientWaypoints.MATRIX_STACK.transformPosition(pos);
				AxolotlClientWaypoints.MATRIX_STACK.popMatrix();
				pos.x = MathHelper.clamp(pos.x, x, x + size);
				pos.y = MathHelper.clamp(pos.y, y, y + size);
				GlStateManager.pushMatrix();
				GlStateManager.translatef(pos.x, pos.y, pos.z);
				GlStateManager.scalef(0.5f, 0.5f, 1);
				GuiElement.fill(-(labelWidth / 2 + 2), -(labelHeight / 2 + 2), labelWidth / 2 + 2, labelHeight / 2 + 2, 0x77888888);
				minecraft.textRenderer.draw(label, -labelWidth / 2, -labelHeight / 2, -1);
				GlStateManager.popMatrix();
			}
		}

		GlStateManager.pushMatrix();
		GlStateManager.translatef(x + radius, y + radius, 0);
		if (lockMapToNorth.get()) {
			GlStateManager.rotatef(minecraft.player.getHeadYaw() + 180, 0, 0, 1);
		}
		GlStateManager.scalef(0.5f * arrowScale.get(), 0.5f * arrowScale.get(), 1);
		int arrowSize = 15;
		GlStateManager.translatef(-arrowSize / 2f, -arrowSize / 2f, 1);
		minecraft.getTextureManager().bind(arrowLocation);
		GuiElement.drawTexture(0, 0, 0, 0, arrowSize, arrowSize, arrowSize, arrowSize);
		GlStateManager.popMatrix();

		GlStateManager.popMatrix();
	}

	private void renderMapWaypoints() {
		if (!AxolotlClientWaypoints.renderWaypoints.get()) return;
		GlStateManager.pushMatrix();
		Vector3f pos = new Vector3f();
		for (Waypoint waypoint : AxolotlClientWaypoints.getCurrentWaypoints()) {
			GlStateManager.pushMatrix();
			float posX = (float) (waypoint.x() - minecraft.player.x);
			float posY = (float) (waypoint.z() - minecraft.player.z);

			{
				pos.zero();
				AxolotlClientWaypoints.MATRIX_STACK.pushMatrix();
				AxolotlClientWaypoints.MATRIX_STACK.identity();
				AxolotlClientWaypoints.MATRIX_STACK.translate(x, y, 0);
				AxolotlClientWaypoints.MATRIX_STACK.translate(radius, radius, 0);
				AxolotlClientWaypoints.MATRIX_STACK.scale((float) Math.sqrt(2), (float) Math.sqrt(2), 1);
				AxolotlClientWaypoints.MATRIX_STACK.scale(mapScale.get(), mapScale.get(), 1);
				if (!lockMapToNorth.get()) {
					AxolotlClientWaypoints.MATRIX_STACK.rotate((float) -Math.toRadians(minecraft.player.headYaw + 180), 0, 0, 1);
				}
				AxolotlClientWaypoints.MATRIX_STACK.translate(posX, posY, 1);
				AxolotlClientWaypoints.MATRIX_STACK.transformPosition(pos);
				AxolotlClientWaypoints.MATRIX_STACK.popMatrix();
			}

			{
				pos.x = MathHelper.clamp(pos.x, x, x + size);
				pos.y = MathHelper.clamp(pos.y, y, y + size);
				GlStateManager.translatef(pos.x, pos.y, pos.z());
			}

			GlStateManager.color3f(1, 1, 1);
			int textWidth = minecraft.textRenderer.getWidth(waypoint.display());
			int textHeight = minecraft.textRenderer.fontHeight;
			GuiElement.fill(-(textWidth / 2) - Waypoint.displayXOffset(), -(textHeight / 2) - Waypoint.displayYOffset(), (textWidth / 2) + Waypoint.displayXOffset(), (textHeight / 2) + Waypoint.displayYOffset(), waypoint.color().toInt());
			minecraft.textRenderer.draw(waypoint.display(), -(textWidth / 2f), -textHeight / 2f, -1, false);
			GlStateManager.popMatrix();
		}
		GlStateManager.popMatrix();
	}

	private static int blockToSectionCoord(int c) {
		return c >> 4;
	}

	public void updateMapView() {
		if (!isEnabled()) {
			updateDuration = -1;
			return;
		}
		long start = System.currentTimeMillis();
		int centerX = (int) (minecraft.player.x + 0.5);
		int centerZ = (int) (minecraft.player.z + 0.5);
		mapCenterX = centerX;
		mapCenterZ = centerZ;
		int texHalfWidth = size / 2;

		BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable();
		BlockPos.Mutable mutableBlockPos2 = new BlockPos.Mutable();

		var level = minecraft.world;
		var centerChunk = level.getChunkAt(blockToSectionCoord(centerX), blockToSectionCoord(centerZ));
		var surface = centerChunk.getHeight(centerX & 15, centerZ & 15);
		mutableBlockPos.set(centerX, surface, centerZ);
		int solidBlocksAbovePlayer = 0;
		boolean atSurface = false;
		if (level.dimension.isDark()) {
			atSurface = (int) (minecraft.player.y + 0.5) >= level.getHeight();
		} else if (surface + 1 <= (int) (minecraft.player.y + 0.5)) {
			atSurface = true;
		} else {
			while (solidBlocksAbovePlayer <= 3 && surface > (int) (minecraft.player.y + 0.5) && surface > WorldMapScreen.MIN_BUILD_HEIGHT) {
				BlockState state = centerChunk.getBlockState(mutableBlockPos);
				mutableBlockPos.set(centerX, surface--, centerZ);
				if (!(state.getBlock().isTranslucent() || !state.getBlock().isOpaque() || !state.getBlock().isViewBlocking())) {
					solidBlocksAbovePlayer++;
				}
			}
			if (solidBlocksAbovePlayer <= 2) {
				atSurface = true;
			}
		}

		boolean updated = false;
		for (int x = 0; x < size; x++) {
			double d = 0.0;
			for (int z = -1; z < size; z++) {
				int chunkX = (centerX + x - texHalfWidth);
				int chunkZ = (centerZ + z - texHalfWidth);
				WorldChunk levelChunk = level.getChunkAt(blockToSectionCoord(chunkX), blockToSectionCoord(chunkZ));
				if (levelChunk != null) {
					int fluidDepth = 0;
					double e = 0.0;
					mutableBlockPos.set(chunkX, 0, chunkZ);
					int y = levelChunk.getHeight(mutableBlockPos.getX() & 15, mutableBlockPos.getZ() & 15) + 1;
					if (!atSurface) {
						y = Math.min(y, (int) (minecraft.player.y + 0.5));
					}
					BlockState blockState;
					if (y <= WorldMapScreen.MIN_BUILD_HEIGHT) {
						blockState = Blocks.AIR.defaultState();
					} else {
						do {
							mutableBlockPos.set(chunkX, --y, chunkZ);
							blockState = levelChunk.getBlockState(mutableBlockPos);
						} while (blockState.getBlock().getMapColor(blockState) == MapColor.AIR && y > WorldMapScreen.MIN_BUILD_HEIGHT);

						if (y > WorldMapScreen.MIN_BUILD_HEIGHT && blockState.getBlock().getMaterial().isLiquid()) {
							int highestFullBlockY = y - 1;
							mutableBlockPos2.set(mutableBlockPos.getX(), mutableBlockPos.getY(), mutableBlockPos.getZ());

							BlockState blockState2;
							do {
								mutableBlockPos2.set(mutableBlockPos.getX(), highestFullBlockY--, mutableBlockPos.getZ());
								blockState2 = levelChunk.getBlockState(mutableBlockPos2);
								fluidDepth++;
							} while (highestFullBlockY > WorldMapScreen.MIN_BUILD_HEIGHT && blockState2.getBlock().getMaterial().isLiquid());
						}
					}

					e += y;
					var mapColor = blockState.getBlock().getMapColor(blockState);

					int color;
					if (mapColor == MapColor.WATER) {
						var floorBlock = levelChunk.getBlockState(mutableBlockPos2);
						var floorColor = floorBlock.getBlock().getMapColor(floorBlock).color;
						int biomeColor = enableBiomeBlending.get() ? BiomeColors.getWaterColor(level, mutableBlockPos) : mapColor.color;
						float shade = 1.0f;
						int waterColor = biomeColor;
						waterColor = ARGB.colorFromFloat(1f, ARGB.redFloat(waterColor) * shade, ARGB.greenFloat(waterColor) * shade, ARGB.blueFloat(waterColor) * shade);
						waterColor = ARGB.average(waterColor, ARGB.scaleRGB(floorColor, 1f - fluidDepth / 15f));
						color = waterColor;
					} else {
						double f = (e - d) * 4.0 / (1 + 4) + ((x + z & 1) - 0.5) * 0.4;
						int brightness;
						if (f > 0.6) {
							brightness = 2;
						} else if (f < -0.6) {
							brightness = 0;
						} else {
							brightness = 1;
						}
						color = mapColor.getColor(brightness);
					}

					d = e;

					if (z >= 0 && pixels[x + z * size] != color) {
						pixels[x + z * size] = ARGB.opaque(color);
						updated = true;
					}
				} else {
					if (z >= 0 && pixels[x + z * size] != 0) {
						pixels[x + z * size] = ARGB.opaque(0);
						updated = true;
					}
				}
			}
		}
		if (updated) {
			tex.upload();
		}
		updateDuration = System.currentTimeMillis() - start;
	}
}
