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

package io.github.axolotlclient.waypoints.waypoints.gui;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.axolotlclient.AxolotlClientConfig.impl.ui.ClickableWidget;
import io.github.axolotlclient.AxolotlClientConfig.impl.ui.Element;
import io.github.axolotlclient.AxolotlClientConfig.impl.ui.TextFieldWidget;
import io.github.axolotlclient.AxolotlClientConfig.impl.ui.vanilla.ElementListWidget;
import io.github.axolotlclient.AxolotlClientConfig.impl.ui.vanilla.widgets.VanillaButtonWidget;
import io.github.axolotlclient.waypoints.AxolotlClientWaypoints;
import io.github.axolotlclient.waypoints.waypoints.Waypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiElement;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.locale.I18n;

public class WaypointsScreen extends io.github.axolotlclient.AxolotlClientConfig.impl.ui.Screen {

	private final Screen parent;

	public WaypointsScreen(Screen screen) {
		super(AxolotlClientWaypoints.tr("manage"));
		this.parent = screen;
	}

	@Override
	public void init() {
		WaypointsList entries = new WaypointsList(minecraft, height, width, height - 60 - 33, 60, 25);
		addDrawableChild(entries);

		var search = addDrawableChild(new TextFieldWidget(textRenderer, width / 2 - 100, 60 / 2 - 37 / 2 + 9 + 8, 200, 20, AxolotlClientWaypoints.tr("waypoint_search")));
		search.setHint(AxolotlClientWaypoints.tr("waypoint_search"));
		search.setChangedListener(entries::applySearch);


		addDrawableChild(new VanillaButtonWidget(width / 2 - 150 - 2, height - 33 / 2 - 20 / 2, 150, 20, AxolotlClientWaypoints.tr("create_waypoint"), btn -> minecraft.openScreen(new CreateWaypointScreen(this))));
		addDrawableChild(new VanillaButtonWidget(width / 2 + 2, height - 33 / 2 - 20 / 2, 150, 20, I18n.translate("gui.done"), btn -> onClose()));

		entries.loadEntries(AxolotlClientWaypoints.getCurrentWaypoints(false));
	}

	public void onClose() {
		minecraft.openScreen(parent);
	}

	@Override
	public void render(int mouseX, int mouseY, float delta) {
		super.render(mouseX, mouseY, delta);
		drawCenteredString(textRenderer, getTitle(), width / 2, 60 / 2 - 37 / 2, -1);
	}

	private class WaypointsList extends ElementListWidget<WaypointsList.Entry> {

		private final List<Waypoint> waypoints = new ArrayList<>();
		private String search = null;

		public WaypointsList(Minecraft minecraft, int screenHeight, int width, int height, int y, int entryHeight) {
			super(minecraft, width, screenHeight, y, y + height, entryHeight);
		}

		public void loadEntries(List<Waypoint> waypoints) {
			this.waypoints.clear();
			this.waypoints.addAll(waypoints);
			applySearch(search);
		}

		public void applySearch(String s) {
			this.search = s;
			clearEntries();
			waypoints.stream().filter(w -> s == null || w.name().contains(s)).forEach(w -> addEntry(new Entry(w)));
		}

		@Override
		public int getRowWidth() {
			return 300;
		}

		private class Entry extends ElementListWidget.Entry<Entry> {

			private final List<ClickableWidget> children = new ArrayList<>();
			private final Waypoint waypoint;
			private final Minecraft minecraft = Minecraft.getInstance();

			private Entry(Waypoint waypoint) {
				this.waypoint = waypoint;
				children.add(new VanillaButtonWidget(0, 0, 50, 20, AxolotlClientWaypoints.tr("edit_waypoint"), btn ->
					minecraft.openScreen(new EditWaypointScreen(WaypointsScreen.this, waypoint))));
				children.add(new VanillaButtonWidget(0, 0, 50, 20, AxolotlClientWaypoints.tr("remove_waypoint"), btn -> {
					removeEntry(this);
					AxolotlClientWaypoints.WAYPOINT_STORAGE.getWaypoints().remove(waypoint);
				}));
			}

			@Override
			public void render(int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
				var font = minecraft.textRenderer;
				GlStateManager.pushMatrix();
				int textWidth = font.getWidth(waypoint.display());
				int displayWidth = textWidth + Waypoint.displayXOffset() * 2;
				int textHeight = font.fontHeight;
				int displayHeight = textHeight + Waypoint.displayYOffset() * 2;
				GlStateManager.translatef(left + width / 6f, top + height / 2f, 0);
				GlStateManager.scalef(Math.min(1, 100 / displayWidth), Math.min(1, 100 / displayWidth), 1);
				GlStateManager.pushMatrix();
				GlStateManager.translatef(-displayWidth / 2f, -displayHeight / 2f, 0);
				GuiElement.fill(0, 0, displayWidth, displayHeight, waypoint.color().toInt());
				GlStateManager.popMatrix();
				GlStateManager.translatef(-textWidth / 2f, -textHeight / 2f, 0);
				font.draw(waypoint.display(), 0, 0, -1);
				GlStateManager.popMatrix();
				GlStateManager.pushMatrix();
				GlStateManager.translatef(left + width / 2f, top + height / 2f, 0);
				int nameWidth = font.getWidth(waypoint.name());
				font.draw(waypoint.name(), -nameWidth / 2, -textHeight / 2, -1);
				GlStateManager.popMatrix();

				int buttonsX = left + width;
				for (int i = children.size() - 1; i >= 0; i--) {
					var w = children.get(i);
					w.setX(buttonsX - w.getWidth());
					buttonsX -= w.getWidth() + 4;
					w.setY(top + height / 2 - w.getHeight() / 2);
					w.render(mouseX, mouseY, partialTick);
				}
			}

			@Override
			public List<? extends Element> children() {
				return children;
			}
		}
	}
}
