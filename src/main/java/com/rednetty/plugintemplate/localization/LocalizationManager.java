package com.rednetty.plugintemplate.localization;

import com.rednetty.plugintemplate.PluginTemplate;
import com.rednetty.plugintemplate.utils.ColorUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Comprehensive localization manager for multi-language support
 *
 * Features:
 * - Support for multiple languages with fallback
 * - Player-specific language preferences
 * - Placeholder replacement with complex patterns
 * - Hot-reloading of language files
 * - Automatic creation of missing language files
 * - Nested key support with dot notation
 * - List message support
 * - Color code translation
 * - Variable interpolation
 * - Pluralization support
 *
 * @author RedNetty
 * @version 1.0.0
 */
public class LocalizationManager {

    private final PluginTemplate plugin;
    private final String defaultLanguage;
    private final File languageFolder;

    // Language configurations
    private final Map<String, FileConfiguration> languageConfigs = new ConcurrentHashMap<>();
    private final Map<String, Long> languageLoadTimes = new ConcurrentHashMap<>();

    // Player language preferences
    private final Map<String, String> playerLanguages = new ConcurrentHashMap<>();

    // Available languages
    private final Map<String, String> availableLanguages = new HashMap<>();

    // Placeholder pattern for variable replacement
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([^%]+)%");
    private static final Pattern ADVANCED_PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)}");

    // Built-in language files to extract
    private static final String[] DEFAULT_LANGUAGES = {
            "en.yml", "es.yml", "fr.yml", "de.yml", "zh.yml", "ja.yml", "ru.yml"
    };

    /**
     * Constructor for LocalizationManager
     *
     * @param plugin The plugin instance
     * @param defaultLanguage Default language code (e.g., "en")
     */
    public LocalizationManager(@NotNull PluginTemplate plugin, @NotNull String defaultLanguage) {
        this.plugin = plugin;
        this.defaultLanguage = defaultLanguage;
        this.languageFolder = new File(plugin.getDataFolder(), "lang");

        initialize();
    }

    /**
     * Initialize the localization system
     */
    private void initialize() {
        plugin.getLogger().info("Initializing localization system...");

        try {
            // Create language folder
            createLanguageFolder();

            // Extract default language files
            extractDefaultLanguageFiles();

            // Load available languages
            loadAvailableLanguages();

            // Load language configurations
            loadLanguageConfigurations();

            plugin.getLogger().info("Localization system initialized with " +
                    languageConfigs.size() + " languages");

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize localization system", e);
        }
    }

    /**
     * Create the language folder if it doesn't exist
     */
    private void createLanguageFolder() {
        if (!languageFolder.exists()) {
            if (!languageFolder.mkdirs()) {
                plugin.getLogger().warning("Failed to create language folder");
            }
        }
    }

    /**
     * Extract default language files from the plugin JAR
     */
    private void extractDefaultLanguageFiles() {
        for (String langFile : DEFAULT_LANGUAGES) {
            File targetFile = new File(languageFolder, langFile);

            if (!targetFile.exists()) {
                try (InputStream inputStream = plugin.getResource("lang/" + langFile)) {
                    if (inputStream != null) {
                        Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        plugin.getLogger().info("Extracted language file: " + langFile);
                    }
                } catch (IOException e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to extract language file: " + langFile, e);
                }
            }
        }
    }

    /**
     * Load available languages and their display names
     */
    private void loadAvailableLanguages() {
        // Default mappings
        availableLanguages.put("en", "English");
        availableLanguages.put("es", "Español");
        availableLanguages.put("fr", "Français");
        availableLanguages.put("de", "Deutsch");
        availableLanguages.put("zh", "中文");
        availableLanguages.put("ja", "日本語");
        availableLanguages.put("ru", "Русский");

        // Scan for additional language files
        File[] langFiles = languageFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (langFiles != null) {
            for (File langFile : langFiles) {
                String langCode = langFile.getName().replace(".yml", "");
                if (!availableLanguages.containsKey(langCode)) {
                    availableLanguages.put(langCode, langCode.toUpperCase());
                }
            }
        }
    }

    /**
     * Load all language configurations
     */
    private void loadLanguageConfigurations() {
        for (String langCode : availableLanguages.keySet()) {
            loadLanguageConfiguration(langCode);
        }
    }

    /**
     * Load a specific language configuration
     *
     * @param langCode The language code
     */
    private void loadLanguageConfiguration(@NotNull String langCode) {
        File langFile = new File(languageFolder, langCode + ".yml");

        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file not found: " + langCode + ".yml");
            return;
        }

        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(langFile);
            languageConfigs.put(langCode, config);
            languageLoadTimes.put(langCode, System.currentTimeMillis());

            plugin.getLogger().fine("Loaded language configuration: " + langCode);

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load language configuration: " + langCode, e);
        }
    }

    /**
     * Get a localized message for a player
     *
     * @param player The player (null for default language)
     * @param key The message key
     * @param fallback Fallback message if key not found
     * @param placeholders Placeholder replacements
     * @return The localized message
     */
    @NotNull
    public String getMessage(@Nullable Player player, @NotNull String key, @NotNull String fallback,
                             @NotNull Map<String, String> placeholders) {
        String language = getPlayerLanguage(player);
        return getMessage(language, key, fallback, placeholders);
    }

    /**
     * Get a localized message for a specific language
     *
     * @param language The language code
     * @param key The message key
     * @param fallback Fallback message if key not found
     * @param placeholders Placeholder replacements
     * @return The localized message
     */
    @NotNull
    public String getMessage(@NotNull String language, @NotNull String key, @NotNull String fallback,
                             @NotNull Map<String, String> placeholders) {
        String message = getMessage(language, key, fallback);
        return replacePlaceholders(message, placeholders);
    }

    /**
     * Get a simple localized message
     *
     * @param key The message key
     * @param fallback Fallback message if key not found
     * @return The localized message
     */
    @NotNull
    public String getMessage(@NotNull String key, @NotNull String fallback) {
        return getMessage(defaultLanguage, key, fallback);
    }

    /**
     * Get a localized message for a specific language
     *
     * @param language The language code
     * @param key The message key
     * @param fallback Fallback message if key not found
     * @return The localized message
     */
    @NotNull
    public String getMessage(@NotNull String language, @NotNull String key, @NotNull String fallback) {
        // Try to get from specified language
        String message = getMessageFromConfig(language, key);

        if (message != null) {
            return ColorUtils.colorize(message);
        }

        // Try default language fallback
        if (!language.equals(defaultLanguage)) {
            message = getMessageFromConfig(defaultLanguage, key);
            if (message != null) {
                return ColorUtils.colorize(message);
            }
        }

        // Return fallback
        return ColorUtils.colorize(fallback);
    }

    /**
     * Get a message from a specific language configuration
     *
     * @param language The language code
     * @param key The message key
     * @return The message or null if not found
     */
    @Nullable
    private String getMessageFromConfig(@NotNull String language, @NotNull String key) {
        FileConfiguration config = languageConfigs.get(language);
        if (config == null) {
            return null;
        }

        // Support nested keys with dot notation
        return config.getString(key);
    }

    /**
     * Get a list of localized messages
     *
     * @param language The language code
     * @param key The message key
     * @param fallback Fallback list if key not found
     * @return The list of localized messages
     */
    @NotNull
    public List<String> getMessageList(@NotNull String language, @NotNull String key, @NotNull List<String> fallback) {
        FileConfiguration config = languageConfigs.get(language);

        if (config != null && config.isList(key)) {
            List<String> messages = config.getStringList(key);
            return messages.stream()
                    .map(ColorUtils::colorize)
                    .toList();
        }

        // Try default language fallback
        if (!language.equals(defaultLanguage)) {
            config = languageConfigs.get(defaultLanguage);
            if (config != null && config.isList(key)) {
                List<String> messages = config.getStringList(key);
                return messages.stream()
                        .map(ColorUtils::colorize)
                        .toList();
            }
        }

        // Return fallback
        return fallback.stream()
                .map(ColorUtils::colorize)
                .toList();
    }

    /**
     * Replace placeholders in a message
     *
     * @param message The message with placeholders
     * @param placeholders The placeholder replacements
     * @return The message with placeholders replaced
     */
    @NotNull
    public String replacePlaceholders(@NotNull String message, @NotNull Map<String, String> placeholders) {
        String result = message;

        // Replace simple placeholders %key%
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(result);
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = placeholders.get(key);
            if (replacement != null) {
                result = result.replace("%" + key + "%", replacement);
            }
        }

        // Replace advanced placeholders {key}
        matcher = ADVANCED_PLACEHOLDER_PATTERN.matcher(result);
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = placeholders.get(key);
            if (replacement != null) {
                result = result.replace("{" + key + "}", replacement);
            }
        }

        return result;
    }

    /**
     * Get a player's preferred language
     *
     * @param player The player (null for default)
     * @return The language code
     */
    @NotNull
    public String getPlayerLanguage(@Nullable Player player) {
        if (player == null) {
            return defaultLanguage;
        }

        return playerLanguages.getOrDefault(player.getUniqueId().toString(), defaultLanguage);
    }

    /**
     * Set a player's preferred language
     *
     * @param player The player
     * @param language The language code
     */
    public void setPlayerLanguage(@NotNull Player player, @NotNull String language) {
        if (availableLanguages.containsKey(language)) {
            playerLanguages.put(player.getUniqueId().toString(), language);

            // Save to database or config if needed
            savePlayerLanguage(player, language);

            plugin.getLogger().fine("Set language for " + player.getName() + " to " + language);
        } else {
            plugin.getLogger().warning("Unknown language code: " + language);
        }
    }

    /**
     * Save player language preference (implement based on your storage method)
     *
     * @param player The player
     * @param language The language code
     */
    private void savePlayerLanguage(@NotNull Player player, @NotNull String language) {
        // Implementation depends on your data storage method
        // This could save to database, config file, etc.

        // Example using plugin config:
        plugin.getConfigManager().getConfig().set("player-languages." + player.getUniqueId(), language);
        plugin.getConfigManager().saveConfig();
    }

    /**
     * Load player language preference (implement based on your storage method)
     *
     * @param player The player
     */
    public void loadPlayerLanguage(@NotNull Player player) {
        // Implementation depends on your data storage method

        // Example using plugin config:
        String language = plugin.getConfigManager().getConfig()
                .getString("player-languages." + player.getUniqueId(), defaultLanguage);

        if (availableLanguages.containsKey(language)) {
            playerLanguages.put(player.getUniqueId().toString(), language);
        }
    }

    /**
     * Get pluralized message based on count
     *
     * @param language The language code
     * @param key The base message key
     * @param count The count for pluralization
     * @param fallback Fallback message
     * @return The pluralized message
     */
    @NotNull
    public String getPluralizedMessage(@NotNull String language, @NotNull String key, int count, @NotNull String fallback) {
        String pluralKey = key + "." + (count == 1 ? "singular" : "plural");
        String message = getMessage(language, pluralKey, fallback);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("count", String.valueOf(count));

        return replacePlaceholders(message, placeholders);
    }

    /**
     * Get all available languages
     *
     * @return Map of language codes to display names
     */
    @NotNull
    public Map<String, String> getAvailableLanguages() {
        return new HashMap<>(availableLanguages);
    }

    /**
     * Check if a language is available
     *
     * @param language The language code
     * @return True if available
     */
    public boolean isLanguageAvailable(@NotNull String language) {
        return availableLanguages.containsKey(language);
    }

    /**
     * Get the default language
     *
     * @return The default language code
     */
    @NotNull
    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    /**
     * Reload all language configurations
     */
    public void reload() {
        plugin.getLogger().info("Reloading localization system...");

        try {
            // Clear current configurations
            languageConfigs.clear();
            languageLoadTimes.clear();

            // Reload available languages
            loadAvailableLanguages();

            // Reload configurations
            loadLanguageConfigurations();

            plugin.getLogger().info("Localization system reloaded successfully");

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to reload localization system", e);
        }
    }

    /**
     * Get localization statistics
     *
     * @return Localization statistics
     */
    @NotNull
    public LocalizationStats getStatistics() {
        int totalKeys = 0;
        for (FileConfiguration config : languageConfigs.values()) {
            totalKeys += config.getKeys(true).size();
        }

        return new LocalizationStats(
                languageConfigs.size(),
                totalKeys,
                playerLanguages.size(),
                defaultLanguage
        );
    }

    /**
     * Localization statistics record
     */
    public record LocalizationStats(
            int totalLanguages,
            int totalKeys,
            int playersWithCustomLanguage,
            String defaultLanguage
    ) {}
}