package io.github.axolotlclient.waypoints.util;

import io.github.axolotlclient.AxolotlClientConfig.impl.ui.vanilla.widgets.BooleanWidget;
import io.github.axolotlclient.waypoints.AxolotlClientWaypoints;
import io.github.axolotlclient.waypoints.BooleanOption;
import net.minecraft.client.gui.components.Tooltip;

@SuppressWarnings("unused")
public class ExtendedBooleanWidget extends BooleanWidget {
	private final BooleanOption option;
	public ExtendedBooleanWidget(int x, int y, int width, int height, BooleanOption option) {
		super(x, y, width, height, option);
		this.option = option;
		update();
	}

	@Override
	public void update() {
		super.update();
		if (option.isForced()) {
			this.active = false;
			setTooltip(Tooltip.create(AxolotlClientWaypoints.tr("option_disabled")));
		}
	}
}
