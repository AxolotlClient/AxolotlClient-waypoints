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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.axolotlclient.AxolotlClientConfig.api.util.Colors;
import io.github.axolotlclient.waypoints.AxolotlClientWaypoints;
import io.github.axolotlclient.waypoints.map.util.LevelChunkStorage;
import io.github.axolotlclient.waypoints.map.widgets.DropdownButton;
import io.github.axolotlclient.waypoints.map.widgets.ImageButton;
import io.github.axolotlclient.waypoints.map.widgets.WidgetSprites;
import io.github.axolotlclient.waypoints.util.ARGB;
import io.github.axolotlclient.waypoints.waypoints.Waypoint;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import org.joml.Vector2i;
import org.joml.Vector3f;

@SuppressWarnings({"DataFlowIssue"})
@Slf4j
public class WorldMapScreen extends Screen {
	private static final int TILE_SIZE = 16;
	private static final ResourceLocation OPTIONS_SPRITE = AxolotlClientWaypoints.rl("textures/gui/sprites/options.png");
	private static final ResourceLocation OPTIONS_HOVERED_SPRITE = AxolotlClientWaypoints.rl("textures/gui/sprites/options_hovered.png");

	private final Map<Vector2i, LazyTile> tiles = new ConcurrentHashMap<>();
	private final Vector3f dragOffset = new Vector3f();
	private float scale = 1f;
	private boolean atSurface;
	private int caveY;
	private ResourceLocation dimension;
	private Waypoint hoveredWaypoint = null;
	private boolean initializedOnce = false;

	public WorldMapScreen() {
		super(AxolotlClientWaypoints.tr("worldmap"));
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		renderBackground(guiGraphics);
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(width / 2f, height / 2f, 0);
		guiGraphics.pose().last().pose().translate(dragOffset);
		guiGraphics.pose().pushPose();
		guiGraphics.pose().scale(scale, scale, 1);

		var playerPos = minecraft.player.position();

		for (LazyTile tile : tiles.values()) {
			tile.render(guiGraphics, (float) playerPos.x(), (float) playerPos.z(), scale, partialTick, caveY, atSurface);
		}

		guiGraphics.pose().popPose();
		renderMapWaypoints(guiGraphics, mouseX, mouseY);

		guiGraphics.pose().last().pose().rotate((float) (((minecraft.player.getVisualRotationYInDegrees() + 180) / 180) * Math.PI), 0, 0, 1);
		guiGraphics.pose().scale(0.5f * AxolotlClientWaypoints.MINIMAP.arrowScale.get(), 0.5f * AxolotlClientWaypoints.MINIMAP.arrowScale.get(), 1);
		int arrowSize = 15;
		guiGraphics.pose().translate(-arrowSize / 2f, -arrowSize / 2f, 0);
		guiGraphics.blit(Minimap.arrowLocation, 0, 0, 0, 0, arrowSize, arrowSize, arrowSize, arrowSize);
		guiGraphics.pose().popPose();
		super.render(guiGraphics, mouseX, mouseY, partialTick);

		if (mouseX > -1 && mouseY > -1) {
			int x = getWorldX(mouseX);
			int z = getWorldZ(mouseY);
			guiGraphics.drawCenteredString(font, AxolotlClientWaypoints.tr("position", String.valueOf(x), String.valueOf(getY(x, z)), String.valueOf(z)), width / 2, height - 15, Colors.GRAY.toInt());
		}
	}

	private int getY(int x, int z) {
		int tileX = x / TILE_SIZE;
		int tileY = z / TILE_SIZE;
		var tile = tiles.get(new Vector2i(tileX - 1, tileY - 1));
		ChunkAccess c;
		if (tile == null || tile.tile == null) {
			c = minecraft.level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z), ChunkStatus.FULL, false);
		} else c = tile.tile.chunk.chunk();
		if (c == null) return minecraft.level.getMinBuildHeight();
		int y = c.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
		if (atSurface) {
			return y;
		}
		y = Math.min(y, caveY);
		BlockState blockState;
		var mutableBlockPos = new BlockPos.MutableBlockPos(x, 0, z);
		do {
			mutableBlockPos.setY(--y);
			blockState = c.getBlockState(mutableBlockPos);
		} while (blockState.getMapColor(minecraft.level, mutableBlockPos) == MapColor.NONE && y > minecraft.level.getMinBuildHeight());
		return y;
	}

	private int getWorldX(int guiX) {
		return Mth.floor(minecraft.player.getX() - ((width / 2f + dragOffset.x()) - guiX) / scale);
	}

	private int getWorldZ(int guiZ) {
		return Mth.floor(minecraft.player.getZ() - ((height / 2f + dragOffset.y()) - guiZ) / scale);
	}

	private void renderMapWaypoints(GuiGraphics graphics, int mouseX, int mouseY) {
		if (!AxolotlClientWaypoints.renderWaypoints.get()) return;
		graphics.pose().pushPose();
		var pos = new Vector3f();
		hoveredWaypoint = null;
		for (Waypoint waypoint : AxolotlClientWaypoints.getCurrentWaypoints()) {
			graphics.pose().pushPose();
			float posX = (float) (waypoint.x() - minecraft.player.getX()) + 1;
			float posY = (float) (waypoint.z() - minecraft.player.getZ()) + 1;

			graphics.pose().translate(posX * scale, posY * scale, 0);
			pos.zero();
			graphics.pose().last().pose().transformPosition(pos);

			int textWidth = font.width(waypoint.display());
			int width = textWidth + Waypoint.displayXOffset() * 2;
			int textHeight = font.lineHeight;
			int height = textHeight + Waypoint.displayYOffset() * 2;
			pos.sub(width / 2f, height / 2f, 0);
			graphics.fill(-(width / 2), -(height / 2), (width / 2), (height / 2), waypoint.color().toInt());
			graphics.drawString(font, waypoint.display(), -(textWidth / 2), -textHeight / 2, -1, false);
			if (hoveredWaypoint == null) {
				if (mouseX >= pos.x() && mouseY >= pos.y() && mouseX < pos.x() + width && mouseY < pos.y() + height) {
					hoveredWaypoint = waypoint;
					graphics.renderOutline(-width / 2, -height / 2, width, height, Colors.WHITE.toInt());
				}
			}
			graphics.pose().popPose();
		}
		graphics.pose().popPose();
	}

	private void collectPlayerYData() {
		var level = minecraft.level;
		int playerX = minecraft.player.getBlockX();
		int playerZ = minecraft.player.getBlockZ();
		var centerChunk = level.getChunk(SectionPos.blockToSectionCoord(playerX), SectionPos.blockToSectionCoord(playerZ));
		var surface = centerChunk.getHeight(Heightmap.Types.WORLD_SURFACE, playerX, playerZ);
		BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos(playerX, surface, playerZ);
		int solidBlocksAbovePlayer = 0;
		atSurface = false;
		if (level.dimensionType().hasCeiling()) {
			atSurface = minecraft.player.getBlockY() >= level.dimensionType().logicalHeight();
		} else if (surface + 1 <= minecraft.player.getBlockY()) {
			atSurface = true;
		} else {
			while (solidBlocksAbovePlayer <= 3 && surface > minecraft.player.getBlockY() && surface > level.getMinBuildHeight()) {
				BlockState state = centerChunk.getBlockState(mutableBlockPos);
				mutableBlockPos.setY(surface--);
				if (!(state.propagatesSkylightDown(level, mutableBlockPos.below()) || !state.canOcclude() || !state.isViewBlocking(level, mutableBlockPos))) {
					solidBlocksAbovePlayer++;
				}
			}
			if (solidBlocksAbovePlayer <= 2) {
				atSurface = true;
			}
		}

		caveY = minecraft.player.getBlockY();
		dimension = level.dimension().location();
	}

	private void createTiles() {
		int playerX = minecraft.player.getBlockX();
		int playerZ = minecraft.player.getBlockZ();

		var playerTile = createTile(playerX, playerZ, atSurface, caveY);
		if (playerTile != null) {
			Map<Vector2i, LazyTile> loadedTiles = new HashMap<>(tiles);
			tiles.clear();
			tiles.put(new Vector2i(playerTile.tilePosX(), playerTile.tilePosY()), playerTile);
			minecraft.execute(() -> {
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
			Lists.reverse(queue).forEach(Runnable::run);
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

	private static ResourceLocation getTileRl(int tileX, int tileY) {
		return AxolotlClientWaypoints.rl("world_map/tile_" + tileX + "_" + tileY);
	}

	private LazyTile createTile(int anchorX, int anchorZ, boolean atSurface, int caveY) {
		return createTile(minecraft, anchorX, anchorZ, atSurface, caveY);
	}

	private static LazyTile createTile(Minecraft minecraft, int anchorX, int anchorZ, boolean atSurface, int caveY) {
		anchorZ = anchorZ - anchorZ % TILE_SIZE;
		anchorX = anchorX - anchorX % TILE_SIZE;
		var level = minecraft.level;
		int tileX = anchorX / TILE_SIZE;
		int tileY = anchorZ / TILE_SIZE;
		ChunkAccess tileChunk = level.getChunk(tileX, tileY, ChunkStatus.FULL, false);
		if (tileChunk != null) {
			return new LazyTile(tileX, tileY, () -> {
				var t = Tile.create(tileX, tileY, tileChunk);
				t.update(caveY, atSurface, level);
				return t;
			});
		}
		return null;
	}

	public static void saveLoadedChunkTile(ChunkPos pos) {
		Minecraft minecraft = Minecraft.getInstance();
		var tile = createTile(minecraft, pos.getMinBlockX(), pos.getMinBlockZ(), true, 0);
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

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!super.mouseClicked(mouseX, mouseY, button)) {
			if (button == 1) {
				if (hoveredWaypoint != null) {
					minecraft.setScreen(new ContextMenuScreen(this, mouseX, mouseY, new ContextMenuScreen.Type.Waypoint(hoveredWaypoint)));
				} else {
					int worldX = getWorldX((int) mouseX);
					int worldZ = getWorldZ((int) mouseY);
					minecraft.setScreen(new ContextMenuScreen(this, mouseX, mouseY, new ContextMenuScreen.Type.Map(dimension.toString(), worldX, getY(worldX, worldZ), worldZ)));
				}
				return true;
			}
			return false;
		}
		return true;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (!super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
			if (button == 0) {
				dragOffset.add((float) dragX, (float) dragY, 0);
				return true;
			}
			return false;
		}
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
		if (!super.mouseScrolled(mouseX, mouseY, scrollY)) {
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
	protected void init() {
		addRenderableWidget(new ImageButton(4, height - 20, 16, 16, new WidgetSprites(OPTIONS_SPRITE, OPTIONS_SPRITE, OPTIONS_HOVERED_SPRITE),
			btn -> minecraft.setScreen(AxolotlClientWaypoints.createOptionsScreen(this)), AxolotlClientWaypoints.tr("options")));
		var slider = addRenderableWidget(new AbstractSliderButton(width - 150, 20, 150, 20, AxolotlClientWaypoints.tr("player_y"), 0) {
			final int min = minecraft.level.getMinBuildHeight();
			final int max = minecraft.level.getMaxBuildHeight()+1;

			@Override
			protected void updateMessage() {
				if (value == 0 || Math.floor((max - min) * value) == 0) {
					setMessage(AxolotlClientWaypoints.tr("player_y"));
				} else {
					setMessage(Component.literal(String.valueOf(caveY - 1)));
				}
			}

			@Override
			protected void applyValue() {
				caveY = (int) (min + (max - min) * value);
				atSurface = false;
				if (value == 0 || Math.floor((max - min) * value) == 0) {
					collectPlayerYData();
				}
				CompletableFuture.runAsync(() -> tiles.values().forEach(t -> t.update(caveY, atSurface, minecraft.level)));
			}
		});
		addRenderableWidget(new DropdownButton(width - 20, 0, 20, 20,
			AxolotlClientWaypoints.tr("open_dropdown"), (btn, val) -> slider.visible = val));
		slider.visible = false;
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
									return Tile.read(file, minecraft.level);
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

		public void render(GuiGraphics guiGraphics, float playerX, float playerZ, float scale, float delta, int caveY, boolean atSurface) {
			float x = tilePosX() * TILE_SIZE - playerX;
			float y = tilePosY() * TILE_SIZE - playerZ;
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(x, y, 0);
			pos.zero();
			guiGraphics.pose().last().pose().transformPosition(pos);
			if (pos.x + TILE_SIZE * scale >= 0 && pos.x < guiGraphics.guiWidth() && pos.y + TILE_SIZE * scale >= 0 && pos.y < guiGraphics.guiHeight()) {
				if (tile == null) {
					if (!loaded) {
						load(caveY, atSurface);
					}
				} else {
					// FIXME - floating point precision errors?
					guiGraphics.blit(tile.rl(), 0, 0, 0, 0, TILE_SIZE, TILE_SIZE, TILE_SIZE, TILE_SIZE);
				}
			}
			guiGraphics.pose().popPose();
		}

		public CompletableFuture<?> load(int caveY, boolean atSurface) {
			if (!loaded) {
				loaded = true;
				return Minecraft.getInstance().submit(supplier).thenApplyAsync(t -> {
					t.update(caveY, atSurface, Minecraft.getInstance().level);
					return t;
				}, Util.backgroundExecutor()).thenApply(t -> tile = t);
			}
			return CompletableFuture.completedFuture(null);
		}

		public void release() {
			if (tile != null) {
				tile.release();
				tile = null;
			}
		}

		public void save(Path dir) throws IOException {
			if (tile != null) {
				tile.save(dir);
			}
		}

		public void update(int caveY, boolean atSurface, Level level) {
			if (tile != null) {
				tile.update(caveY, atSurface, level);
			}
		}
	}

	private record Tile(int tilePosX, int tilePosY, ResourceLocation rl, DynamicTexture tex,
						LevelChunkStorage.Entry chunk) {
		public static final String FILE_EXTENSION = ".bin";

		public void release() {
			Minecraft.getInstance().getTextureManager().release(rl);
		}

		public void save(Path dir) throws IOException {
			var out = dir.resolve("%d_%d%s".formatted(tilePosX, tilePosY, FILE_EXTENSION));
			chunk.write(out);
		}

		public static Tile create(int x, int y, ChunkAccess chunk) {
			return create(x, y, new LevelChunkStorage.Entry(chunk));
		}

		public static Tile create(int x, int y, LevelChunkStorage.Entry chunk) {
			var rl = getTileRl(x, y);
			var tex = new DynamicTexture(new NativeImage(TILE_SIZE, TILE_SIZE, false));
			tex.getPixels().fillRect(0, 0, TILE_SIZE, TILE_SIZE, Colors.BLACK.toInt());
			Minecraft.getInstance().getTextureManager().register(rl, tex);
			return new Tile(x, y, rl, tex, chunk);
		}

		public static Tile read(Path p, Level level) throws IOException {
			var name = p.getFileName().toString();
			name = name.substring(0, name.indexOf("."));
			var coords = name.split("_");
			int x = Integer.parseInt(coords[0]);
			int y = Integer.parseInt(coords[1]);
			return create(x, y, LevelChunkStorage.Entry.read(p, level));
		}

		public void update(int caveY, boolean atSurface, Level level) {
			if (chunk.chunk() instanceof LevelChunk levelChunk && levelChunk.isEmpty()) {
				tex.getPixels().fillRect(0, 0, TILE_SIZE, TILE_SIZE, Colors.BLACK.toInt());
				tex.upload();
				return;
			}
			int levelMinY = level.getMinBuildHeight();
			int centerX = (tilePosX * TILE_SIZE) + TILE_SIZE / 2;
			int centerZ = (tilePosY * TILE_SIZE) + TILE_SIZE / 2;

			var pixels = tex.getPixels();
			int size = pixels.getWidth();
			int texHalfWidth = size / 2;

			BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
			BlockPos.MutableBlockPos mutableBlockPos2 = new BlockPos.MutableBlockPos();
			boolean updated = false;
			for (int x = 0; x < size; x++) {
				double d = 0;
				for (int z = -1; z < size; z++) {
					int chunkX = (centerX + x - texHalfWidth);
					int chunkZ = (centerZ + z - texHalfWidth);

					int fluidDepth = 0;
					double e = 0.0;
					mutableBlockPos.set(chunkX, 0, chunkZ);
					ChunkAccess levelChunk;
					int y;
					if (z < 0 || z >= TILE_SIZE) {
						mutableBlockPos.setZ(chunkZ + 1);
					}
					levelChunk = chunk.chunk();
					y = levelChunk.getHeight(Heightmap.Types.WORLD_SURFACE, mutableBlockPos.getX(), mutableBlockPos.getZ()) + 1;
					if (!atSurface) {
						y = Math.min(y, caveY);
					}
					BlockState blockState;
					if (y <= levelMinY) {
						blockState = Blocks.AIR.defaultBlockState();
					} else {
						do {
							mutableBlockPos.setY(--y);
							blockState = levelChunk.getBlockState(mutableBlockPos);
						} while (blockState.getMapColor(level, mutableBlockPos) == MapColor.NONE && y > levelMinY);

						if (y > levelMinY && !blockState.getFluidState().isEmpty()) {
							int highestFullBlockY = y - 1;
							mutableBlockPos2.set(mutableBlockPos);

							BlockState blockState2;
							do {
								mutableBlockPos2.setY(highestFullBlockY--);
								blockState2 = levelChunk.getBlockState(mutableBlockPos2);
								fluidDepth++;
							} while (highestFullBlockY > levelMinY && !blockState2.getFluidState().isEmpty());

							FluidState fluidState = blockState.getFluidState();
							blockState = !fluidState.isEmpty() && !blockState.isFaceSturdy(level, mutableBlockPos, Direction.UP) ? fluidState.createLegacyBlock() : blockState;
						}
					}

					e += y;
					var mapColor = blockState.getMapColor(level, mutableBlockPos);

					MapColor.Brightness brightness;
					if (mapColor == MapColor.WATER) {
						double f = fluidDepth * 0.1 + (x + z & 1) * 0.2;
						if (f < 0.5) {
							brightness = MapColor.Brightness.HIGH;
						} else if (f > 0.9) {
							brightness = MapColor.Brightness.LOW;
						} else {
							brightness = MapColor.Brightness.NORMAL;
						}
					} else {
						double f = (e - d) * 4.0 / (1 + 4) + ((x + z & 1) - 0.5) * 0.4;
						if (f > 0.6) {
							brightness = MapColor.Brightness.HIGH;
						} else if (f < -0.6) {
							brightness = MapColor.Brightness.LOW;
						} else {
							brightness = MapColor.Brightness.NORMAL;
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
						int color = mapColor.calculateRGBColor(brightness);
						if (z >= 0 && Integer.rotateRight(pixels.getPixelRGBA(x, z), 4) != color) {
							pixels.setPixelRGBA(x, z, ARGB.opaque(color));
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
