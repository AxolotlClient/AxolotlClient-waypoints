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
import java.util.Locale;

import io.github.axolotlclient.AxolotlClientConfig.api.util.Colors;
import io.github.axolotlclient.AxolotlClientConfig.impl.options.ColorOption;
import io.github.axolotlclient.AxolotlClientConfig.impl.ui.vanilla.widgets.ColorWidget;
import io.github.axolotlclient.waypoints.AxolotlClientWaypoints;
import io.github.axolotlclient.waypoints.waypoints.Waypoint;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.*;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class EditWaypointScreen extends Screen {

	private final HeaderAndFooterLayout haFL = new HeaderAndFooterLayout(this);
	private final Screen parent;
	private double x = -1, y = -1, z = -1;
	private Button save;
	private boolean initialized;
	private final List<LayoutElement> centeredLayouts = new ArrayList<>();
	private final Waypoint toEdit;

	public EditWaypointScreen(Screen screen, Waypoint toEdit) {
		super(AxolotlClientWaypoints.tr("edit_waypoint_title"));
		this.parent = screen;
		this.toEdit = toEdit;
	}

	@Override
	protected void init() {
		if (!initialized) {
			haFL.addTitleHeader(getTitle(), getFont());
			ColorOption color = new ColorOption("", toEdit.color().immutable());

			var contents = haFL.addToContents(LinearLayout.vertical()).spacing(4);
			contents.addChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_position"), getFont())).alignCenter().setWidth(haFL.getWidth());
			var dimensionLine = contents.addChild(LinearLayout.horizontal()).spacing(4);
			dimensionLine.addChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_position.world_label"), getFont())).setHeight(20);
			var world = dimensionLine.addChild(new EditBox(getFont(), 150, 20, AxolotlClientWaypoints.tr("waypoint_position.world")));
			world.setValue(toEdit.world());
			dimensionLine.addChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_position.dimension"), getFont())).setHeight(20);
			var dimension = dimensionLine.addChild(new EditBox(getFont(), 150, 20, AxolotlClientWaypoints.tr("waypoint_position_dimension")));
			dimension.setValue(toEdit.dimension());
			var positionLine = contents.addChild(LinearLayout.horizontal()).spacing(4);
			positionLine.addChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_position.x_label"), getFont())).setHeight(20);
			var x = positionLine.addChild(new EditBox(getFont(), 75, 20, AxolotlClientWaypoints.tr("waypoint_position.x")));
			positionLine.addChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_position.y_label"), getFont())).setHeight(20);
			var y = positionLine.addChild(new EditBox(getFont(), 75, 20, AxolotlClientWaypoints.tr("waypoint_position.y")));
			positionLine.addChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_position.z_label"), getFont())).setHeight(20);
			var z = positionLine.addChild(new EditBox(getFont(), 75, 20, AxolotlClientWaypoints.tr("waypoint_position.z")));
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
			x.setValue(String.valueOf(toEdit.x()));
			y.setValue(String.valueOf(toEdit.y()));
			z.setValue(String.valueOf(toEdit.z()));
			contents.addChild(SpacerElement.height(10));

			contents.addChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_display"), getFont())).alignCenter().setWidth(haFL.getWidth());
			var nameLine = contents.addChild(LinearLayout.horizontal()).spacing(4);
			nameLine.addChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_display.name_label"), getFont())).setHeight(20);
			var name = nameLine.addChild(new EditBox(getFont(), 100, 20, AxolotlClientWaypoints.tr("waypoint_display.name")));
			nameLine.addChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_display.display_label"), getFont())).setHeight(20);
			var display = nameLine.addChild(new EditBox(getFont(), 50, 20, AxolotlClientWaypoints.tr("waypoint_display.display")));
			var lockButton = nameLine.addChild(CycleButton.onOffBuilder(true).create(AxolotlClientWaypoints.tr("waypoint_display.unlock_display"), (btn, v) -> {
				display.active = !v;
			}));
			lockButton.setWidth(100);
			name.setValue(toEdit.name());
			name.setResponder(s -> {
				if (lockButton.getValue()) {
					var d = s.trim();
					display.setValue(d.isEmpty() ? "" : d.substring(0, 1).toUpperCase(Locale.ROOT));
				}
			});
			display.active = false;
			display.setValue(toEdit.display());
			name.setMaxLength(150);
			display.setMaxLength(10);
			contents.addChild(SpacerElement.height(10));

			contents.addChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_color"), getFont())).alignCenter().setWidth(haFL.getWidth());
			var colorLine = contents.addChild(LinearLayout.horizontal()).spacing(4);
			colorLine.addChild(new AbstractWidget(0, 0, 100, 20, Component.empty()) {
				@Override
				protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
					guiGraphics.fill(getX(), getY(), getRight(), getBottom(), color.get().toInt());
					guiGraphics.renderOutline(getX(), getY(), getWidth(), getHeight(), Colors.BLACK.toInt());
				}

				@Override
				protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

				}
			}).active = false;
			colorLine.addChild(new ColorWidget(0, 0, 100, 20, color));

			var footer = haFL.addToFooter(LinearLayout.horizontal()).spacing(4);
			save = footer.addChild(Button.builder(CommonComponents.GUI_DONE, btn -> {
				AxolotlClientWaypoints.WAYPOINT_STORAGE.replace(toEdit, new Waypoint(world.getValue(), dimension.getValue(), this.x, this.y, this.z, color.getOriginal(), name.getValue(), display.getValue()));
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
		save.active = x != -1 && y != -1 && z != -1;
	}
}
