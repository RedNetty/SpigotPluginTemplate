package com.rednetty.plugintemplate.core.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rednetty.plugintemplate.PluginTemplate;
import com.rednetty.plugintemplate.database.DatabaseManager;
import com.rednetty.plugintemplate.database.repositories.PlayerDataRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Comprehensive player data management system
 *
 * Features:
 * - In-memory caching for performance
 * - Asynchronous database operations
 * - JSON-based data storage
 * - Automatic save intervals
 * - Data validation and migration
 * - Event-driven updates
 * - Thread-safe operations
 * - Player session tracking
 *
 * @author RedNetty
 * @version 1.0.0
 */
public class PlayerDataManager implements Listener {

    private final PluginTemplate plugin;
    private final DatabaseManager databaseManager;
    private final PlayerDataRepository repository;
    private final Gson gson;

    // In-memory cache
    private final Map<UUID, PlayerData> playerDataCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastSaveTime = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerSession> activeSessions = new ConcurrentHashMap<>();

    // Configuration
    private final int autoSaveInterval; // seconds
    private final boolean debugMode;

    /**
     * Player data container
     */
    public static class PlayerData {
        private final UUID uuid;
        private String username;
        private long firstJoin;
        private long lastJoin;
        private long totalPlayTime;
        private int loginCount;
        private final Map<String, Object> customData = new HashMap<>();

        // Statistics
        private final Map<String, Long> statistics = new HashMap<>();

        // Settings
        private final Map<String, Object> settings = new HashMap<>();

        public PlayerData(@NotNull UUID uuid, @NotNull String username) {
            this.uuid = uuid;
            this.username = username;
            this.firstJoin = System.currentTimeMillis();
            this.lastJoin = this.firstJoin;
            this.totalPlayTime = 0;
            this.loginCount = 1;
        }

        // Getters
        public UUID getUUID() { return uuid; }
        public String getUsername() { return username; }
        public long getFirstJoin() { return firstJoin; }
        public long getLastJoin() { return lastJoin; }
        public long getTotalPlayTime() { return totalPlayTime; }
        public int getLoginCount() { return loginCount; }

        // Setters
        public void setUsername(@NotNull String username) { this.username = username; }
        public void setLastJoin(long lastJoin) { this.lastJoin = lastJoin; }
        public void setTotalPlayTime(long totalPlayTime) { this.totalPlayTime = totalPlayTime; }
        public void incrementLoginCount() { this.loginCount++; }

        // Custom data methods
        public void setData(@NotNull String key, @Nullable Object value) {
            if (value == null) {
                customData.remove(key);
            } else {
                customData.put(key, value);
            }
        }

        @SuppressWarnings("unchecked")
        public <T> T getData(@NotNull String key, @NotNull Class<T> type, @Nullable T defaultValue) {
            Object value = customData.get(key);
            if (value != null && type.isInstance(value)) {
                return (T) value;
            }
            return defaultValue;
        }

        public String getString(@NotNull String key, @Nullable String defaultValue) {
            return getData(key, String.class, defaultValue);
        }

        public Integer getInt(@NotNull String key, @Nullable Integer defaultValue) {
            return getData(key, Integer.class, defaultValue);
        }

        public Long getLong(@NotNull String key, @Nullable Long defaultValue) {
            return getData(key, Long.class, defaultValue);
        }

        public Boolean getBoolean(@NotNull String key, @Nullable Boolean defaultValue) {
            return getData(key, Boolean.class, defaultValue);
        }

        public Double getDouble(@NotNull String key, @Nullable Double defaultValue) {
            return getData(key, Double.class, defaultValue);
        }

        // Statistics methods
        public void setStat(@NotNull String key, long value) {
            statistics.put(key, value);
        }

        public long getStat(@NotNull String key, long defaultValue) {
            return statistics.getOrDefault(key, defaultValue);
        }

        public void incrementStat(@NotNull String key, long amount) {
            statistics.put(key, getStat(key, 0) + amount);
        }

        public void incrementStat(@NotNull String key) {
            incrementStat(key, 1);
        }

        // Settings methods
        public void setSetting(@NotNull String key, @Nullable Object value) {
            if (value == null) {
                settings.remove(key);
            } else {
                settings.put(key, value);
            }
        }

        @SuppressWarnings("unchecked")
        public <T> T getSetting(@NotNull String key, @NotNull Class<T> type, @Nullable T defaultValue) {
            Object value = settings.get(key);
            if (value != null && type.isInstance(value)) {
                return (T) value;
            }
            return defaultValue;
        }

        // JSON serialization
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("uuid", uuid.toString());
            json.addProperty("username", username);
            json.addProperty("firstJoin", firstJoin);
            json.addProperty("lastJoin", lastJoin);
            json.addProperty("totalPlayTime", totalPlayTime);
            json.addProperty("loginCount", loginCount);

            // Add custom data
            JsonObject customDataJson = new JsonObject();
            for (Map.Entry<String, Object> entry : customData.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof String) {
                    customDataJson.addProperty(key, (String) value);
                } else if (value instanceof Number) {
                    customDataJson.addProperty(key, (Number) value);
                } else if (value instanceof Boolean) {
                    customDataJson.addProperty(key, (Boolean) value);
                }
            }
            json.add("customData", customDataJson);

            // Add statistics
            JsonObject statisticsJson = new JsonObject();
            for (Map.Entry<String, Long> entry : statistics.entrySet()) {
                statisticsJson.addProperty(entry.getKey(), entry.getValue());
            }
            json.add("statistics", statisticsJson);

            // Add settings
            JsonObject settingsJson = new JsonObject();
            for (Map.Entry<String, Object> entry : settings.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof String) {
                    settingsJson.addProperty(key, (String) value);
                } else if (value instanceof Number) {
                    settingsJson.addProperty(key, (Number) value);
                } else if (value instanceof Boolean) {
                    settingsJson.addProperty(key, (Boolean) value);
                }
            }
            json.add("settings", settingsJson);

            return json;
        }

        // JSON deserialization
        public static PlayerData fromJson(@NotNull JsonObject json) {
            UUID uuid = UUID.fromString(json.get("uuid").getAsString());
            String username = json.get("username").getAsString();

            PlayerData data = new PlayerData(uuid, username);
            data.firstJoin = json.get("firstJoin").getAsLong();
            data.lastJoin = json.get("lastJoin").getAsLong();
            data.totalPlayTime = json.get("totalPlayTime").getAsLong();
            data.loginCount = json.get("loginCount").getAsInt();

            // Load custom data
            if (json.has("customData")) {
                JsonObject customDataJson = json.getAsJsonObject("customData");
                for (String key : customDataJson.keySet()) {
                    if (customDataJson.get(key).isJsonPrimitive()) {
                        if (customDataJson.get(key).getAsJsonPrimitive().isString()) {
                            data.customData.put(key, customDataJson.get(key).getAsString());
                        } else if (customDataJson.get(key).getAsJsonPrimitive().isNumber()) {
                            data.customData.put(key, customDataJson.get(key).getAsLong());
                        } else if (customDataJson.get(key).getAsJsonPrimitive().isBoolean()) {
                            data.customData.put(key, customDataJson.get(key).getAsBoolean());
                        }
                    }
                }
            }

            // Load statistics
            if (json.has("statistics")) {
                JsonObject statisticsJson = json.getAsJsonObject("statistics");
                for (String key : statisticsJson.keySet()) {
                    data.statistics.put(key, statisticsJson.get(key).getAsLong());
                }
            }

            // Load settings
            if (json.has("settings")) {
                JsonObject settingsJson = json.getAsJsonObject("settings");
                for (String key : settingsJson.keySet()) {
                    if (settingsJson.get(key).isJsonPrimitive()) {
                        if (settingsJson.get(key).getAsJsonPrimitive().isString()) {
                            data.settings.put(key, settingsJson.get(key).getAsString());
                        } else if (settingsJson.get(key).getAsJsonPrimitive().isNumber()) {
                            data.settings.put(key, settingsJson.get(key).getAsLong());
                        } else if (settingsJson.get(key).getAsJsonPrimitive().isBoolean()) {
                            data.settings.put(key, settingsJson.get(key).getAsBoolean());
                        }
                    }
                }
            }

            return data;
        }
    }

    /**
     * Player session tracking
     */
    public static class PlayerSession {
        private final UUID playerUUID;
        private final long sessionStart;
        private long lastActivity;
        private final Map<String, Object> sessionData = new HashMap<>();

        public PlayerSession(@NotNull UUID playerUUID) {
            this.playerUUID = playerUUID;
            this.sessionStart = System.currentTimeMillis();
            this.lastActivity = this.sessionStart;
        }

        public UUID getPlayerUUID() { return playerUUID; }
        public long getSessionStart() { return sessionStart; }
        public long getLastActivity() { return lastActivity; }
        public long getSessionDuration() { return System.currentTimeMillis() - sessionStart; }

        public void updateActivity() {
            this.lastActivity = System.currentTimeMillis();
        }

        public void setSessionData(@NotNull String key, @Nullable Object value) {
            if (value == null) {
                sessionData.remove(key);
            } else {
                sessionData.put(key, value);
            }
        }

        @SuppressWarnings("unchecked")
        public <T> T getSessionData(@NotNull String key, @NotNull Class<T> type, @Nullable T defaultValue) {
            Object value = sessionData.get(key);
            if (value != null && type.isInstance(value)) {
                return (T) value;
            }
            return defaultValue;
        }
    }

    /**
     * Constructor for PlayerDataManager
     *
     * @param plugin Plugin instance
     */
    public PlayerDataManager(@NotNull PluginTemplate plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.repository = databaseManager.getRepository(PlayerDataRepository.class);
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        // Load configuration
        this.autoSaveInterval = plugin.getConfigManager().getConfig().getInt("player-data.auto-save-interval", 300);
        this.debugMode = plugin.getConfigManager().getConfig().getBoolean("plugin.debug", false);
    }

    /**
     * Initialize the player data manager
     */
    public void initialize() {
        plugin.getLogger().info("Initializing PlayerDataManager...");

        // Register event listener
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Start auto-save task
        if (autoSaveInterval > 0) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::autoSave,
                    20L * autoSaveInterval, 20L * autoSaveInterval);
        }

        // Load data for online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadPlayerData(player);
        }

        plugin.getLogger().info("PlayerDataManager initialized successfully!");
    }

    /**
     * Load player data from database
     *
     * @param player The player
     * @return CompletableFuture with player data
     */
    @NotNull
    public CompletableFuture<PlayerData> loadPlayerData(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PlayerData data = repository.getPlayerData(player.getUniqueId());

                if (data == null) {
                    // Create new player data
                    data = new PlayerData(player.getUniqueId(), player.getName());
                    if (debugMode) {
                        plugin.getLogger().info("Created new player data for " + player.getName());
                    }
                } else {
                    // Update username if changed
                    if (!data.getUsername().equals(player.getName())) {
                        data.setUsername(player.getName());
                    }

                    // Update login information
                    data.setLastJoin(System.currentTimeMillis());
                    data.incrementLoginCount();

                    if (debugMode) {
                        plugin.getLogger().info("Loaded existing player data for " + player.getName());
                    }
                }

                // Cache the data
                playerDataCache.put(player.getUniqueId(), data);

                // Create session
                activeSessions.put(player.getUniqueId(), new PlayerSession(player.getUniqueId()));

                return data;

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load player data for " + player.getName(), e);

                // Create temporary data
                PlayerData tempData = new PlayerData(player.getUniqueId(), player.getName());
                playerDataCache.put(player.getUniqueId(), tempData);
                return tempData;
            }
        });
    }

    /**
     * Save player data to database
     *
     * @param player The player
     * @return CompletableFuture indicating completion
     */
    @NotNull
    public CompletableFuture<Void> savePlayerData(@NotNull Player player) {
        return savePlayerData(player.getUniqueId());
    }

    /**
     * Save player data to database by UUID
     *
     * @param playerUUID The player UUID
     * @return CompletableFuture indicating completion
     */
    @NotNull
    public CompletableFuture<Void> savePlayerData(@NotNull UUID playerUUID) {
        return CompletableFuture.runAsync(() -> {
            PlayerData data = playerDataCache.get(playerUUID);
            if (data == null) {
                return;
            }

            try {
                // Update play time from session
                PlayerSession session = activeSessions.get(playerUUID);
                if (session != null) {
                    long sessionTime = session.getSessionDuration();
                    data.setTotalPlayTime(data.getTotalPlayTime() + sessionTime);
                }

                // Save to database
                repository.savePlayerData(data);
                lastSaveTime.put(playerUUID, System.currentTimeMillis());

                if (debugMode) {
                    plugin.getLogger().fine("Saved player data for " + data.getUsername());
                }

            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save player data for " + data.getUsername(), e);
            }
        });
    }

    /**
     * Get player data from cache
     *
     * @param player The player
     * @return Player data or null if not cached
     */
    @Nullable
    public PlayerData getPlayerData(@NotNull Player player) {
        return getPlayerData(player.getUniqueId());
    }

    /**
     * Get player data from cache by UUID
     *
     * @param playerUUID The player UUID
     * @return Player data or null if not cached
     */
    @Nullable
    public PlayerData getPlayerData(@NotNull UUID playerUUID) {
        return playerDataCache.get(playerUUID);
    }

    /**
     * Get player session
     *
     * @param player The player
     * @return Player session or null if not active
     */
    @Nullable
    public PlayerSession getPlayerSession(@NotNull Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    /**
     * Auto-save all cached player data
     */
    private void autoSave() {
        if (playerDataCache.isEmpty()) {
            return;
        }

        plugin.getLogger().fine("Auto-saving player data for " + playerDataCache.size() + " players...");

        CompletableFuture<Void>[] futures = playerDataCache.keySet().stream()
                .map(this::savePlayerData)
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).thenRun(() -> {
            plugin.getLogger().fine("Auto-save completed!");
        }).exceptionally(throwable -> {
            plugin.getLogger().log(Level.WARNING, "Auto-save failed", throwable);
            return null;
        });
    }

    /**
     * Save all player data
     */
    public void saveAllData() {
        plugin.getLogger().info("Saving all player data...");

        CompletableFuture<Void>[] futures = playerDataCache.keySet().stream()
                .map(this::savePlayerData)
                .toArray(CompletableFuture[]::new);

        try {
            CompletableFuture.allOf(futures).get(); // Wait for completion
            plugin.getLogger().info("All player data saved successfully!");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save all player data", e);
        }
    }

    /**
     * Validate data integrity
     *
     * @return True if all data is valid
     */
    public boolean validateIntegrity() {
        try {
            for (PlayerData data : playerDataCache.values()) {
                if (data.getUUID() == null || data.getUsername() == null || data.getUsername().isEmpty()) {
                    plugin.getLogger().warning("Invalid player data found: " + data.getUUID());
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Data integrity check failed", e);
            return false;
        }
    }

    /**
     * Shutdown the player data manager
     */
    public void shutdown() {
        plugin.getLogger().info("Shutting down PlayerDataManager...");

        // Save all data before shutdown
        saveAllData();

        // Clear caches
        playerDataCache.clear();
        lastSaveTime.clear();
        activeSessions.clear();

        plugin.getLogger().info("PlayerDataManager shutdown completed!");
    }

    // ===== EVENT HANDLERS =====

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Load player data asynchronously
        loadPlayerData(player).thenAccept(data -> {
            // Notify core about player join
            plugin.getCore().handlePlayerJoin(player);

            // Load player language preference
            plugin.getLocalizationManager().loadPlayerLanguage(player);

        }).exceptionally(throwable -> {
            plugin.getLogger().log(Level.SEVERE, "Failed to handle player join: " + player.getName(), throwable);
            return null;
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Save player data asynchronously
        savePlayerData(player).thenRun(() -> {
            // Remove from cache after save
            playerDataCache.remove(player.getUniqueId());
            activeSessions.remove(player.getUniqueId());
            lastSaveTime.remove(player.getUniqueId());

            // Notify core about player quit
            plugin.getCore().handlePlayerQuit(player);

        }).exceptionally(throwable -> {
            plugin.getLogger().log(Level.WARNING, "Failed to handle player quit: " + player.getName(), throwable);

            // Still remove from cache to prevent memory leak
            playerDataCache.remove(player.getUniqueId());
            activeSessions.remove(player.getUniqueId());
            lastSaveTime.remove(player.getUniqueId());

            return null;
        });
    }

    /**
     * Get statistics about player data manager
     *
     * @return Statistics
     */
    @NotNull
    public PlayerDataStats getStatistics() {
        return new PlayerDataStats(
                playerDataCache.size(),
                activeSessions.size(),
                autoSaveInterval,
                validateIntegrity()
        );
    }

    /**
     * Player data statistics record
     */
    public record PlayerDataStats(
            int cachedPlayers,
            int activeSessions,
            int autoSaveInterval,
            boolean dataIntegrityValid
    ) {}
}