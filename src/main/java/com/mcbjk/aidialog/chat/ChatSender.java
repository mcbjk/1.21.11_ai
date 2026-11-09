package com.mcbjk.aidialog.chat;

import com.mcbjk.aidialog.AiDialogMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

/**
 * 统一发送出口。所有自动回复都从这里走，方便做历史记录和日志。
 */
public final class ChatSender {
	private ChatSender() {
	}

	public static void send(String text) {
		if (text == null || text.isBlank()) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		ClientPacketListener connection = client.getConnection();
		if (connection == null) {
			AiDialogMod.LOGGER.warn("未连接到服务器，放弃发送：{}", text);
			return;
		}
		try {
			connection.sendChat(text);
			ChatHistory.recordOwn(text);
			AiDialogMod.LOGGER.info("已自动回复：{}", text);
		} catch (Exception e) {
			AiDialogMod.LOGGER.error("发送聊天消息失败：{}", e.getMessage());
		}
	}
}
