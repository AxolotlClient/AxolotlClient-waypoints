package io.github.axolotlclient.waypoints.waypoints.gui.util;

import java.util.function.Consumer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;

@Environment(EnvType.CLIENT)
public class LinearLayout implements Layout {
	private final GridLayout wrapped;
	private final Orientation orientation;
	private int nextChildIndex = 0;

	private LinearLayout(Orientation orientation) {
		this(0, 0, orientation);
	}

	public LinearLayout(int width, int height, Orientation orientation) {
		this.wrapped = new GridLayout(width, height);
		this.orientation = orientation;
	}

	public LinearLayout spacing(int spacing) {
		this.orientation.setSpacing(this.wrapped, spacing);
		return this;
	}

	public LayoutSettings newCellSettings() {
		return this.wrapped.newCellSettings();
	}

	public LayoutSettings defaultCellSetting() {
		return this.wrapped.defaultCellSetting();
	}

	public <T extends LayoutElement> T addChild(T child, LayoutSettings layoutSettings) {
		return this.orientation.addChild(this.wrapped, child, this.nextChildIndex++, layoutSettings);
	}

	public <T extends LayoutElement> T addChild(T child) {
		return this.addChild(child, this.newCellSettings());
	}

	public <T extends LayoutElement> T addChild(T child, Consumer<LayoutSettings> layoutSettingsFactory) {
		return this.orientation.addChild(this.wrapped, child, this.nextChildIndex++, Util.make(this.newCellSettings(), layoutSettingsFactory));
	}

	@Override
	public void visitChildren(Consumer<LayoutElement> visitor) {
		this.wrapped.visitChildren(visitor);
	}

	@Override
	public void arrangeElements() {
		this.wrapped.arrangeElements();
	}

	@Override
	public int getWidth() {
		return this.wrapped.getWidth();
	}

	@Override
	public int getHeight() {
		return this.wrapped.getHeight();
	}

	@Override
	public void setX(int x) {
		this.wrapped.setX(x);
	}

	@Override
	public void setY(int y) {
		this.wrapped.setY(y);
	}

	@Override
	public int getX() {
		return this.wrapped.getX();
	}

	@Override
	public int getY() {
		return this.wrapped.getY();
	}

	public static LinearLayout vertical() {
		return new LinearLayout(Orientation.VERTICAL);
	}

	public static LinearLayout horizontal() {
		return new LinearLayout(Orientation.HORIZONTAL);
	}

	public boolean rectContainsPoint(int x, int y) {
		var rect = getRectangle();
		return x >= rect.left() && x < rect.right() && y >= rect.top() && y < rect.bottom();
	}

	@Environment(EnvType.CLIENT)
	public enum Orientation {
		HORIZONTAL,
		VERTICAL;

		void setSpacing(GridLayout layout, int spacing) {
			switch (this) {
				case HORIZONTAL:
					layout.columnSpacing(spacing);
					break;
				case VERTICAL:
					layout.rowSpacing(spacing);
			}
		}

		public <T extends LayoutElement> T addChild(GridLayout layout, T element, int index, LayoutSettings layoutSettings) {
			return switch (this) {
				case HORIZONTAL -> layout.addChild(element, 0, index, layoutSettings);
				case VERTICAL -> layout.addChild(element, index, 0, layoutSettings);
			};
		}
	}
}
