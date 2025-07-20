package io.github.axolotlclient.waypoints.util;

import java.util.Arrays;

import io.github.axolotlclient.AxolotlClientConfig.impl.ui.vanilla.widgets.BooleanWidget;
import io.github.axolotlclient.waypoints.AxolotlClientWaypoints;
import io.github.axolotlclient.waypoints.BooleanOption;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.TextRenderer;

@SuppressWarnings("unused")
public class ExtendedBooleanWidget extends BooleanWidget {
	private final BooleanOption option;
	private String[] tooltip;

	public ExtendedBooleanWidget(int x, int y, int width, int height, BooleanOption option) {
		super(x, y, width, height, option);
		this.option = option;
		update();
	}

	@Override
	protected void drawWidget(int mouseX, int mouseY, float delta) {
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
