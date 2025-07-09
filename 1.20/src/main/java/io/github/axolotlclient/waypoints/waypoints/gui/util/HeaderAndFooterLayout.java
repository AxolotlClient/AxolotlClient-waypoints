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

import java.util.function.Consumer;

import lombok.Getter;
import lombok.Setter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class HeaderAndFooterLayout implements Layout {
	public static final int DEFAULT_HEADER_AND_FOOTER_HEIGHT = 33;
	private static final int CONTENT_MARGIN_TOP = 30;
	private final FrameLayout headerFrame = new FrameLayout();
	private final FrameLayout footerFrame = new FrameLayout();
	private final FrameLayout contentsFrame = new FrameLayout();
	private final Screen screen;
	@Getter
	@Setter
	private int headerHeight;
	@Getter
	@Setter
	private int footerHeight;

	public HeaderAndFooterLayout(Screen screen) {
		this(screen, DEFAULT_HEADER_AND_FOOTER_HEIGHT);
	}

	public HeaderAndFooterLayout(Screen screen, int height) {
		this(screen, height, height);
	}

	public HeaderAndFooterLayout(Screen screen, int headerHeight, int footerHeight) {
		this.screen = screen;
		this.headerHeight = headerHeight;
		this.footerHeight = footerHeight;
		this.headerFrame.defaultChildLayoutSetting().align(0.5F, 0.5F);
		this.footerFrame.defaultChildLayoutSetting().align(0.5F, 0.5F);
	}

	@Override
	public void setX(int x) {
	}

	@Override
	public void setY(int y) {
	}

	@Override
	public int getX() {
		return 0;
	}

	@Override
	public int getY() {
		return 0;
	}

	@Override
	public int getWidth() {
		return this.screen.width;
	}

	@Override
	public int getHeight() {
		return this.screen.height;
	}

	public int getContentHeight() {
		return this.screen.height - this.getHeaderHeight() - this.getFooterHeight();
	}

	@Override
	public void visitChildren(Consumer<LayoutElement> visitor) {
		this.headerFrame.visitChildren(visitor);
		this.contentsFrame.visitChildren(visitor);
		this.footerFrame.visitChildren(visitor);
	}

	@Override
	public void arrangeElements() {
		int i = this.getHeaderHeight();
		int j = this.getFooterHeight();
		this.headerFrame.setMinWidth(this.screen.width);
		this.headerFrame.setMinHeight(i);
		this.headerFrame.setPosition(0, 0);
		this.headerFrame.arrangeElements();
		this.footerFrame.setMinWidth(this.screen.width);
		this.footerFrame.setMinHeight(j);
		this.footerFrame.arrangeElements();
		this.footerFrame.setY(this.screen.height - j);
		this.contentsFrame.setMinWidth(this.screen.width);
		this.contentsFrame.arrangeElements();
		int k = i + CONTENT_MARGIN_TOP;
		int l = this.screen.height - j - this.contentsFrame.getHeight();
		this.contentsFrame.setPosition(0, Math.min(k, l));
	}

	public <T extends LayoutElement> T addToHeader(T child) {
		return this.headerFrame.addChild(child);
	}

	public <T extends LayoutElement> T addToHeader(T child, Consumer<LayoutSettings> layoutSettingsFactory) {
		return this.headerFrame.addChild(child, layoutSettingsFactory);
	}

	public void addTitleHeader(Component message, Font font) {
		this.headerFrame.addChild(new StringWidget(message, font));
	}

	public <T extends LayoutElement> T addToFooter(T child) {
		return this.footerFrame.addChild(child);
	}

	public <T extends LayoutElement> T addToFooter(T child, Consumer<LayoutSettings> layoutSettingsFactory) {
		return this.footerFrame.addChild(child, layoutSettingsFactory);
	}

	public <T extends LayoutElement> T addToContents(T child) {
		return this.contentsFrame.addChild(child);
	}

	public <T extends LayoutElement> T addToContents(T child, Consumer<LayoutSettings> layoutSettingFactory) {
		return this.contentsFrame.addChild(child, layoutSettingFactory);
	}
}
