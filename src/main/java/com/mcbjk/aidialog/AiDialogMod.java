package com.mcbjk.aidialog;

import com.mcbjk.aidialog.chat.AutoChatHandler;
import com.mcbjk.aidialog.chat.ChatHistory;
import com.mcbjk.aidialog.config.ModConfig;
import com.mcbjk.aidialog.gui.AiDialogScreen;
import com.mcbjk.aidialog.gui.ModKeybinds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public class AiDialogMod implements ClientModInitializer {
	public static final String MOD_ID = "aidialog";
	public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("AI-AutoChat");

	@Override
	public void onInitializeClient() {
		ModConfig.load();
		ChatHistory.install();
		AutoChatHandler.install();
		ModKeybinds.register();

		ClientTickEvents.END_CLIENT_TICK.register(this::onEndTick);

		LOGGER.info("AI 自动对话已加载，创作者 _mcbjk_");
	}

	private void onEndTick(Minecraft client) {
		while (ModKeybinds.openSettings.consumeClick()) {
			client.setScreen(new AiDialogScreen(client.screen));
		}
		while (ModKeybinds.toggleMod.consumeClick()) {
			ModConfig config = ModConfig.get();
			config.enabled = !config.enabled;
			ModConfig.save();
			AutoChatHandler.notifyToggle(config.enabled);
		}
	}
}
