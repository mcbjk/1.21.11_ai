package com.mcbjk.aidialog.chat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 触发词匹配规则。对应需求：玩家在聊天栏提到登录 ID 才触发。 */
class TriggerMatcherTest {

	@Test
	@DisplayName("英文 ID 整词匹配，不会命中更长的词")
	void wholeWordOnly() {
		assertTrue(TriggerMatcher.matches("hey Player669 come here", "Player669", true, true));
		assertTrue(TriggerMatcher.matches("Player669", "Player669", true, true));
		assertTrue(TriggerMatcher.matches("Player669, 在吗", "Player669", true, true));
		assertFalse(TriggerMatcher.matches("Player6690 is here", "Player669", true, true));
		assertFalse(TriggerMatcher.matches("xPlayer669", "Player669", true, true));
		assertFalse(TriggerMatcher.matches("Player669x", "Player669", true, true));
	}

	@Test
	@DisplayName("下划线算词内字符，_Player669_ 不算独立出现")
	void underscoreIsWordChar() {
		assertFalse(TriggerMatcher.matches("_Player669_", "Player669", true, true));
		assertTrue(TriggerMatcher.matches("_Player669_", "Player669", true, false));
	}

	@Test
	@DisplayName("关闭整词匹配后退化成子串匹配")
	void substringMode() {
		assertTrue(TriggerMatcher.matches("Player6690", "Player669", true, false));
		assertTrue(TriggerMatcher.matches("xPlayer669y", "Player669", true, false));
	}

	@Test
	@DisplayName("大小写按开关处理")
	void caseSensitivity() {
		assertTrue(TriggerMatcher.matches("hey PLAYER669", "player669", true, true));
		assertFalse(TriggerMatcher.matches("hey PLAYER669", "player669", false, true));
	}

	@Test
	@DisplayName("中文触发词走子串匹配，不受整词开关影响")
	void chineseTrigger() {
		assertTrue(TriggerMatcher.matches("小明你在干嘛", "小明", true, true));
		assertTrue(TriggerMatcher.matches("红石大师在吗", "红石", true, true));
		assertFalse(TriggerMatcher.matches("小红在吗", "小明", true, true));
	}

	@Test
	@DisplayName("空输入一律不触发")
	void blanks() {
		assertFalse(TriggerMatcher.matches("", "Player669", true, true));
		assertFalse(TriggerMatcher.matches("hello", "", true, true));
		assertFalse(TriggerMatcher.matches(null, "Player669", true, true));
		assertFalse(TriggerMatcher.matches("hello", null, true, true));
	}

	@Test
	@DisplayName("正则元字符不会破坏匹配")
	void regexMetaChars() {
		assertTrue(TriggerMatcher.matches("hi a.b", "a.b", true, false));
		assertTrue(TriggerMatcher.matches("hi (test)", "(test)", true, false));
		// . 在整词模式下不是 ASCII 词字符，因此走子串分支
		assertTrue(TriggerMatcher.matches("say a.b now", "a.b", true, true));
		// 纯 ASCII 词里的特殊字符不会被当成通配
		assertFalse(TriggerMatcher.matches("axb", "a.b", true, false));
	}

	@Test
	@DisplayName("isAsciiWord 判定")
	void asciiWord() {
		assertTrue(TriggerMatcher.isAsciiWord("Player669"));
		assertTrue(TriggerMatcher.isAsciiWord("a_b"));
		assertFalse(TriggerMatcher.isAsciiWord("小明"));
		assertFalse(TriggerMatcher.isAsciiWord("a b"));
		assertFalse(TriggerMatcher.isAsciiWord("a-b"));
		assertFalse(TriggerMatcher.isAsciiWord(""));
	}
}
