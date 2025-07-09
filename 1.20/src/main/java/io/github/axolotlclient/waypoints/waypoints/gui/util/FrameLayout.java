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

package io.github.axolotlclient.waypoints.waypoints.gui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.gui.layouts.AbstractLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
public class FrameLayout extends AbstractLayout {
	private final List<FrameLayout.ChildContainer> children = new ArrayList<>();
	private int minWidth;
	private int minHeight;
	private final LayoutSettings defaultChildLayoutSettings = LayoutSettings.defaults().align(0.5F, 0.5F);

	public FrameLayout() {
		this(0, 0, 0, 0);
	}

	public FrameLayout(int width, int height) {
		this(0, 0, width, height);
	}

	public FrameLayout(int x, int y, int width, int height) {
		super(x, y, width, height);
		this.setMinDimensions(width, height);
	}

	public FrameLayout setMinDimensions(int minWidth, int minHeight) {
		return this.setMinWidth(minWidth).setMinHeight(minHeight);
	}

	public FrameLayout setMinHeight(int minHeight) {
		this.minHeight = minHeight;
		return this;
	}

	public FrameLayout setMinWidth(int minWidth) {
		this.minWidth = minWidth;
		return this;
	}

	public LayoutSettings newChildLayoutSettings() {
		return this.defaultChildLayoutSettings.copy();
	}

	public LayoutSettings defaultChildLayoutSetting() {
		return this.defaultChildLayoutSettings;
	}

	@Override
	public void arrangeElements() {
		super.arrangeElements();
		int i = this.minWidth;
		int j = this.minHeight;

		for (FrameLayout.ChildContainer childContainer : this.children) {
			i = Math.max(i, childContainer.getWidth());
			j = Math.max(j, childContainer.getHeight());
		}

		for (FrameLayout.ChildContainer childContainer : this.children) {
			childContainer.setX(this.getX(), i);
			childContainer.setY(this.getY(), j);
		}

		this.width = i;
		this.height = j;
	}

	public <T extends LayoutElement> T addChild(T child) {
		return this.addChild(child, this.newChildLayoutSettings());
	}

	public <T extends LayoutElement> T addChild(T child, LayoutSettings layoutSettings) {
		this.children.add(new FrameLayout.ChildContainer(child, layoutSettings));
		return child;
	}

	public <T extends LayoutElement> T addChild(T child, Consumer<LayoutSettings> layoutSettingsFactory) {
		return this.addChild(child, Util.make(this.newChildLayoutSettings(), layoutSettingsFactory));
	}

	@Override
	public void visitChildren(Consumer<LayoutElement> visitor) {
		this.children.forEach(childContainer -> visitor.accept(childContainer.child));
	}

	public static void centerInRectangle(LayoutElement child, int x, int y, int width, int height) {
		alignInRectangle(child, x, y, width, height, 0.5F, 0.5F);
	}

	public static void centerInRectangle(LayoutElement child, ScreenRectangle rectangle) {
		centerInRectangle(child, rectangle.position().x(), rectangle.position().y(), rectangle.width(), rectangle.height());
	}

	public static void alignInRectangle(LayoutElement child, ScreenRectangle rectangle, float deltaX, float deltaY) {
		alignInRectangle(child, rectangle.left(), rectangle.top(), rectangle.width(), rectangle.height(), deltaX, deltaY);
	}

	public static void alignInRectangle(LayoutElement child, int x, int y, int width, int height, float deltaX, float deltaY) {
		alignInDimension(x, width, child.getWidth(), child::setX, deltaX);
		alignInDimension(y, height, child.getHeight(), child::setY, deltaY);
	}

	public static void alignInDimension(int position, int rectangleLength, int childLength, Consumer<Integer> setter, float delta) {
		int i = (int) Mth.lerp(delta, 0.0F, (float) (rectangleLength - childLength));
		setter.accept(position + i);
	}

	@Environment(EnvType.CLIENT)
	static class ChildContainer extends AbstractChildWrapper {
		protected ChildContainer(LayoutElement layoutElement, LayoutSettings layoutSettings) {
			super(layoutElement, layoutSettings);
		}
	}
}
