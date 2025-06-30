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

package io.github.axolotlclient.waypoints.waypoints;

import io.github.axolotlclient.AxolotlClientConfig.api.util.Color;
import org.joml.Vector3f;

public record Waypoint(String world, String dimension, double x, double y, double z, Color color, String name,
					   String display) {

	public double distTo(double x, double y, double z) {
		return Math.sqrt(squaredDistTo(x, y, z));
	}

	public double squaredDistTo(Vector3f vec) {
		return squaredDistTo(vec.x(), vec.y(), vec.z());
	}

	public double squaredDistTo(double x, double y, double z) {
		double x2 = x() - x;
		double y2 = y() - y;
		double z2 = z() - z;
		return x2 * x2 + y2 * y2 + z2 * z2;
	}

	public boolean closerToThan(double x, double y, double z, double distance) {
		return squaredDistTo(x, y, z) < distance * distance;
	}

	public static int displayXOffset() {
		return 2;
	}

	public static int displayYOffset() {
		return 2;
	}
}
