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
