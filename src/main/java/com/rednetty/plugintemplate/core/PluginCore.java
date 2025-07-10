package com.rednetty.plugintemplate.core;

import com.rednetty.plugintemplate.PluginTemplate;
import com.rednetty.plugintemplate.core.managers.DataManager;
import com.rednetty.plugintemplate.core.managers.PermissionManager;
import com.rednetty.plugintemplate.core.managers.PlayerDataManager;
import com.rednetty.plugintemplate.core.registry.CommandRegistry;
import com.rednetty.plugintemplate.core.registry.ListenerRegistry;
import com.rednetty.plugintemplate.core.services.CacheService;
import com.rednetty.plugintemplate.core.services.NotificationService;
import com.rednetty.plugintemplate.core.services.ValidationService;
import com.rednetty.plugintemplate.exceptions.PluginException;
import com.rednetty.plugintemplate.utils.ComponentBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

/**
 * Core plugin functionality handler
 *
 * This class manages the central operations of the plugin including:
 * - Manager initialization and lifecycle
 * - Service coordination
 * - Data persistence
 * - Player management
 * - System health monitoring
 *
 * @author RedNetty
 * @version 1.0.0
 */
public class PluginCore {

    private final PluginTemplate plugin;

    // Core Managers
    private DataManager dataManager;
    private PlayerDataManager playerDataManager;
    private PermissionManager permissionManager;

    // Registries
    private CommandRegistry commandRegistry;
    private ListenerRegistry listenerRegistry;

    // Services
    private CacheService cacheService;
    private NotificationService notificationService;
    private ValidationService validationService;

    // State Management
    private final ConcurrentMap<String, Object> runtimeData;
    private boolean initialized = false;
    private long initializationTime;

    /**
     * Constructs a new PluginCore instance
     *
     * @param plugin The main plugin instance
     */
    public PluginCore(@NotNull PluginTemplate plugin) {
        this.plugin = plugin;
        this.runtimeData = new ConcurrentHashMap<>();
        this.initializationTime = System.currentTimeMillis();
    }

    /**
     * Initialize all core systems
     *
     * @throws PluginException If initialization fails
     */
    public void initialize() throws PluginException {
        plugin.getLogger().info("Initializing PluginCore...");

        try {
            // Initialize managers
            initializeManagers();

            // Initialize registries
            initializeRegistries();

            // Initialize services
            initializeServices();

            // Register services with service manager
            registerServices();

            // Perform post-initialization tasks
            postInitialize();

            this.initialized = true;

            long duration = System.currentTimeMillis() - initializationTime;
            plugin.getLogger().info(String.format("PluginCore initialized successfully! (took %dms)", duration));

        } catch (Exception e) {
            throw new PluginException("Failed to initialize PluginCore", e);
        }
    }

    /**
     * Initialize all managers
     */
    private void initializeManagers() {
        plugin.getLogger().info("Initializing managers...");

        // Initialize data manager
        this.dataManager = new DataManager(plugin);
        dataManager.initialize();

        // Initialize player data manager
        this.playerDataManager = new PlayerDataManager(plugin);
        playerDataManager.initialize();

        // Initialize permission manager
        this.permissionManager = new PermissionManager(plugin);
        permissionManager.initialize();
    }

    /**
     * Initialize all registries
     */
    private void initializeRegistries() {
        plugin.getLogger().info("Initializing registries...");

        // Initialize command registry
        this.commandRegistry = new CommandRegistry(plugin);
        commandRegistry.initialize();

        // Initialize listener registry
        this.listenerRegistry = new ListenerRegistry(plugin);
        listenerRegistry.initialize();
    }

    /**
     * Initialize all services
     */
    private void initializeServices() {
        plugin.getLogger().info("Initializing services...");

        // Initialize cache service
        this.cacheService = new CacheService(plugin);
        cacheService.initialize();

        // Initialize notification service
        this.notificationService = new NotificationService(plugin);
        notificationService.initialize();

        // Initialize validation service
        this.validationService = new ValidationService(plugin);
        validationService.initialize();
    }

    /**
     * Register all services with the service manager
     */
    private void registerServices() {
        var serviceManager = plugin.getServiceManager();

        // Register managers
        serviceManager.registerService(DataManager.class, dataManager);
        serviceManager.registerService(PlayerDataManager.class, playerDataManager);
        serviceManager.registerService(PermissionManager.class, permissionManager);

        // Register registries
        serviceManager.registerService(CommandRegistry.class, commandRegistry);
        serviceManager.registerService(ListenerRegistry.class, listenerRegistry);

        // Register services
        serviceManager.registerService(CacheService.class, cacheService);
        serviceManager.registerService(NotificationService.class, notificationService);
        serviceManager.registerService(ValidationService.class, validationService);
    }

    /**
     * Perform post-initialization tasks
     */
    private void postInitialize() {
        plugin.getLogger().info("Performing post-initialization tasks...");

        // Schedule health checks
        scheduleHealthChecks();

        // Setup runtime monitoring
        setupRuntimeMonitoring();

        // Load cached data
        loadCachedData();
    }

    /**
     * Schedule periodic health checks
     */
    private void scheduleHealthChecks() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                performHealthCheck();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Health check failed", e);
            }
        }, 20L * 60L, 20L * 60L * 5L); // Every 5 minutes
    }

    /**
     * Setup runtime monitoring
     */
    private void setupRuntimeMonitoring() {
        // Monitor memory usage
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;

            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;

            runtimeData.put("memory_usage_percent", memoryUsagePercent);
            runtimeData.put("memory_used_mb", usedMemory / 1024 / 1024);
            runtimeData.put("memory_max_mb", maxMemory / 1024 / 1024);

            // Log warning if memory usage is high
            if (memoryUsagePercent > 85) {
                plugin.getLogger().warning(String.format("High memory usage detected: %.2f%%", memoryUsagePercent));
            }

        }, 20L * 30L, 20L * 30L); // Every 30 seconds
    }

    /**
     * Load cached data from previous session
     */
    private void loadCachedData() {
        CompletableFuture.runAsync(() -> {
            try {
                cacheService.loadFromDisk();
                plugin.getLogger().info("Cached data loaded successfully!");
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load cached data", e);
            }
        });
    }

    /**
     * Perform a comprehensive health check
     */
    public void performHealthCheck() {
        plugin.getLogger().fine("Performing health check...");

        // Check database connectivity
        if (plugin.getDatabaseManager() != null) {
            boolean dbHealthy = plugin.getDatabaseManager().isHealthy();
            runtimeData.put("database_healthy", dbHealthy);

            if (!dbHealthy) {
                plugin.getLogger().warning("Database health check failed!");
            }
        }

        // Check service health
        boolean servicesHealthy = checkServicesHealth();
        runtimeData.put("services_healthy", servicesHealthy);

        // Check player data integrity
        boolean playerDataHealthy = playerDataManager.validateIntegrity();
        runtimeData.put("player_data_healthy", playerDataHealthy);

        // Update last health check time
        runtimeData.put("last_health_check", System.currentTimeMillis());

        plugin.getLogger().fine("Health check completed successfully!");
    }

    /**
     * Check the health of all services
     *
     * @return True if all services are healthy
     */
    private boolean checkServicesHealth() {
        try {
            return cacheService.isHealthy() &&
                    notificationService.isHealthy() &&
                    validationService.isHealthy();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Service health check failed", e);
            return false;
        }
    }

    /**
     * Handle player joining the server
     *
     * @param player The player who joined
     */
    public void handlePlayerJoin(@NotNull Player player) {
        CompletableFuture.runAsync(() -> {
            try {
                // Load player data
                playerDataManager.loadPlayerData(player);

                // Apply permissions
                permissionManager.applyPlayerPermissions(player);

                // Send welcome message if configured
                sendWelcomeMessage(player);

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "Failed to handle player join: " + player.getName(), e);
            }
        });
    }

    /**
     * Handle player leaving the server
     *
     * @param player The player who left
     */
    public void handlePlayerQuit(@NotNull Player player) {
        CompletableFuture.runAsync(() -> {
            try {
                // Save player data
                playerDataManager.savePlayerData(player);

                // Clean up cache
                cacheService.cleanupPlayerData(player.getUniqueId());

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "Failed to handle player quit: " + player.getName(), e);
            }
        });
    }

    /**
     * Send welcome message to player
     *
     * @param player The player to send the message to
     */
    private void sendWelcomeMessage(@NotNull Player player) {
        if (plugin.getConfigManager().getConfig().getBoolean("messages.welcome.enabled", true)) {
            String welcomeKey = "messages.welcome.text";
            String fallback = "&aWelcome to the server, %player_name%!";

            Component message = ComponentBuilder.create(plugin)
                    .fromLocalization(welcomeKey, fallback)
                    .withPlaceholder("player_name", player.getName())
                    .build();

            notificationService.sendMessage(player, message);
        }
    }

    /**
     * Get runtime data value
     *
     * @param key The data key
     * @param <T> The value type
     * @return The value, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getRuntimeData(@NotNull String key) {
        return (T) runtimeData.get(key);
    }

    /**
     * Set runtime data value
     *
     * @param key The data key
     * @param value The value to set
     */
    public void setRuntimeData(@NotNull String key, Object value) {
        runtimeData.put(key, value);
    }

    /**
     * Shutdown all core systems
     */
    public void shutdown() {
        plugin.getLogger().info("Shutting down PluginCore...");

        try {
            // Save all data
            saveAllData();

            // Shutdown services
            shutdownServices();

            // Shutdown managers
            shutdownManagers();

            // Clear runtime data
            runtimeData.clear();

            this.initialized = false;

            plugin.getLogger().info("PluginCore shutdown completed!");

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error during PluginCore shutdown", e);
        }
    }

    /**
     * Save all data before shutdown
     */
    private void saveAllData() {
        plugin.getLogger().info("Saving all data...");

        // Save player data
        if (playerDataManager != null) {
            playerDataManager.saveAllData();
        }

        // Save cache to disk
        if (cacheService != null) {
            cacheService.saveToDisk();
        }

        // Save data manager data
        if (dataManager != null) {
            dataManager.saveAll();
        }
    }

    /**
     * Shutdown all services
     */
    private void shutdownServices() {
        if (cacheService != null) {
            cacheService.shutdown();
        }

        if (notificationService != null) {
            notificationService.shutdown();
        }

        if (validationService != null) {
            validationService.shutdown();
        }
    }

    /**
     * Shutdown all managers
     */
    private void shutdownManagers() {
        if (playerDataManager != null) {
            playerDataManager.shutdown();
        }

        if (permissionManager != null) {
            permissionManager.shutdown();
        }

        if (dataManager != null) {
            dataManager.shutdown();
        }
    }

    /**
     * Check if the core is initialized
     *
     * @return True if initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Get the main plugin instance
     *
     * @return The plugin instance
     */
    @NotNull
    public PluginTemplate getPlugin() {
        return plugin;
    }

    /**
     * Get the data manager
     *
     * @return The data manager
     */
    @NotNull
    public DataManager getDataManager() {
        return dataManager;
    }

    /**
     * Get the player data manager
     *
     * @return The player data manager
     */
    @NotNull
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    /**
     * Get the permission manager
     *
     * @return The permission manager
     */
    @NotNull
    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    /**
     * Get the cache service
     *
     * @return The cache service
     */
    @NotNull
    public CacheService getCacheService() {
        return cacheService;
    }

    /**
     * Get the notification service
     *
     * @return The notification service
     */
    @NotNull
    public NotificationService getNotificationService() {
        return notificationService;
    }

    /**
     * Get the validation service
     *
     * @return The validation service
     */
    @NotNull
    public ValidationService getValidationService() {
        return validationService;
    }

    /**
     * Get the command registry
     *
     * @return The command registry
     */
    @NotNull
    public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    /**
     * Get the listener registry
     *
     * @return The listener registry
     */
    @NotNull
    public ListenerRegistry getListenerRegistry() {
        return listenerRegistry;
    }
}