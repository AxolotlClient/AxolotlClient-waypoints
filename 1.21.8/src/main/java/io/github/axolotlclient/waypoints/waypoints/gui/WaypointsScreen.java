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

import io.github.axolotlclient.waypoints.AxolotlClientWaypoints;
import io.github.axolotlclient.waypoints.waypoints.Waypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;

public class WaypointsScreen extends Screen {

	private final Screen parent;
	private WaypointsList entries;
	private final HeaderAndFooterLayout haF = new HeaderAndFooterLayout(this, 60, 33);
	private boolean initialized;

	public WaypointsScreen(Screen screen) {
		super(AxolotlClientWaypoints.tr("manage"));
		this.parent = screen;
	}

	@Override
	protected void init() {
		if (!initialized) {
			initialized = true;
			entries = new WaypointsList(minecraft, haF.getWidth(), haF.getContentHeight(), haF.getHeaderHeight(), 25);
			haF.addToContents(entries);

			var header = haF.addToHeader(LinearLayout.vertical()).spacing(8);
			header.addChild(new StringWidget(getTitle(), getFont()).alignCenter()).setWidth(200);
			var search = header.addChild(new EditBox(getFont(), 200, 20, AxolotlClientWaypoints.tr("waypoint_search")));
			search.setHint(AxolotlClientWaypoints.tr("waypoint_search"));
			search.setResponder(entries::applySearch);

			var footer = haF.addToFooter(LinearLayout.horizontal()).spacing(4);
			footer.addChild(Button.builder(AxolotlClientWaypoints.tr("create_waypoint"), btn -> minecraft.setScreen(new CreateWaypointScreen(this))).build());
			footer.addChild(Button.builder(CommonComponents.GUI_DONE, btn -> onClose()).build());
		} else {
			entries.updateSize(width, haF);
		}

		haF.arrangeElements();
		haF.visitWidgets(this::addRenderableWidget);

		entries.loadEntries(AxolotlClientWaypoints.getCurrentWaypoints(false));
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	private class WaypointsList extends ContainerObjectSelectionList<WaypointsList.Entry> {

		private final List<Waypoint> waypoints = new ArrayList<>();
		private String search = null;

		public WaypointsList(Minecraft minecraft, int width, int height, int y, int entryHeight) {
			super(minecraft, width, height, y, entryHeight);
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

		private class Entry extends ContainerObjectSelectionList.Entry<Entry> {

			private final List<AbstractWidget> children = new ArrayList<>();
			private final Waypoint waypoint;
			private final Minecraft minecraft = Minecraft.getInstance();

			private Entry(Waypoint waypoint) {
				this.waypoint = waypoint;
				children.add(Button.builder(AxolotlClientWaypoints.tr("edit_waypoint"), btn ->
					minecraft.setScreen(new EditWaypointScreen(WaypointsScreen.this, waypoint))).width(50).build());
				children.add(Button.builder(AxolotlClientWaypoints.tr("remove_waypoint"), btn -> {
					removeEntry(this);
					AxolotlClientWaypoints.WAYPOINT_STORAGE.getWaypoints().remove(waypoint);
				}).width(50).build());
			}

			@Override
			public List<? extends NarratableEntry> narratables() {
				return children;
			}

			@Override
			public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
				var font = minecraft.font;
				guiGraphics.pose().pushMatrix();
				int textWidth = font.width(waypoint.display());
				int displayWidth = textWidth + Waypoint.displayXOffset() * 2;
				int textHeight = font.lineHeight;
				int displayHeight = textHeight + Waypoint.displayYOffset() * 2;
				guiGraphics.pose().translate(left + width / 6f, top + height / 2f);
				guiGraphics.pose().scale(Math.min(1, 100 / displayWidth));
				guiGraphics.pose().pushMatrix();
				guiGraphics.pose().translate(-displayWidth / 2f, -displayHeight / 2f);
				guiGraphics.fill(0, 0, displayWidth, displayHeight, waypoint.color().toInt());
				guiGraphics.pose().popMatrix();
				guiGraphics.pose().translate(-textWidth / 2f, -textHeight / 2f);
				guiGraphics.drawString(font, waypoint.display(), 0, 0, -1);
				guiGraphics.pose().popMatrix();
				guiGraphics.pose().pushMatrix();
				guiGraphics.pose().translate(left + width / 2f, top + height / 2f);
				int nameWidth = font.width(waypoint.name());
				guiGraphics.drawString(font, waypoint.name(), -nameWidth / 2, -font.lineHeight / 2, -1);
				guiGraphics.pose().popMatrix();

				int buttonsX = left + width;
				for (int i = children.size() - 1; i >= 0; i--) {
					AbstractWidget w = children.get(i);
					w.setX(buttonsX - w.getWidth());
					buttonsX -= w.getWidth() + 4;
					w.setY(top + height / 2 - w.getHeight() / 2);
					w.render(guiGraphics, mouseX, mouseY, partialTick);
				}
			}

			@Override
			public List<? extends GuiEventListener> children() {
				return children;
			}
		}
	}
}
