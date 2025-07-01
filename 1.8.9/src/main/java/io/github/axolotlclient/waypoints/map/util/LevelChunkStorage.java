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

package io.github.axolotlclient.waypoints.map.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

public class LevelChunkStorage {

	public record Entry(ChunkAccess chunk) {
		public void write(FriendlyByteBuf buf) {
			LevelChunkStorage.write(chunk, buf);
		}

		public static Entry read(Level level, FriendlyByteBuf buf) {
			return new Entry(LevelChunkStorage.read(level, buf));
		}

		public void write(Path path) throws IOException {

			var buf = buffer();
			write(buf);
			Files.copy(new ByteBufInputStream(buf), path, StandardCopyOption.REPLACE_EXISTING);

		}

		public static Entry read(Path path, Level level) throws IOException {
			return read(level, new FriendlyByteBuf(Unpooled.wrappedBuffer(Files.readAllBytes(path))));
		}
	}

	public static void writeEntries(List<Entry> entries, FriendlyByteBuf buf) {
		buf.writeCollection(entries, (b, e) -> e.write(b));
	}

	public static List<Entry> readEntries(Level level, FriendlyByteBuf buf) {
		return buf.readList(e -> Entry.read(level, e));
	}

	public static FriendlyByteBuf buffer() {
		return new FriendlyByteBuf(Unpooled.buffer());
	}

	private static final Object2IntMap<Heightmap.Types> IDS = Util.make(() -> {
		Object2IntMap<Heightmap.Types> map = new Object2IntArrayMap<>(5);
		map.put(Heightmap.Types.WORLD_SURFACE_WG, 0);
		map.put(Heightmap.Types.WORLD_SURFACE, 1);
		map.put(Heightmap.Types.OCEAN_FLOOR_WG, 2);
		map.put(Heightmap.Types.OCEAN_FLOOR, 3);
		map.put(Heightmap.Types.MOTION_BLOCKING, 4);
		map.put(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 5);
		return map;
	});

	private static final IntFunction<Heightmap.Types> BY_ID = ByIdMap.continuous(IDS::getInt, Heightmap.Types.values(), ByIdMap.OutOfBoundsStrategy.ZERO);
	public static final StreamCodec<ByteBuf, Heightmap.Types> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, IDS::getInt);

	public static void write(ChunkAccess chunk, FriendlyByteBuf buf) {
		buf.writeChunkPos(chunk.getPos());
		var heightmaps = chunk.getHeightmaps()
			.stream()
			.filter(entryx -> entryx.getKey().sendToClient())
			.collect(Collectors.toMap(Map.Entry::getKey, entryx -> entryx.getValue().getRawData().clone()));
		buf.writeMap(heightmaps, STREAM_CODEC, FriendlyByteBuf::writeLongArray);

		buf.writeInt(chunk.getSections().length);
		for (LevelChunkSection section : chunk.getSections()) {
			section.write(buf);
			section.getBiomes().write(buf);
		}


	}

	public static LevelChunk read(Level level, FriendlyByteBuf buf) {
		var pos = buf.readChunkPos();
		var heightmaps = buf.readMap(STREAM_CODEC, FriendlyByteBuf::readLongArray);

		var chunk = new LevelChunk(level, pos);
		int sectionCount = buf.readInt();
		if (sectionCount != chunk.getSections().length) {
			return chunk;
		}
		for (var section : chunk.getSections()) {
			section.read(buf);
			section.readBiomes(buf);
		}
		heightmaps.forEach(chunk::setHeightmap);
		chunk.initializeLightSources();
		return chunk;
	}
}
