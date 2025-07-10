package com.rednetty.plugintemplate.core.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rednetty.plugintemplate.PluginTemplate;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * CacheService - High-performance caching system
 *
 * Features:
 * - TTL (Time To Live) support
 * - Memory management
 * - Persistent cache storage
 * - Cache statistics
 * - Automatic cleanup
 * - Type-safe operations
 *
 * @author RedNetty
 * @version 1.0.0
 */
public class CacheService {

    private final PluginTemplate plugin;
    private final Map<String, CacheEntry<?>> cache = new ConcurrentHashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // Statistics
    private long hits = 0;
    private long misses = 0;
    private long evictions = 0;

    // Cleanup task
    private BukkitTask cleanupTask;

    /**
     * Cache entry with TTL support
     */
    private static class CacheEntry<T> {
        private final T value;
        private final long creationTime;
        private final long ttl; // Time to live in milliseconds
        private long lastAccessed;

        public CacheEntry(T value, long ttl) {
            this.value = value;
            this.creationTime = System.currentTimeMillis();
            this.lastAccessed = this.creationTime;
            this.ttl = ttl;
        }

        public T getValue() {
            this.lastAccessed = System.currentTimeMillis();
            return value;
        }

        public boolean isExpired() {
            return ttl > 0 && (System.currentTimeMillis() - creationTime) > ttl;
        }

        public long getAge() {
            return System.currentTimeMillis() - creationTime;
        }

        public long getLastAccessed() {
            return lastAccessed;
        }
    }

    public CacheService(@NotNull PluginTemplate plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize the cache service
     */
    public void initialize() {
        plugin.getLogger().info("Initializing CacheService...");

        // Start cleanup task
        this.cleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                this::cleanup, 20L * 60L, 20L * 60L); // Every minute

        plugin.getLogger().info("CacheService initialized successfully!");
    }

    /**
     * Put value in cache with default TTL
     *
     * @param key Cache key
     * @param value Value to cache
     * @param <T> Value type
     */
    public <T> void put(@NotNull String key, @NotNull T value) {
        put(key, value, TimeUnit.HOURS.toMillis(1)); // 1 hour default TTL
    }

    /**
     * Put value in cache with custom TTL
     *
     * @param key Cache key
     * @param value Value to cache
     * @param ttl Time to live in milliseconds
     * @param <T> Value type
     */
    public <T> void put(@NotNull String key, @NotNull T value, long ttl) {
        cache.put(key, new CacheEntry<>(value, ttl));
    }

    /**
     * Put value in cache with TimeUnit TTL
     *
     * @param key Cache key
     * @param value Value to cache
     * @param duration TTL duration
     * @param unit Time unit
     * @param <T> Value type
     */
    public <T> void put(@NotNull String key, @NotNull T value, long duration, @NotNull TimeUnit unit) {
        put(key, value, unit.toMillis(duration));
    }

    /**
     * Get value from cache
     *
     * @param key Cache key
     * @param type Expected value type
     * @param <T> Value type
     * @return Cached value or null if not found/expired
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T get(@NotNull String key, @NotNull Class<T> type) {
        CacheEntry<?> entry = cache.get(key);

        if (entry == null) {
            misses++;
            return null;
        }

        if (entry.isExpired()) {
            cache.remove(key);
            evictions++;
            misses++;
            return null;
        }

        hits++;
        Object value = entry.getValue();

        if (type.isInstance(value)) {
            return (T) value;
        }

        return null;
    }

    /**
     * Get value or compute if absent
     *
     * @param key Cache key
     * @param type Expected value type
     * @param supplier Value supplier if not cached
     * @param <T> Value type
     * @return Cached or computed value
     */
    @NotNull
    public <T> T getOrCompute(@NotNull String key, @NotNull Class<T> type, @NotNull java.util.function.Supplier<T> supplier) {
        T cached = get(key, type);
        if (cached != null) {
            return cached;
        }

        T computed = supplier.get();
        put(key, computed);
        return computed;
    }

    /**
     * Check if key exists and is not expired
     *
     * @param key Cache key
     * @return True if exists and valid
     */
    public boolean containsKey(@NotNull String key) {
        CacheEntry<?> entry = cache.get(key);
        if (entry == null) {
            return false;
        }

        if (entry.isExpired()) {
            cache.remove(key);
            evictions++;
            return false;
        }

        return true;
    }

    /**
     * Remove key from cache
     *
     * @param key Cache key
     * @return True if key was present
     */
    public boolean remove(@NotNull String key) {
        return cache.remove(key) != null;
    }

    /**
     * Clear all cache entries
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Get cache size
     *
     * @return Number of cached entries
     */
    public int size() {
        return cache.size();
    }

    /**
     * Cleanup expired entries
     */
    public void cleanup() {
        List<String> expiredKeys = new ArrayList<>();

        for (Map.Entry<String, CacheEntry<?>> entry : cache.entrySet()) {
            if (entry.getValue().isExpired()) {
                expiredKeys.add(entry.getKey());
            }
        }

        for (String key : expiredKeys) {
            cache.remove(key);
            evictions++;
        }

        if (!expiredKeys.isEmpty()) {
            plugin.getLogger().fine("Cleaned up " + expiredKeys.size() + " expired cache entries");
        }
    }

    /**
     * Cleanup player-specific data
     *
     * @param playerUUID Player UUID
     */
    public void cleanupPlayerData(@NotNull UUID playerUUID) {
        String playerPrefix = "player:" + playerUUID.toString();
        List<String> keysToRemove = cache.keySet().stream()
                .filter(key -> key.startsWith(playerPrefix))
                .toList();

        for (String key : keysToRemove) {
            cache.remove(key);
        }
    }

    /**
     * Save cache to disk
     */
    public void saveToDisk() {
        try {
            File cacheFile = new File(plugin.getDataFolder(), "cache.json");
            Map<String, Object> serializable = new HashMap<>();

            for (Map.Entry<String, CacheEntry<?>> entry : cache.entrySet()) {
                CacheEntry<?> cacheEntry = entry.getValue();
                if (!cacheEntry.isExpired()) {
                    Map<String, Object> entryData = new HashMap<>();
                    entryData.put("value", cacheEntry.getValue());
                    entryData.put("creationTime", cacheEntry.creationTime);
                    entryData.put("ttl", cacheEntry.ttl);
                    serializable.put(entry.getKey(), entryData);
                }
            }

            try (FileWriter writer = new FileWriter(cacheFile)) {
                gson.toJson(serializable, writer);
            }

            plugin.getLogger().fine("Cache saved to disk");

        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save cache to disk", e);
        }
    }

    /**
     * Load cache from disk
     */
    public void loadFromDisk() {
        try {
            File cacheFile = new File(plugin.getDataFolder(), "cache.json");
            if (!cacheFile.exists()) {
                return;
            }

            try (FileReader reader = new FileReader(cacheFile)) {
                Map<?, ?> data = gson.fromJson(reader, Map.class);

                for (Map.Entry<?, ?> entry : data.entrySet()) {
                    String key = entry.getKey().toString();
                    Map<?, ?> entryData = (Map<?, ?>) entry.getValue();

                    Object value = entryData.get("value");
                    long creationTime = ((Number) entryData.get("creationTime")).longValue();
                    long ttl = ((Number) entryData.get("ttl")).longValue();

                    // Check if still valid
                    if (ttl <= 0 || (System.currentTimeMillis() - creationTime) < ttl) {
                        cache.put(key, new CacheEntry<>(value, ttl));
                    }
                }
            }

            plugin.getLogger().fine("Cache loaded from disk");

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load cache from disk", e);
        }
    }

    /**
     * Get cache statistics
     *
     * @return Cache statistics
     */
    @NotNull
    public CacheStats getStatistics() {
        long total = hits + misses;
        double hitRate = total > 0 ? (double) hits / total * 100 : 0;

        return new CacheStats(hits, misses, evictions, hitRate, cache.size());
    }

    /**
     * Check if cache service is healthy
     *
     * @return True if healthy
     */
    public boolean isHealthy() {
        return cleanupTask != null && !cleanupTask.isCancelled();
    }

    /**
     * Shutdown the cache service
     */
    public void shutdown() {
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel();
        }

        saveToDisk();
        clear();

        plugin.getLogger().info("CacheService shutdown completed!");
    }

    /**
     * Cache statistics record
     */
    public record CacheStats(long hits, long misses, long evictions, double hitRate, int size) {}
}
