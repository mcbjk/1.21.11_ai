package com.mcbjk.aidialog.config;

/**
 * 内置厂商预设。GUI 里循环切换这些项，选中后自动填充接口地址与默认模型，
 * 所有字段依旧可以手动改，最后一项「自定义」用于接入任意自建服务。
 */
public enum ProviderPreset {
	OPENAI("OpenAI", "https://api.openai.com/v1", "gpt-4o-mini", AiStyle.OPENAI),
	DEEPSEEK("DeepSeek 深度求索", "https://api.deepseek.com/v1", "deepseek-chat", AiStyle.OPENAI),
	MOONSHOT("Kimi 月之暗面", "https://api.moonshot.cn/v1", "moonshot-v1-8k", AiStyle.OPENAI),
	ZHIPU("智谱 GLM", "https://open.bigmodel.cn/api/paas/v4", "glm-4-flash", AiStyle.OPENAI),
	DASHSCOPE("通义千问 DashScope", "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-plus", AiStyle.OPENAI),
	SILICONFLOW("硅基流动 SiliconFlow", "https://api.siliconflow.cn/v1", "Qwen/Qwen2.5-7B-Instruct", AiStyle.OPENAI),
	QIANFAN("百度文心一言", "https://qianfan.baidubce.com/v2", "ernie-4.5-turbo-128k", AiStyle.OPENAI),
	MINIMAX("MiniMax", "https://api.minimax.chat/v1", "abab6.5s-chat", AiStyle.OPENAI),
	STEPFUN("阶跃星辰 StepFun", "https://api.stepfun.com/v1", "step-2-mini", AiStyle.OPENAI),
	GEMINI("Google Gemini", "https://generativelanguage.googleapis.com/v1beta", "gemini-2.0-flash", AiStyle.GEMINI),
	CLAUDE("Anthropic Claude", "https://api.anthropic.com/v1", "claude-sonnet-4-20250514", AiStyle.ANTHROPIC),
	OLLAMA("Ollama 本地模型", "http://localhost:11434/v1", "llama3.1", AiStyle.OPENAI),
	LMSTUDIO("LM Studio 本地", "http://localhost:1234/v1", "local-model", AiStyle.OPENAI),
	CUSTOM("自定义接口", "", "", AiStyle.OPENAI);

	private final String displayName;
	private final String baseUrl;
	private final String defaultModel;
	private final AiStyle style;

	ProviderPreset(String displayName, String baseUrl, String defaultModel, AiStyle style) {
		this.displayName = displayName;
		this.baseUrl = baseUrl;
		this.defaultModel = defaultModel;
		this.style = style;
	}

	public String displayName() {
		return displayName;
	}

	public String baseUrl() {
		return baseUrl;
	}

	public String defaultModel() {
		return defaultModel;
	}

	public AiStyle style() {
		return style;
	}

	public boolean isCustom() {
		return this == CUSTOM;
	}

	public ProviderPreset next() {
		ProviderPreset[] values = values();
		return values[(ordinal() + 1) % values.length];
	}

	public static ProviderPreset byName(String name) {
		if (name != null) {
			for (ProviderPreset preset : values()) {
				if (preset.name().equalsIgnoreCase(name)) {
					return preset;
				}
			}
		}
		return CUSTOM;
	}
}
