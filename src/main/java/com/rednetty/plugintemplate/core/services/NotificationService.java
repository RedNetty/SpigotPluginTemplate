package com.rednetty.plugintemplate.core.services;


import com.rednetty.plugintemplate.PluginTemplate;
import com.rednetty.plugintemplate.utils.ComponentBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * NotificationService - Player notification system
 *
 * Features:
 * - Chat messages with formatting
 * - Action bar notifications
 * - Title/subtitle displays
 * - Sound effects
 * - Delayed notifications
 * - Broadcast capabilities
 *
 * @author RedNetty
 * @version 1.0.0
 */
public class NotificationService {

    private final PluginTemplate plugin;

    public NotificationService(@NotNull PluginTemplate plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize the notification service
     */
    public void initialize() {
        plugin.getLogger().info("NotificationService initialized successfully!");
    }

    /**
     * Send chat message to player
     *
     * @param player Target player
     * @param message Message component
     */
    public void sendMessage(@NotNull Player player, @NotNull Component message) {
        plugin.getAudiences().player(player).sendMessage(message);
    }

    /**
     * Send chat message to player with localization
     *
     * @param player Target player
     * @param key Localization key
     * @param fallback Fallback message
     */
    public void sendLocalizedMessage(@NotNull Player player, @NotNull String key, @NotNull String fallback) {
        Component message = ComponentBuilder.create(plugin)
                .fromLocalization(key, fallback)
                .build();
        sendMessage(player, message);
    }

    /**
     * Send action bar message
     *
     * @param player Target player
     * @param message Message component
     */
    public void sendActionBar(@NotNull Player player, @NotNull Component message) {
        plugin.getAudiences().player(player).sendActionBar(message);
    }

    /**
     * Send title and subtitle
     *
     * @param player Target player
     * @param title Title component
     * @param subtitle Subtitle component
     */
    public void sendTitle(@NotNull Player player, @NotNull Component title, @NotNull Component subtitle) {
        sendTitle(player, title, subtitle, Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500));
    }

    /**
     * Send title with custom timing
     *
     * @param player Target player
     * @param title Title component
     * @param subtitle Subtitle component
     * @param fadeIn Fade in duration
     * @param stay Stay duration
     * @param fadeOut Fade out duration
     */
    public void sendTitle(@NotNull Player player, @NotNull Component title, @NotNull Component subtitle,
                          @NotNull Duration fadeIn, @NotNull Duration stay, @NotNull Duration fadeOut) {
        Title titleObj = Title.title(title, subtitle, Title.Times.times(fadeIn, stay, fadeOut));
        plugin.getAudiences().player(player).showTitle(titleObj);
    }

    /**
     * Play sound for player
     *
     * @param player Target player
     * @param sound Sound to play
     * @param volume Sound volume
     * @param pitch Sound pitch
     */
    public void playSound(@NotNull Player player, @NotNull Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    /**
     * Send notification with sound
     *
     * @param player Target player
     * @param message Message component
     * @param sound Sound to play
     */
    public void sendNotificationWithSound(@NotNull Player player, @NotNull Component message, @NotNull Sound sound) {
        sendMessage(player, message);
        playSound(player, sound, 1.0f, 1.0f);
    }

    /**
     * Send success notification
     *
     * @param player Target player
     * @param message Message
     */
    public void sendSuccess(@NotNull Player player, @NotNull String message) {
        Component component = ComponentBuilder.create(plugin)
                .text("✓ " + message)
                .color(NamedTextColor.GREEN)
                .build();
        sendNotificationWithSound(player, component, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
    }

    /**
     * Send error notification
     *
     * @param player Target player
     * @param message Message
     */
    public void sendError(@NotNull Player player, @NotNull String message) {
        Component component = ComponentBuilder.create(plugin)
                .text("✗ " + message)
                .color(NamedTextColor.RED)
                .build();
        sendNotificationWithSound(player, component, Sound.BLOCK_NOTE_BLOCK_BASS);
    }

    /**
     * Send warning notification
     *
     * @param player Target player
     * @param message Message
     */
    public void sendWarning(@NotNull Player player, @NotNull String message) {
        Component component = ComponentBuilder.create(plugin)
                .text("⚠ " + message)
                .color(NamedTextColor.YELLOW)
                .build();
        sendNotificationWithSound(player, component, Sound.BLOCK_NOTE_BLOCK_PLING);
    }

    /**
     * Send info notification
     *
     * @param player Target player
     * @param message Message
     */
    public void sendInfo(@NotNull Player player, @NotNull String message) {
        Component component = ComponentBuilder.create(plugin)
                .text("ℹ " + message)
                .color(NamedTextColor.AQUA)
                .build();
        sendMessage(player, component);
    }

    /**
     * Broadcast message to all players
     *
     * @param message Message component
     */
    public void broadcast(@NotNull Component message) {
        plugin.getAudiences().all().sendMessage(message);
    }

    /**
     * Broadcast to players with permission
     *
     * @param message Message component
     * @param permission Required permission
     */
    public void broadcastWithPermission(@NotNull Component message, @NotNull String permission) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(permission)) {
                sendMessage(player, message);
            }
        }
    }

    /**
     * Send delayed notification
     *
     * @param player Target player
     * @param message Message component
     * @param delayTicks Delay in ticks
     */
    public void sendDelayed(@NotNull Player player, @NotNull Component message, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                sendMessage(player, message);
            }
        }, delayTicks);
    }

    /**
     * Check if notification service is healthy
     *
     * @return True if healthy
     */
    public boolean isHealthy() {
        return plugin.getAudiences() != null;
    }

    /**
     * Shutdown the notification service
     */
    public void shutdown() {
        plugin.getLogger().info("NotificationService shutdown completed!");
    }
}
