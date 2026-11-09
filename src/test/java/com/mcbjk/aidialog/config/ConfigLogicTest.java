package com.mcbjk.aidialog.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 触发词解析与厂商预设。 */
class ConfigLogicTest {

	@Test
	@DisplayName("触发词按中英文逗号、分号、竖线、空白切分")
	void splitTriggerWords() {
		ModConfig config = new ModConfig();
		config.triggerKeyword = "小明, 小红，小刚;小强|阿伟 老王";
		config.useAccountIdAsTrigger = false;
		List<String> words = config.triggerWords("Player669");
		assertEquals(List.of("小明", "小红", "小刚", "小强", "阿伟", "老王"), words);
	}

	@Test
	@DisplayName("登录名会自动补进触发词，且不会重复")
	void accountIdAppended() {
		ModConfig config = new ModConfig();
		config.triggerKeyword = "小明";
		config.useAccountIdAsTrigger = true;
		assertEquals(List.of("小明", "Player669"), config.triggerWords("Player669"));

		config.triggerKeyword = "小明, player669";
		List<String> words = config.triggerWords("Player669");
		assertEquals(2, words.size(), "已存在的登录名不应重复添加：" + words);
	}

	@Test
	@DisplayName("关掉开关后不再自动使用登录名")
	void accountIdDisabled() {
		ModConfig config = new ModConfig();
		config.triggerKeyword = "";
		config.useAccountIdAsTrigger = false;
		assertTrue(config.triggerWords("Player669").isEmpty());
	}

	@Test
	@DisplayName("空触发词 + 开启登录名 = 只用登录名")
	void defaultBehaviour() {
		ModConfig config = new ModConfig();
		assertEquals(List.of("Player669"), config.triggerWords("Player669"));
	}

	@Test
	@DisplayName("预设会填充地址、模型与请求格式，但不动 API Key")
	void presetApplication() {
		ModConfig config = new ModConfig();
		config.apiKey = "sk-secret";
		config.applyPreset(ProviderPreset.GEMINI);
		assertEquals("https://generativelanguage.googleapis.com/v1beta", config.baseUrl);
		assertEquals("gemini-2.0-flash", config.model);
		assertEquals(AiStyle.GEMINI, config.style());
		assertEquals("sk-secret", config.apiKey);
	}

	@Test
	@DisplayName("自定义预设不会覆盖用户已填的地址与模型")
	void customPresetKeepsValues() {
		ModConfig config = new ModConfig();
		config.baseUrl = "http://localhost:1234/v1";
		config.model = "local-model";
		config.applyPreset(ProviderPreset.CUSTOM);
		assertEquals("http://localhost:1234/v1", config.baseUrl);
		assertEquals("local-model", config.model);
		assertTrue(config.preset().isCustom());
	}

	@Test
	@DisplayName("所有厂商预设的字段都非空（自定义除外）")
	void presetsWellFormed() {
		for (ProviderPreset preset : ProviderPreset.values()) {
			assertFalse(preset.displayName().isBlank(), preset + " 缺少显示名");
			if (!preset.isCustom()) {
				assertFalse(preset.baseUrl().isBlank(), preset + " 缺少接口地址");
				assertFalse(preset.defaultModel().isBlank(), preset + " 缺少默认模型");
			}
		}
	}

	@Test
	@DisplayName("配置序列化 / 反序列化往返不丢字段")
	void jsonRoundTrip() {
		Gson gson = new GsonBuilder().create();
		ModConfig config = new ModConfig();
		config.baseUrl = "http://example.com/v1";
		config.model = "test-model";
		config.apiKey = "sk-abc";
		config.triggerKeyword = "小明";
		config.cooldownSeconds = 42;
		config.systemPrompt = "你是 {player}";

		String json = gson.toJson(config);
		ModConfig back = gson.fromJson(json, ModConfig.class);

		assertEquals(config.baseUrl, back.baseUrl);
		assertEquals(config.model, back.model);
		assertEquals(config.apiKey, back.apiKey);
		assertEquals(config.triggerKeyword, back.triggerKeyword);
		assertEquals(42, back.cooldownSeconds);
		assertEquals("你是 {player}", back.systemPrompt);
	}

	@Test
	@DisplayName("中文在没有 escapeHtml 时保持可读（UTF-8 直存）")
	void jsonKeepsChineseReadable() {
		Gson gson = new GsonBuilder().disableHtmlEscaping().create();
		ModConfig config = new ModConfig();
		config.systemPrompt = "你是玩家 {player}";
		String json = gson.toJson(config);
		assertTrue(json.contains("你是玩家"), json);
	}

	@Test
	@DisplayName("枚举按名称解析，未知值有安全兜底")
	void enumLookup() {
		assertEquals(AiStyle.GEMINI, AiStyle.byName("gemini"));
		assertEquals(AiStyle.OPENAI, AiStyle.byName("no-such-style"));
		assertEquals(AiStyle.OPENAI, AiStyle.byName(null));
		assertEquals(ProviderPreset.DEEPSEEK, ProviderPreset.byName("deepseek"));
		assertEquals(ProviderPreset.CUSTOM, ProviderPreset.byName("no-such-provider"));
		assertEquals(ProviderPreset.CUSTOM, ProviderPreset.byName(null));
	}

	@Test
	@DisplayName("循环切换覆盖所有取值且能回到起点（GUI 左键循环用）")
	void cyclingWrapsAround() {
		ProviderPreset preset = ProviderPreset.values()[0];
		for (int i = 0; i < ProviderPreset.values().length; i++) {
			preset = preset.next();
		}
		assertEquals(ProviderPreset.values()[0], preset);

		AiStyle style = AiStyle.values()[0];
		for (int i = 0; i < AiStyle.values().length; i++) {
			style = style.next();
		}
		assertEquals(AiStyle.values()[0], style);
	}
}
