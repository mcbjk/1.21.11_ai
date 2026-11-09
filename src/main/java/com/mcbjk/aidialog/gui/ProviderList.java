package com.mcbjk.aidialog.gui;

import com.mcbjk.aidialog.config.ModConfig;
import com.mcbjk.aidialog.config.ProviderPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 厂商选择面板：列出所有内置厂商 + 自定义，点击即应用预设。
 * 自带内部滚动，不依赖 scissor（避免和外层滚动区域冲突）。
 */
public class ProviderList extends AbstractWidget {
	private static final int ROW_HEIGHT = 24;

	/**
	 * 列表内部滚动位置。做成静态的，因为本控件在每次配置变更后都会被整个重建
	 * （重建是为了刷新各行的选中态），存在实例字段里的话每次都会归零，
	 * 表现就是点一下厂商列表就跳回第一个。
	 */
	private static int scroll;

	private final Font font;
	private final ModConfig config;
	private final Runnable onChanged;
	private final List<ProviderPreset> presets = new ArrayList<>();

	public ProviderList(int x, int y, int width, int height, ModConfig config, Runnable onChanged) {
		super(x, y, width, height, Component.literal("厂商列表"));
		this.font = Minecraft.getInstance().font;
		this.config = config;
		this.onChanged = onChanged;
		for (ProviderPreset preset : ProviderPreset.values()) {
			presets.add(preset);
		}
	}

	private int visibleRows() {
		return Math.max(1, height / ROW_HEIGHT);
	}

	private int maxScroll() {
		return Math.max(0, presets.size() - visibleRows());
	}

	/**
	 * 把当前选中的厂商行滚进可视范围。
	 * 构造函数里自动调用，这样选中项永远不会停在视野外面。
	 */
	public void ensureSelectedVisible() {
		int index = presets.indexOf(config.preset());
		if (index < 0) {
			return;
		}
		int rows = visibleRows();
		if (index < scroll) {
			scroll = index;
		} else if (index >= scroll + rows) {
			scroll = index - rows + 1;
		}
		scroll = Math.max(0, Math.min(maxScroll(), scroll));
	}

	/** 已显示的行数（用于判断要不要画滚动条）。 */
	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(getX() - 1, getY() - 1, getX() + width + 1, getY() + height + 1, 0xFF0A0A0A);

		ProviderPreset selected = config.preset();
		int rows = visibleRows();
		for (int i = 0; i < rows; i++) {
			int index = scroll + i;
			if (index >= presets.size()) {
				break;
			}
			ProviderPreset preset = presets.get(index);
			int rowY = getY() + i * ROW_HEIGHT;
			boolean isSelected = preset == selected;
			boolean hovered = mouseX >= getX() && mouseX < getX() + width
					&& mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;

			int bg = isSelected ? 0xFF244A24 : hovered ? 0xFF2C2C2C : 0xFF1B1B1B;
			graphics.fill(getX(), rowY, getX() + width, rowY + ROW_HEIGHT - 1, bg);
			if (isSelected) {
				graphics.fill(getX(), rowY, getX() + 2, rowY + ROW_HEIGHT - 1, 0xFF55FF55);
			}

			graphics.drawString(font, preset.displayName(), getX() + 6, rowY + 3,
					isSelected ? 0xFFB9FFB9 : 0xFFE0E0E0, false);

			String sub = preset.isCustom()
					? "手动填写接口地址与模型"
					: preset.baseUrl().replaceFirst("^https?://", "") + " · " + preset.defaultModel();
			sub = font.plainSubstrByWidth(sub, width - 12);
			graphics.drawString(font, sub, getX() + 6, rowY + 13, 0xFF707070, false);
		}

		int max = maxScroll();
		if (max > 0) {
			int barX = getX() + width - 3;
			int barHeight = Math.max(12, height * visibleRows() / presets.size());
			int barY = getY() + (height - barHeight) * scroll / max;
			graphics.fill(barX, getY(), barX + 2, getY() + height, 0xFF262626);
			graphics.fill(barX, barY, barX + 2, barY + barHeight, 0xFF808080);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (!isMouseOver(event.x(), event.y())) {
			return false;
		}
		int index = scroll + (int) ((event.y() - getY()) / ROW_HEIGHT);
		if (index >= 0 && index < presets.size()) {
			config.applyPreset(presets.get(index));
			if (onChanged != null) {
				onChanged.run();
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (!isOverUsableArea(mouseX, mouseY)) {
			return false;
		}
		int max = maxScroll();
		// 列表内部已经滚到顶/底、或者内容本来就不用滚时返回 false，
		// 把事件让给外层页面继续滚：否则鼠标停在列表上，页面就再也滚不动了。
		if (max <= 0) {
			return false;
		}
		int next = Math.max(0, Math.min(max, scroll - (int) Math.signum(scrollY)));
		if (next == scroll) {
			return false;
		}
		scroll = next;
		return true;
	}

	/** 鼠标是否真的落在列表矩形内（原版 isMouseOver 不看 visible，这里补上）。 */
	private boolean isOverUsableArea(double mouseX, double mouseY) {
		return visible && active && isMouseOver(mouseX, mouseY);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
