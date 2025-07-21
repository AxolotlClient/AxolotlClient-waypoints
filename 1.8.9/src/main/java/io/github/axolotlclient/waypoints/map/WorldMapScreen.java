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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlStateManager;
import io.github.axolotlclient.AxolotlClientConfig.api.util.Colors;
import io.github.axolotlclient.AxolotlClientConfig.impl.ui.Element;
import io.github.axolotlclient.AxolotlClientConfig.impl.ui.Screen;
import io.github.axolotlclient.AxolotlClientConfig.impl.ui.Selectable;
import io.github.axolotlclient.AxolotlClientConfig.impl.util.DrawUtil;
import io.github.axolotlclient.waypoints.AxolotlClientWaypoints;
import io.github.axolotlclient.waypoints.map.util.LevelChunkStorage;
import io.github.axolotlclient.waypoints.map.widgets.AbstractSliderButton;
import io.github.axolotlclient.waypoints.map.widgets.DropdownButton;
import io.github.axolotlclient.waypoints.map.widgets.ImageButton;
import io.github.axolotlclient.waypoints.mixin.LevelAccessor;
import io.github.axolotlclient.waypoints.util.ARGB;
import io.github.axolotlclient.waypoints.waypoints.Waypoint;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.TextRenderer;
import net.minecraft.client.render.texture.DynamicTexture;
import net.minecraft.resource.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4fStack;
import org.joml.Vector2i;
import org.joml.Vector3f;

@Slf4j
public class WorldMapScreen extends Screen {
	public static final int MIN_BUILD_HEIGHT = 0;
	private static final int TILE_SIZE = 16;
	private static final Identifier OPTIONS_SPRITE = AxolotlClientWaypoints.rl("textures/gui/sprites/options.png");
	private static final Identifier OPTIONS_HOVERED_SPRITE = AxolotlClientWaypoints.rl("textures/gui/sprites/options_hovered.png");

	private final Map<Vector2i, LazyTile> tiles = new ConcurrentHashMap<>();
	private final Vector3f dragOffset = new Vector3f();
	private float scale = 1f;
	public static boolean allowCaves = true, allowCavesNether;
	private boolean atSurface;
	private int caveY;
	private String dimension;
	private Waypoint hoveredWaypoint = null;
	private boolean initializedOnce = false;
	private Runnable optionUpdate;
	private final TextRenderer font = Minecraft.getInstance().textRenderer;
	private final Matrix4fStack matrixStack = new Matrix4fStack(16);

	public WorldMapScreen() {
		super(AxolotlClientWaypoints.tr("worldmap"));
	}

	@Override
	public void renderBackground() {

	}

	@Override
	public void render(int mouseX, int mouseY, float partialTick) {
		super.renderBackground(0);
		GlStateManager.pushMatrix();
		matrixStack.clear();
		matrixStack.pushMatrix();
		GlStateManager.translatef(width / 2f, height / 2f, 0);
		matrixStack.translate(width / 2f, height / 2f, 0);
		GlStateManager.translatef(dragOffset.x(), dragOffset.y(), dragOffset.z());
		matrixStack.translate(dragOffset.x(), dragOffset.y(), dragOffset.z());
		GlStateManager.pushMatrix();
		matrixStack.pushMatrix();
		GlStateManager.scalef(scale, scale, 1);
		matrixStack.scale(scale, scale, 1);

		var player = minecraft.player;

		for (LazyTile tile : tiles.values()) {
			tile.render(matrixStack, (float) player.x, (float) player.z, scale, partialTick, caveY, atSurface, width, height);
		}

		matrixStack.popMatrix();
		GlStateManager.popMatrix();
		renderMapWaypoints(mouseX, mouseY);

		GlStateManager.rotatef(minecraft.player.getHeadYaw() + 180, 0, 0, 1);
		GlStateManager.scalef(0.5f * AxolotlClientWaypoints.MINIMAP.arrowScale.get(), 0.5f * AxolotlClientWaypoints.MINIMAP.arrowScale.get(), 1);
		int arrowSize = 15;
		GlStateManager.translatef(-arrowSize / 2f, -arrowSize / 2f, 5);
		minecraft.getTextureManager().bind(Minimap.arrowLocation);
		drawTexture(0, 0, 0, 0, arrowSize, arrowSize, arrowSize, arrowSize);
		matrixStack.popMatrix();
		GlStateManager.popMatrix();
		super.render(mouseX, mouseY, partialTick);

		if (mouseX > -1 && mouseY > -1) {
			int x = getWorldX(mouseX);
			int z = getWorldZ(mouseY);
			drawCenteredString(font, AxolotlClientWaypoints.tr("position", String.valueOf(x), String.valueOf(getY(x, z)), String.valueOf(z)), width / 2, height - 15, Colors.GRAY.toInt());
		}
	}

	private static int blockToSectionCoord(int c) {
		return c >> 4;
	}

	private int getY(int x, int z) {
		int tileX = x / TILE_SIZE;
		int tileY = z / TILE_SIZE;
		var tile = tiles.get(new Vector2i(tileX - 1, tileY - 1));
		WorldChunk c;
		if (tile == null || tile.tile == null) {
			c = ((LevelAccessor) minecraft.world).invokeChunkLoadedAt(blockToSectionCoord(x), blockToSectionCoord(z), true) ? minecraft.world.getChunkAt(blockToSectionCoord(x), blockToSectionCoord(z)) : null;
		} else c = tile.tile.chunk.chunk();
		if (c == null) return MIN_BUILD_HEIGHT;
		int y = c.getHeight(x & 15, z & 15);
		if (atSurface) {
			return y;
		}
		y = Math.min(y, caveY);
		BlockState blockState;
		var mutableBlockPos = new BlockPos.Mutable(x, 0, z);
		do {
			mutableBlockPos.set(mutableBlockPos.getX(), --y, mutableBlockPos.getZ());
			blockState = c.getBlockState(mutableBlockPos);
		} while (blockState.getBlock().getMapColor(blockState) == MapColor.AIR && y > MIN_BUILD_HEIGHT);
		return y;
	}

	private int getWorldX(double guiX) {
		return MathHelper.floor(minecraft.player.z - ((width / 2f + dragOffset.x()) - guiX) / scale);
	}

	private int getWorldZ(double guiZ) {
		return MathHelper.floor(minecraft.player.z - ((height / 2f + dragOffset.y()) - guiZ) / scale);
	}

	private void renderMapWaypoints(int mouseX, int mouseY) {
		if (!AxolotlClientWaypoints.renderWaypoints.get()) return;
		GlStateManager.pushMatrix();
		matrixStack.pushMatrix();
		var pos = new Vector3f();
		hoveredWaypoint = null;
		for (Waypoint waypoint : AxolotlClientWaypoints.getCurrentWaypoints()) {
			GlStateManager.pushMatrix();
			matrixStack.pushMatrix();
			float posX = (float) (waypoint.x() - minecraft.player.x) + 1;
			float posY = (float) (waypoint.z() - minecraft.player.z) + 1;

			matrixStack.translate(posX * scale, posY * scale, 0);
			GlStateManager.translatef(posX * scale, posY * scale, 0);
			pos.zero();
			matrixStack.transformPosition(pos);

			int textWidth = font.getWidth(waypoint.display());
			int width = textWidth + Waypoint.displayXOffset() * 2;
			int textHeight = font.fontHeight;
			int height = textHeight + Waypoint.displayYOffset() * 2;
			pos.sub(width / 2f, height / 2f, 0);
			fill(-(width / 2), -(height / 2), (width / 2), (height / 2), waypoint.color().toInt());
			font.draw(waypoint.display(), -(textWidth / 2f), -textHeight / 2f, -1, false);
			if (hoveredWaypoint == null) {
				if (mouseX >= pos.x() && mouseY >= pos.y() && mouseX < pos.x() + width && mouseY < pos.y() + height) {
					hoveredWaypoint = waypoint;
					GlStateManager.translatef(0, 0, 2);
					DrawUtil.outlineRect(-width / 2, -height / 2, width, height, Colors.WHITE.toInt());
				}
			}
			matrixStack.popMatrix();
			GlStateManager.popMatrix();
		}
		matrixStack.popMatrix();
		GlStateManager.popMatrix();
	}

	private void collectPlayerYData() {
		var level = minecraft.world;
		if (allowCaves || (allowCavesNether && level.dimension.isDark())) {
			int playerX = (int) (minecraft.player.x + 0.5);
			int playerZ = (int) (minecraft.player.z + 0.5);
			var centerChunk = level.getChunkAt(blockToSectionCoord(playerX), blockToSectionCoord(playerZ));
			var surface = centerChunk.getHeight(playerX & 15, playerZ & 15);
			BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable(playerX, surface, playerZ);
			int solidBlocksAbovePlayer = 0;
			atSurface = false;
			if (level.dimension.isDark()) {
				atSurface = (int) (minecraft.player.y + 0.5) >= level.getHeight();
			} else if (surface + 1 <= (int) (minecraft.player.y + 0.5)) {
				atSurface = true;
			} else {
				while (solidBlocksAbovePlayer <= 3 && surface > (int) (minecraft.player.y + 0.5) && surface > MIN_BUILD_HEIGHT) {
					BlockState state = centerChunk.getBlockState(mutableBlockPos);
					mutableBlockPos.set(playerX, surface--, playerZ);
					if (!(state.getBlock().isTranslucent() || !state.getBlock().isOpaque() || !state.getBlock().isViewBlocking())) {
						solidBlocksAbovePlayer++;
					}
				}
				if (solidBlocksAbovePlayer <= 2) {
					atSurface = true;
				}
			}
		} else {
			atSurface = true;
		}

		caveY = (int) (minecraft.player.y + 0.5);
		dimension = level.dimension.getName();
	}

	private void createTiles() {
		int playerX = (int) (minecraft.player.x + 0.5);
		int playerZ = (int) (minecraft.player.z + 0.5);

		var playerTile = createTile(playerX, playerZ, atSurface, caveY);
		if (playerTile != null) {
			Map<Vector2i, LazyTile> loadedTiles = new HashMap<>(tiles);
			tiles.clear();
			tiles.put(new Vector2i(playerTile.tilePosX(), playerTile.tilePosY()), playerTile);
			minecraft.submit(() -> {
				triggerNeighbourLoad(playerTile, atSurface, caveY, playerTile);
				loadedTiles.forEach((v, t) -> {
					if (!tiles.containsKey(v)) {
						tiles.put(v, t);
					}
				});
			});
		}
	}

	private void triggerNeighbourLoad(LazyTile tile, boolean atSurface, int caveY, LazyTile origin) {
		List<Runnable> queue = new ArrayList<>();
		loadNeighbour(tile, -1, -1, atSurface, caveY, queue, origin);
		loadNeighbour(tile, -1, 0, atSurface, caveY, queue, origin);
		loadNeighbour(tile, -1, 1, atSurface, caveY, queue, origin);
		loadNeighbour(tile, 0, 1, atSurface, caveY, queue, origin);
		loadNeighbour(tile, 1, 1, atSurface, caveY, queue, origin);
		loadNeighbour(tile, 1, 0, atSurface, caveY, queue, origin);
		loadNeighbour(tile, 1, -1, atSurface, caveY, queue, origin);
		loadNeighbour(tile, 0, -1, atSurface, caveY, queue, origin);
		if (!queue.isEmpty()) {
			for (Runnable runnable : Lists.reverse(queue)) {
				runnable.run();
			}
		}
	}

	private void loadNeighbour(LazyTile origin, int tileOffsetX, int tileOffsetY, boolean atSurface, int caveY, List<Runnable> loadQueue, LazyTile mapOrigin) {
		if (tileOffsetY != 0 || tileOffsetX != 0) {
			var tileXLeft = origin.tilePosX() + tileOffsetX;
			var tileYLeft = origin.tilePosY() + tileOffsetY;
			var vec = new Vector2i(tileXLeft, tileYLeft);
			if (!tiles.containsKey(vec)) {
				var anchorXLeft = tileXLeft * TILE_SIZE;
				var anchorYLeft = tileYLeft * TILE_SIZE;
				var tile = createTile(anchorXLeft, anchorYLeft, atSurface, caveY);
				if (tile != null) {
					tiles.put(vec, tile);
					loadQueue.add(() -> triggerNeighbourLoad(tile, atSurface, caveY, mapOrigin));
				}
			}
		}
	}

	private static Identifier getTileRl(int tileX, int tileY) {
		return AxolotlClientWaypoints.rl("world_map/tile_" + tileX + "_" + tileY);
	}

	private LazyTile createTile(int anchorX, int anchorZ, boolean atSurface, int caveY) {
		return createTile(minecraft, anchorX, anchorZ, atSurface, caveY);
	}

	private static LazyTile createTile(Minecraft minecraft, int anchorX, int anchorZ, boolean atSurface, int caveY) {
		anchorZ = anchorZ - anchorZ % TILE_SIZE;
		anchorX = anchorX - anchorX % TILE_SIZE;
		var level = minecraft.world;
		int tileX = anchorX / TILE_SIZE;
		int tileY = anchorZ / TILE_SIZE;
		WorldChunk tileChunk = ((LevelAccessor) level).invokeChunkLoadedAt(tileX, tileY, false) ? level.getChunkAt(tileX, tileY) : null;
		if (tileChunk != null) {
			return new LazyTile(tileX, tileY, () -> {
				var t = Tile.create(tileX, tileY, tileChunk);
				t.update(caveY, atSurface, level);
				return t;
			});
		}
		return null;
	}

	public static void saveLoadedChunkTile(int chunkX, int chunkZ) {
		Minecraft minecraft = Minecraft.getInstance();
		var tile = createTile(minecraft, chunkX << 4, chunkZ << 4, true, 0);
		if (tile != null) {
			tile.load(0, true)
				.thenRun(() -> {
					var dir = getCurrentLevelMapSaveDir();
					try {
						Files.createDirectories(dir);

						saveTile(tile, dir);
					} catch (IOException e) {
						log.error("Failed to create world map save dir!", e);
					}
				});
		}
	}

	private double dragMouseX;
	private double dragMouseY;

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!super.mouseClicked(mouseX, mouseY, button)) {
			if (button == 1) {
				if (hoveredWaypoint != null) {
					minecraft.openScreen(new ContextMenuScreen(this, mouseX, mouseY, new ContextMenuScreen.Type.Waypoint(hoveredWaypoint)));
				} else {
					int worldX = getWorldX(mouseX);
					int worldZ = getWorldZ(mouseY);
					minecraft.openScreen(new ContextMenuScreen(this, mouseX, mouseY, new ContextMenuScreen.Type.Map(dimension, worldX, getY(worldX, worldZ), worldZ)));
				}
				return true;
			} else if (button == 0) {
				dragMouseX = (mouseX - dragOffset.x);
				dragMouseY = (mouseY - dragOffset.y);
			}
			return false;
		}
		return true;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (!super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
			if (button == 0) {
				dragOffset.set((float) (mouseX - dragMouseX), (float) (mouseY - dragMouseY), 0);
				return true;
			}
			return false;
		}
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (!super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
			if (scrollY > 0) {
				scale *= 2;
				var offsetX = width / 2f + dragOffset.x();
				var offsetY = height / 2f + dragOffset.y();
				var mirroredOnOffsetX = offsetX - (mouseX - offsetX);
				var mirroredOnOffsetY = offsetY - (mouseY - offsetY);
				dragOffset.set(mirroredOnOffsetX - width / 2f, mirroredOnOffsetY - height / 2f, 0);
			} else {
				scale /= 2;
				var offsetX = width / 2f + dragOffset.x();
				var offsetY = height / 2f + dragOffset.y();
				var mirroredOnOffsetX = offsetX + (mouseX - offsetX) / 2f;
				var mirroredOnOffsetY = offsetY + (mouseY - offsetY) / 2f;
				dragOffset.set(mirroredOnOffsetX - width / 2f, mirroredOnOffsetY - height / 2f, 0);
			}
		}
		return true;
	}

	@Override
	public void init() {
		class Overlay implements Element, Selectable {

			@Override
			public boolean isFocused() {
				return true;
			}

			@Override
			public void setFocused(boolean focused) {

			}

			@Override
			public SelectionType getType() {
				return SelectionType.NONE;
			}

			@Override
			public boolean isMouseOver(double mouseX, double mouseY) {
				return true;
			}

			boolean called = false;

			@Override
			public boolean mouseScrolled(double mouseX, double mouseY, double amountX, double amountY) {
				if (called) return false;
				called = true;
				var bl = WorldMapScreen.this.mouseScrolled(mouseX, mouseY, amountX, amountY);
				called = false;
				return bl;
			}

			@Override
			public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
				if (called) return false;
				called = true;
				var bl = WorldMapScreen.super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
				called = false;
				return bl;
			}
		}
		addSelectableChild(new Overlay());
		addDrawableChild(new ImageButton(4, height - 20, 16, 16, new ImageButton.WidgetSprites(OPTIONS_SPRITE, OPTIONS_SPRITE, OPTIONS_HOVERED_SPRITE),
			btn -> minecraft.openScreen(AxolotlClientWaypoints.createOptionsScreen(this)), AxolotlClientWaypoints.tr("options")));
		var slider = addDrawableChild(new AbstractSliderButton(width - 150, 20, 150, 20, AxolotlClientWaypoints.tr("player_y"), 0) {
			final int min = MIN_BUILD_HEIGHT - 1;
			final int max = minecraft.world.getHeight() + 1;

			@Override
			protected void updateMessage() {
				if (value == 0) {
					setMessage(AxolotlClientWaypoints.tr("player_y"));
				} else {
					setMessage(String.valueOf(caveY - 1));
				}
			}

			@Override
			protected void applyValue() {
				caveY = (int) (min + (max - min) * value);
				atSurface = false;
				if (value == 0) {
					collectPlayerYData();
				}
				CompletableFuture.runAsync(() -> tiles.values().forEach(t -> t.update(caveY, atSurface, minecraft.world)));
			}
		});
		addDrawableChild(new DropdownButton(width - 20, 0, 20, 20,
			AxolotlClientWaypoints.tr("open_dropdown"), (btn, val) -> slider.visible = val));
		slider.visible = false;
		optionUpdate = () -> {
			var allowsCaves = allowCaves || (allowCavesNether && minecraft.world.dimension.isDark());
			boolean updated = slider.active != allowsCaves;
			if (updated) {
				slider.active = allowsCaves;
				if (!allowsCaves) {
					atSurface = true;
					CompletableFuture.runAsync(() -> tiles.values().forEach(t -> t.update(caveY, atSurface, minecraft.world)));
				} else {
					slider.applyValue();
				}
			}
		};
		AxolotlClientWaypoints.NETWORK_LISTENER.postReceive.add(optionUpdate);
		optionUpdate.run();
		if (tiles.isEmpty()) {
			minecraft.submit(() -> {
				if (!initializedOnce) {
					collectPlayerYData();
				}
				loadSavedTiles();
				createTiles();
			});
		}
		initializedOnce = true;
	}

	@Override
	public void tick() {

	}

	private void loadSavedTiles() {
		var dir = getCurrentLevelMapSaveDir();
		if (Files.exists(dir)) {
			try (var s = Files.list(dir)) {
				s.forEach(file -> {
					var name = file.getFileName().toString();
					if (!name.endsWith(Tile.FILE_EXTENSION)) {
						return;
					}
					name = name.substring(0, name.indexOf("."));
					var coords = name.split("_");
					int x = Integer.parseInt(coords[0]);
					int y = Integer.parseInt(coords[1]);
					var key = new Vector2i(x, y);
					if (!tiles.containsKey(key)) {
						try {
							var tile = new LazyTile(x, y, () -> {
								try {
									return Tile.read(file, minecraft.world);
								} catch (IOException e) {
									return null;
								}
							});
							tiles.put(key, tile);
						} catch (Exception e) {
							log.warn("Failed to load tile at {}, {}", x, y, e);
						}
					}
				});
			} catch (IOException e) {
				log.info("Failed to load saved world map tiles!", e);
			}
		}
	}

	private void saveTiles() {
		var dir = getCurrentLevelMapSaveDir();
		try {
			Files.createDirectories(dir);

			tiles.values().forEach((tile) -> saveTile(tile, dir));
		} catch (IOException e) {
			log.error("Failed to create world map save dir!", e);
		}
	}

	private static void saveTile(LazyTile tile, Path dir) {
		try {
			tile.save(dir);
		} catch (IOException e) {
			log.warn("Failed to save tile at {}, {}", tile.tilePosX(), tile.tilePosY(), e);
		}
	}

	private static Path getCurrentLevelMapSaveDir() {
		return AxolotlClientWaypoints.getCurrentStorageDir().resolve("worldmap");
	}

	@Override
	public void removed() {
		if (optionUpdate != null) {
			AxolotlClientWaypoints.NETWORK_LISTENER.postReceive.remove(optionUpdate);
		}
		if (minecraft.screen == null) {
			saveTiles();
			tiles.values().forEach(LazyTile::release);
			tiles.clear();
		}
	}

	@RequiredArgsConstructor
	private static class LazyTile {
		private Tile tile;
		@Getter
		@Accessors(fluent = true)
		private final int tilePosX, tilePosY;
		private final Supplier<Tile> supplier;
		private final Vector3f pos = new Vector3f();
		private boolean loaded;

		public void render(Matrix4fStack matrixStack, float playerX, float playerZ, float scale, float delta, int caveY, boolean atSurface, int guiWidth, int guiHeight) {
			float x = tilePosX() * TILE_SIZE - playerX;
			float y = tilePosY() * TILE_SIZE - playerZ;
			GlStateManager.pushMatrix();
			matrixStack.pushMatrix();
			GlStateManager.translatef(x, y, 0);
			matrixStack.translate(x, y, 0);
			pos.zero();
			matrixStack.transformPosition(pos);
			if (pos.x + TILE_SIZE * scale >= 0 && pos.x < guiWidth && pos.y + TILE_SIZE * scale >= 0 && pos.y < guiHeight) {
				if (tile == null) {
					if (!loaded) {
						load(caveY, atSurface);
					}
				} else {
					// FIXME - floating point precision errors?
					Minecraft.getInstance().getTextureManager().bind(tile.rl());
					drawTexture(0, 0, 0, 0, TILE_SIZE, TILE_SIZE, TILE_SIZE, TILE_SIZE);
				}
			}
			matrixStack.popMatrix();
			GlStateManager.popMatrix();
		}

		public CompletableFuture<?> load(int caveY, boolean atSurface) {
			if (!loaded) {
				loaded = true;
				return CompletableFuture.supplyAsync(supplier, Minecraft.getInstance()::submit).thenApplyAsync(t -> {
					t.update(caveY, atSurface, Minecraft.getInstance().world);
					return t;
				}, ForkJoinPool.commonPool()).thenApply(t -> tile = t);
			}
			return CompletableFuture.completedFuture(null);
		}

		public void release() {
			if (tile != null) {
				tile.release();
			}
		}

		public void save(Path dir) throws IOException {
			if (tile != null) {
				tile.save(dir);
			}
		}

		public void update(int caveY, boolean atSurface, World level) {
			if (tile != null) {
				tile.update(caveY, atSurface, level);
			}
		}
	}

	private record Tile(int tilePosX, int tilePosY, Identifier rl, DynamicTexture tex,
						LevelChunkStorage.Entry chunk) {
		public static final String FILE_EXTENSION = ".bin";

		public void release() {
			Minecraft.getInstance().getTextureManager().close(rl);
		}

		public void save(Path dir) throws IOException {
			var out = dir.resolve("%d_%d%s".formatted(tilePosX, tilePosY, FILE_EXTENSION));
			chunk.write(out);
		}

		public static Tile create(int x, int y, WorldChunk chunk) {
			return create(x, y, new LevelChunkStorage.Entry(chunk));
		}

		public static Tile create(int x, int y, LevelChunkStorage.Entry chunk) {
			var rl = getTileRl(x, y);
			var tex = new DynamicTexture(TILE_SIZE, TILE_SIZE);
			Arrays.fill(tex.getPixels(), Colors.BLACK.toInt());
			Minecraft.getInstance().getTextureManager().register(rl, tex);
			return new Tile(x, y, rl, tex, chunk);
		}

		public static Tile read(Path p, World level) throws IOException {
			var name = p.getFileName().toString();
			name = name.substring(0, name.indexOf("."));
			var coords = name.split("_");
			int x = Integer.parseInt(coords[0]);
			int y = Integer.parseInt(coords[1]);
			return create(x, y, LevelChunkStorage.Entry.read(p, level));
		}

		public void update(int caveY, boolean atSurface, World level) {
			if (chunk.chunk().isEmpty()) {
				Arrays.fill(tex.getPixels(), Colors.BLACK.toInt());
				Minecraft.getInstance().submit(tex::upload);
				return;
			}
			int levelMinY = MIN_BUILD_HEIGHT;
			int centerX = (tilePosX * TILE_SIZE) + TILE_SIZE / 2;
			int centerZ = (tilePosY * TILE_SIZE) + TILE_SIZE / 2;

			var pixels = tex.getPixels();
			int size = TILE_SIZE;
			int texHalfWidth = size / 2;

			BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable();
			BlockPos.Mutable mutableBlockPos2 = new BlockPos.Mutable();
			boolean updated = false;
			for (int x = 0; x < size; x++) {
				double d = 0;
				for (int z = -1; z < size; z++) {
					int chunkX = (centerX + x - texHalfWidth);
					int chunkZ = (centerZ + z - texHalfWidth);

					int fluidDepth = 0;
					double e = 0.0;
					mutableBlockPos.set(chunkX, 0, chunkZ);
					WorldChunk levelChunk;
					int y;
					if (z < 0) {
						mutableBlockPos.set(chunkX, 0, chunkZ + 1);
					}
					levelChunk = chunk.chunk();
					y = levelChunk.getHeight(mutableBlockPos.getX() & 15, mutableBlockPos.getZ() & 15) + 1;
					if (!atSurface) {
						y = Math.min(y, caveY);
					}
					BlockState blockState;
					if (y <= levelMinY) {
						blockState = Blocks.AIR.defaultState();
					} else {
						do {
							mutableBlockPos.set(mutableBlockPos.getX(), --y, mutableBlockPos.getZ());
							blockState = levelChunk.getBlockState(mutableBlockPos);
						} while (blockState.getBlock().getMapColor(blockState) == MapColor.AIR && y > levelMinY);

						if (y > levelMinY && blockState.getBlock().getMaterial().isLiquid()) {
							int highestFullBlockY = y - 1;
							mutableBlockPos2.set(mutableBlockPos.getX(), mutableBlockPos.getY(), mutableBlockPos.getZ());

							BlockState blockState2;
							do {
								mutableBlockPos2.set(mutableBlockPos2.getX(), highestFullBlockY--, mutableBlockPos2.getZ());
								blockState2 = levelChunk.getBlockState(mutableBlockPos2);
								fluidDepth++;
							} while (highestFullBlockY > levelMinY && blockState2.getBlock().getMaterial().isLiquid());

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
					/*if (Minimap.useTextureSampling.get()) {
						if (z >= 0 && !blockState.isAir()) {
							final int fz = z, fx = x;
							TextureSampler.getSample(blockState, level, mutableBlockPos, brightness).thenAccept(color -> {
								color = ARGB.opaque(color);
								if (Integer.rotateRight(pixels.getPixelRGBA(fx, fz), 4) != color) {
									pixels.setPixelRGBA(fx, fz, Integer.rotateLeft(color, 4));
									tex.upload();
								}
							});
						}
					} else*/
					{
						int color = mapColor.getColor(brightness);
						if (z >= 0 && pixels[x + z * TILE_SIZE] != color) {
							pixels[x + z * TILE_SIZE] = ARGB.opaque(color);
							updated = true;
						}
					}
				}
			}

			if (updated) {
				Minecraft.getInstance().submit(tex::upload);
			}
		}
	}
}
