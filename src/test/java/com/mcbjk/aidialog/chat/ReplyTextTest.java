package com.mcbjk.aidialog.chat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 回复文本清理与聊天栏长度约束。 */
class ReplyTextTest {

	@Test
	@DisplayName("换行会被压成空格，因为聊天栏发不出多行")
	void newlinesCollapse() {
		assertEquals("你好 我是小明", ReplyText.sanitize("你好\n我是小明"));
		assertEquals("a b", ReplyText.sanitize("a\r\nb"));
		assertEquals("a b c", ReplyText.sanitize("a  \n\n  b\nc"));
	}

	@Test
	@DisplayName("代码块围栏被去掉")
	void codeFence() {
		assertEquals("hello world", ReplyText.sanitize("```json hello world ```"));
	}

	@Test
	@DisplayName("模型加的玩家名前缀被去掉")
	void namePrefix() {
		assertEquals("我在挖矿", ReplyText.sanitize("Player669：我在挖矿"));
		assertEquals("我在挖矿", ReplyText.sanitize("Player669: 我在挖矿"));
		assertEquals("我在挖矿", ReplyText.sanitize("[Player669] 我在挖矿"));
		assertEquals("我在挖矿", ReplyText.sanitize("player_669: 我在挖矿"));
	}

	@Test
	@DisplayName("只剥离 ASCII 名字前缀：中文「注意：」这类正文必须保留")
	void chinesePrefixIsNotStripped() {
		// Minecraft 玩家名只可能是 ASCII（字母/数字/下划线，最长 16）。
		// 如果连中文前缀一起剥离，「注意：小心苦力怕」会被啃掉开头，得不偿失。
		assertEquals("注意：小心苦力怕", ReplyText.sanitize("注意：小心苦力怕"));
		assertEquals("提示: 前面有岩浆", ReplyText.sanitize("提示: 前面有岩浆"));
		// 但 [中文] 这种方括号包裹的形式仍然会被清掉，因为它明显是标签而非正文
		assertEquals("我在挖矿", ReplyText.sanitize("[系统] 我在挖矿"));
	}

	@Test
	@DisplayName("包裹引号被去掉，句内引号保留")
	void quotes() {
		assertEquals("我在挖矿", ReplyText.sanitize("\"我在挖矿\""));
		assertEquals("我在挖矿", ReplyText.sanitize("“我在挖矿”"));
		assertEquals("我在挖矿", ReplyText.sanitize("「我在挖矿」"));
		assertEquals("他说\"好\"", ReplyText.sanitize("他说\"好\""));
	}

	@Test
	@DisplayName("截断不超过配置值")
	void truncateToConfigured() {
		String longText = "字".repeat(500);
		assertEquals(64, ReplyText.truncate(longText, 64).length());
		assertEquals(100, ReplyText.truncate("a".repeat(100), 200).length());
	}

	@Test
	@DisplayName("配置值再大也不会突破聊天栏硬上限 256")
	void truncateNeverExceedsHardLimit() {
		String longText = "字".repeat(1000);
		assertEquals(ReplyText.CHAT_HARD_LIMIT, ReplyText.truncate(longText, 9999).length());
		assertEquals(ReplyText.CHAT_HARD_LIMIT, ReplyText.truncate(longText, ReplyText.CHAT_HARD_LIMIT).length());
		assertTrue(ReplyText.truncate(longText, 9999).length() <= 256);
	}

	@Test
	@DisplayName("硬上限就是 256")
	void hardLimitValue() {
		assertEquals(256, ReplyText.CHAT_HARD_LIMIT);
	}

	@Test
	@DisplayName("清理与截断的组合：超长回复最终一定塞得进聊天栏")
	void sanitizeThenTruncate() {
		String messy = "Player669: " + "这是一段很长的回复。".repeat(60) + "\n结尾";
		String cleaned = ReplyText.sanitize(messy);
		String finalText = ReplyText.truncate(cleaned, 256);
		assertTrue(finalText.length() <= 256, "最终长度 " + finalText.length());
		assertTrue(!finalText.contains("\n"));
		assertTrue(!finalText.startsWith("Player669"));
	}

	@Test
	@DisplayName("空值安全")
	void nullSafe() {
		assertEquals("", ReplyText.sanitize(null));
		assertEquals("", ReplyText.truncate(null, 100));
	}
}
