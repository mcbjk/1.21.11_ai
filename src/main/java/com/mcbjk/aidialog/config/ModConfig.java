package com.mcbjk.aidialog.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mcbjk.aidialog.AiDialogMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 全局配置，落地到 config/aidialog.json。
 */
public class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private static ModConfig INSTANCE;

	// ---- 总开关 ----
	/** 模组总开关，关闭后完全不处理聊天。 */
	public boolean enabled = true;

	// ---- 触发条件 ----
	/** 用于匹配的关键字。留空则自动使用当前登录账号的 ID。 */
	public String triggerKeyword = "";
	/** 是否自动使用登录账号 ID 作为额外触发词。 */
	public boolean useAccountIdAsTrigger = true;
	/** 是否忽略大小写匹配。 */
	public boolean ignoreCase = true;
	/** 触发词必须作为独立单词出现（避免 "Steve" 命中 "Stevecraft"）。 */
	public boolean wholeWordOnly = true;
	/** 同一个玩家触发的最小冷却（秒），避免刷屏。 */
	public int cooldownSeconds = 15;
	/** 触发后最多等待多久再回复（毫秒区间，制造真人打字感）。 */
	public int replyDelayMinMs = 1200;
	public int replyDelayMaxMs = 4000;
	// ---- AI 接口 ----
	public String provider = ProviderPreset.DEEPSEEK.name();
	public String apiStyle = AiStyle.OPENAI.name();
	public String baseUrl = "https://api.deepseek.com/v1";
	public String apiKey = "";
	public String model = "deepseek-chat";
	public double temperature = 0.8;
	public int maxTokens = 120;
	/** 请求超时（秒）。 */
	public int timeoutSeconds = 30;

	// ---- 人设 ----
	public String systemPrompt = "你是一个正在玩 Minecraft 的玩家，名字叫 {player}。"
			+ "你在游戏里随口聊天，说话简短、口语化、有点幽默，不用书面语，不解释自己是 AI。"
			+ "绝对不要使用换行，不要加任何前缀。";
	/** 是否把最近的聊天记录作为上下文一起发给模型。 */
	public boolean sendContext = true;
	public int contextMessageCount = 8;
	/** 回复长度上限（字符），不得超过 Minecraft 聊天栏上限。 */
	public int maxReplyLength = 256;
	/** 发送前是否做一次本地文本清理。 */
	public boolean sanitizeOutput = true;

	// ---- 运行时 ----
	public boolean logRequests = false;

	/**
	 * 配置文件路径。刻意做成懒加载：静态字段初始化会强制加载 FabricLoader，
	 * 而纯逻辑单元测试不该依赖 Fabric 运行时。
	 */
	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("aidialog.json");
	}

	public static ModConfig get() {
		if (INSTANCE == null) {
			load();
		}
		return INSTANCE;
	}

	public static void load() {
		ModConfig config = new ModConfig();
		Path path = path();
		if (Files.exists(path)) {
			try {
				String json = Files.readString(path, StandardCharsets.UTF_8);
				ModConfig loaded = GSON.fromJson(json, ModConfig.class);
				if (loaded != null) {
					config = loaded;
				}
			} catch (Exception e) {
				AiDialogMod.LOGGER.error("读取配置失败，使用默认配置", e);
			}
		}
		config.clamp();
		INSTANCE = config;
		save();
	}

	public static void save() {
		if (INSTANCE == null) {
			return;
		}
		try {
			Path path = path();
			Files.createDirectories(path.getParent());
			Files.writeString(path, GSON.toJson(INSTANCE), StandardCharsets.UTF_8);
		} catch (IOException e) {
			AiDialogMod.LOGGER.error("保存配置失败", e);
		}
	}

	private void clamp() {
		if (apiStyle == null) {
			apiStyle = AiStyle.OPENAI.name();
		}
		if (provider == null) {
			provider = ProviderPreset.CUSTOM.name();
		}
		if (baseUrl == null) {
			baseUrl = "";
		}
		if (apiKey == null) {
			apiKey = "";
		}
		if (model == null) {
			model = "";
		}
		if (triggerKeyword == null) {
			triggerKeyword = "";
		}
		if (systemPrompt == null) {
			systemPrompt = "";
		}
		cooldownSeconds = Math.max(0, Math.min(cooldownSeconds, 3600));
		replyDelayMinMs = Math.max(0, Math.min(replyDelayMinMs, 60000));
		replyDelayMaxMs = Math.max(replyDelayMinMs, Math.min(replyDelayMaxMs, 60000));
		maxTokens = Math.max(16, Math.min(maxTokens, 1024));
		maxReplyLength = Math.max(16, Math.min(maxReplyLength, 256));
		contextMessageCount = Math.max(0, Math.min(contextMessageCount, 50));
		timeoutSeconds = Math.max(5, Math.min(timeoutSeconds, 180));
		temperature = Math.max(0.0, Math.min(temperature, 2.0));
	}

	public ProviderPreset preset() {
		return ProviderPreset.byName(provider);
	}

	public AiStyle style() {
		return AiStyle.byName(apiStyle);
	}

	/** 应用某个厂商预设：填充地址、模型与请求风格，不动 API Key。 */
	public void applyPreset(ProviderPreset preset) {
		provider = preset.name();
		apiStyle = preset.style().name();
		if (!preset.isCustom()) {
			baseUrl = preset.baseUrl();
			model = preset.defaultModel();
		}
	}

	/** 当前实际用于匹配的触发词列表（已去重、去空）。 */
	public java.util.List<String> triggerWords(String loggedInName) {
		java.util.List<String> words = new java.util.ArrayList<>();
		if (triggerKeyword != null && !triggerKeyword.isBlank()) {
			for (String part : triggerKeyword.split("[,，;；|\\s]+")) {
				if (!part.isBlank()) {
					words.add(part.trim());
				}
			}
		}
		if (useAccountIdAsTrigger && loggedInName != null && !loggedInName.isBlank()) {
			boolean exists = words.stream().anyMatch(w -> w.equalsIgnoreCase(loggedInName));
			if (!exists) {
				words.add(loggedInName);
			}
		}
		return words;
	}
}
