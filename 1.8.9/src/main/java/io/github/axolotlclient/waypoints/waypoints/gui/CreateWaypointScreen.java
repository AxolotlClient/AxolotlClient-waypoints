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

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Supplier;

import io.github.axolotlclient.AxolotlClientConfig.api.util.Colors;
import io.github.axolotlclient.AxolotlClientConfig.impl.options.ColorOption;
import io.github.axolotlclient.AxolotlClientConfig.impl.options.StringArrayOption;
import io.github.axolotlclient.AxolotlClientConfig.impl.ui.ButtonWidget;
import io.github.axolotlclient.AxolotlClientConfig.impl.ui.ClickableWidget;
import io.github.axolotlclient.AxolotlClientConfig.impl.ui.TextFieldWidget;
import io.github.axolotlclient.AxolotlClientConfig.impl.ui.vanilla.widgets.ColorWidget;
import io.github.axolotlclient.AxolotlClientConfig.impl.ui.vanilla.widgets.StringArrayWidget;
import io.github.axolotlclient.AxolotlClientConfig.impl.ui.vanilla.widgets.VanillaButtonWidget;
import io.github.axolotlclient.AxolotlClientConfig.impl.util.DrawUtil;
import io.github.axolotlclient.waypoints.AxolotlClientWaypoints;
import io.github.axolotlclient.waypoints.waypoints.Waypoint;
import io.github.axolotlclient.waypoints.waypoints.gui.util.CycleButton;
import io.github.axolotlclient.waypoints.waypoints.gui.util.StringWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.locale.I18n;
import net.minecraft.util.math.MathHelper;

public class CreateWaypointScreen extends io.github.axolotlclient.AxolotlClientConfig.impl.ui.Screen {

	private final Screen parent;
	private double x, y, z;
	private ButtonWidget create;
	private final ColorOption color = new ColorOption("", Colors.GREEN.withAlpha(127));
	private String world, dimension, name, display;

	public CreateWaypointScreen(Screen screen) {
		this(screen, Minecraft.getInstance().getCamera());
	}

	public CreateWaypointScreen(Screen screen, Entity entity) {
		this(screen, MathHelper.floor(entity.x) + 0.5, MathHelper.floor(entity.y) + entity.getEyeHeight(), MathHelper.floor(entity.z) + 0.5);
	}

	public CreateWaypointScreen(Screen parent, double x, double y, double z) {
		super(AxolotlClientWaypoints.tr("create_waypoint"));
		this.parent = parent;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public void render(int mouseX, int mouseY, float delta) {
		super.render(mouseX, mouseY, delta);
		drawCenteredString(textRenderer, getTitle(), width / 2, 33 / 2, -1);
	}

	@Override
	public void init() {
		boolean singleplayer = minecraft.getServer().isSingleplayer();

		var posTitle = addDrawableChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_position"), textRenderer)).alignCenter();
		posTitle.setWidth(width);
		posTitle.setY(33 + 4);

		var worldLabel = addDrawableChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_position.world_label"), textRenderer));
		var dimensionLabel = addDrawableChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_position.dimension"), textRenderer));
		dimensionLabel.setHeight(20);
		worldLabel.setHeight(20);
		worldLabel.setPosition(width / 2 - (dimensionLabel.getWidth() + 150 * 2 + worldLabel.getWidth()) / 2, 33 + 4 + 9 + 4);
		dimensionLabel.setPosition(worldLabel.getX() + worldLabel.getWidth() + 4 + 150 + 4, worldLabel.getY());
		var world = addDrawableChild(new TextFieldWidget(textRenderer, worldLabel.getX() + worldLabel.getWidth() + 4, worldLabel.getY(), 150, 20, AxolotlClientWaypoints.tr("waypoint_position.world")));
		if (singleplayer) {
			world.setText((minecraft.getServer().getWorldSaveName()));
		} else {
			world.setText(minecraft.getCurrentServerEntry().address);
		}
		world.setEditable(false);
		if (this.world != null) {
			world.setText(this.world);
		}
		world.setChangedListener(s -> this.world = s);
		Supplier<String> dimensionSupplier;
		if (singleplayer) {
			StringArrayOption dimensions = new StringArrayOption("", Arrays.stream(minecraft.getServer().worlds).map(k -> k.dimension.getName()).toArray(String[]::new), dimension != null ? dimension : minecraft.world.dimension.getName());
			addDrawableChild(new StringArrayWidget(dimensionLabel.getX() + dimensionLabel.getWidth() + 4, worldLabel.getY(), 150, 20, dimensions));
			dimensionSupplier = dimensions::get;
		} else {
			var dimension = addDrawableChild(new TextFieldWidget(textRenderer, dimensionLabel.getX() + dimensionLabel.getWidth() + 4, worldLabel.getY(), 150, 20, AxolotlClientWaypoints.tr("waypoint_position_dimension")));
			dimension.setText(minecraft.world.dimension.getName());
			dimensionSupplier = dimension::getText;
			if (this.dimension != null) {
				dimension.setText(this.dimension);
			}
			dimension.setChangedListener(s -> this.dimension = s);
		}

		var xLabel = addDrawableChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_position.x_label"), textRenderer));
		xLabel.setHeight(20);
		var yLabel = addDrawableChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_position.y_label"), textRenderer));
		yLabel.setHeight(20);
		var zLabel = addDrawableChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_position.z_label"), textRenderer));
		zLabel.setHeight(20);
		xLabel.setPosition(width / 2 - (xLabel.getWidth() + 4 + yLabel.getWidth() + 4 + zLabel.getWidth() + (75 + 4) * 3) / 2, worldLabel.getY() + 20 + 4);
		yLabel.setPosition(xLabel.getX() + xLabel.getWidth() + 4 + 75 + 4, xLabel.getY());
		zLabel.setPosition(yLabel.getX() + yLabel.getWidth() + 4 + 75 + 4, yLabel.getY());
		var x = addDrawableChild(new TextFieldWidget(textRenderer, xLabel.getX() + xLabel.getWidth() + 4, xLabel.getY(), 75, 20, AxolotlClientWaypoints.tr("waypoint_position.x")));
		var y = addDrawableChild(new TextFieldWidget(textRenderer, yLabel.getX() + yLabel.getWidth() + 4, xLabel.getY(), 75, 20, AxolotlClientWaypoints.tr("waypoint_position.y")));
		var z = addDrawableChild(new TextFieldWidget(textRenderer, zLabel.getX() + zLabel.getWidth() + 4, xLabel.getY(), 75, 20, AxolotlClientWaypoints.tr("waypoint_position.z")));
		x.setChangedListener(s -> {
			try {
				this.x = Double.parseDouble(s);
			} catch (Exception ignored) {
				this.x = -1;
			}
		});
		y.setChangedListener(s -> {
			try {
				this.y = Double.parseDouble(s);
			} catch (Exception ignored) {
				this.y = -1;
			}
		});
		z.setChangedListener(s -> {
			try {
				this.z = Double.parseDouble(s);
			} catch (Exception ignored) {
				this.z = -1;
			}
		});
		x.setText("%.2f".formatted(this.x));
		y.setText("%.2f".formatted(this.y));
		z.setText("%.2f".formatted(this.z));

		var displayTitle = addDrawableChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_display"), textRenderer)).alignCenter();
		displayTitle.setWidth(width);
		displayTitle.setY(y.getY() + y.getHeight() + 10 + 4 + 4);

		var nameLabel = addDrawableChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_display.name_label"), textRenderer));
		var displayLabel = addDrawableChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_display.display_label"), textRenderer));
		nameLabel.setHeight(20);
		nameLabel.setPosition(width / 2 - (nameLabel.getWidth() + 4 + displayLabel.getWidth() + 4 + 100 + 4 + 50 + 100 + 4) / 2, displayTitle.getY() + 9 + 4);
		displayLabel.setHeight(20);
		displayLabel.setPosition(nameLabel.getX() + nameLabel.getWidth() + 4 + 100 + 4, nameLabel.getY());
		var name = addDrawableChild(new TextFieldWidget(textRenderer, nameLabel.getX() + nameLabel.getWidth() + 4, nameLabel.getY(), 100, 20, AxolotlClientWaypoints.tr("waypoint_display.name")));
		var display = addDrawableChild(new TextFieldWidget(textRenderer, displayLabel.getX() + displayLabel.getWidth() + 4, displayLabel.getY(), 50, 20, AxolotlClientWaypoints.tr("waypoint_display.display")));
		var lockButton = addDrawableChild(CycleButton.onOffBuilder(true)
			.create(display.getX() + display.getWidth() + 4, displayLabel.getY(), 100, 20, AxolotlClientWaypoints.tr("waypoint_display.unlock_display"),
				(btn, v) -> display.active = !v));
		name.setChangedListener(s -> {
			this.name = s;
			if (lockButton.getValue()) {
				var d = s.trim();
				display.setText(d.isEmpty() ? "" : d.substring(0, 1).toUpperCase(Locale.ROOT));
			}
		});
		name.setText(this.name != null ? this.name : AxolotlClientWaypoints.tra("default_waypoint_name"));
		display.active = false;
		name.setMaxLength(150);
		display.setMaxLength(10);
		if (this.display != null) {
			display.setText(this.display);
		}
		display.setChangedListener(s -> this.display = s);

		var colorTitle = addDrawableChild(new StringWidget(AxolotlClientWaypoints.tr("waypoint_color"), textRenderer)).alignCenter();
		colorTitle.setWidth(width);
		colorTitle.setY(displayLabel.getY() + displayLabel.getHeight() + 4 + 10 + 4);

		addDrawableChild(new ClickableWidget(width / 2 - 2 - 100, colorTitle.getY() + colorTitle.getHeight() + 4, 100, 20, "") {
			@Override
			public void drawWidget(int mouseX, int mouseY, float partialTick) {
				fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), color.get().toInt());
				DrawUtil.outlineRect(getX(), getY(), getWidth(), getHeight(), Colors.BLACK.toInt());
			}
		}).active = false;
		addDrawableChild(new ColorWidget(width / 2 + 2, colorTitle.getY() + colorTitle.getHeight() + 4, 100, 20, color));

		int footerY = height - 33 / 2 - 20 / 2;
		create = addDrawableChild(new VanillaButtonWidget(width / 2 - 2 - 150, footerY, 150, 20, AxolotlClientWaypoints.tr("create_waypoint"), btn -> {
			AxolotlClientWaypoints.WAYPOINT_STORAGE.create(new Waypoint(world.getText(), dimensionSupplier.get(), this.x, this.y, this.z, color.getOriginal(), name.getText(), display.getText()));
			minecraft.openScreen(parent);
		}));
		addDrawableChild(new VanillaButtonWidget(width / 2 + 20, footerY, 150, 20, I18n.translate("gui.cancel"), btn -> minecraft.openScreen(parent)));
	}

	@Override
	public void tick() {
		create.active = x != -1 && y != -1 && z != -1;
	}
}
