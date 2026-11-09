package com.mcbjk.aidialog.gui;

import com.mcbjk.aidialog.AiDialogMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/**
 * 快捷键注册。会出现在「选项 → 控制 → 按键绑定」里。
 */
public final class ModKeybinds {
	/** 自定义分类，需要 lang 文件里 key.category.aidialog.controls 的翻译。 */
	public static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath(AiDialogMod.MOD_ID, "controls"));

	public static KeyMapping openSettings;
	public static KeyMapping toggleMod;

	private ModKeybinds() {
	}

	public static void register() {
		openSettings = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.aidialog.open_settings",
				InputConstants.Type.KEYSYM,
				InputConstants.KEY_K,
				CATEGORY
		));

		toggleMod = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.aidialog.toggle",
				InputConstants.Type.KEYSYM,
				InputConstants.KEY_O,
				CATEGORY
		));
	}
}
