package com.rednetty.plugintemplate;

import com.rednetty.plugintemplate.api.PluginAPI;
import com.rednetty.plugintemplate.commands.impl.MainCommand;
import com.rednetty.plugintemplate.config.ConfigManager;
import com.rednetty.plugintemplate.core.PluginCore;
import com.rednetty.plugintemplate.core.services.ServiceManager;
import com.rednetty.plugintemplate.database.DatabaseManager;
import com.rednetty.plugintemplate.exceptions.PluginException;
import com.rednetty.plugintemplate.integrations.VaultIntegration;
import com.rednetty.plugintemplate.integrations.PlaceholderAPIIntegration;
import com.rednetty.plugintemplate.listeners.PlayerJoinListener;
import com.rednetty.plugintemplate.localization.LocalizationManager;
import com.rednetty.plugintemplate.tasks.AutoSaveTask;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.bstats.bukkit.Metrics;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Main plugin class for PluginTemplate
 *
 * Features:
 * - Service-oriented architecture with dependency injection
 * - Modern Adventure API integration
 * - Database abstraction with HikariCP
 * - Comprehensive configuration management
 * - Multi-language support
 * - Plugin metrics and auto-updater
 * - Integration with Vault and PlaceholderAPI
 *
 * @author RedNetty
 * @version 1.0.0
 */
public final class PluginTemplate extends JavaPlugin {

    // Core Components
    private PluginCore core;
    private ServiceManager serviceManager;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private LocalizationManager localizationManager;
    private BukkitAudiences audiences;

    // Integration Components
    private VaultIntegration vaultIntegration;
    private PlaceholderAPIIntegration placeholderAPIIntegration;

    // Tasks
    private AutoSaveTask autoSaveTask;

    // Metrics
    private Metrics metrics;

    // Plugin State
    private boolean isShuttingDown = false;

    @Override
    public void onLoad() {
        // Pre-initialization logging
        getLogger().info("Loading PluginTemplate v" + getDescription().getVersion());

        // Initialize Adventure API early
        this.audiences = BukkitAudiences.create(this);

        // Initialize service manager
        this.serviceManager = new ServiceManager();

        getLogger().info("Plugin loaded successfully!");
    }

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        try {
            // Initialize core systems
            initializeCore();

            // Load configuration
            loadConfiguration();

            // Initialize database
            initializeDatabase();

            // Initialize localization
            initializeLocalization();

            // Register services
            registerServices();

            // Register commands
            registerCommands();

            // Register listeners
            registerListeners();

            // Initialize integrations
            initializeIntegrations();

            // Start background tasks
            startBackgroundTasks();

            // Initialize metrics
            initializeMetrics();

            // Register API
            registerAPI();

            long loadTime = System.currentTimeMillis() - startTime;
            getLogger().info(String.format("PluginTemplate has been enabled successfully! (took %dms)", loadTime));

            // Send startup message to console with fancy formatting
            if (configManager.getConfig().getBoolean("plugin.debug", false)) {
                getLogger().info("Debug mode is enabled!");
            }

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable PluginTemplate!", e);
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        this.isShuttingDown = true;

        getLogger().info("Disabling PluginTemplate...");

        // Stop background tasks
        stopBackgroundTasks();

        // Shutdown integrations
        shutdownIntegrations();

        // Save all data
        saveAllData();

        // Shutdown database
        shutdownDatabase();

        // Shutdown core
        if (this.core != null) {
            this.core.shutdown();
        }

        // Close Adventure API
        if (this.audiences != null) {
            this.audiences.close();
        }

        getLogger().info("PluginTemplate has been disabled!");
    }

    /**
     * Initialize core plugin systems
     */
    private void initializeCore() throws PluginException {
        getLogger().info("Initializing core systems...");
        this.core = new PluginCore(this);
        this.core.initialize();
    }

    /**
     * Load and validate configuration
     */
    private void loadConfiguration() {
        getLogger().info("Loading configuration...");
        this.configManager = new ConfigManager(this);

        // Register config manager as a service
        serviceManager.registerService(ConfigManager.class, configManager);
    }

    /**
     * Initialize database connection and setup tables
     */
    private void initializeDatabase() {
        getLogger().info("Initializing database...");
        this.databaseManager = new DatabaseManager();

        String databaseType = configManager.getConfig().getString("database.type", "sqlite");
        String jdbcUrl;

        if ("mysql".equalsIgnoreCase(databaseType)) {
            String host = configManager.getConfig().getString("database.host", "localhost");
            int port = configManager.getConfig().getInt("database.port", 3306);
            String database = configManager.getConfig().getString("database.database", "plugintemplate");
            String username = configManager.getConfig().getString("database.username", "root");
            String password = configManager.getConfig().getString("database.password", "");

            jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC",
                    host, port, database);
            databaseManager.initialize(jdbcUrl, username, password);
        } else {
            // SQLite (default)
            String fileName = configManager.getConfig().getString("database.file", "database.db");
            jdbcUrl = "jdbc:sqlite:" + getDataFolder().getAbsolutePath() + "/" + fileName;
            databaseManager.initialize(jdbcUrl);
        }

        // Register database manager as a service
        serviceManager.registerService(DatabaseManager.class, databaseManager);

        // Setup database tables
        CompletableFuture.runAsync(() -> {
            try {
                databaseManager.setupTables();
                getLogger().info("Database tables initialized successfully!");
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to setup database tables!", e);
            }
        });
    }

    /**
     * Initialize localization system
     */
    private void initializeLocalization() {
        getLogger().info("Initializing localization...");
        String defaultLanguage = configManager.getConfig().getString("localization.default-language", "en");
        this.localizationManager = new LocalizationManager(this, defaultLanguage);

        // Register localization manager as a service
        serviceManager.registerService(LocalizationManager.class, localizationManager);
    }

    /**
     * Register all services in the service manager
     */
    private void registerServices() {
        getLogger().info("Registering services...");

        // Register core services
        serviceManager.registerService(PluginTemplate.class, this);
        serviceManager.registerService(PluginCore.class, core);
        serviceManager.registerService(BukkitAudiences.class, audiences);
        serviceManager.registerService(ServiceManager.class, serviceManager);
    }

    /**
     * Register plugin commands
     */
    private void registerCommands() {
        getLogger().info("Registering commands...");

        // Register main command
        MainCommand mainCommand = new MainCommand(this);
        getCommand("plugintemplate").setExecutor(mainCommand);
        getCommand("plugintemplate").setTabCompleter(mainCommand);
    }

    /**
     * Register event listeners
     */
    private void registerListeners() {
        getLogger().info("Registering event listeners...");

        // Register player join listener
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
    }

    /**
     * Initialize third-party plugin integrations
     */
    private void initializeIntegrations() {
        getLogger().info("Initializing integrations...");

        // Initialize Vault integration
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            this.vaultIntegration = new VaultIntegration();
            if (vaultIntegration.initialize()) {
                serviceManager.registerService(VaultIntegration.class, vaultIntegration);
                getLogger().info("Vault integration enabled!");
            } else {
                getLogger().warning("Vault integration failed to initialize!");
            }
        }

        // Initialize PlaceholderAPI integration
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.placeholderAPIIntegration = new PlaceholderAPIIntegration(this);
            if (placeholderAPIIntegration.register()) {
                serviceManager.registerService(PlaceholderAPIIntegration.class, placeholderAPIIntegration);
                getLogger().info("PlaceholderAPI integration enabled!");
            } else {
                getLogger().warning("PlaceholderAPI integration failed to initialize!");
            }
        }
    }

    /**
     * Start background tasks
     */
    private void startBackgroundTasks() {
        getLogger().info("Starting background tasks...");

        // Start auto-save task
        int autoSaveInterval = configManager.getConfig().getInt("plugin.auto-save-interval", 300); // 5 minutes default
        if (autoSaveInterval > 0) {
            this.autoSaveTask = new AutoSaveTask(this);
            autoSaveTask.runTaskTimerAsynchronously(this, 20L * autoSaveInterval, 20L * autoSaveInterval);
        }
    }

    /**
     * Initialize metrics collection
     */
    private void initializeMetrics() {
        if (configManager.getConfig().getBoolean("plugin.metrics", true)) {
            getLogger().info("Initializing metrics...");
            this.metrics = new Metrics(this, 12345); // Replace with actual bStats plugin ID

            // Add custom charts
            metrics.addCustomChart(new Metrics.SimplePie("database_type", () ->
                    configManager.getConfig().getString("database.type", "sqlite")));

            metrics.addCustomChart(new Metrics.SimplePie("language", () ->
                    configManager.getConfig().getString("localization.default-language", "en")));
        }
    }


    /**
     * Register public API
     */
    private void registerAPI() {
        PluginAPI.initialize(this);
        getLogger().info("Public API registered!");
    }

    /**
     * Stop all background tasks
     */
    private void stopBackgroundTasks() {
        if (autoSaveTask != null && !autoSaveTask.isCancelled()) {
            autoSaveTask.cancel();
        }
    }

    /**
     * Shutdown all integrations
     */
    private void shutdownIntegrations() {
        if (placeholderAPIIntegration != null) {
            placeholderAPIIntegration.unregister();
        }
    }

    /**
     * Save all plugin data
     */
    private void saveAllData() {
        getLogger().info("Saving all data...");

        // Save configuration
        if (configManager != null) {
            configManager.saveConfig();
        }

        // Perform any other data saving operations
        CompletableFuture.runAsync(() -> {
            // Save player data, etc.
        });
    }

    /**
     * Shutdown database connections
     */
    private void shutdownDatabase() {
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
    }

    /**
     * Get the plugin core instance
     *
     * @return The plugin core
     */
    @NotNull
    public PluginCore getCore() {
        return this.core;
    }

    /**
     * Get the service manager
     *
     * @return The service manager
     */
    @NotNull
    public ServiceManager getServiceManager() {
        return this.serviceManager;
    }

    /**
     * Get the configuration manager
     *
     * @return The configuration manager
     */
    @NotNull
    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    /**
     * Get the database manager
     *
     * @return The database manager
     */
    @NotNull
    public DatabaseManager getDatabaseManager() {
        return this.databaseManager;
    }

    /**
     * Get the localization manager
     *
     * @return The localization manager
     */
    @NotNull
    public LocalizationManager getLocalizationManager() {
        return this.localizationManager;
    }

    /**
     * Get the Adventure audiences instance
     *
     * @return The Adventure audiences
     */
    @NotNull
    public BukkitAudiences getAudiences() {
        return this.audiences;
    }

    /**
     * Get the Vault integration
     *
     * @return The Vault integration, or null if not available
     */
    public VaultIntegration getVaultIntegration() {
        return this.vaultIntegration;
    }

    /**
     * Get the PlaceholderAPI integration
     *
     * @return The PlaceholderAPI integration, or null if not available
     */
    public PlaceholderAPIIntegration getPlaceholderAPIIntegration() {
        return this.placeholderAPIIntegration;
    }

    /**
     * Check if the plugin is shutting down
     *
     * @return True if shutting down
     */
    public boolean isShuttingDown() {
        return this.isShuttingDown;
    }

    /**
     * Reload the plugin configuration and systems
     */
    public void reload() {
        getLogger().info("Reloading PluginTemplate...");

        // Reload configuration
        configManager.reloadConfig();

        // Reload localization
        localizationManager.reload();

        getLogger().info("Plugin reloaded successfully!");
    }
}