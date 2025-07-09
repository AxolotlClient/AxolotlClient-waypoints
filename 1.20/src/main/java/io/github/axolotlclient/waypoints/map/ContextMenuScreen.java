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

import java.util.List;

import io.github.axolotlclient.AxolotlClientConfig.api.util.Colors;
import io.github.axolotlclient.waypoints.AxolotlClientWaypoints;
import io.github.axolotlclient.waypoints.mixin.AbstractWidgetAccessor;
import io.github.axolotlclient.waypoints.waypoints.gui.CreateWaypointScreen;
import io.github.axolotlclient.waypoints.waypoints.gui.EditWaypointScreen;
import io.github.axolotlclient.waypoints.waypoints.gui.util.LinearLayout;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractStringWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@SuppressWarnings("DataFlowIssue")
@Slf4j
public class ContextMenuScreen extends Screen {

	private final Screen parent;
	private final int posX;
	private final int posY;
	private final LinearLayout layout = LinearLayout.vertical();
	private final Type type;

	public ContextMenuScreen(Screen parent, int x, int y, Type type) {
		super(AxolotlClientWaypoints.tr("context_menu"));
		this.parent = parent;
		this.posX = x + 4;
		this.posY = y + 4;
		this.type = type;
	}

	public ContextMenuScreen(Screen parent, double x, double y, Type type) {
		this(parent, (int) x, (int) y, type);
	}

	@Override
	protected void init() {
		parent.init(minecraft, width, height);

		type.build(minecraft, parent).forEach(layout::addChild);

		layout.visitWidgets(w -> {
			w.setWidth(100);
			((AbstractWidgetAccessor) w).setHeight(12);
		});

		layout.setPosition(posX, posY);
		layout.defaultCellSetting().alignHorizontallyCenter();
		layout.arrangeElements();

		boolean updated = false;
		if (layout.getY() + layout.getHeight() > height) {
			layout.setY(height - layout.getHeight());
			updated = true;
		}
		if (layout.getX() + layout.getWidth() > width) {
			layout.setX(width - layout.getWidth());
			updated = true;
		}
		if (updated) {
			layout.arrangeElements();
		}

		layout.visitWidgets(this::addRenderableWidget);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		if (parent != null) {
			parent.render(guiGraphics, posX - 4, posY - 4, partialTick);
		}
		guiGraphics.renderOutline(layout.getX() - 1, layout.getY() - 1, layout.getWidth() + 2, layout.getHeight() + 2, Colors.GRAY.toInt());
		guiGraphics.fill(layout.getX() - 1, layout.getY() - 1, layout.getX() + layout.getWidth() + 2, layout.getY() + layout.getHeight() + 2, Colors.DARK_GRAY.withAlpha(100).toInt());
		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (layout.rectContainsPoint((int) mouseX, (int) mouseY)) {
			return super.mouseClicked(mouseX, mouseY, button);
		}
		onClose();
		return parent.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	@Override
	public void removed() {
		parent.removed();
	}

	private static class TitleWidget extends AbstractStringWidget {

		public TitleWidget(int x, int y, int width, int height, Component message, Font font) {
			super(x, y, width, height, message, font);
		}

		public TitleWidget(Component message, Font font) {
			this(0, 0, font.width(message), font.lineHeight, message, font);
		}

		@Override
		protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
			renderScrollingString(guiGraphics, getFont(), getMessage(), getX() + 1, getY(), getX() + getWidth() - 1, getY() + getHeight(), getColor());
		}
	}

	public sealed interface Type permits Type.Map, Type.Waypoint {
		record Map(String dimension, int worldPosX, int worldPosY, int worldPosZ) implements Type {

			@Override
			public List<LayoutElement> build(Minecraft minecraft, Screen parent) {
				List<LayoutElement> entries = new java.util.ArrayList<>();
				entries.add(SpacerElement.height(4));
				entries.add(new TitleWidget(AxolotlClientWaypoints.tr("position", String.valueOf(worldPosX), String.valueOf(worldPosY), String.valueOf(worldPosZ)), minecraft.font));
				entries.add(SpacerElement.height(4));
				entries.add(Button.builder(AxolotlClientWaypoints.tr("create_waypoint"), btn ->
					minecraft.setScreen(new CreateWaypointScreen(parent, worldPosX + 0.5f, worldPosY, worldPosZ + 0.5f))).build());
				if (AxolotlClientWaypoints.playerHasOp()) {
					entries.add(Button.builder(AxolotlClientWaypoints.tr("teleport_waypoint"), btn -> {
						minecraft.getConnection().sendCommand("execute in %s run teleport @s %s %s %s".formatted(dimension, worldPosX, worldPosY + 1, worldPosZ));
						parent.onClose();
					}).build());
				}
				return entries;
			}
		}

		record Waypoint(io.github.axolotlclient.waypoints.waypoints.Waypoint waypoint) implements Type {
			@Override
			public List<LayoutElement> build(Minecraft minecraft, Screen parent) {
				List<LayoutElement> entries = new java.util.ArrayList<>();
				entries.add(SpacerElement.height(4));
				entries.add(new TitleWidget(Component.literal(waypoint.name()), minecraft.font));
				entries.add(SpacerElement.height(4));
				entries.add(Button.builder(AxolotlClientWaypoints.tr("edit_waypoint"), btn ->
					minecraft.setScreen(new EditWaypointScreen(parent, waypoint))).build());
				if (AxolotlClientWaypoints.playerHasOp()) {
					entries.add(Button.builder(AxolotlClientWaypoints.tr("teleport_waypoint"), btn -> {
						minecraft.getConnection().sendCommand("execute in %s run teleport @s %s %s %s".formatted(waypoint.dimension(), waypoint.x(), waypoint.y() + 1, waypoint.z()));
						parent.onClose();
					}).build());
				}
				return entries;
			}
		}

		List<LayoutElement> build(Minecraft minecraft, Screen parent);
	}
}
