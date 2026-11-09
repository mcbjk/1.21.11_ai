package com.mcbjk.aidialog.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcbjk.aidialog.config.AiStyle;
import com.mcbjk.aidialog.config.ModConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 三种请求体风格的结构，以及三种响应的解析。
 * 这些是纯 JSON 逻辑，不需要联网。
 */
class AiPayloadTest {

	private static ModConfig baseConfig(AiStyle style) {
		ModConfig config = new ModConfig();
		config.apiStyle = style.name();
		config.model = "test-model";
		config.temperature = 0.7;
		config.maxTokens = 128;
		return config;
	}

	private static final List<ChatMessage> MESSAGES = List.of(
			ChatMessage.system("你是 {player}"),
			ChatMessage.user("Player669 在吗")
	);

	@Test
	@DisplayName("OpenAI 风格：messages 数组 + model + stream=false")
	void openAiBody() {
		JsonObject root = JsonParser.parseString(AiPayload.buildBody(baseConfig(AiStyle.OPENAI), MESSAGES)).getAsJsonObject();
		assertEquals("test-model", root.get("model").getAsString());
		assertFalse(root.get("stream").getAsBoolean());
		assertEquals(128, root.get("max_tokens").getAsInt());
		JsonArray messages = root.getAsJsonArray("messages");
		assertEquals(2, messages.size());
		assertEquals("system", messages.get(0).getAsJsonObject().get("role").getAsString());
		assertEquals("user", messages.get(1).getAsJsonObject().get("role").getAsString());
		assertEquals("Player669 在吗", messages.get(1).getAsJsonObject().get("content").getAsString());
	}

	@Test
	@DisplayName("Gemini 风格：system 提到 system_instruction，角色用 model")
	void geminiBody() {
		JsonObject root = JsonParser.parseString(AiPayload.buildBody(baseConfig(AiStyle.GEMINI), MESSAGES)).getAsJsonObject();
		assertEquals("你是 {player}", root.getAsJsonObject("system_instruction")
				.getAsJsonArray("parts").get(0).getAsJsonObject().get("text").getAsString());
		JsonArray contents = root.getAsJsonArray("contents");
		assertEquals(1, contents.size(), "system 不该出现在 contents 里");
		assertEquals("user", contents.get(0).getAsJsonObject().get("role").getAsString());
		assertEquals(128, root.getAsJsonObject("generationConfig").get("maxOutputTokens").getAsInt());
	}

	@Test
	@DisplayName("Claude 风格：system 是顶层字符串，messages 只有 user/assistant")
	void claudeBody() {
		JsonObject root = JsonParser.parseString(AiPayload.buildBody(baseConfig(AiStyle.ANTHROPIC), MESSAGES)).getAsJsonObject();
		assertEquals("你是 {player}", root.get("system").getAsString());
		assertEquals(128, root.get("max_tokens").getAsInt());
		JsonArray messages = root.getAsJsonArray("messages");
		assertEquals(1, messages.size());
		assertEquals("user", messages.get(0).getAsJsonObject().get("role").getAsString());
	}

	@Test
	@DisplayName("端点是按风格拼出来的")
	void endpoints() {
		assertEquals("https://api.deepseek.com/v1/chat/completions",
				AiPayload.endpoint("https://api.deepseek.com/v1/", AiStyle.OPENAI, "m"));
		assertEquals("https://api.anthropic.com/v1/messages",
				AiPayload.endpoint("https://api.anthropic.com/v1", AiStyle.ANTHROPIC, "m"));
		assertEquals("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent",
				AiPayload.endpoint("https://generativelanguage.googleapis.com/v1beta", AiStyle.GEMINI, "gemini-2.0-flash"));
	}

	@Test
	@DisplayName("用户直接填完整端点时不再重复拼接")
	void endpointNotDoubleJoined() {
		assertEquals("https://x/v1/chat/completions",
				AiPayload.endpoint("https://x/v1/chat/completions", AiStyle.OPENAI, "m"));
		assertEquals("https://x/v1/messages",
				AiPayload.endpoint("https://x/v1/messages", AiStyle.ANTHROPIC, "m"));
	}

	@Test
	@DisplayName("解析 OpenAI 响应")
	void parseOpenAi() {
		String body = """
				{"choices":[{"index":0,"message":{"role":"assistant","content":"我在挖矿"},"finish_reason":"stop"}]}
				""";
		assertEquals("我在挖矿", AiPayload.parse(AiStyle.OPENAI, body));
	}

	@Test
	@DisplayName("解析 Gemini 响应（多个 part 要拼起来）")
	void parseGemini() {
		String body = """
				{"candidates":[{"content":{"role":"model","parts":[{"text":"我在"},{"text":"挖矿"}]}}]}
				""";
		assertEquals("我在挖矿", AiPayload.parse(AiStyle.GEMINI, body));
	}

	@Test
	@DisplayName("解析 Claude 响应")
	void parseClaude() {
		String body = """
				{"content":[{"type":"text","text":"我在挖矿"}],"stop_reason":"end_turn"}
				""";
		assertEquals("我在挖矿", AiPayload.parse(AiStyle.ANTHROPIC, body));
	}

	@Test
	@DisplayName("从各家错误响应里抽出可读原因")
	void parseError() {
		assertEquals("Invalid API key",
				AiPayload.extractError("{\"error\":{\"message\":\"Invalid API key\"}}"));
		assertEquals("quota exceeded",
				AiPayload.extractError("{\"error\":\"quota exceeded\"}"));
		assertEquals("bad request",
				AiPayload.extractError("{\"message\":\"bad request\"}"));
	}

	@Test
	@DisplayName("非 JSON 的错误响应退化成截断原文，不抛异常")
	void parseErrorFallback() {
		String html = "<html><body>502 Bad Gateway</body></html>";
		String result = AiPayload.extractError(html);
		assertTrue(result.contains("502") || result.contains("html"));
		assertTrue(result.length() <= 210);
	}

	@Test
	@DisplayName("响应结构缺失时返回 null 而不是抛异常")
	void parseMissingFields() {
		assertTrue(AiPayload.parse(AiStyle.OPENAI, "{}") == null);
		assertTrue(AiPayload.parse(AiStyle.GEMINI, "{}") == null);
		assertTrue(AiPayload.parse(AiStyle.ANTHROPIC, "{}") == null);
	}
}
