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

package io.github.axolotlclient.waypoints.waypoints.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import io.github.axolotlclient.AxolotlClientConfig.api.util.Colors;
import io.github.axolotlclient.AxolotlClientConfig.impl.options.ColorOption;
import io.github.axolotlclient.AxolotlClientConfig.impl.options.StringArrayOption;
import io.github.axolotlclient.AxolotlClientConfig.impl.ui.vanilla.widgets.ColorWidget;
import io.github.axolotlclient.AxolotlClientConfig.impl.ui.vanilla.widgets.StringArrayWidget;
import io.github.axolotlclient.waypoints.AxolotlClientWaypoints;
import io.github.axolotlclient.waypoints.mixin.MinecraftServerAccessor;
import io.github.axolotlclient.waypoints.waypoints.Waypoint;
import io.github.axolotlclient.waypoints.waypoints.gui.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

@SuppressWarnings("DataFlowIssue")
public class CreateWaypointScreen extends Screen {

	private final HeaderAndFooterLayout haFL = new HeaderAndFooterLayout(this);
	private final Screen parent;
	private double x, y, z;
	private Button create;
	private boolean initialized;
	private final List<LayoutElement> centeredLayouts = new ArrayList<>();

	public CreateWaypointScreen(Screen screen) {
		this(screen, Minecraft.getInstance().getCameraEntity());
	}

	public CreateWaypointScreen(Screen screen, Entity entity) {
		this(screen, entity.getBlockX() + 0.5, entity.getBlockY() + entity.getEyeHeight(), entity.getBlockZ() + 0.5);
	}

	public CreateWaypointScreen(Screen parent, double x, double y, double z) {
		super(AxolotlClientWaypoints.tr("create_waypoint"));
		this.parent = parent;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	@Override
	protected void init() {
		if (!initialized) {
			boolean singleplayer = minecraft.getSingleplayerServer() != null;
			ColorOption color = new ColorOption("", Colors.GREEN.withAlpha(127));

			haFL.addTitleHeader(getTitle(), font);

			var contents = haFL.addToContents(LinearLayout.vertical()).spacing(4);
			contents.addChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_position"), font)).alignCenter().setWidth(haFL.getWidth());
			var dimensionLine = contents.addChild(LinearLayout.horizontal()).spacing(4);
			dimensionLine.addChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_position.world_label"), font)).setHeight(20);
			var world = dimensionLine.addChild(new EditBox(font, 0, 0, 150, 20, AxolotlClientWaypoints.tr("waypoint_position.world")));
			if (singleplayer) {
				world.setValue(((MinecraftServerAccessor) minecraft.getSingleplayerServer()).getStorageSource().getLevelId());
			} else {
				world.setValue(minecraft.getCurrentServer().ip);
			}
			world.active = false;
			dimensionLine.addChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_position.dimension"), font)).setHeight(20);
			Supplier<String> dimensionSupplier;
			if (singleplayer) {
				StringArrayOption dimensions = new StringArrayOption("", minecraft.getSingleplayerServer().levelKeys().stream().map(k -> k.location().toString()).toArray(String[]::new), minecraft.level.dimension().location().toString());
				dimensionLine.addChild(new StringArrayWidget(0, 0, 150, 20, dimensions));
				dimensionSupplier = dimensions::get;
			} else {
				var dimension = dimensionLine.addChild(new EditBox(font, 0, 0, 150, 20, AxolotlClientWaypoints.tr("waypoint_position_dimension")));
				dimension.setValue(minecraft.level.dimension().location().toString());
				dimensionSupplier = dimension::getValue;
			}
			var positionLine = contents.addChild(LinearLayout.horizontal()).spacing(4);
			positionLine.addChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_position.x_label"), font)).setHeight(20);
			var x = positionLine.addChild(new EditBox(font, 0, 0, 75, 20, AxolotlClientWaypoints.tr("waypoint_position.x")));
			positionLine.addChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_position.y_label"), font)).setHeight(20);
			var y = positionLine.addChild(new EditBox(font, 0, 0, 75, 20, AxolotlClientWaypoints.tr("waypoint_position.y")));
			positionLine.addChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_position.z_label"), font)).setHeight(20);
			var z = positionLine.addChild(new EditBox(font, 0, 0, 75, 20, AxolotlClientWaypoints.tr("waypoint_position.z")));
			x.setResponder(s -> {
				try {
					this.x = Double.parseDouble(s);
				} catch (Exception ignored) {
					this.x = -1;
				}
			});
			y.setResponder(s -> {
				try {
					this.y = Double.parseDouble(s);
				} catch (Exception ignored) {
					this.y = -1;
				}
			});
			z.setResponder(s -> {
				try {
					this.z = Double.parseDouble(s);
				} catch (Exception ignored) {
					this.z = -1;
				}
			});
			x.setValue("%.2f".formatted(this.x));
			y.setValue("%.2f".formatted(this.y));
			z.setValue("%.2f".formatted(this.z));
			contents.addChild(SpacerElement.height(10));

			contents.addChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_display"), font)).alignCenter().setWidth(haFL.getWidth());
			var nameLine = contents.addChild(LinearLayout.horizontal()).spacing(4);
			nameLine.addChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_display.name_label"), font)).setHeight(20);
			var name = nameLine.addChild(new EditBox(font, 0, 0, 100, 20, AxolotlClientWaypoints.tr("waypoint_display.name")));
			nameLine.addChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_display.display_label"), font)).setHeight(20);
			var display = nameLine.addChild(new EditBox(font, 0, 0, 50, 20, AxolotlClientWaypoints.tr("waypoint_display.display")));
			var lockButton = nameLine.addChild(CycleButton.onOffBuilder(true)
				.create(0, 0, 100, 20, AxolotlClientWaypoints.tr("waypoint_display.unlock_display"),
					(btn, v) -> display.active = !v));
			lockButton.setWidth(100);
			name.setResponder(s -> {
				if (lockButton.getValue()) {
					var d = s.trim();
					display.setValue(d.isEmpty() ? "" : d.substring(0, 1).toUpperCase(Locale.ROOT));
				}
			});
			name.setValue(AxolotlClientWaypoints.tra("default_waypoint_name"));
			display.active = false;
			name.setMaxLength(150);
			display.setMaxLength(10);
			contents.addChild(SpacerElement.height(10));

			contents.addChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_color"), font)).alignCenter().setWidth(haFL.getWidth());
			var colorLine = contents.addChild(LinearLayout.horizontal()).spacing(4);
			colorLine.addChild(new AbstractWidget(0, 0, 100, 20, Component.empty()) {
				@Override
				protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
					guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), color.get().toInt());
					guiGraphics.renderOutline(getX(), getY(), getWidth(), getHeight(), Colors.BLACK.toInt());
				}

				@Override
				protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

				}
			}).active = false;
			colorLine.addChild(new ColorWidget(0, 0, 100, 20, color));

			var footer = haFL.addToFooter(LinearLayout.horizontal()).spacing(4);
			create = footer.addChild(Button.builder(AxolotlClientWaypoints.tr("create_waypoint"), btn -> {
				AxolotlClientWaypoints.WAYPOINT_STORAGE.create(new Waypoint(world.getValue(), dimensionSupplier.get(), this.x, this.y, this.z, color.getOriginal(), name.getValue(), display.getValue()));
				minecraft.setScreen(parent);
			}).build());
			footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, btn -> minecraft.setScreen(parent)).build());

			initialized = true;
			centeredLayouts.add(dimensionLine);
			centeredLayouts.add(positionLine);
			centeredLayouts.add(nameLine);
			centeredLayouts.add(colorLine);
		}
		haFL.arrangeElements();
		centeredLayouts.forEach(child -> FrameLayout.centerInRectangle(child, 0, child.getY(), haFL.getWidth(), child.getHeight()));
		haFL.visitWidgets(this::addRenderableWidget);
	}

	@Override
	public void tick() {
		children().stream().filter(e -> e instanceof EditBox)
			.map(e -> (EditBox) e).forEach(EditBox::tick);
		create.active = x != -1 && y != -1 && z != -1;
	}
}
