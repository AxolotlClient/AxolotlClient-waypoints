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

package io.github.axolotlclient.waypoints.util;

import java.util.Arrays;

import io.github.axolotlclient.AxolotlClientConfig.impl.ui.rounded.widgets.BooleanWidget;
import io.github.axolotlclient.waypoints.AxolotlClientWaypoints;
import io.github.axolotlclient.waypoints.BooleanOption;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.TextRenderer;

@SuppressWarnings("unused")
public class ExtendedBooleanWidgetRounded extends BooleanWidget {
	private final BooleanOption option;
	private String[] tooltip;

	public ExtendedBooleanWidgetRounded(int x, int y, int width, int height, BooleanOption option) {
		super(x, y, width, height, option);
		this.option = option;
		update();
	}

	@Override
	public void drawWidget(int mouseX, int mouseY, float delta) {
		super.drawWidget(mouseX, mouseY, delta);
		if (tooltip != null) {
			this.renderTooltip(Arrays.asList(tooltip), mouseX - 2, mouseY + 12 + 3 + 10);
		}
	}

	@Override
	public void update() {
		super.update();
		tooltip = null;
		if (option.isForced()) {
			this.active = false;
			TextRenderer renderer = Minecraft.getInstance().textRenderer;
			tooltip = Arrays.stream(AxolotlClientWaypoints.tr("option_disabled").split("<br>"))
				.flatMap((text1) -> renderer.split(text1, 170).stream())
				.toArray(String[]::new);

		}
	}
}
