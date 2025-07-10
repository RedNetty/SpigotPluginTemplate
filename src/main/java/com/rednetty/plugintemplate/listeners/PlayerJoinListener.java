// =============================================================================
// LISTENERS
// =============================================================================

package com.rednetty.plugintemplate.listeners;

import com.rednetty.plugintemplate.PluginTemplate;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

/**
 * PlayerJoinListener - Handle player join/quit events
 *
 * Features:
 * - Welcome messages
 * - Data loading/saving
 * - Permission application
 * - Statistics tracking
 *
 * @author RedNetty
 * @version 1.0.0
 */
class PlayerJoinListener implements Listener {

    private final PluginTemplate plugin;

    public PlayerJoinListener(@NotNull PluginTemplate plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        var player = event.getPlayer();

        // The actual logic is handled by PlayerDataManager
        // This listener just ensures the event is captured
        plugin.getLogger().fine("Player joined: " + player.getName());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        var player = event.getPlayer();

        // The actual logic is handled by PlayerDataManager
        // This listener just ensures the event is captured
        plugin.getLogger().fine("Player quit: " + player.getName());
    }
}
