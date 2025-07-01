package io.github.axolotlclient.waypoints.map.widgets;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public record WidgetSprites(ResourceLocation enabled, ResourceLocation disabled, ResourceLocation enabledFocused, ResourceLocation disabledFocused) {
	public WidgetSprites(ResourceLocation enabled, ResourceLocation disabled) {
		this(enabled, enabled, disabled, disabled);
	}

	public WidgetSprites(ResourceLocation enabled, ResourceLocation disabled, ResourceLocation enabledFocused) {
		this(enabled, disabled, enabledFocused, disabled);
	}

	public ResourceLocation get(boolean enabled, boolean focused) {
		if (enabled) {
			return focused ? this.enabledFocused : this.enabled;
		} else {
			return focused ? this.disabledFocused : this.disabled;
		}
	}
}
