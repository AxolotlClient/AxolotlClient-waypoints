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

package io.github.axolotlclient.waypoints.map.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntFunction;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkNibbleStorage;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.WorldChunkSection;

public class LevelChunkStorage {

	public record Entry(WorldChunk chunk) {
		public void write(FriendlyByteBuf buf) {
			LevelChunkStorage.write(chunk, buf);
		}

		public static Entry read(World level, FriendlyByteBuf buf) {
			return new Entry(LevelChunkStorage.read(level, buf));
		}

		public void write(Path path) throws IOException {
			var buf = buffer();
			write(buf);
			Files.copy(new ByteBufInputStream(buf), path, StandardCopyOption.REPLACE_EXISTING);

		}

		public static Entry read(Path path, World level) throws IOException {
			return read(level, new FriendlyByteBuf(Unpooled.wrappedBuffer(Files.readAllBytes(path))));
		}
	}

	public static void writeEntries(List<Entry> entries, FriendlyByteBuf buf) {
		buf.writeCollection(entries, (b, e) -> e.write(b));
	}

	public static List<Entry> readEntries(World level, FriendlyByteBuf buf) {
		return buf.readList(e -> Entry.read(level, e));
	}

	public static FriendlyByteBuf buffer() {
		return new FriendlyByteBuf(Unpooled.buffer());
	}

	public static void write(WorldChunk chunk, FriendlyByteBuf buf) {
		buf.writeLong(ChunkPos.toLong(chunk.chunkX, chunk.chunkZ));
		buf.writeVarIntArray(chunk.getHeightMap());

		buf.writeInt(chunk.getSections().length);
		for (WorldChunkSection section : chunk.getSections()) {
			if (section == null) {
				buf.writeCharArray(new char[0]);
				continue;
			}
			buf.writeCharArray(section.getBlockStates());
			buf.writeByteArray(section.getBlockLightStorage().getData());
			buf.writeByteArray(section.getSkyLightStorage().getData());
		}
	}

	public static WorldChunk read(World level, FriendlyByteBuf buf) {
		long packedChunkPos = buf.readLong();
		int chunkX = (int) packedChunkPos;
		int chunkZ = (int) (packedChunkPos >> 32);
		var heightmaps = buf.readVarIntArray();

		var chunk = new WorldChunk(level, chunkX, chunkZ);
		int sectionCount = buf.readInt();
		if (sectionCount != chunk.getSections().length) {
			return chunk;
		}
		WorldChunkSection[] sections = chunk.getSections();
		for (int i = 0; i < sectionCount; i++) {
			var section = sections[i];

			char[] blockstates = buf.readCharArray();
			if (blockstates.length == 0) {
				continue;
			}
			if (section == null) {
				sections[i] = section = new WorldChunkSection(i << 4, !level.dimension.isDark());
			}
			section.setBlockStates(blockstates);
			section.setBlockLightStorage(new ChunkNibbleStorage(buf.readByteArray()));
			section.setSkyLightStorage(new ChunkNibbleStorage(buf.readByteArray()));
		}
		chunk.setHeightmap(heightmaps);
		chunk.clearLightChecks();
		return chunk;
	}

	public static class FriendlyByteBuf extends PacketByteBuf {

		public FriendlyByteBuf(ByteBuf byteBuf) {
			super(byteBuf);
		}

		public <T> void writeCollection(Collection<T> collection, BiConsumer<? super FriendlyByteBuf, T> elementWriter) {
			this.writeVarInt(collection.size());

			for (T object : collection) {
				elementWriter.accept(this, object);
			}

		}

		public <T> List<T> readList(Function<? super FriendlyByteBuf, T> elementReader) {
			return this.readCollection(Lists::newArrayListWithCapacity, elementReader);
		}

		public <T, C extends Collection<T>> C readCollection(IntFunction<C> collectionFactory, Function<? super FriendlyByteBuf, T> elementReader) {
			int i = this.readVarInt();
			C collection = collectionFactory.apply(i);

			for (int j = 0; j < i; ++j) {
				collection.add(elementReader.apply(this));
			}

			return collection;
		}

		public FriendlyByteBuf writeVarIntArray(int[] array) {
			this.writeVarInt(array.length);

			for (int i : array) {
				this.writeVarInt(i);
			}

			return this;
		}

		public int[] readVarIntArray() {
			return this.readVarIntArray(this.readableBytes());
		}

		public int[] readVarIntArray(int maxLength) {
			int i = this.readVarInt();
			if (i > maxLength) {
				throw new DecoderException("VarIntArray with size " + i + " is bigger than allowed " + maxLength);
			} else {
				int[] is = new int[i];

				for (int j = 0; j < is.length; ++j) {
					is[j] = this.readVarInt();
				}

				return is;
			}
		}

		public FriendlyByteBuf writeCharArray(char[] array) {
			super.writeVarInt(array.length);

			for (char i : array) {
				writeChar(i);
			}

			return this;
		}

		public char[] readCharArray() {
			return this.readCharArray(this.readableBytes());
		}

		public char[] readCharArray(int maxLength) {
			int i = this.readVarInt();
			if (i > maxLength) {
				throw new DecoderException("CharArray with size " + i + " is bigger than allowed " + maxLength);
			} else {
				char[] is = new char[i];

				for (int j = 0; j < is.length; ++j) {
					is[j] = this.readChar();
				}

				return is;
			}
		}

	}
}
