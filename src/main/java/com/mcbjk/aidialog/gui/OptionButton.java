package com.mcbjk.aidialog.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * 轻量表单控件：左标签 + 右数值，点击循环切换。
 * 用来在无额外依赖的情况下做出设置界面。
 */
public class OptionButton extends AbstractWidget {
	private final Font font;
	private final Component label;
	private final BooleanSupplier getter;
	private final Consumer<Boolean> setter;
	private final String[] values;

	/** 布尔开关。 */
	public static OptionButton toggle(int x, int y, int width, Component label, BooleanSupplier getter, Consumer<Boolean> setter) {
		return new OptionButton(x, y, width, label, getter, b -> setter.accept(b), null);
	}

	/** 枚举循环。 */
	public static OptionButton cycle(int x, int y, int width, Component label, String[] values,
									 java.util.function.Supplier<String> getter, Consumer<String> setter) {
		return new OptionButton(x, y, width, label, null, null, values, getter, setter);
	}

	private final java.util.function.Supplier<String> valueGetter;
	private final Consumer<String> valueSetter;

	private OptionButton(int x, int y, int width, Component label, BooleanSupplier getter, Consumer<Boolean> setter,
						 String[] values) {
		this(x, y, width, label, getter, setter, values, null, null);
	}

	private OptionButton(int x, int y, int width, Component label, BooleanSupplier getter, Consumer<Boolean> setter,
						 String[] values, java.util.function.Supplier<String> valueGetter, Consumer<String> valueSetter) {
		super(x, y, width, 18, label);
		this.font = Minecraft.getInstance().font;
		this.label = label;
		this.getter = getter;
		this.setter = setter;
		this.values = values;
		this.valueGetter = valueGetter;
		this.valueSetter = valueSetter;
	}

	private String currentValue() {
		if (values != null && valueGetter != null) {
			String value = valueGetter.get();
			return value == null ? "" : value;
		}
		return getter != null && getter.getAsBoolean() ? "开" : "关";
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int bg = isHovered() ? 0xFF3A3A3A : 0xFF262626;
		graphics.fill(getX(), getY(), getX() + width, getY() + height, bg);
		graphics.fill(getX(), getY(), getX() + width, getY() + 1, 0xFF555555);
		graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, 0xFF151515);

		graphics.drawString(font, label, getX() + 5, getY() + 5, 0xFFA0A0A0, false);

		String value = currentValue();
		boolean on = values == null && getter != null && getter.getAsBoolean();
		int valueColor = on ? 0xFF55FF55 : 0xFFFFD479;
		if (values != null) {
			valueColor = 0xFF8FC7FF;
		}
		int textWidth = font.width(value);
		graphics.drawString(font, value, getX() + width - 5 - textWidth, getY() + 5, valueColor, false);
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		if (values != null) {
			String current = currentValue();
			int index = 0;
			for (int i = 0; i < values.length; i++) {
				if (values[i].equals(current)) {
					index = i;
					break;
				}
			}
			int next = (index + (event.button() == 1 ? -1 : 1) + values.length) % values.length;
			if (valueSetter != null) {
				valueSetter.accept(values[next]);
			}
		} else if (setter != null && getter != null) {
			setter.accept(!getter.getAsBoolean());
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
