package com.rednetty.plugintemplate.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.IOException;

/**
 * Advanced configuration management system
 */
public class ConfigManager {

    private final Plugin plugin;
    private final File configFile;
    private FileConfiguration config;

    public ConfigManager(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        loadConfig();
    }

    private void loadConfig() {
        if (configFile.exists()) {
            plugin.saveDefaultConfig();
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reloadConfig() {
        loadConfig();
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config: " + e.getMessage());
        }
    }

    @NotNull
    public FileConfiguration getConfig() {
        return config;
    }
}
