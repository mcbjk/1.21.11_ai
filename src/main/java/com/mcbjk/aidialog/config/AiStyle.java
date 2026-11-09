package com.mcbjk.aidialog.config;

/**
 * 请求体风格：不同的 AI 厂商使用不同的 HTTP 请求结构。
 */
public enum AiStyle {
	/** OpenAI 兼容格式，绝大多数国内厂商与本地模型（Ollama / vLLM / LM Studio）都走这个。 */
	OPENAI("OpenAI 兼容"),
	/** Google Gemini 原生格式。 */
	GEMINI("Gemini"),
	/** Anthropic Claude 原生格式。 */
	ANTHROPIC("Claude");

	private final String displayName;

	AiStyle(String displayName) {
		this.displayName = displayName;
	}

	public String displayName() {
		return displayName;
	}

	public AiStyle next() {
		AiStyle[] values = values();
		return values[(ordinal() + 1) % values.length];
	}

	public static AiStyle byName(String name) {
		for (AiStyle style : values()) {
			if (style.name().equalsIgnoreCase(name)) {
				return style;
			}
		}
		return OPENAI;
	}
}
