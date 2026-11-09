package com.mcbjk.aidialog.ai;

import com.mcbjk.aidialog.AiDialogMod;
import com.mcbjk.aidialog.config.AiStyle;
import com.mcbjk.aidialog.config.ModConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 极简 AI 客户端。只用 JDK 自带的 java.net.http，不引入额外依赖，也不会阻塞游戏线程。
 * 支持三种请求体风格：OpenAI 兼容 / Gemini 原生 / Claude 原生。
 * 请求体构造与响应解析都在 {@link AiPayload} 里，这里只负责传输。
 */
public final class AiClient {
	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "AI-Dialog-Worker");
		t.setDaemon(true);
		return t;
	});

	private static final HttpClient HTTP = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(15))
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();

	private static final AtomicBoolean BUSY = new AtomicBoolean(false);

	private AiClient() {
	}

	public interface Callback {
		/** 成功时回调。运行在游戏主线程。 */
		void onSuccess(String text);

		/** 失败时回调，message 是给玩家看的中文原因。运行在游戏主线程。 */
		void onFailure(String message);
	}

	/** 发起一次对话请求。立刻返回，结果通过 callback 异步交回。 */
	public static void request(List<ChatMessage> messages, Callback callback) {
		if (!BUSY.compareAndSet(false, true)) {
			callback.onFailure("上一次的 AI 请求还没结束");
			return;
		}
		ModConfig cfg = ModConfig.get();
		EXECUTOR.submit(() -> {
			try {
				String text = call(cfg, messages);
				deliver(() -> callback.onSuccess(text));
			} catch (Exception e) {
				String msg = describe(e);
				AiDialogMod.LOGGER.warn("AI 请求失败: {}", msg, e);
				deliver(() -> callback.onFailure(msg));
			} finally {
				BUSY.set(false);
			}
		});
	}

	/** 把回调切回客户端主线程。 */
	private static void deliver(Runnable runnable) {
		net.minecraft.client.Minecraft.getInstance().execute(runnable);
	}

	private static String call(ModConfig cfg, List<ChatMessage> messages) throws Exception {
		if (cfg.baseUrl == null || cfg.baseUrl.isBlank()) {
			throw new IllegalStateException("没有填写 API 接口地址，请打开设置界面配置");
		}
		if (cfg.model == null || cfg.model.isBlank()) {
			throw new IllegalStateException("没有填写模型名称");
		}

		AiStyle style = cfg.style();
		String endpoint = AiPayload.endpoint(cfg.baseUrl, style, cfg.model);

		if (style == AiStyle.GEMINI && !cfg.apiKey.isBlank()) {
			endpoint = endpoint + (endpoint.contains("?") ? "&" : "?") + "key=" + cfg.apiKey.trim();
		}

		String body = AiPayload.buildBody(cfg, messages);
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoint))
				.header("Content-Type", "application/json");

		switch (style) {
			case ANTHROPIC -> {
				builder.header("anthropic-version", "2023-06-01");
				if (!cfg.apiKey.isBlank()) {
					builder.header("x-api-key", cfg.apiKey.trim());
				}
			}
			case GEMINI -> {
				// key 已在 query 上，无需额外头
			}
			default -> {
				if (!cfg.apiKey.isBlank()) {
					builder.header("Authorization", "Bearer " + cfg.apiKey.trim());
				}
			}
		}

		if (cfg.logRequests) {
			AiDialogMod.LOGGER.info("POST {}\n{}", endpoint, body);
		}

		HttpRequest request = builder
				.timeout(Duration.ofSeconds(cfg.timeoutSeconds))
				.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
				.build();

		HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		int status = response.statusCode();
		String raw = response.body() == null ? "" : response.body();

		if (cfg.logRequests) {
			AiDialogMod.LOGGER.info("HTTP {} <- {}", status, AiPayload.truncate(raw, 800));
		}

		if (status < 200 || status >= 300) {
			throw new ApiException(status, AiPayload.extractError(raw));
		}

		String text = AiPayload.parse(style, raw);
		if (text == null || text.isBlank()) {
			throw new ApiException(status, "模型返回了空内容");
		}
		return text;
	}

	private static String describe(Exception e) {
		if (e instanceof ApiException api) {
			return "HTTP " + api.status + "：" + api.detail;
		}
		if (e instanceof java.net.http.HttpTimeoutException) {
			return "请求超时，检查网络或加大超时时间";
		}
		if (e instanceof java.net.ConnectException) {
			return "无法连接服务器，检查 API 地址是否正确";
		}
		if (e instanceof IllegalArgumentException) {
			return "API 地址格式不正确：" + e.getMessage();
		}
		String message = e.getMessage();
		return e.getClass().getSimpleName() + (message == null ? "" : "：" + message);
	}

	public static class ApiException extends Exception {
		public final int status;
		public final String detail;

		public ApiException(int status, String detail) {
			super("HTTP " + status + " " + detail);
			this.status = status;
			this.detail = detail;
		}
	}
}
