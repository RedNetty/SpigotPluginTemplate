package com.rednetty.plugintemplate.core.managers;

import com.rednetty.plugintemplate.PluginTemplate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * DataManager - General data management and persistence
 *
 * Features:
 * - File-based data storage
 * - JSON serialization
 * - Backup management
 * - Data validation
 * - Automatic saving
 *
 * @author RedNetty
 * @version 1.0.0
 */
class DataManager {

    private final PluginTemplate plugin;
    private final File dataDirectory;
    private final Map<String, Object> dataCache = new ConcurrentHashMap<>();

    // Backup settings
    private final int maxBackups;
    private final boolean autoBackup;

    public DataManager(@NotNull PluginTemplate plugin) {
        this.plugin = plugin;
        this.dataDirectory = new File(plugin.getDataFolder(), "data");
        this.maxBackups = plugin.getConfigManager().getConfig().getInt("data.max-backups", 5);
        this.autoBackup = plugin.getConfigManager().getConfig().getBoolean("data.auto-backup", true);
    }

    /**
     * Initialize the data manager
     */
    public void initialize() {
        plugin.getLogger().info("Initializing DataManager...");

        // Create data directory
        if (!dataDirectory.exists()) {
            if (!dataDirectory.mkdirs()) {
                plugin.getLogger().warning("Failed to create data directory");
            }
        }

        // Load existing data
        loadAllData();

        plugin.getLogger().info("DataManager initialized successfully!");
    }

    /**
     * Store data with key
     *
     * @param key Data key
     * @param data Data to store
     */
    public void setData(@NotNull String key, @Nullable Object data) {
        if (data == null) {
            dataCache.remove(key);
        } else {
            dataCache.put(key, data);
        }
    }

    /**
     * Get data by key
     *
     * @param key Data key
     * @param type Expected data type
     * @param <T> Data type
     * @return Data or null if not found
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getData(@NotNull String key, @NotNull Class<T> type) {
        Object data = dataCache.get(key);
        if (data != null && type.isInstance(data)) {
            return (T) data;
        }
        return null;
    }

    /**
     * Get data with default value
     *
     * @param key Data key
     * @param type Expected data type
     * @param defaultValue Default value if not found
     * @param <T> Data type
     * @return Data or default value
     */
    @NotNull
    public <T> T getData(@NotNull String key, @NotNull Class<T> type, @NotNull T defaultValue) {
        T data = getData(key, type);
        return data != null ? data : defaultValue;
    }

    /**
     * Check if data exists
     *
     * @param key Data key
     * @return True if exists
     */
    public boolean hasData(@NotNull String key) {
        return dataCache.containsKey(key);
    }

    /**
     * Save data to file
     *
     * @param key Data key
     */
    public void saveData(@NotNull String key) {
        Object data = dataCache.get(key);
        if (data == null) {
            return;
        }

        try {
            File dataFile = new File(dataDirectory, key + ".dat");

            // Create backup if enabled
            if (autoBackup && dataFile.exists()) {
                createBackup(key);
            }

            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dataFile))) {
                oos.writeObject(data);
            }

            plugin.getLogger().fine("Saved data: " + key);

        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save data: " + key, e);
        }
    }

    /**
     * Load data from file
     *
     * @param key Data key
     */
    public void loadData(@NotNull String key) {
        try {
            File dataFile = new File(dataDirectory, key + ".dat");
            if (!dataFile.exists()) {
                return;
            }

            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(dataFile))) {
                Object data = ois.readObject();
                dataCache.put(key, data);
            }

            plugin.getLogger().fine("Loaded data: " + key);

        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load data: " + key, e);
        }
    }

    /**
     * Load all data files
     */
    private void loadAllData() {
        File[] dataFiles = dataDirectory.listFiles((dir, name) -> name.endsWith(".dat"));
        if (dataFiles == null) {
            return;
        }

        for (File file : dataFiles) {
            String key = file.getName().substring(0, file.getName().length() - 4);
            loadData(key);
        }
    }

    /**
     * Save all cached data
     */
    public void saveAll() {
        for (String key : dataCache.keySet()) {
            saveData(key);
        }
    }

    /**
     * Create backup of data file
     *
     * @param key Data key
     */
    private void createBackup(@NotNull String key) {
        try {
            File dataFile = new File(dataDirectory, key + ".dat");
            if (!dataFile.exists()) {
                return;
            }

            File backupDir = new File(dataDirectory, "backups");
            if (!backupDir.exists() && !backupDir.mkdirs()) {
                plugin.getLogger().warning("Failed to create backup directory");
                return;
            }

            // Create backup with timestamp
            String timestamp = String.valueOf(System.currentTimeMillis());
            File backupFile = new File(backupDir, key + "_" + timestamp + ".dat");

            // Copy file
            try (FileInputStream fis = new FileInputStream(dataFile);
                 FileOutputStream fos = new FileOutputStream(backupFile)) {

                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
            }

            // Clean old backups
            cleanOldBackups(key, backupDir);

        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create backup for: " + key, e);
        }
    }

    /**
     * Clean old backup files
     *
     * @param key Data key
     * @param backupDir Backup directory
     */
    private void cleanOldBackups(@NotNull String key, @NotNull File backupDir) {
        File[] backups = backupDir.listFiles((dir, name) -> name.startsWith(key + "_") && name.endsWith(".dat"));
        if (backups == null || backups.length <= maxBackups) {
            return;
        }

        // Sort by modification time
        Arrays.sort(backups, Comparator.comparingLong(File::lastModified));

        // Delete oldest files
        for (int i = 0; i < backups.length - maxBackups; i++) {
            if (backups[i].delete()) {
                plugin.getLogger().fine("Deleted old backup: " + backups[i].getName());
            }
        }
    }

    /**
     * Delete data
     *
     * @param key Data key
     */
    public void deleteData(@NotNull String key) {
        dataCache.remove(key);
        File dataFile = new File(dataDirectory, key + ".dat");
        if (dataFile.exists()) {
            if (dataFile.delete()) {
                plugin.getLogger().fine("Deleted data file: " + key);
            }
        }
    }

    /**
     * Get all data keys
     *
     * @return Set of data keys
     */
    @NotNull
    public Set<String> getDataKeys() {
        return new HashSet<>(dataCache.keySet());
    }

    /**
     * Shutdown the data manager
     */
    public void shutdown() {
        saveAll();
        dataCache.clear();
        plugin.getLogger().info("DataManager shutdown completed!");
    }
}


