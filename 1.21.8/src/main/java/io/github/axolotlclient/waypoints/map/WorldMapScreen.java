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

import com.mojang.blaze3d.platform.NativeImage;
import io.github.axolotlclient.AxolotlClientConfig.api.util.Colors;
import io.github.axolotlclient.waypoints.AxolotlClientWaypoints;
import io.github.axolotlclient.waypoints.map.util.LevelChunkStorage;
import io.github.axolotlclient.waypoints.map.widgets.DropdownButton;
import io.github.axolotlclient.waypoints.util.ARGB;
import io.github.axolotlclient.waypoints.waypoints.Waypoint;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
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
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import org.joml.Vector2f;
import org.joml.Vector2i;

@SuppressWarnings({"DataFlowIssue", "ResultOfMethodCallIgnored"})
@Slf4j
public class WorldMapScreen extends Screen {
	private static final int TILE_SIZE = 16;
	private static final ResourceLocation OPTIONS_SPRITE = AxolotlClientWaypoints.rl("options");
	private static final ResourceLocation OPTIONS_HOVERED_SPRITE = OPTIONS_SPRITE.withSuffix("_hovered");

	private final Map<Vector2i, LazyTile> tiles = new ConcurrentHashMap<>();
	private final Vector2f dragOffset = new Vector2f();
	private float scale = 1f;
	public static boolean allowCaves = true, allowCavesNether;
	private boolean atSurface;
	private int caveY;
	private ResourceLocation dimension;
	private Waypoint hoveredWaypoint = null;
	private boolean initializedOnce = false;
	private Runnable optionUpdate;

	public WorldMapScreen() {
		super(AxolotlClientWaypoints.tr("worldmap"));
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		guiGraphics.pose().pushMatrix();
		guiGraphics.pose().translate(width / 2f, height / 2f);
		guiGraphics.pose().translate(dragOffset);
		guiGraphics.pose().pushMatrix();
		guiGraphics.pose().scale(scale);

		var playerPos = minecraft.player.position();

		for (LazyTile tile : tiles.values()) {
			tile.render(guiGraphics, (float) playerPos.x(), (float) playerPos.z(), scale, partialTick, caveY, atSurface);
		}

		int x = getWorldX(mouseX);
		int z = getWorldZ(mouseY);
		{
			guiGraphics.pose().pushMatrix();
			guiGraphics.pose().translate((float) -playerPos.x(), (float) -playerPos.z());
			int tileX = x / TILE_SIZE;
			int tileY = z / TILE_SIZE;
			if (x < 0 && x % TILE_SIZE != 0) {
				tileX -= 1;
			}
			if (z < 0 && z % TILE_SIZE != 0) {
				tileY -= 1;
			}
			guiGraphics.pose().translate(tileX*TILE_SIZE, tileY*TILE_SIZE);
			guiGraphics.fill(0, 0, TILE_SIZE, TILE_SIZE, 0x33FFFFFF);
			guiGraphics.renderOutline(0, 0, TILE_SIZE, TILE_SIZE, 0x33FFFFFF);
			guiGraphics.pose().popMatrix();
		}

		guiGraphics.pose().popMatrix();

		renderMapWaypoints(guiGraphics, mouseX, mouseY);

		guiGraphics.pose().rotate((float) (((minecraft.player.getVisualRotationYInDegrees() + 180) / 180) * Math.PI));
		guiGraphics.pose().scale(0.5f * AxolotlClientWaypoints.MINIMAP.arrowScale.get(), 0.5f * AxolotlClientWaypoints.MINIMAP.arrowScale.get());
		int arrowSize = 15;
		guiGraphics.pose().translate(-arrowSize / 2f, -arrowSize / 2f);
		guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, Minimap.arrowLocation, 0, 0, arrowSize, arrowSize);
		guiGraphics.pose().popMatrix();

		super.render(guiGraphics, mouseX, mouseY, partialTick);

		guiGraphics.drawCenteredString(getFont(), AxolotlClientWaypoints.tr("position", String.valueOf(x), String.valueOf(getY(x, z)), String.valueOf(z)), width / 2, height - 15, Colors.GRAY.toInt());
	}

	private int getY(int x, int z) {
		int tileX = x / TILE_SIZE;
		int tileY = z / TILE_SIZE;
		if (x < 0 && x % TILE_SIZE != 0) {
			tileX -= 1;
		}
		if (z < 0 && z % TILE_SIZE != 0) {
			tileY -= 1;
		}
		var tile = tiles.get(new Vector2i(tileX, tileY));
		ChunkAccess c;
		if (tile == null || tile.tile == null) {
			c = minecraft.level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z), ChunkStatus.FULL, false);
		} else c = tile.tile.chunk.chunk();
		if (c == null) return minecraft.level.getMinY();
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
		} while (blockState.getMapColor(minecraft.level, mutableBlockPos) == MapColor.NONE && y > minecraft.level.getMinY());
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
		graphics.pose().pushMatrix();
		var pos = new Vector2f();
		hoveredWaypoint = null;
		for (Waypoint waypoint : AxolotlClientWaypoints.getCurrentWaypoints()) {
			graphics.pose().pushMatrix();
			float posX = (float) (waypoint.x() - minecraft.player.getX()) + 1;
			float posY = (float) (waypoint.z() - minecraft.player.getZ()) + 1;

			graphics.pose().translate(posX * scale, posY * scale);
			pos.zero();
			graphics.pose().transformPosition(pos);

			int textWidth = getFont().width(waypoint.display());
			int width = textWidth + Waypoint.displayXOffset() * 2;
			int textHeight = getFont().lineHeight;
			int height = textHeight + Waypoint.displayYOffset() * 2;
			pos.sub(width / 2f, height / 2f);
			graphics.fill(-(width / 2), -(height / 2), (width / 2), (height / 2), waypoint.color().toInt());
			graphics.drawString(getFont(), waypoint.display(), -(textWidth / 2), -textHeight / 2, -1, false);
			if (hoveredWaypoint == null) {
				if (mouseX >= pos.x() && mouseY >= pos.y() && mouseX < pos.x() + width && mouseY < pos.y() + height) {
					hoveredWaypoint = waypoint;
					graphics.renderOutline(-width / 2, -height / 2, width, height, Colors.WHITE.toInt());
				}
			}
			graphics.pose().popMatrix();
		}
		graphics.pose().popMatrix();
	}

	private void collectPlayerYData() {
		var level = minecraft.level;
		if (allowCaves || (allowCavesNether && level.dimensionType().hasCeiling())) {
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
		} else {
			atSurface = true;
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
			triggerNeighbourLoad(playerTile, atSurface, caveY, playerTile);
			loadedTiles.forEach((v, t) -> {
				if (!tiles.containsKey(v)) {
					tiles.put(v, t);
				}
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
			queue.reversed().forEach(Runnable::run);
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
		if (anchorX < 0 && anchorX % TILE_SIZE != 0) {
			tileX -= 1;
		}
		if (anchorZ < 0 && anchorZ % TILE_SIZE != 0) {
			tileY -= 1;
		}
		ChunkAccess tileChunk = level.getChunk(tileX, tileY, ChunkStatus.FULL, false);
		if (tileChunk != null) {
			int finalTileX = tileX;
			int finalTileY = tileY;
			return new LazyTile(tileX, tileY, () -> Tile.create(finalTileX, finalTileY, tileChunk));
		}
		return null;
	}

	public static void saveLoadedChunkTile(ChunkPos pos) {
		Minecraft minecraft = Minecraft.getInstance();
		var anchorX = pos.getMinBlockX();
		var anchorZ = pos.getMinBlockZ();
		anchorZ = anchorZ - anchorZ % TILE_SIZE;
		anchorX = anchorX - anchorX % TILE_SIZE;
		var level = minecraft.level;
		int tileX = anchorX / TILE_SIZE;
		int tileY = anchorZ / TILE_SIZE;
		if (anchorX < 0 && anchorX % TILE_SIZE != 0) {
			tileX -= 1;
		}
		if (anchorZ < 0 && anchorZ % TILE_SIZE != 0) {
			tileY -= 1;
		}
		ChunkAccess tileChunk = level.getChunk(tileX, tileY, ChunkStatus.FULL, false);
		if (tileChunk != null) {
			var dir = getCurrentLevelMapSaveDir();
			var out = dir.resolve("%d_%d%s".formatted(tileX, tileY, Tile.FILE_EXTENSION));
			try {
				Files.createDirectories(dir);
				new LevelChunkStorage.Entry(tileChunk).write(out);
			} catch (IOException e) {
				log.warn("Failed to save tile at {}, {}", tileX, tileY, e);
			}
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
				dragOffset.add((float) dragX, (float) dragY);
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
				dragOffset.set(mirroredOnOffsetX - width / 2f, mirroredOnOffsetY - height / 2f);
			} else {
				scale /= 2;
				var offsetX = width / 2f + dragOffset.x();
				var offsetY = height / 2f + dragOffset.y();
				var mirroredOnOffsetX = offsetX + (mouseX - offsetX) / 2f;
				var mirroredOnOffsetY = offsetY + (mouseY - offsetY) / 2f;
				dragOffset.set(mirroredOnOffsetX - width / 2f, mirroredOnOffsetY - height / 2f);
			}
		}
		return true;
	}

	@Override
	protected void init() {
		addRenderableWidget(new ImageButton(4, height - 20, 16, 16, new WidgetSprites(OPTIONS_SPRITE, OPTIONS_SPRITE, OPTIONS_HOVERED_SPRITE),
			btn -> minecraft.setScreen(AxolotlClientWaypoints.createOptionsScreen(this)), AxolotlClientWaypoints.tr("options")));
		var slider = addRenderableWidget(new AbstractSliderButton(width - 150, 20, 150, 20, AxolotlClientWaypoints.tr("player_y"), 0) {
			final int min = minecraft.level.getMinY() - 1;
			final int max = minecraft.level.getMaxY() + 1;

			@Override
			protected void updateMessage() {
				if (value == 0) {
					setMessage(AxolotlClientWaypoints.tr("player_y"));
				} else {
					setMessage(Component.literal(String.valueOf(caveY - 1)));
				}
			}

			@Override
			protected void applyValue() {
				caveY = (int) (min + (max - min) * value);
				atSurface = false;
				if (value == 0) {
					collectPlayerYData();
				}
				updateTiles();
			}
		});
		addRenderableWidget(new DropdownButton(width - 20, 0, 20, 20,
			AxolotlClientWaypoints.tr("open_dropdown"), (btn, val) -> slider.visible = val));
		slider.visible = false;
		optionUpdate = () -> {
			var allowsCaves = allowCaves || (allowCavesNether && minecraft.level.dimensionType().hasCeiling());
			boolean updated = slider.active != allowsCaves;
			if (updated) {
				slider.active = allowsCaves;
				if (!allowsCaves) {
					atSurface = true;
					updateTiles();
				} else {
					slider.applyValue();
				}
			}
		};
		AxolotlClientWaypoints.NETWORK_LISTENER.postReceive.add(optionUpdate);
		optionUpdate.run();

		if (tiles.isEmpty()) {
			if (!initializedOnce) {
				collectPlayerYData();
			}
			CompletableFuture.runAsync(() -> {
				loadSavedTiles();
				createTiles();
			});
		}
		initializedOnce = true;
	}

	private void updateTiles() {
		CompletableFuture.runAsync(() -> tiles.values().forEach(t ->
			CompletableFuture.runAsync(() -> t.update(caveY, atSurface, minecraft.level))));
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
		private final Vector2f pos = new Vector2f();
		private boolean loaded;

		public void render(GuiGraphics guiGraphics, float playerX, float playerZ, float scale, float delta, int caveY, boolean atSurface) {
			float x = tilePosX() * TILE_SIZE - playerX;
			float y = tilePosY() * TILE_SIZE - playerZ;
			guiGraphics.pose().pushMatrix();
			guiGraphics.pose().translate(x, y);
			pos.zero();
			guiGraphics.pose().transformPosition(pos);
			if (pos.x + TILE_SIZE * scale >= 0 && pos.x < guiGraphics.guiWidth() && pos.y + TILE_SIZE * scale >= 0 && pos.y < guiGraphics.guiHeight()) {
				if (tile == null) {
					if (!loaded) {
						load().thenRunAsync(() -> {
							if (tile != null) {
								tile.update(caveY, atSurface, Minecraft.getInstance().level);
							}
						});
					}
				} else {
					// FIXME - floating point precision errors?
					guiGraphics.blit(RenderPipelines.GUI_TEXTURED, tile.rl, 0, 0, 0, 0, TILE_SIZE, TILE_SIZE, TILE_SIZE, TILE_SIZE);
				}
			}
			guiGraphics.pose().popMatrix();
		}

		public CompletableFuture<?> load() {
			if (!loaded) {
				loaded = true;
				return Minecraft.getInstance().submit(supplier).thenApply(t -> tile = t);
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

	@SuppressWarnings("ResultOfMethodCallIgnored")
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
			var tex = new DynamicTexture(rl::toString, new NativeImage(TILE_SIZE, TILE_SIZE, false));
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
			int levelMinY = level.getMinY();
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

					int color;
					if (mapColor == MapColor.WATER) {
						var floorBlock = levelChunk.getBlockState(mutableBlockPos2);
						var floorColor = floorBlock.getMapColor(level, mutableBlockPos2).col;
						int biomeColor = mapColor.col;
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
				}
			}

			if (updated) {
				Minecraft.getInstance().submit(tex::upload);
			}
		}
	}
}
