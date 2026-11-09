package com.mcbjk.aidialog.chat;

import com.mcbjk.aidialog.AiDialogMod;
import com.mcbjk.aidialog.ai.AiClient;
import com.mcbjk.aidialog.ai.ChatMessage;
import com.mcbjk.aidialog.config.ModConfig;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 核心判定逻辑：
 * 1. 读取当前登录账号的名字（启动时 + 进入世界时刷新）；
 * 2. 监听聊天栏里其他玩家的发言；
 * 3. 发言中包含触发词（默认就是本账号 ID）时，交给 AI 生成回复并自动发送。
 */
public final class AutoChatHandler {
	private static final Map<UUID, Long> LAST_TRIGGER = new HashMap<>();

	private static String cachedSelfName;

	private AutoChatHandler() {
	}

	public static void install() {
		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
			if (sender == null) {
				return;
			}
			String text = message.getString();
			handlePlayerChat(sender.id(), sender.name(), text);
		});

		// 进入世界时刷新登录账号，并把该账号的 ID 作为触发词绑定下来。
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> rememberAccount());
		// 退出服务器/存档时清空聊天上下文与冷却表，避免把上一局的记录带进新会话。
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ChatHistory.resetOnDisconnect();
			LAST_TRIGGER.clear();
		});
	}

	/** 当前登录账号名字（Mojang 账号名 / 离线登录名）。 */
	public static String selfName() {
		Minecraft client = Minecraft.getInstance();
		if (client.player != null) {
			String name = client.player.getScoreboardName();
			if (name != null && !name.isBlank()) {
				cachedSelfName = name;
				return name;
			}
		}
		if (client.getUser() != null && client.getUser().getName() != null) {
			cachedSelfName = client.getUser().getName();
			return cachedSelfName;
		}
		return cachedSelfName == null ? "" : cachedSelfName;
	}

	/** 启动 / 换号时刷新一次并打到日志里。 */
	public static void rememberAccount() {
		Minecraft client = Minecraft.getInstance();
		String name = null;
		UUID uuid = null;
		if (client.getUser() != null) {
			name = client.getUser().getName();
			uuid = client.getUser().getProfileId();
		}
		if (client.player != null) {
			name = client.player.getScoreboardName();
			uuid = client.player.getUUID();
		}
		cachedSelfName = name;
		AiDialogMod.LOGGER.info("已识别当前登录账号：{} ({})", name, uuid);
	}

	private static void handlePlayerChat(UUID senderId, String senderName, String rawText) {
		ModConfig config = ModConfig.get();
		if (!config.enabled) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.getConnection() == null) {
			return;
		}

		String self = selfName();
		if (self.isBlank()) {
			rememberAccount();
			self = selfName();
			if (self.isBlank()) {
				return;
			}
		}

		// 自己说的话不触发
		if (senderId != null && senderId.equals(client.player.getUUID())) {
			return;
		}
		if (senderName != null && senderName.equalsIgnoreCase(self)) {
			return;
		}

		String text = rawText == null ? "" : rawText.trim();
		if (text.isEmpty()) {
			return;
		}

		String matched = findTrigger(config, text, self);
		if (matched == null) {
			return;
		}

		// 冷却
		long now = System.currentTimeMillis();
		long cooldownMs = config.cooldownSeconds * 1000L;
		UUID key = senderId != null ? senderId : UUID.nameUUIDFromBytes(String.valueOf(senderName).getBytes());
		Long last = LAST_TRIGGER.get(key);
		if (last != null && now - last < cooldownMs) {
			AiDialogMod.LOGGER.debug("{} 触发了但还在冷却中", senderName);
			return;
		}
		LAST_TRIGGER.put(key, now);

		AiDialogMod.LOGGER.info("{} 提到了「{}」，准备自动回复", senderName, matched);
		requestReply(senderId, senderName, text, self);
	}

	/** 判断一句话是否命中触发词，命中返回命中的词。 */
	static String findTrigger(ModConfig config, String text, String self) {
		for (String word : config.triggerWords(self)) {
			if (word.isBlank()) {
				continue;
			}
			if (TriggerMatcher.matches(text, word, config.ignoreCase, config.wholeWordOnly)) {
				return word;
			}
		}
		return null;
	}

	private static void requestReply(UUID senderId, String senderName, String triggerText, String self) {
		ModConfig config = ModConfig.get();
		List<ChatMessage> messages = buildMessages(config, senderName, triggerText, self);

		AiClient.request(messages, new AiClient.Callback() {
			@Override
			public void onSuccess(String text) {
				sendReply(config, text);
			}

			@Override
			public void onFailure(String message) {
				Minecraft client = Minecraft.getInstance();
				if (client.player != null) {
					client.player.displayClientMessage(
							Component.literal("[AI自动对话] 生成失败：" + message).withStyle(ChatFormatting.RED), false);
				}
			}
		});
	}

	private static List<ChatMessage> buildMessages(ModConfig config, String senderName, String triggerText, String self) {
		List<ChatMessage> messages = new ArrayList<>();
		String system = config.systemPrompt == null ? "" : config.systemPrompt.replace("{player}", self);
		if (!system.isBlank()) {
			messages.add(ChatMessage.system(system));
		}

		StringBuilder user = new StringBuilder();
		if (config.sendContext) {
			String transcript = ChatHistory.recentAsTranscript(config.contextMessageCount, self, null, null);
			if (!transcript.isBlank()) {
				user.append("以下是最近聊天栏的内容：\n").append(transcript).append("\n\n");
			}
		}
		user.append("玩家 ").append(senderName).append(" 在聊天栏里说：").append(triggerText).append('\n');
		user.append("请以玩家 ").append(self).append(" 的身份，直接在聊天栏回一句话。只输出要说的话本身，不要引号，不要换行。");
		messages.add(ChatMessage.user(user.toString()));
		return messages;
	}

	/** 清理并发送。长度严格控制在 Minecraft 聊天栏上限内。 */
	static void sendReply(ModConfig config, String raw) {
		Minecraft client = Minecraft.getInstance();
		ClientPacketListener connection = client.getConnection();
		if (connection == null || client.player == null) {
			return;
		}

		String text = raw;
		if (config.sanitizeOutput) {
			text = ReplyText.sanitize(text);
		}
		text = truncateToChatLimit(client, config, text);
		if (text.isBlank()) {
			return;
		}

		final String toSend = text;
		int min = Math.max(0, config.replyDelayMinMs);
		int max = Math.max(min, config.replyDelayMaxMs);
		long delay = min + (long) (Math.random() * (max - min));

		if (delay <= 0) {
			ChatSender.send(toSend);
			return;
		}
		new Thread(() -> {
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
			client.execute(() -> ChatSender.send(toSend));
		}, "AI-Dialog-Delay").start();
	}

	/**
	 * 按 Minecraft 聊天栏的硬上限截断。
	 * 上限取 SharedConstants.MAX_CHAT_LENGTH（1.21.11 为 256），同时受配置里的 maxReplyLength 约束。
	 * 注意：客户端「允许超过 256 字符的聊天」这个选项只影响本地输入框，
	 * 服务端仍会按自己的上限拒绝，所以这里始终按硬上限处理。
	 */
	static String truncateToChatLimit(Minecraft client, ModConfig config, String text) {
		String limited = ReplyText.truncate(text, config.maxReplyLength);
		// 再按像素宽度兜一层：聊天栏一行约 320 像素宽
		if (client.font != null) {
			int maxPixels = 300;
			while (!limited.isEmpty() && client.font.width(limited) > maxPixels) {
				limited = limited.substring(0, limited.length() - 1);
			}
		}
		return limited.trim();
	}

	public static void notifyToggle(boolean enabled) {
		Minecraft client = Minecraft.getInstance();
		if (client.player != null) {
			client.player.displayClientMessage(
					Component.literal("[AI自动对话] " + (enabled ? "已开启" : "已关闭"))
							.withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY), false);
		}
	}

	/** 供 GUI 测试按钮调用。 */
	public static void testConnection() {
		ModConfig config = ModConfig.get();
		String self = selfName();
		List<ChatMessage> messages = new ArrayList<>();
		String system = config.systemPrompt == null ? "" : config.systemPrompt.replace("{player}", self);
		if (!system.isBlank()) {
			messages.add(ChatMessage.system(system));
		}
		messages.add(ChatMessage.user("测试一下：随便说一句话，10 个字以内。"));
		AiClient.request(messages, new AiClient.Callback() {
			@Override
			public void onSuccess(String text) {
				Minecraft client = Minecraft.getInstance();
				if (client.player != null) {
					client.player.displayClientMessage(
							Component.literal("[AI自动对话] 测试成功：" + ReplyText.sanitize(text))
									.withStyle(ChatFormatting.GREEN), false);
				}
			}

			@Override
			public void onFailure(String message) {
				Minecraft client = Minecraft.getInstance();
				if (client.player != null) {
					client.player.displayClientMessage(
							Component.literal("[AI自动对话] 测试失败：" + message).withStyle(ChatFormatting.RED), false);
				}
			}
		});
	}

	/** 在线玩家名列表，GUI 里展示用。 */
	public static List<String> onlinePlayerNames() {
		Minecraft client = Minecraft.getInstance();
		List<String> names = new ArrayList<>();
		ClientPacketListener connection = client.getConnection();
		if (connection == null) {
			return names;
		}
		for (PlayerInfo info : connection.getOnlinePlayers()) {
			names.add(info.getProfile().name());
		}
		return names;
	}
}
