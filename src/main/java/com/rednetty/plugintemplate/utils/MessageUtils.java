package com.rednetty.plugintemplate.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * Utilities for message formatting and sending
 */
public class MessageUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public static void sendMessage(@NotNull CommandSender sender, @NotNull String message) {
        Component component = MINI_MESSAGE.deserialize(message);
        sender.sendMessage(String.valueOf(component));
    }

    public static Component parseMessage(@NotNull String message) {
        return MINI_MESSAGE.deserialize(message);
    }
}
