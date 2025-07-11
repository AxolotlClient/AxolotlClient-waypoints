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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import io.github.axolotlclient.AxolotlClientConfig.api.util.Color;
import io.github.axolotlclient.AxolotlClientConfig.api.util.Colors;
import io.github.axolotlclient.waypoints.AxolotlClientWaypointsCommon;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WaypointStorage {

	private final Gson GSON = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(Waypoint.class, new WaypointTypeAdapter()).create();
	private static final TypeToken<List<Waypoint>> WAYPOINT_LIST_TYPE = new TypeToken<>() {
	};

	@Getter
	private final List<Waypoint> waypoints = new ArrayList<>();

	public int getWaypointCount() {
		return waypoints.size();
	}

	public List<Waypoint> getCurrentlyAvailableWaypoints(String dimension) {
		return waypoints.stream().filter(w -> dimension == null || w.dimension().equals(dimension)).toList();
	}

	private Path getCurrentPath() {
		return AxolotlClientWaypointsCommon.getCurrentWorldStorageDir().resolve("waypoints.json");
	}

	private Path currentSave = null;

	public void load() {
		currentSave = getCurrentPath();
		if (Files.exists(currentSave)) {
			try (var reader = Files.newBufferedReader(currentSave)) {
				List<Waypoint> loaded = GSON.fromJson(reader, WAYPOINT_LIST_TYPE);
				waypoints.addAll(loaded);
				log.info("Loaded {} waypoints!", waypoints.size());
			} catch (IOException e) {
				log.warn("Failed to load waypoints!", e);
			}
		} else {
			save();
		}
	}

	public void save() {
		try {
			Files.createDirectories(currentSave.getParent());
			var writer = Files.newBufferedWriter(currentSave);
			GSON.toJson(waypoints, writer);
			writer.close();
		} catch (IOException e) {
			log.warn("Failed to save waypoints!", e);
		}
	}

	public void create(Waypoint waypoint) {
		waypoints.add(waypoint);
		save();
	}

	public void replace(Waypoint oldPoint, Waypoint newPoint) {
		waypoints.set(waypoints.indexOf(oldPoint), newPoint);
		save();
	}

	public static class WaypointTypeAdapter extends TypeAdapter<Waypoint> {

		@Override
		public void write(JsonWriter out, Waypoint value) throws IOException {
			if (value == null) {
				out.nullValue();
				return;
			}
			out.beginObject();
			out.name("dimension").value(value.dimension());
			out.name("x").value(value.x());
			out.name("y").value(value.y());
			out.name("z").value(value.z());
			out.name("color").value(value.color().toString());
			out.name("name").value(value.name());
			out.name("display").value(value.display());
			out.endObject();
		}

		@Override
		public Waypoint read(JsonReader in) throws IOException {
			double x = 0, y = 0, z = 0;
			Color color = Colors.TRANSPARENT;
			String dimension = "", name = "", display = "";
			in.beginObject();
			while (in.peek() != JsonToken.END_OBJECT) {
				String jsonName = in.nextName();
				switch (jsonName) {
					case "dimension" -> dimension = in.nextString();
					case "x" -> x = in.nextDouble();
					case "y" -> y = in.nextDouble();
					case "z" -> z = in.nextDouble();
					case "color" -> color = Color.parse(in.nextString());
					case "name" -> name = in.nextString();
					case "display" -> display = in.nextString();
					default -> in.skipValue();
				}
			}
			in.endObject();
			return new Waypoint(dimension, x, y, z, color, name, display);
		}
	}
}
