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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.axolotlclient.AxolotlClientConfig.api.AxolotlClientConfig;
import io.github.axolotlclient.AxolotlClientConfig.api.options.OptionCategory;
import io.github.axolotlclient.AxolotlClientConfig.api.util.Colors;
import io.github.axolotlclient.AxolotlClientConfig.impl.managers.JsonConfigManager;
import io.github.axolotlclient.AxolotlClientConfig.impl.options.BooleanOption;
import io.github.axolotlclient.AxolotlClientConfig.impl.options.ColorOption;
import io.github.axolotlclient.AxolotlClientConfig.impl.options.IntegerOption;
import io.github.axolotlclient.AxolotlClientConfig.impl.util.DrawUtil;
import io.github.axolotlclient.modules.hud.HudManager;
import io.github.axolotlclient.modules.hud.gui.component.HudEntry;
import io.github.axolotlclient.waypoints.AxolotlClientWaypoints;
import io.github.axolotlclient.waypoints.util.ARGB;
import io.github.axolotlclient.waypoints.waypoints.Waypoint;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiElement;
import net.minecraft.client.render.Window;
import net.minecraft.client.render.texture.DynamicTexture;
import net.minecraft.resource.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;

public class Minimap {

	public final ColorOption outlineColor = new ColorOption("outline_color", Colors.WHITE);
	public final BooleanOption minimapOutline = new BooleanOption("minimap_outline", true);
	public final IntegerOption arrowScale = new IntegerOption("arrow_scale", 2, 1, 4);
	private final BooleanOption lockMapToNorth = new BooleanOption("lock_map_north", true);
	private final BooleanOption enabled = new BooleanOption("enabled", true);
	private final IntegerOption mapScale = new IntegerOption("map_scale", 1, 1, 5);
	private final BooleanOption showWaypoints = new BooleanOption("show_waypoints", true);
	//public static final BooleanOption useTextureSampling = new BooleanOption("use_texture_sampling", false);
	private static final OptionCategory minimap = OptionCategory.create("minimap");
	final int radius = 64, size = radius * 2;
	private static final Identifier texLocation = AxolotlClientWaypoints.rl("minimap");
	public static final Identifier arrowLocation = AxolotlClientWaypoints.rl("textures/gui/sprites/arrow.png");
	private int[] pixels;
	public long updateDuration = -1;
	private DynamicTexture tex;
	@Getter
	@Setter
	private int x, y;
	private int mapCenterX, mapCenterZ;
	private boolean usingHud;
	private final Matrix4fStack matrixStack = new Matrix4fStack(5);

	private final Minecraft minecraft = Minecraft.getInstance();

	public void init() {
		minimap.add(enabled, /* useTextureSampling,*/ lockMapToNorth, arrowScale, minimapOutline, outlineColor, mapScale, showWaypoints);
		AxolotlClientWaypoints.category.add(Minimap.minimap);
		if (AxolotlClientWaypoints.AXOLOTLCLIENT_PRESENT) {
			usingHud = true;
			var hud = new MinimapHudEntry(this);
			hud.setEnabled(true);
			var hudConfigManager = new JsonConfigManager(AxolotlClientWaypoints.OPTIONS_PATH.resolveSibling(hud.getId().getPath()+".json"), hud.getAllOptions());
			hudConfigManager.suppressName("x");
			hudConfigManager.suppressName("y");
			hudConfigManager.suppressName(minimapOutline.getName());
			hudConfigManager.suppressName(outlineColor.getName());
			AxolotlClientConfig.getInstance().register(hudConfigManager);
			Runtime.getRuntime().addShutdownHook(new Thread(hudConfigManager::save));
			minimap.add(hud.getAllOptions(), false);
			try {
				var f = HudManager.class.getDeclaredField("entries");
				f.setAccessible(true);
				@SuppressWarnings("unchecked") var entries = (Map<Identifier, HudEntry>) f.get(HudManager.getInstance());
				entries.put(hud.getId(), hud);
			} catch (Exception ignored) {
				usingHud = false;
			}
		}
	}

	public boolean isEnabled() {
		return enabled.get();
	}

	public void setup() {
		minecraft.getTextureManager().register(texLocation, tex = new DynamicTexture(size, size));
		pixels = tex.getPixels();
		this.x = new Window(minecraft).getWidth() - size - 10;
		this.y = 10;
	}

	public void renderMapOverlay() {
		if (!isEnabled() || usingHud) {
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

	public void renderMap() {
		matrixStack.clear().pushMatrix();
		GlStateManager.pushMatrix();
		{
			DrawUtil.pushScissor(x, y, size, size);
			GlStateManager.pushMatrix();
			GlStateManager.translatef(x, y, 0);
			GlStateManager.translatef(radius, radius, 0);
			if (!lockMapToNorth.get()) {
				GlStateManager.rotatef(-(minecraft.player.getHeadYaw() + 180), 0, 0, 1);
			}
			GlStateManager.scalef((float) Math.sqrt(2), (float) Math.sqrt(2), 1);
			GlStateManager.scalef(mapScale.get(), mapScale.get(), 1);
			GlStateManager.translatef(-size / 2f, -size / 2f, 0);
			float offX, offZ;
			offX = -(float) (minecraft.player.x - mapCenterX);
			offZ = -(float) (minecraft.player.z - mapCenterZ);
			GlStateManager.translatef(offX / mapScale.get(), offZ / mapScale.get(), 0);
			minecraft.getTextureManager().bind(texLocation);
			GuiElement.drawTexture(0, 0, 0, 0, size, size, size, size);
			GlStateManager.popMatrix();
			DrawUtil.popScissor();
		}

		if (minimapOutline.get() && !usingHud) {
			DrawUtil.outlineRect(x, y, size, size, outlineColor.get().toInt());
		}
		if (showWaypoints.get()) {
			renderMapWaypoints();
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

		matrixStack.popMatrix();
		GlStateManager.popMatrix();
	}

	private void renderMapWaypoints() {
		if (!AxolotlClientWaypoints.renderWaypoints.get()) return;
		matrixStack.pushMatrix();
		GlStateManager.pushMatrix();
		Vector3f pos = new Vector3f();
		for (Waypoint waypoint : AxolotlClientWaypoints.getCurrentWaypoints()) {
			matrixStack.pushMatrix();
			GlStateManager.pushMatrix();
			float posX = (float) (waypoint.x() - minecraft.player.x);
			float posY = (float) (waypoint.z() - minecraft.player.z);

			{
				pos.zero();
				matrixStack.pushMatrix();
				matrixStack.translate(x, y, 0);
				matrixStack.translate(radius, radius, 0);
				matrixStack.scale((float) Math.sqrt(2), (float) Math.sqrt(2), 1);
				matrixStack.scale(mapScale.get(), mapScale.get(), 1);
				if (!lockMapToNorth.get()) {
					matrixStack.rotate((float) -Math.toRadians(minecraft.player.headYaw + 180), 0, 0, 1);
				}
				matrixStack.translate(posX, posY, 1);
				matrixStack.transformPosition(pos);
				matrixStack.popMatrix();
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
			matrixStack.popMatrix();
			GlStateManager.popMatrix();
		}
		matrixStack.popMatrix();
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

		AtomicBoolean updated = new AtomicBoolean(false);
		List<CompletableFuture<?>> futs = new ArrayList<>();
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

					int brightness;
					if (mapColor == MapColor.WATER) {
						double f = fluidDepth * 0.1 + (x + z & 1) * 0.2;
						if (f < 0.5) {
							brightness = 2;
						} else if (f > 0.9) {
							brightness = 0;
						} else {
							brightness = 1;
						}
					} else {
						double f = (e - d) * 4.0 / (1 + 4) + ((x + z & 1) - 0.5) * 0.4;
						if (f > 0.6) {
							brightness = 2;
						} else if (f < -0.6) {
							brightness = 0;
						} else {
							brightness = 1;
						}
					}

					d = e;

					/*if (useTextureSampling.get()) {
						final int fz = z, fx = x;
						futs.add(TextureSampler.getSample(blockState, level, mutableBlockPos, brightness).thenAccept(color -> {
							color = ARGB.opaque(color);
							if (fz >= 0 && Integer.rotateRight(pixels.getPixelRGBA(fx, fz), 4) != color) {
								pixels.setPixelRGBA(fx, fz, Integer.rotateLeft(color, 4));
								updated.set(true);
							}
						}));
					} else*/
					{
						int color = mapColor.getColor(brightness);
						if (z >= 0 && pixels[x + z * size] != color) {
							pixels[x + z * size] = ARGB.opaque(color);
							updated.set(true);
						}
					}
				} else {
					if (z >= 0 && pixels[x + z * size] != 0) {
						pixels[x + z * size] = ARGB.opaque(0);
						updated.set(true);
					}
				}
			}
		}
		CompletableFuture.allOf(futs.toArray(CompletableFuture[]::new)).thenRun(() -> {
			if (updated.get()) {
				tex.upload();
			}
		});
		updateDuration = System.currentTimeMillis() - start;
	}
}
