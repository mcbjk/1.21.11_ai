package com.mcbjk.aidialog.chat;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 触发词匹配。刻意不依赖 Minecraft / Fabric 的任何类型，
 * 这样匹配规则可以被单元测试直接覆盖。
 */
public final class TriggerMatcher {
	private TriggerMatcher() {
	}

	/**
	 * @param text          聊天栏里收到的原话
	 * @param word          触发词
	 * @param ignoreCase    是否忽略大小写
	 * @param wholeWordOnly 英文/数字触发词是否要求整词匹配
	 */
	public static boolean matches(String text, String word, boolean ignoreCase, boolean wholeWordOnly) {
		if (text == null || word == null || word.isEmpty()) {
			return false;
		}
		String haystack = ignoreCase ? text.toLowerCase(Locale.ROOT) : text;
		String needle = ignoreCase ? word.toLowerCase(Locale.ROOT) : word;

		if (!wholeWordOnly || !isAsciiWord(needle)) {
			// 中文没有词边界概念，一律用子串匹配
			return haystack.contains(needle);
		}
		return Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(needle) + "(?![A-Za-z0-9_])")
				.matcher(haystack)
				.find();
	}

	/** 只有纯 ASCII 字母/数字/下划线才谈得上「整词」。 */
	public static boolean isAsciiWord(String value) {
		if (value == null || value.isEmpty()) {
			return false;
		}
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_';
			if (!ok) {
				return false;
			}
		}
		return true;
	}
}
