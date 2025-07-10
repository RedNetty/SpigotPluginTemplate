
// =============================================================================
// TASKS
// =============================================================================

package com.rednetty.plugintemplate.tasks;

import com.rednetty.plugintemplate.PluginTemplate;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * AutoSaveTask - Periodic data saving task
 *
 * Features:
 * - Automatic data persistence
 * - Configurable intervals
 * - Error handling
 * - Performance monitoring
 *
 * @author RedNetty
 * @version 1.0.0
 */
class AutoSaveTask extends BukkitRunnable {

    private final PluginTemplate plugin;

    public AutoSaveTask(@NotNull PluginTemplate plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getLogger().fine("Running auto-save task...");

        try {
            // Save player data
            if (plugin.getCore() != null && plugin.getCore().getPlayerDataManager() != null) {
                plugin.getCore().getPlayerDataManager().saveAllData();
            }

            // Save configuration
            if (plugin.getConfigManager() != null) {
                plugin.getConfigManager().saveConfig();
            }

            // Save cache
            if (plugin.getCore() != null && plugin.getCore().getCacheService() != null) {
                plugin.getCore().getCacheService().saveToDisk();
            }

            plugin.getLogger().fine("Auto-save completed successfully!");

        } catch (Exception e) {
            plugin.getLogger().warning("Auto-save failed: " + e.getMessage());
        }
    }
}
