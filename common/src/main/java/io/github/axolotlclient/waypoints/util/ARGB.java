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

package io.github.axolotlclient.waypoints.util;

import org.joml.Vector3f;

public class ARGB {
	public static int alpha(int color) {
		return color >>> 24;
	}

	public static int red(int color) {
		return color >> 16 & 0xFF;
	}

	public static int green(int color) {
		return color >> 8 & 0xFF;
	}

	public static int blue(int color) {
		return color & 0xFF;
	}

	public static int color(int alpha, int red, int green, int blue) {
		return alpha << 24 | red << 16 | green << 8 | blue;
	}

	public static int color(int red, int green, int blue) {
		return color(255, red, green, blue);
	}

	/*public static int color(Vec3 color) {
		return color(as8BitChannel((float)color.x()), as8BitChannel((float)color.y()), as8BitChannel((float)color.z()));
	}*/

	public static int multiply(int color1, int color2) {
		if (color1 == -1) {
			return color2;
		} else {
			return color2 == -1
				? color1
				: color(alpha(color1) * alpha(color2) / 255, red(color1) * red(color2) / 255, green(color1) * green(color2) / 255, blue(color1) * blue(color2) / 255);
		}
	}

	public static int scaleRGB(int color, float scale) {
		return scaleRGB(color, scale, scale, scale);
	}

	public static int scaleRGB(int color, float redScale, float greenScale, float blueScale) {
		return color(
			alpha(color),
			clamp((int) (red(color) * redScale), 0, 255),
			clamp((int) (green(color) * greenScale), 0, 255),
			clamp((int) (blue(color) * blueScale), 0, 255)
		);
	}

	public static int scaleRGB(int color, int scale) {
		return color(
			alpha(color),
			clamp((long) red(color) * scale / 255L, 0, 255),
			clamp((long) green(color) * scale / 255L, 0, 255),
			clamp((long) blue(color) * scale / 255L, 0, 255)
		);
	}

	public static int greyscale(int color) {
		int i = (int) (red(color) * 0.3F + green(color) * 0.59F + blue(color) * 0.11F);
		return color(i, i, i);
	}

	public static int lerp(float delta, int color1, int color2) {
		int i = lerpInt(delta, alpha(color1), alpha(color2));
		int j = lerpInt(delta, red(color1), red(color2));
		int k = lerpInt(delta, green(color1), green(color2));
		int l = lerpInt(delta, blue(color1), blue(color2));
		return color(i, j, k, l);
	}

	public static int opaque(int color) {
		return color | 0xFF000000;
	}

	public static int transparent(int color) {
		return color & 16777215;
	}

	public static int color(int alpha, int color) {
		return alpha << 24 | color & 16777215;
	}

	public static int color(float f, int i) {
		return as8BitChannel(f) << 24 | i & 16777215;
	}

	public static int white(float alpha) {
		return as8BitChannel(alpha) << 24 | 16777215;
	}

	public static int colorFromFloat(float alpha, float red, float green, float blue) {
		return color(as8BitChannel(alpha), as8BitChannel(red), as8BitChannel(green), as8BitChannel(blue));
	}

	public static Vector3f vector3fFromRGB24(int color) {
		float f = red(color) / 255.0F;
		float g = green(color) / 255.0F;
		float h = blue(color) / 255.0F;
		return new Vector3f(f, g, h);
	}

	public static int average(int color1, int color2) {
		return color((alpha(color1) + alpha(color2)) / 2, (red(color1) + red(color2)) / 2, (green(color1) + green(color2)) / 2, (blue(color1) + blue(color2)) / 2);
	}

	public static int as8BitChannel(float value) {
		return floor(value * 255.0F);
	}

	public static float alphaFloat(int color) {
		return from8BitChannel(alpha(color));
	}

	public static float redFloat(int color) {
		return from8BitChannel(red(color));
	}

	public static float greenFloat(int color) {
		return from8BitChannel(green(color));
	}

	public static float blueFloat(int color) {
		return from8BitChannel(blue(color));
	}

	private static float from8BitChannel(int value) {
		return value / 255.0F;
	}

	public static int toABGR(int color) {
		return color & -16711936 | (color & 0xFF0000) >> 16 | (color & 0xFF) << 16;
	}

	public static int fromABGR(int color) {
		return toABGR(color);
	}

	public static int setBrightness(int i, float f) {
		int j = red(i);
		int k = green(i);
		int l = blue(i);
		int m = alpha(i);
		int n = Math.max(Math.max(j, k), l);
		int o = Math.min(Math.min(j, k), l);
		float g = n - o;
		float h;
		if (n != 0) {
			h = g / n;
		} else {
			h = 0.0F;
		}

		float p;
		if (h == 0.0F) {
			p = 0.0F;
		} else {
			float q = (n - j) / g;
			float r = (n - k) / g;
			float s = (n - l) / g;
			if (j == n) {
				p = s - r;
			} else if (k == n) {
				p = 2.0F + q - s;
			} else {
				p = 4.0F + r - q;
			}

			p /= 6.0F;
			if (p < 0.0F) {
				p++;
			}
		}

		if (h == 0.0F) {
			j = k = l = Math.round(f * 255.0F);
		} else {
			float qx = (p - (float) Math.floor(p)) * 6.0F;
			float rx = qx - (float) Math.floor(qx);
			float sx = f * (1.0F - h);
			float t = f * (1.0F - h * rx);
			float u = f * (1.0F - h * (1.0F - rx));
			switch ((int) qx) {
				case 0:
					j = Math.round(f * 255.0F);
					k = Math.round(u * 255.0F);
					l = Math.round(sx * 255.0F);
					break;
				case 1:
					j = Math.round(t * 255.0F);
					k = Math.round(f * 255.0F);
					l = Math.round(sx * 255.0F);
					break;
				case 2:
					j = Math.round(sx * 255.0F);
					k = Math.round(f * 255.0F);
					l = Math.round(u * 255.0F);
					break;
				case 3:
					j = Math.round(sx * 255.0F);
					k = Math.round(t * 255.0F);
					l = Math.round(f * 255.0F);
					break;
				case 4:
					j = Math.round(u * 255.0F);
					k = Math.round(sx * 255.0F);
					l = Math.round(f * 255.0F);
					break;
				case 5:
					j = Math.round(f * 255.0F);
					k = Math.round(sx * 255.0F);
					l = Math.round(t * 255.0F);
			}

		}
		return color(m, j, k, l);
	}

	private static int clamp(long value, int min, int max) {
		if (min > max) {
			throw new IllegalArgumentException(min + " > " + max);
		}
		return (int) Math.min(max, Math.max(value, min));
	}

	private static int lerpInt(float delta, int start, int end) {
		return start + floor(delta * (end - start));
	}

	private static int floor(float value) {
		int i = (int) value;
		return value < i ? i - 1 : i;
	}
}
