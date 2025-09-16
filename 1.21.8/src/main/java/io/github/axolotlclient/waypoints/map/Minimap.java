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

import com.mojang.blaze3d.platform.NativeImage;
import io.github.axolotlclient.bridge.render.AxoRenderContext;
import io.github.axolotlclient.waypoints.AxolotlClientWaypoints;
import io.github.axolotlclient.waypoints.AxolotlClientWaypointsCommon;
import io.github.axolotlclient.waypoints.HudCreator;
import io.github.axolotlclient.waypoints.util.ARGB;
import io.github.axolotlclient.waypoints.waypoints.Waypoint;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import org.joml.Vector2f;

@SuppressWarnings("DataFlowIssue")
public class Minimap extends MinimapCommon {

	private static final ResourceLocation texLocation = AxolotlClientWaypoints.rl("minimap");
	public static final ResourceLocation arrowLocation = AxolotlClientWaypoints.rl("arrow");
	private final NativeImage pixels = new NativeImage(viewDistance, viewDistance, false);
	public long updateDuration = -1;
	private DynamicTexture tex;
	private int mapCenterX, mapCenterZ;
	private boolean usingHud;
	public boolean allowCaves = true;

	private final Minecraft minecraft = Minecraft.getInstance();

	public void init() {
		super.init();
		AxolotlClientWaypointsCommon.category.add(minimap);
		if (AxolotlClientWaypointsCommon.AXOLOTLCLIENT_PRESENT) {
			usingHud = true;
			HudCreator.createHud(this);
		}
	}

	public boolean isEnabled() {
		return enabled.get();
	}

	public void setup() {
		minecraft.getTextureManager().register(texLocation, tex = new DynamicTexture(texLocation::toString, pixels));
		pixels.fillRect(0, 0, pixels.getWidth(), pixels.getHeight(), ARGB.opaque(0));
	}

	public void renderMapOverlay(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
		if (usingHud) {
			return;
		}
		x = minecraft.getWindow().getGuiScaledWidth() - size - 10;
		y = 10;
		if (minecraft.isDemo()) {
			this.y += 15;
		}
		if (!minecraft.player.getActiveEffects().isEmpty() && (minecraft.screen == null || !minecraft.screen.showsActiveEffects())) {
			if (minecraft.player.getActiveEffects().stream().anyMatch(e -> !e.getEffect().value().isBeneficial())) {
				this.y += 26;
			}
			this.y += 20;
		}
		renderMap(guiGraphics);
	}

	public void renderMap(AxoRenderContext ctx) {
		renderMap((GuiGraphics) ctx);
	}

	public void renderMap(GuiGraphics guiGraphics) {
		if (!isEnabled()) {
			return;
		}
		guiGraphics.pose().pushMatrix();
		{
			guiGraphics.enableScissor(x, y, x + size, y + size);
			guiGraphics.pose().pushMatrix();
			guiGraphics.pose().translate(x, y);
			guiGraphics.pose().translate(radius, radius);
			if (!lockMapToNorth.get()) {
				guiGraphics.pose().rotate((float) -(((minecraft.player.getVisualRotationYInDegrees() + 180) / 180) * Math.PI));
			}
			guiGraphics.pose().scale((float) Math.sqrt(2), (float) Math.sqrt(2));
			guiGraphics.pose().scale(mapScale.get(), mapScale.get());
			guiGraphics.pose().translate(-radius, -radius);
			float offX, offZ;
			offX = -(float) (minecraft.player.getX() - mapCenterX);
			offZ = -(float) (minecraft.player.getZ() - mapCenterZ);
			guiGraphics.pose().translate(offX, offZ);
			guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texLocation, 0, 0, 0, 0, pixels.getWidth(), pixels.getHeight(), size, size);
			guiGraphics.pose().popMatrix();
			guiGraphics.disableScissor();
		}

		if (minimapOutline.get() && !usingHud) {
			guiGraphics.renderOutline(x, y, size, size, outlineColor.get().toInt());
		}
		if (showWaypoints.get()) {
			renderMapWaypoints(guiGraphics);
		}
		if (showCardinalDirections.get()) {
			Vector2f pos = new Vector2f();
			guiGraphics.pose().pushMatrix();

			int size = this.size;
			var directions = new String[]{"N", "W", "E", "S"};
			for (int i : new int[]{-2, 1, 2, -1}) {
				var label = directions[i < 0 ? i + 2 : i + 1];
				var labelWidth = minecraft.font.width(label);
				var labelHeight = minecraft.font.lineHeight;
				guiGraphics.pose().pushMatrix();
				guiGraphics.pose().identity();
				guiGraphics.pose().translate(x + radius, y + radius);
				if (!lockMapToNorth.get()) {
					guiGraphics.pose().rotate((float) -(((minecraft.player.getVisualRotationYInDegrees() + 180) / 180) * Math.PI));
				}
				guiGraphics.pose().translate((i % 2) * size, ((int) (i / 2f)) * size);
				pos.zero();
				guiGraphics.pose().transformPosition(pos);
				guiGraphics.pose().popMatrix();
				pos.x = Math.clamp(pos.x, x, x + size);
				pos.y = Math.clamp(pos.y, y, y + size);
				guiGraphics.pose().pushMatrix();
				guiGraphics.pose().translate(pos);
				guiGraphics.pose().scale(0.5f, 0.5f);
				guiGraphics.fill(-(labelWidth / 2 + 2), -(labelHeight / 2 + 2), labelWidth / 2 + 2, labelHeight / 2 + 2, 0x77888888);
				guiGraphics.drawString(minecraft.font, label, -labelWidth / 2, -labelHeight / 2, -1);
				guiGraphics.pose().popMatrix();
			}
			guiGraphics.pose().popMatrix();
		}

		guiGraphics.pose().pushMatrix();
		guiGraphics.pose().translate(x + radius, y + radius);
		if (lockMapToNorth.get()) {
			guiGraphics.pose().rotate((float) (((minecraft.player.getVisualRotationYInDegrees() + 180) / 180) * Math.PI));
		}
		guiGraphics.pose().scale(0.5f * arrowScale.get(), 0.5f * arrowScale.get());
		int arrowSize = 15;
		guiGraphics.pose().translate(-arrowSize / 2f, -arrowSize / 2f);
		guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, arrowLocation, 0, 0, arrowSize, arrowSize);
		guiGraphics.pose().popMatrix();

		guiGraphics.pose().popMatrix();
	}

	private void renderMapWaypoints(GuiGraphics graphics) {
		if (!AxolotlClientWaypoints.renderWaypoints.get()) return;
		graphics.pose().pushMatrix();
		Vector2f pos = new Vector2f();
		for (Waypoint waypoint : AxolotlClientWaypoints.getCurrentWaypoints()) {
			graphics.pose().pushMatrix();
			float posX = (float) (waypoint.x() - minecraft.player.getX());
			float posY = (float) (waypoint.z() - minecraft.player.getZ());

			{
				pos.zero();
				graphics.pose().pushMatrix();
				graphics.pose().identity();
				graphics.pose().translate(x, y);
				graphics.pose().translate(radius, radius);
				graphics.pose().scale((float) Math.sqrt(2), (float) Math.sqrt(2));
				graphics.pose().scale(mapScale.get(), mapScale.get());
				if (!lockMapToNorth.get()) {
					graphics.pose().rotate((float) -Math.toRadians(minecraft.player.getVisualRotationYInDegrees() + 180));
				}
				graphics.pose().translate(posX, posY);
				graphics.pose().transformPosition(pos);
				graphics.pose().popMatrix();
			}

			{
				pos.x = Mth.clamp(pos.x, x, x + size);
				pos.y = Mth.clamp(pos.y, y, y + size);
				graphics.pose().translate(pos);
			}

			int textWidth = minecraft.font.width(waypoint.display());
			int textHeight = minecraft.font.lineHeight;
			graphics.fill(-(textWidth / 2) - Waypoint.displayXOffset(), -(textHeight / 2) - Waypoint.displayYOffset(), (textWidth / 2) + Waypoint.displayXOffset(), (textHeight / 2) + Waypoint.displayYOffset(), waypoint.color().toInt());
			graphics.drawString(minecraft.font, waypoint.display(), -(textWidth / 2), -textHeight / 2, -1, false);
			graphics.pose().popMatrix();
		}
		graphics.pose().popMatrix();
	}

	public void updateMapView() {
		if (!isEnabled()) {
			updateDuration = -1;
			return;
		}
		long start = Util.getNanos();
		int centerX = minecraft.player.getBlockX();
		int centerZ = minecraft.player.getBlockZ();
		mapCenterX = centerX;
		mapCenterZ = centerZ;
		int size = pixels.getWidth();
		int texHalfWidth = size / 2;

		BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos mutableBlockPos2 = new BlockPos.MutableBlockPos();

		var level = minecraft.level;
		var centerChunk = level.getChunk(SectionPos.blockToSectionCoord(centerX), SectionPos.blockToSectionCoord(centerZ));
		var surface = centerChunk.getHeight(Heightmap.Types.WORLD_SURFACE, centerX, centerZ);
		mutableBlockPos.set(centerX, surface, centerZ);
		int solidBlocksAbovePlayer = 0;
		boolean atSurface = false;
		if (!allowCaves) {
			atSurface = true;
		} else {
			if (level.dimensionType().hasCeiling()) {
				atSurface = minecraft.player.getBlockY() >= level.dimensionType().logicalHeight();
			} else if (surface + 1 <= minecraft.player.getBlockY()) {
				atSurface = true;
			} else {
				while (solidBlocksAbovePlayer <= 3 && surface > minecraft.player.getBlockY() && surface > level.getMinY()) {
					BlockState state = centerChunk.getBlockState(mutableBlockPos);
					mutableBlockPos.setY(surface--);
					if (!(state.propagatesSkylightDown() || !state.canOcclude() || !state.isViewBlocking(level, mutableBlockPos))) {
						solidBlocksAbovePlayer++;
					}
				}
				if (solidBlocksAbovePlayer <= 2) {
					atSurface = true;
				}
			}
		}

		boolean updated = false;
		for (int x = 0; x < size; x++) {
			double d = 0.0;
			for (int z = -1; z < size; z++) {
				int chunkX = (centerX + x - texHalfWidth);
				int chunkZ = (centerZ + z - texHalfWidth);
				ChunkAccess levelChunk = level.getChunk(SectionPos.blockToSectionCoord(chunkX), SectionPos.blockToSectionCoord(chunkZ), ChunkStatus.FULL, false);
				if (levelChunk != null) {
					int fluidDepth = 0;
					double e = 0.0;
					mutableBlockPos.set(chunkX, 0, chunkZ);
					int y = levelChunk.getHeight(Heightmap.Types.WORLD_SURFACE, mutableBlockPos.getX(), mutableBlockPos.getZ()) + 1;
					if (!atSurface) {
						y = Math.min(y, minecraft.player.getBlockY());
					}
					BlockState blockState;
					if (y <= level.getMinY()) {
						blockState = Blocks.AIR.defaultBlockState();
					} else {
						do {
							mutableBlockPos.setY(--y);
							blockState = levelChunk.getBlockState(mutableBlockPos);
						} while (blockState.getMapColor(level, mutableBlockPos) == MapColor.NONE && y > level.getMinY());

						if (y > level.getMinY() && !blockState.getFluidState().isEmpty()) {
							int highestFullBlockY = y - 1;
							mutableBlockPos2.set(mutableBlockPos);

							BlockState blockState2;
							do {
								mutableBlockPos2.setY(highestFullBlockY--);
								blockState2 = levelChunk.getBlockState(mutableBlockPos2);
								fluidDepth++;
							} while (highestFullBlockY > level.getMinY() && !blockState2.getFluidState().isEmpty());

							FluidState fluidState = blockState.getFluidState();
							blockState = !fluidState.isEmpty() && !blockState.isFaceSturdy(level, mutableBlockPos, Direction.UP) ? fluidState.createLegacyBlock() : blockState;
						}
					}

					e += y;
					var mapColor = blockState.getMapColor(level, mutableBlockPos);

					int color;
					if (mapColor == MapColor.WATER) {
						var floorBlock = levelChunk.getBlockState(mutableBlockPos2);
						var floorColor = floorBlock.getMapColor(level, mutableBlockPos2).col;
						int biomeColor = enableBiomeBlending.get() ? BiomeColors.getAverageWaterColor(level, mutableBlockPos) : mapColor.col;
						float shade = level.getShade(Direction.UP, true);
						int waterColor = biomeColor;
						waterColor = ARGB.colorFromFloat(1f, ARGB.redFloat(waterColor) * shade, ARGB.greenFloat(waterColor) * shade, ARGB.blueFloat(waterColor) * shade);
						waterColor = ARGB.average(waterColor, ARGB.scaleRGB(floorColor, 1f - fluidDepth / 15f));
						color = waterColor;
					} else {
						double f = (e - d) * 4.0 / (1 + 4) + ((x + z & 1) - 0.5) * 0.4;
						MapColor.Brightness brightness;
						if (f > 0.6) {
							brightness = MapColor.Brightness.HIGH;
						} else if (f < -0.6) {
							brightness = MapColor.Brightness.LOW;
						} else {
							brightness = MapColor.Brightness.NORMAL;
						}
						color = mapColor.calculateARGBColor(brightness);
					}

					d = e;

					if (z >= 0 && pixels.getPixel(x, z) != color) {
						pixels.setPixel(x, z, ARGB.opaque(color));
						updated = true;
					}
				} else {
					if (z >= 0 && pixels.getPixel(x, z) != 0) {
						pixels.setPixel(x, z, ARGB.opaque(0));
						updated = true;
					}
				}
			}
		}

		if (updated) {
			tex.upload();
		}
		updateDuration = Util.getNanos() - start;
	}
}
