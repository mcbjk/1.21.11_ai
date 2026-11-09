package com.mcbjk.aidialog.gui;

import com.mcbjk.aidialog.AiDialogMod;
import com.mcbjk.aidialog.chat.AutoChatHandler;
import com.mcbjk.aidialog.config.AiStyle;
import com.mcbjk.aidialog.config.ModConfig;
import com.mcbjk.aidialog.config.ProviderPreset;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 主设置界面。分三页：厂商与接口 / 触发与回复 / 人设与测试。
 * 内容区可滚动，适配任意 GUI 缩放（小窗口也不会挤爆）。
 */
public class AiDialogScreen extends Screen {
	private static final int TAB_HEIGHT = 20;
	private static final int PANEL_BG = 0xF0121212;
	private static final int ACCENT = 0xFF55FF55;
	private static final int LABEL = 0xFF9A9A9A;
	private static final int WHITE = 0xFFE8E8E8;
	private static final int ROW = 28;

	private final Screen parent;
	private final ModConfig config = ModConfig.get();

	private int tab;
	private int left;
	private int top;
	private int contentWidth;
	private int contentTop;
	private int contentBottom;

	private int scroll;

	private final List<AbstractWidget> scrolledWidgets = new ArrayList<>();
	private final List<Integer> widgetBaseY = new ArrayList<>();
	private final List<Label> labels = new ArrayList<>();

	private record Label(String text, int x, int baseY, int width, String hint) {
	}

	public AiDialogScreen(Screen parent) {
		super(Component.literal("AI 自动对话设置"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		scrolledWidgets.clear();
		widgetBaseY.clear();
		labels.clear();

		contentWidth = Math.min(this.width - 24, 460);
		left = (this.width - contentWidth) / 2;
		top = 22;
		contentTop = top + TAB_HEIGHT + 6;
		contentBottom = this.height - 28;

		int tabWidth = contentWidth / 3;
		addRenderableWidget(Button.builder(Component.literal("厂商与接口"), b -> switchTab(0))
				.bounds(left, top, tabWidth - 2, TAB_HEIGHT).build());
		addRenderableWidget(Button.builder(Component.literal("触发与回复"), b -> switchTab(1))
				.bounds(left + tabWidth, top, tabWidth - 2, TAB_HEIGHT).build());
		addRenderableWidget(Button.builder(Component.literal("人设与测试"), b -> switchTab(2))
				.bounds(left + tabWidth * 2, top, tabWidth, TAB_HEIGHT).build());

		int savedScroll = scroll;
		switch (tab) {
			case 0 -> buildProviderTab();
			case 1 -> buildTriggerTab();
			default -> buildPersonaTab();
		}

		int bottom = this.height - 24;
		addRenderableWidget(Button.builder(Component.literal("保存并关闭"), b -> {
			ModConfig.save();
			onClose();
		}).bounds(left + contentWidth - 172, bottom, 90, 20).build());
		addRenderableWidget(Button.builder(Component.literal("取消"), b -> {
			ModConfig.load();
			onClose();
		}).bounds(left + contentWidth - 78, bottom, 78, 20).build());

		// 布局完成后按内容高度夹一次，并恢复重建前的滚动位置。
		setScroll(Math.min(savedScroll, maxScroll()));
		AiDialogMod.LOGGER.info("[GUI布局] {}{}", debugLayout(), debugWidgets());
	}

	private void switchTab(int index) {
		tab = index;
		rebuildWidgets();
	}

	private int maxScroll() {
		int contentBottomY = 0;
		for (int i = 0; i < scrolledWidgets.size(); i++) {
			int h = scrolledWidgets.get(i).getHeight();
			contentBottomY = Math.max(contentBottomY, widgetBaseY.get(i) + h);
		}
		for (Label label : labels) {
			contentBottomY = Math.max(contentBottomY, label.baseY() + 9);
		}
		int visible = contentBottom - contentTop;
		return Math.max(0, contentBottomY - contentTop - visible);
	}

	private void setScroll(int value) {
		scroll = Math.max(0, Math.min(value, maxScroll()));
		for (int i = 0; i < scrolledWidgets.size(); i++) {
			// 只需挪位置。裁剪交给 render 里的 scissor：
			// AbstractWidget.render 会自己按 scissor 判定 isHovered，
			// 因此滚出可视区的控件既不会被画出来，也不会保留高亮。
			scrolledWidgets.get(i).setY(contentTop + widgetBaseY.get(i) - scroll);
		}
	}

	private <T extends AbstractWidget> T addScrolled(T widget, int baseY) {
		scrolledWidgets.add(widget);
		widgetBaseY.add(baseY);
		addRenderableWidget(widget);
		return widget;
	}

	private void addLabel(String text, int baseY, String hint) {
		labels.add(new Label(text, left, baseY, contentWidth, hint));
	}

	/** 诊断：把当前页的布局尺寸打到日志，用于确认控件是否落在可视区内。 */
	public String debugLayout() {
		int contentBottomY = 0;
		for (int i = 0; i < scrolledWidgets.size(); i++) {
			contentBottomY = Math.max(contentBottomY, widgetBaseY.get(i) + scrolledWidgets.get(i).getHeight());
		}
		for (Label label : labels) {
			contentBottomY = Math.max(contentBottomY, label.baseY() + 9);
		}
		return "tab=" + tab
				+ " screen=" + this.width + "x" + this.height
				+ " view=" + (contentBottom - contentTop)
				+ " contentHeight=" + contentBottomY
				+ " maxScroll=" + maxScroll()
				+ " widgets=" + scrolledWidgets.size()
				+ " labels=" + labels.size()
				+ " systemPrompt.len=" + (config.systemPrompt == null ? -1 : config.systemPrompt.length());
	}

	/** 诊断：列出每个控件的类型、基准 Y 与高度，确认有没有控件被挤出可视区。 */
	private String debugWidgets() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < scrolledWidgets.size(); i++) {
			net.minecraft.client.gui.components.AbstractWidget w = scrolledWidgets.get(i);
			sb.append('\n').append("    #").append(i).append(' ')
					.append(w.getClass().getSimpleName())
					.append(" baseY=").append(widgetBaseY.get(i))
					.append(" h=").append(w.getHeight())
					.append(" bottom=").append(widgetBaseY.get(i) + w.getHeight());
		}
		return sb.toString();
	}

	// ------------------------------------------------------------------ 通用控件

	/** 文本输入行：上方标签 + 下方输入框。返回下一行基准 Y。 */
	private int textRow(int baseY, String label, String value, String hint, Consumer<String> setter) {
		addLabel(label, baseY, hint);
		EditBox box = new EditBox(this.font, left, contentTop, contentWidth, 16, Component.literal(label));
		box.setMaxLength(512);
		box.setValue(value == null ? "" : value);
		box.setHint(Component.literal(hint == null ? "" : hint).withStyle(ChatFormatting.DARK_GRAY));
		box.setResponder(setter);
		addScrolled(box, baseY + 10);
		return baseY + ROW + 2;
	}

	/** 布尔行（读取实时值）。 */
	private int toggleRowLive(int baseY, String label, String hint,
							  java.util.function.BooleanSupplier getter, Consumer<Boolean> setter) {
		addScrolled(OptionButton.toggle(left, contentTop, contentWidth, Component.literal(label), getter, setter), baseY);
		addLabel("", baseY, hint);
		return baseY + 22;
	}

	private int cycleRow(int baseY, String label, String hint, String[] values,
						 java.util.function.Supplier<String> getter, Consumer<String> setter) {
		addScrolled(OptionButton.cycle(left, contentTop, contentWidth, Component.literal(label), values, getter, setter), baseY);
		addLabel("", baseY, hint);
		return baseY + 22;
	}

	// ------------------------------------------------------------------ 厂商页

	private void buildProviderTab() {
		int y = 12;

		ProviderList list = new ProviderList(left, contentTop, contentWidth, 132, config, () -> {
			ModConfig.save();
			rebuildWidgets();
		});
		list.ensureSelectedVisible();
		addScrolled(list, y);
		addLabel("厂商预设（左键选择 / 右键上一个；选中后字段仍可手改）", 0,
				"也可以选最下面的「自定义接口」接任意 OpenAI 兼容服务，例如自建 one-api、vLLM、LM Studio。");
		y += 132 + 14;

		// 当前厂商：显示选中项，也让用户不滚回列表就能确认自己选了什么。
		addLabel("当前厂商", y, "改下面的接口地址或模型会自动切成「自定义接口」。");
		y += 11;
		OptionButton presetCycle = OptionButton.cycle(left, contentTop, contentWidth,
				Component.literal("当前厂商"), presetNames(),
				() -> config.preset().displayName(), name -> {
					config.applyPreset(ProviderPreset.byName(reversePresetName(name)));
					ModConfig.save();
					rebuildWidgets();
				});
		addScrolled(presetCycle, y);
		y += 24;

		y = textRow(y, "接口地址 Base URL", config.baseUrl,
				"例如 https://api.deepseek.com/v1", v -> {
					config.baseUrl = v;
					config.provider = ProviderPreset.CUSTOM.name();
				});

		y = textRow(y, "API Key", config.apiKey, "留空则不带鉴权头", v -> config.apiKey = v);

		y = textRow(y, "模型名称", config.model, "例如 deepseek-chat", v -> {
			config.model = v;
			config.provider = ProviderPreset.CUSTOM.name();
		});

		y = cycleRow(y, "请求格式", "绝大多数厂商用 OpenAI 兼容格式",
				new String[]{"OpenAI 兼容", "Gemini", "Claude"},
				() -> config.style().displayName(), name -> {
					for (AiStyle style : AiStyle.values()) {
						if (style.displayName().equals(name)) {
							config.apiStyle = style.name();
							break;
						}
					}
				});

		y = cycleRow(y, "随机性 temperature", "聊天建议 0.7 ~ 1.0，太低会呆板",
				new String[]{"0.3", "0.5", "0.7", "0.8", "0.9", "1.0", "1.2"},
				() -> String.valueOf(config.temperature), name -> config.temperature = Double.parseDouble(name));

		y = cycleRow(y, "最大 token", "聊天栏一行话，60 ~ 120 足够",
				new String[]{"40", "60", "80", "120", "200", "400"},
				() -> String.valueOf(config.maxTokens), name -> config.maxTokens = Integer.parseInt(name));

		y = cycleRow(y, "请求超时（秒）", "网络慢就调大",
				new String[]{"10", "20", "30", "60", "120"},
				() -> String.valueOf(config.timeoutSeconds), name -> config.timeoutSeconds = Integer.parseInt(name));

		Button test = Button.builder(Component.literal("测试连接（结果看聊天栏）"), b -> {
			ModConfig.save();
			AutoChatHandler.testConnection();
		}).bounds(left, contentTop, contentWidth, 20).build();
		addScrolled(test, y);
		addLabel("", y, "会用当前配置真实调用一次接口。");
	}

	/** 所有厂商的显示名，给「当前厂商」循环控件用。 */
	private static String[] presetNames() {
		ProviderPreset[] values = ProviderPreset.values();
		String[] names = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			names[i] = values[i].displayName();
		}
		return names;
	}

	/** 从显示名反查枚举名（ProviderPreset.byName 接受的是枚举名而非显示名）。 */
	private static String reversePresetName(String displayName) {
		for (ProviderPreset preset : ProviderPreset.values()) {
			if (preset.displayName().equals(displayName)) {
				return preset.name();
			}
		}
		return ProviderPreset.CUSTOM.name();
	}

	// ------------------------------------------------------------------ 触发页
	private void buildTriggerTab() {
		int y = 4;

		y = toggleRowLive(y, "总开关", "关闭后完全不再自动回复，快捷键也能切", () -> config.enabled, v -> config.enabled = v);

		y = textRow(y, "触发词（逗号分隔）", config.triggerKeyword,
				"留空 = 只用登录名 " + AutoChatHandler.selfName(), v -> config.triggerKeyword = v);

		y = toggleRowLive(y, "用登录名自动触发", "当前账号 " + AutoChatHandler.selfName() + " 会作为触发词",
				() -> config.useAccountIdAsTrigger, v -> config.useAccountIdAsTrigger = v);

		y = toggleRowLive(y, "忽略大小写", "Steve 与 steve 视为同一个词",
				() -> config.ignoreCase, v -> config.ignoreCase = v);

		y = toggleRowLive(y, "整词匹配", "避免 Steve 命中 Stevecraft；中文触发词不受影响",
				() -> config.wholeWordOnly, v -> config.wholeWordOnly = v);

		y = cycleRow(y, "每人冷却（秒）", "同一玩家在这段时间内只回一次",
				new String[]{"0", "5", "10", "15", "30", "60", "120", "300"},
				() -> String.valueOf(config.cooldownSeconds), name -> config.cooldownSeconds = Integer.parseInt(name));

		y = cycleRow(y, "最短回复延迟", "收到后等一会儿再发，模拟打字",
				new String[]{"0", "500", "1000", "1500", "2000", "3000", "5000"},
				() -> String.valueOf(config.replyDelayMinMs), name -> {
					config.replyDelayMinMs = Integer.parseInt(name);
					if (config.replyDelayMaxMs < config.replyDelayMinMs) {
						config.replyDelayMaxMs = config.replyDelayMinMs;
					}
				});

		y = cycleRow(y, "最长回复延迟", "实际延迟在这个区间内随机",
				new String[]{"0", "1500", "2000", "3000", "4000", "6000", "10000"},
				() -> String.valueOf(config.replyDelayMaxMs),
				name -> config.replyDelayMaxMs = Math.max(Integer.parseInt(name), config.replyDelayMinMs));

		y = cycleRow(y, "回复长度上限", "硬上限 256 字符，超出自动截断",
				new String[]{"64", "96", "128", "180", "220", "256"},
				() -> String.valueOf(config.maxReplyLength), name -> config.maxReplyLength = Integer.parseInt(name));

		y = toggleRowLive(y, "携带聊天上下文", "把最近几条聊天一起发给模型，回复更连贯",
				() -> config.sendContext, v -> config.sendContext = v);

		y = cycleRow(y, "上下文条数", "只影响发给模型的历史条数",
				new String[]{"0", "4", "6", "8", "12", "20"},
				() -> String.valueOf(config.contextMessageCount), name -> config.contextMessageCount = Integer.parseInt(name));

		List<String> online = AutoChatHandler.onlinePlayerNames();
		addLabel("当前在线：" + (online.isEmpty() ? "（未连接服务器或无其他玩家）" : String.join(", ", online)),
				y + 2, null);
	}

	// ------------------------------------------------------------------ 人设页

	private void buildPersonaTab() {
		int y = 4;

		addLabel("人设 System Prompt（{player} 会替换成你的游戏 ID）", y, "写清楚语气、身份、说话长度；不要写换行。");
		MultiLineEditBox box = MultiLineEditBox.builder()
				.setX(left)
				.setY(contentTop)
				.setShowBackground(true)
				.setShowDecorations(true)
				.setPlaceholder(Component.literal("用 {player} 代表你的游戏 ID"))
				.build(this.font, contentWidth, 84, Component.literal("人设"));
		box.setValue(config.systemPrompt == null ? "" : config.systemPrompt);
		box.setValueListener(v -> config.systemPrompt = v);
		addScrolled(box, y + 11);
		y += 11 + 84 + 12;

		y = toggleRowLive(y, "输出净化", "去掉换行、代码块、模型加的名字前缀和引号",
				() -> config.sanitizeOutput, v -> config.sanitizeOutput = v);

		y = toggleRowLive(y, "调试日志", "把完整请求与响应写进 latest.log",
				() -> config.logRequests, v -> config.logRequests = v);

		Button test = Button.builder(Component.literal("测试人设（结果看聊天栏）"), b -> {
			ModConfig.save();
			AutoChatHandler.testConnection();
		}).bounds(left, contentTop, contentWidth, 20).build();
		addScrolled(test, y);
		y += 26;

		addLabel("账号：" + AutoChatHandler.selfName(), y, null);
		y += 12;
		addLabel("接口：" + config.baseUrl, y, null);
		y += 12;
		addLabel("模型：" + config.model + " / " + config.style().displayName(), y, null);
	}

	// ------------------------------------------------------------------ 渲染

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// 背景模糊由 Screen#renderWithTooltipAndSubtitles 统一处理，
		// 这里再调一次 renderBackground 会触发「Can only blur once per frame」。
		graphics.fill(left - 8, top - 14, left + contentWidth + 8, this.height - 10, PANEL_BG);
		graphics.fill(left - 8, top - 14, left + contentWidth + 8, top - 13, ACCENT);

		graphics.drawString(this.font, "AI 自动对话  §7创作者 _mcbjk_", left, top - 11, ACCENT, false);
		String statusText = (config.enabled ? "§a● 运行中" : "§c● 已暂停") + "§7  |  账号 §f" + AutoChatHandler.selfName();
		graphics.drawString(this.font, statusText, left + contentWidth - this.font.width(statusText), top - 11, WHITE, false);

		// 内容区整体裁剪。滚出可视区的控件会被裁掉，
		// 且 AbstractWidget.render 会据此判定 isHovered，不会留下高亮。
		// scissor 只包住内容区，页签栏和底部按钮在外面，不受影响。
		graphics.enableScissor(left - 8, contentTop - 1, left + contentWidth + 8, contentBottom + 1);

		for (Label label : labels) {
			if (label.text().isEmpty()) {
				continue;
			}
			graphics.drawString(this.font, label.text(), label.x(),
					contentTop + label.baseY() - scroll, LABEL, false);
		}
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.disableScissor();

		renderScrollbar(graphics);
		renderHint(graphics, mouseX, mouseY);
	}

	private void renderScrollbar(GuiGraphics graphics) {
		int max = maxScroll();
		if (max <= 0) {
			return;
		}
		int barX = left + contentWidth + 4;
		int height = contentBottom - contentTop;
		int barHeight = Math.max(14, height * height / (height + max));
		int barY = contentTop + (height - barHeight) * scroll / max;
		graphics.fill(barX, contentTop, barX + 2, contentBottom, 0xFF2A2A2A);
		graphics.fill(barX, barY, barX + 2, barY + barHeight, 0xFF808080);
	}

	private void renderHint(GuiGraphics graphics, int mouseX, int mouseY) {
		if (mouseY < contentTop || mouseY > contentBottom) {
			return;
		}
		for (Label label : labels) {
			if (label.hint() == null || label.hint().isBlank()) {
				continue;
			}
			int y = contentTop + label.baseY() - scroll;
			boolean hovered = mouseY >= y - 2 && mouseY <= y + 20;
			if (!hovered) {
				continue;
			}
			List<FormattedCharSequence> lines = this.font.split(Component.literal(label.hint()), Math.min(220, this.width - 30));
			int boxWidth = 0;
			for (FormattedCharSequence line : lines) {
				boxWidth = Math.max(boxWidth, this.font.width(line));
			}
			int boxX = Math.max(4, Math.min(mouseX + 8, this.width - boxWidth - 10));
			int boxY = Math.min(mouseY + 12, this.height - lines.size() * 10 - 10);
			graphics.fill(boxX - 3, boxY - 3, boxX + boxWidth + 3, boxY + lines.size() * 10 + 1, 0xF0100018);
			for (int i = 0; i < lines.size(); i++) {
				graphics.drawString(this.font, lines.get(i), boxX, boxY + i * 10, 0xFFFFE0A0, false);
			}
			return;
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		// 必须先让原版把滚轮事件分发给鼠标下方的子控件。
		// 原版 ContainerEventHandler.mouseScrolled 只会把事件交给 getChildAt() 找到的
		// 那一个控件：若它消费了（返回 true），事件就到此为止，外层页面不该再滚；
		// 若它没消费（例如落在普通输入框上），才轮到页面滚动。
		// 之前这里无条件先滚动页面并返回 true，导致厂商列表永远收不到滚轮。
		if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
			return true;
		}
		if (isOverContent(mouseX, mouseY) && scrollY != 0) {
			setScroll(scroll - (int) Math.signum(scrollY) * 22);
			return true;
		}
		return false;
	}

	/** 鼠标是否落在可滚动的内容区里（把页签栏和底部按钮排除在外）。 */
	private boolean isOverContent(double mouseX, double mouseY) {
		return mouseX >= left && mouseX <= left + contentWidth
				&& mouseY >= contentTop && mouseY <= contentBottom;
	}

	/**
	 * 覆盖子控件命中判定：被滚出内容区的控件不该再接收点击与滚轮。
	 * 原版 AbstractWidget.isMouseOver / getChildAt 都不检查 scissor，
	 * 而 setScroll 只是把控件挪出可视区，它们仍会命中并吞掉事件，
	 * 结果就是鼠标停在某个被滚走的输入框上时，页面再也滚不动。
	 */
	@Override
	public java.util.Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
		java.util.Optional<GuiEventListener> found = super.getChildAt(mouseX, mouseY);
		if (found.isPresent() && scrolledWidgets.contains(found.get()) && !isOverContent(mouseX, mouseY)) {
			return java.util.Optional.empty();
		}
		return found;
	}

	@Override
	public void onClose() {
		ModConfig.save();
		this.minecraft.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
