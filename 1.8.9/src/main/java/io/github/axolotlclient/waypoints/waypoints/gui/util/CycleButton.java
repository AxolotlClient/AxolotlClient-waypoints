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

import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import io.github.axolotlclient.AxolotlClientConfig.impl.ui.vanilla.widgets.VanillaButtonWidget;
import lombok.Getter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.locale.I18n;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class CycleButton<T> extends VanillaButtonWidget {
	public static final BooleanSupplier DEFAULT_ALT_LIST_SELECTOR = Screen::isAltDown;
	private static final List<Boolean> BOOLEAN_OPTIONS = ImmutableList.of(Boolean.TRUE, Boolean.FALSE);
	private final String name;
	private int index;
	@Getter
	private T value;
	private final CycleButton.ValueListSupplier<T> values;
	private final Function<T, String> valueStringifier;
	private final CycleButton.OnValueChange<T> onValueChange;
	private final boolean displayOnlyValue;

	CycleButton(
		int x,
		int y,
		int width,
		int height,
		String message,
		String name,
		int index,
		T value,
		CycleButton.ValueListSupplier<T> values,
		Function<T, String> valueStringifier,
		CycleButton.OnValueChange<T> onValueChange,
		boolean displayOnlyValue
	) {
		super(x, y, width, height, message, b -> {
		});
		this.name = name;
		this.index = index;
		this.value = value;
		this.values = values;
		this.valueStringifier = valueStringifier;
		this.onValueChange = onValueChange;
		this.displayOnlyValue = displayOnlyValue;
	}

	@Override
	public void onPress() {
		if (Screen.isShiftDown()) {
			this.cycleValue(-1);
		} else {
			this.cycleValue(1);
		}
	}

	private void cycleValue(int delta) {
		List<T> list = this.values.getSelectedList();
		this.index = Math.floorMod(this.index + delta, list.size());
		T object = list.get(this.index);
		this.updateValue(object);
		this.onValueChange.onValueChange(this, object);
	}

	private T getCycledValue(int delta) {
		List<T> list = this.values.getSelectedList();
		return list.get(Math.floorMod(this.index + delta, list.size()));
	}

	/*@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (delta > 0.0) {
			this.cycleValue(-1);
		} else if (delta < 0.0) {
			this.cycleValue(1);
		}

		return true;
	}*/

	public void setValue(T value) {
		List<T> list = this.values.getSelectedList();
		int i = list.indexOf(value);
		if (i != -1) {
			this.index = i;
		}

		this.updateValue(value);
	}

	private void updateValue(T value) {
		String component = this.createLabelForValue(value);
		this.setMessage(component);
		this.value = value;
	}

	private String createLabelForValue(T value) {
		return this.displayOnlyValue ? this.valueStringifier.apply(value) : this.createFullName(value);
	}

	private String createFullName(T value) {
		return this.name + ": " + this.valueStringifier.apply(value);
	}

	public static <T> CycleButton.Builder<T> builder(Function<T, String> valueStringifier) {
		return new CycleButton.Builder<>(valueStringifier);
	}

	public static CycleButton.Builder<Boolean> booleanBuilder(String componentOn, String componentOff) {
		return new CycleButton.Builder<Boolean>(value -> value ? componentOn : componentOff).withValues(BOOLEAN_OPTIONS);
	}

	public static CycleButton.Builder<Boolean> onOffBuilder() {
		return new CycleButton.Builder<Boolean>(value -> value ? I18n.translate("options.on") : I18n.translate("options.off")).withValues(BOOLEAN_OPTIONS);
	}

	public static CycleButton.Builder<Boolean> onOffBuilder(boolean initialValue) {
		return onOffBuilder().withInitialValue(initialValue);
	}

	@Environment(EnvType.CLIENT)
	public static class Builder<T> {
		private int initialIndex;
		@Nullable
		private T initialValue;
		private final Function<T, String> valueStringifier;
		private CycleButton.ValueListSupplier<T> values = CycleButton.ValueListSupplier.create(ImmutableList.<T>of());
		private boolean displayOnlyValue;

		public Builder(Function<T, String> valueStringifier) {
			this.valueStringifier = valueStringifier;
		}

		public CycleButton.Builder<T> withValues(Collection<T> values) {
			return this.withValues(CycleButton.ValueListSupplier.create(values));
		}

		@SafeVarargs
		public final CycleButton.Builder<T> withValues(T... values) {
			return this.withValues(ImmutableList.copyOf(values));
		}

		public CycleButton.Builder<T> withValues(List<T> defaultList, List<T> selectedList) {
			return this.withValues(CycleButton.ValueListSupplier.create(CycleButton.DEFAULT_ALT_LIST_SELECTOR, defaultList, selectedList));
		}

		public CycleButton.Builder<T> withValues(BooleanSupplier altListSelector, List<T> defaultList, List<T> selectedList) {
			return this.withValues(CycleButton.ValueListSupplier.create(altListSelector, defaultList, selectedList));
		}

		public CycleButton.Builder<T> withValues(CycleButton.ValueListSupplier<T> values) {
			this.values = values;
			return this;
		}

		public CycleButton.Builder<T> withInitialValue(T initialValue) {
			this.initialValue = initialValue;
			int i = this.values.getDefaultList().indexOf(initialValue);
			if (i != -1) {
				this.initialIndex = i;
			}

			return this;
		}

		public CycleButton.Builder<T> displayOnlyValue() {
			this.displayOnlyValue = true;
			return this;
		}

		public CycleButton<T> create(int x, int y, int width, int height, String name) {
			return this.create(x, y, width, height, name, (cycleButton, value) -> {
			});
		}

		public CycleButton<T> create(int x, int y, int width, int height, String name, CycleButton.OnValueChange<T> onValueChange) {
			List<T> list = this.values.getDefaultList();
			if (list.isEmpty()) {
				throw new IllegalStateException("No values for cycle button");
			} else {
				T object = this.initialValue != null ? this.initialValue : list.get(this.initialIndex);
				String component = this.valueStringifier.apply(object);
				String component2 = this.displayOnlyValue ? component : name + ": " + component;
				return new CycleButton<>(
					x,
					y,
					width,
					height,
					component2,
					name,
					this.initialIndex,
					object,
					this.values,
					this.valueStringifier,
					onValueChange,
					this.displayOnlyValue
				);
			}
		}
	}

	@Environment(EnvType.CLIENT)
	public interface OnValueChange<T> {
		void onValueChange(CycleButton<T> cycleButton, T object);
	}

	@Environment(EnvType.CLIENT)
	public interface ValueListSupplier<T> {
		List<T> getSelectedList();

		List<T> getDefaultList();

		static <T> CycleButton.ValueListSupplier<T> create(Collection<T> values) {
			final List<T> list = ImmutableList.copyOf(values);
			return new CycleButton.ValueListSupplier<T>() {
				@Override
				public List<T> getSelectedList() {
					return list;
				}

				@Override
				public List<T> getDefaultList() {
					return list;
				}
			};
		}

		static <T> CycleButton.ValueListSupplier<T> create(BooleanSupplier altListSelector, List<T> defaultList, List<T> selectedList) {
			final List<T> list = ImmutableList.copyOf(defaultList);
			final List<T> list2 = ImmutableList.copyOf(selectedList);
			return new CycleButton.ValueListSupplier<T>() {
				@Override
				public List<T> getSelectedList() {
					return altListSelector.getAsBoolean() ? list2 : list;
				}

				@Override
				public List<T> getDefaultList() {
					return list;
				}
			};
		}
	}
}
