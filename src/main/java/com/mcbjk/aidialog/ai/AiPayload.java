package com.mcbjk.aidialog.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcbjk.aidialog.config.AiStyle;
import com.mcbjk.aidialog.config.ModConfig;

import java.util.List;

/**
 * 请求体构造与响应解析。全部是纯 JSON 逻辑，不碰网络也不碰 Minecraft，
 * 因此可以被单元测试完整覆盖。
 */
public final class AiPayload {
	private AiPayload() {
	}

	/** 按风格拼出最终请求地址。用户若已填完整端点则原样使用。 */
	public static String endpoint(String baseUrl, AiStyle style, String model) {
		String base = baseUrl == null ? "" : baseUrl.trim();
		if (base.contains(":generateContent") || base.endsWith("/chat/completions") || base.endsWith("/messages")) {
			return base;
		}
		while (base.endsWith("/")) {
			base = base.substring(0, base.length() - 1);
		}
		return switch (style) {
			case GEMINI -> base + "/models/" + model + ":generateContent";
			case ANTHROPIC -> base + "/messages";
			default -> base + "/chat/completions";
		};
	}

	public static String buildBody(ModConfig config, List<ChatMessage> messages) {
		return switch (config.style()) {
			case GEMINI -> buildGeminiBody(config, messages);
			case ANTHROPIC -> buildClaudeBody(config, messages);
			default -> buildOpenAiBody(config, messages);
		};
	}

	public static String buildOpenAiBody(ModConfig config, List<ChatMessage> messages) {
		JsonObject root = new JsonObject();
		root.addProperty("model", config.model);
		JsonArray array = new JsonArray();
		for (ChatMessage message : messages) {
			JsonObject entry = new JsonObject();
			entry.addProperty("role", message.role());
			entry.addProperty("content", message.content());
			array.add(entry);
		}
		root.add("messages", array);
		root.addProperty("temperature", config.temperature);
		root.addProperty("max_tokens", config.maxTokens);
		root.addProperty("stream", false);
		return root.toString();
	}

	public static String buildGeminiBody(ModConfig config, List<ChatMessage> messages) {
		JsonObject root = new JsonObject();

		String system = null;
		JsonArray contents = new JsonArray();
		for (ChatMessage message : messages) {
			if ("system".equals(message.role())) {
				system = message.content();
				continue;
			}
			JsonObject entry = new JsonObject();
			entry.addProperty("role", "assistant".equals(message.role()) ? "model" : "user");
			JsonArray parts = new JsonArray();
			JsonObject part = new JsonObject();
			part.addProperty("text", message.content());
			parts.add(part);
			entry.add("parts", parts);
			contents.add(entry);
		}
		root.add("contents", contents);

		if (system != null && !system.isBlank()) {
			JsonObject instruction = new JsonObject();
			JsonArray parts = new JsonArray();
			JsonObject part = new JsonObject();
			part.addProperty("text", system);
			parts.add(part);
			instruction.add("parts", parts);
			root.add("system_instruction", instruction);
		}

		JsonObject generation = new JsonObject();
		generation.addProperty("temperature", config.temperature);
		generation.addProperty("maxOutputTokens", config.maxTokens);
		root.add("generationConfig", generation);
		return root.toString();
	}

	public static String buildClaudeBody(ModConfig config, List<ChatMessage> messages) {
		JsonObject root = new JsonObject();
		root.addProperty("model", config.model);
		root.addProperty("max_tokens", config.maxTokens);
		root.addProperty("temperature", config.temperature);

		StringBuilder system = new StringBuilder();
		JsonArray array = new JsonArray();
		for (ChatMessage message : messages) {
			if ("system".equals(message.role())) {
				if (!system.isEmpty()) {
					system.append('\n');
				}
				system.append(message.content());
				continue;
			}
			JsonObject entry = new JsonObject();
			entry.addProperty("role", "assistant".equals(message.role()) ? "assistant" : "user");
			entry.addProperty("content", message.content());
			array.add(entry);
		}
		if (!system.isEmpty()) {
			root.addProperty("system", system.toString());
		}
		root.add("messages", array);
		return root.toString();
	}

	/** 按风格解析回复文本；结构不符时返回 null（由调用方报错）。 */
	public static String parse(AiStyle style, String raw) {
		return switch (style) {
			case GEMINI -> parseGemini(raw);
			case ANTHROPIC -> parseClaude(raw);
			default -> parseOpenAi(raw);
		};
	}

	private static String parseOpenAi(String raw) {
		JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
		JsonArray choices = root.getAsJsonArray("choices");
		if (choices == null || choices.isEmpty()) {
			return null;
		}
		JsonObject choice = choices.get(0).getAsJsonObject();
		JsonObject message = choice.getAsJsonObject("message");
		if (message != null && message.has("content") && !message.get("content").isJsonNull()) {
			return message.get("content").getAsString();
		}
		if (choice.has("text") && !choice.get("text").isJsonNull()) {
			return choice.get("text").getAsString();
		}
		return null;
	}

	private static String parseGemini(String raw) {
		JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
		JsonArray candidates = root.getAsJsonArray("candidates");
		if (candidates == null || candidates.isEmpty()) {
			return null;
		}
		JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
		if (content == null) {
			return null;
		}
		JsonArray parts = content.getAsJsonArray("parts");
		if (parts == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for (JsonElement element : parts) {
			JsonObject part = element.getAsJsonObject();
			if (part.has("text")) {
				sb.append(part.get("text").getAsString());
			}
		}
		return sb.toString();
	}

	private static String parseClaude(String raw) {
		JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
		JsonArray content = root.getAsJsonArray("content");
		if (content == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for (JsonElement element : content) {
			JsonObject block = element.getAsJsonObject();
			if (block.has("text")) {
				sb.append(block.get("text").getAsString());
			}
		}
		return sb.toString();
	}

	/** 从各家错误响应里抽出可读原因。 */
	public static String extractError(String raw) {
		if (raw == null) {
			return "";
		}
		try {
			JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
			if (root.has("error")) {
				JsonElement error = root.get("error");
				if (error.isJsonObject() && error.getAsJsonObject().has("message")) {
					return error.getAsJsonObject().get("message").getAsString();
				}
				if (error.isJsonPrimitive()) {
					return error.getAsString();
				}
			}
			if (root.has("message")) {
				return root.get("message").getAsString();
			}
		} catch (Exception ignored) {
			// 不是 JSON，退化成截断原文
		}
		return truncate(raw, 200);
	}

	public static String truncate(String text, int max) {
		if (text == null) {
			return "";
		}
		return text.length() <= max ? text : text.substring(0, max) + "…";
	}
}
