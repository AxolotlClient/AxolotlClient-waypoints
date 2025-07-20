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

package io.github.axolotlclient.waypoints;

import lombok.Getter;

public class BooleanOption extends io.github.axolotlclient.AxolotlClientConfig.impl.options.BooleanOption {
	@Getter
	private boolean forced;
	private boolean forcedValue;

	public BooleanOption(String name, Boolean defaultValue) {
		super(name, defaultValue);
	}

	public BooleanOption(String name, Boolean defaultValue, ChangeListener<Boolean> changeListener) {
		super(name, defaultValue, changeListener);
	}

	public void force(boolean forced, boolean value) {
		this.forced = forced;
		this.forcedValue = value;
	}

	@SuppressWarnings("RedundantMethodOverride")
	@Override
	public String toSerializedValue() {
		return String.valueOf(super.get());
	}

	@Override
	public Boolean get() {
		if (forced) {
			return forcedValue;
		}
		return super.get();
	}

	@Override
	public String getWidgetIdentifier() {
		return AxolotlClientWaypointsCommon.MODID+".boolean";
	}
}
