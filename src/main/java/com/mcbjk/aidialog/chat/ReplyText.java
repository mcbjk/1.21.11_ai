package com.mcbjk.aidialog.chat;

/**
 * 回复文本的清理与长度约束。同样不依赖 Minecraft，便于测试。
 */
public final class ReplyText {
	/** Minecraft 聊天栏硬上限（SharedConstants.MAX_CHAT_LENGTH）。 */
	public static final int CHAT_HARD_LIMIT = 256;

	private ReplyText() {
	}

	/**
	 * 去掉模型输出里不该进聊天栏的东西：换行、代码块、名字前缀、包裹引号。
	 */
	public static String sanitize(String raw) {
		if (raw == null) {
			return "";
		}
		String text = raw.replace("\r", " ").replace("\n", " ");
		text = text.replaceAll("```[a-zA-Z]*", " ");
		// 模型偶尔会自作主张加前缀："Steve：" / "[Steve]" / "AI:"
		text = text.replaceAll("^\\s*\\[[^\\]]{1,20}]\\s*", " ");
		text = text.replaceAll("^\\s*[A-Za-z0-9_]{2,16}\\s*[：:]\\s*", " ");
		text = text.replaceAll("\\s+", " ").trim();
		if (text.length() >= 2) {
			char first = text.charAt(0);
			char last = text.charAt(text.length() - 1);
			if ((first == '"' && last == '"')
					|| (first == '“' && last == '”')
					|| (first == '「' && last == '」')
					|| (first == '\'' && last == '\'')) {
				text = text.substring(1, text.length() - 1).trim();
			}
		}
		return text;
	}

	/**
	 * 按字符数截断。上限同时受配置值与聊天栏硬上限约束。
	 */
	public static String truncate(String text, int configuredLimit) {
		if (text == null) {
			return "";
		}
		int limit = Math.max(1, Math.min(configuredLimit, CHAT_HARD_LIMIT));
		return text.length() <= limit ? text : text.substring(0, limit).trim();
	}
}
