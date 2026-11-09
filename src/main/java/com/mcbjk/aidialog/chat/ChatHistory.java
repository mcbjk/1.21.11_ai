package com.mcbjk.aidialog.chat;

import com.mcbjk.aidialog.AiDialogMod;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 记录最近的聊天栏内容，用于给 AI 提供上下文。
 * 只保留公开聊天（玩家发言 / 系统提示），不落盘。
 */
public final class ChatHistory {
	private static final int MAX_ENTRIES = 40;
	private static final Deque<Entry> ENTRIES = new ArrayDeque<>();

	public record Entry(String speaker, String text, long timestamp, boolean isOwn) {
	}

	private ChatHistory() {
	}

	public static void install() {
		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
			String speaker = sender != null ? sender.name() : "?";
			String text = message.getString();
			record(speaker, text);
		});

		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (overlay) {
				// 快捷栏上方的动作栏文字，噪声太多，不记
				return;
			}
			record(null, message.getString());
		});
	}

	public static synchronized void record(String speaker, String text) {
		if (text == null || text.isBlank()) {
			return;
		}
		boolean own = false;
		var client = net.minecraft.client.Minecraft.getInstance();
		if (client.player != null && speaker != null) {
			own = speaker.equalsIgnoreCase(client.player.getScoreboardName());
		}
		ENTRIES.addLast(new Entry(speaker, text.trim(), System.currentTimeMillis(), own));
		while (ENTRIES.size() > MAX_ENTRIES) {
			ENTRIES.removeFirst();
		}
		AiDialogMod.LOGGER.debug("[chat] {}: {}", speaker == null ? "<system>" : speaker, text);
	}

	public static synchronized void recordOwn(String text) {
		String self = selfName();
		ENTRIES.addLast(new Entry(self, text, System.currentTimeMillis(), true));
		while (ENTRIES.size() > MAX_ENTRIES) {
			ENTRIES.removeFirst();
		}
	}

	public static synchronized List<Entry> recent(int count) {
		if (count <= 0) {
			return List.of();
		}
		List<Entry> all = new ArrayList<>(ENTRIES);
		int from = Math.max(0, all.size() - count);
		return new ArrayList<>(all.subList(from, all.size()));
	}

	/** 取最近聊天拼成给模型看的一段纯文本上下文。 */
	public static synchronized String recentAsTranscript(int count, String selfName, String triggerSpeaker,
													   String triggerText) {
		StringBuilder sb = new StringBuilder();
		for (Entry entry : recent(count)) {
			if (entry.speaker() == null) {
				continue;
			}
			sb.append(entry.isOwn() ? selfName : entry.speaker()).append("：").append(entry.text()).append('\n');
		}
		if (triggerSpeaker != null) {
			sb.append(triggerSpeaker).append("：").append(triggerText).append('\n');
		}
		return sb.toString().trim();
	}

	public static synchronized void clear() {
		ENTRIES.clear();
	}

	private static String selfName() {
		var client = net.minecraft.client.Minecraft.getInstance();
		if (client.player != null) {
			return client.player.getScoreboardName();
		}
		return client.getUser() != null ? client.getUser().getName() : "我";
	}

	/** 玩家断线时清空，避免把上一个服务器/存档的聊天带到新会话。 */
	public static void resetOnDisconnect() {
		clear();
	}
}
