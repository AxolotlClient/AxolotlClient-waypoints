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

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.axolotlclient.AxolotlClientConfig.api.util.Color;
import io.github.axolotlclient.AxolotlClientConfig.api.util.Colors;
import io.github.axolotlclient.AxolotlClientConfig.api.util.Rectangle;
import io.github.axolotlclient.AxolotlClientConfig.impl.ui.ClickableWidget;
import io.github.axolotlclient.AxolotlClientConfig.impl.ui.vanilla.widgets.VanillaButtonWidget;
import io.github.axolotlclient.AxolotlClientConfig.impl.util.DrawUtil;
import io.github.axolotlclient.waypoints.AxolotlClientWaypoints;
import io.github.axolotlclient.waypoints.waypoints.gui.CreateWaypointScreen;
import io.github.axolotlclient.waypoints.waypoints.gui.EditWaypointScreen;
import io.github.axolotlclient.waypoints.waypoints.gui.util.StringWidget;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.TextRenderer;

@Slf4j
public class ContextMenuScreen extends io.github.axolotlclient.AxolotlClientConfig.impl.ui.Screen {

	private final Screen parent;
	private int posX;
	private int posY;
	private final Type type;
	private Rectangle menu;

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
	public void init() {
		parent.init(minecraft, width, height);

		posX = Math.min(width - 100, posX);

		int lastY = posY;
		List<ClickableWidget> widgets = type.build(minecraft, parent);
		for (ClickableWidget clickableWidget : widgets) {
			clickableWidget.setPosition(posX, clickableWidget.getY() + lastY);
			clickableWidget.setWidth(100);
			clickableWidget.setHeight(12);
			lastY = clickableWidget.getY()+ clickableWidget.getHeight();
			addDrawableChild(clickableWidget);
		}

		int height = lastY - posY;

		/*if (posY + height > this.height) {
			int newY = this.height - height;
			int diff = newY - posY;
			posY = newY;
			widgets.forEach(w -> {
				w.setY(w.getY() - diff);
			});
		}*/

		menu = new Rectangle(posX, posY, 100, height);
	}

	@Override
	public void renderBackground() {

	}

	@Override
	public void render(int mouseX, int mouseY, float partialTick) {
		if (parent != null) {
			GlStateManager.pushMatrix();
			GlStateManager.translatef(0, 0, -100);
			parent.render(posX - 4, posY - 4, partialTick);
			GlStateManager.popMatrix();
		}
		DrawUtil.outlineRect(menu.x() - 1, menu.y() - 1, menu.width() + 2, menu.height() + 2, Colors.GRAY.toInt());
		fill(menu.x() - 1, menu.y() - 1, menu.x() + menu.width() + 2, menu.y() + menu.height() + 2, Colors.DARK_GRAY.withAlpha(100).toInt());
		super.render(mouseX, mouseY, partialTick);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (mouseX >= menu.x() && mouseX < menu.x() + menu.width() && mouseY >= menu.y() && mouseY < menu.y() + menu.height()) {
			return super.mouseClicked(mouseX, mouseY, button);
		}
		onClose();
		if (parent instanceof io.github.axolotlclient.AxolotlClientConfig.impl.ui.Screen s) {
			return s.mouseClicked(mouseX, mouseY, button);
		}
		parent.mouseClicked((int) mouseX, (int) mouseY, button);
		return true;
	}

	public void onClose() {
		minecraft.openScreen(parent);
	}

	@Override
	public void removed() {
		parent.removed();
	}

	private static class TitleWidget extends StringWidget {

		public TitleWidget(int x, int y, int width, int height, String message, TextRenderer font) {
			super(x, y, width, height, message, font, 0.5f);
		}

		@Override
		public void render(int mouseX, int mouseY, float partialTick) {
			DrawUtil.drawScrollingText(getMessage(), getX() + 1, getY(), getWidth() - 1, getHeight(), new Color(getColor()));
		}
	}

	public sealed interface Type permits Type.Map, Type.Waypoint {
		record Map(String dimension, int worldPosX, int worldPosY, int worldPosZ) implements Type {

			@Override
			public List<ClickableWidget> build(Minecraft minecraft, Screen parent) {
				List<ClickableWidget> entries = new java.util.ArrayList<>();
				entries.add(new TitleWidget(0, 4, 0, 0, AxolotlClientWaypoints.tr("position", String.valueOf(worldPosX), String.valueOf(worldPosY), String.valueOf(worldPosZ)), minecraft.textRenderer));
				entries.add(new VanillaButtonWidget(0, 8, 0, 0, AxolotlClientWaypoints.tr("create_waypoint"), btn ->
					minecraft.openScreen(new CreateWaypointScreen(parent, worldPosX + 0.5f, worldPosY, worldPosZ + 0.5f))));
				/*if (AxolotlClientWaypoints.playerHasOp() && minecraft.world.dimension.getName().equals(dimension)) {
					entries.add(new VanillaButtonWidget(0, 8, 0, 0, AxolotlClientWaypoints.tr("teleport_waypoint"), btn -> {
						minecraft.player.sendChat("/tp %s %s %s".formatted(worldPosX, worldPosY + 1, worldPosZ));
						minecraft.openScreen(null);
					}));
				}*/
				return entries;
			}
		}

		record Waypoint(io.github.axolotlclient.waypoints.waypoints.Waypoint waypoint) implements Type {
			@Override
			public List<ClickableWidget> build(Minecraft minecraft, Screen parent) {
				List<ClickableWidget> entries = new java.util.ArrayList<>();
				entries.add(new TitleWidget(0, 4, 0, 0, waypoint.name(), minecraft.textRenderer));
				entries.add(new VanillaButtonWidget(0, 8, 0, 0, AxolotlClientWaypoints.tr("edit_waypoint"), btn ->
					minecraft.openScreen(new EditWaypointScreen(parent, waypoint))));
				/*if (AxolotlClientWaypoints.playerHasOp()) {
					entries.add(new VanillaButtonWidget(0, 8, 0, 0, AxolotlClientWaypoints.tr("teleport_waypoint"), btn -> {
						minecraft.player.sendChat("/tp %s %s %s".formatted(waypoint.x(), waypoint.y() + 1, waypoint.z()));
						minecraft.openScreen(null);
					}));
				}*/
				return entries;
			}
		}

		List<ClickableWidget> build(Minecraft minecraft, Screen parent);
	}
}
