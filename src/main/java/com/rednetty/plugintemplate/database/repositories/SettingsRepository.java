package com.rednetty.plugintemplate.database.repositories;

import com.rednetty.plugintemplate.database.DatabaseManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException; /**
 * SettingsRepository - Database operations for plugin settings
 *
 * Features:
 * - Key-value storage for settings
 * - Type-safe operations
 * - Bulk operations
 * - Setting validation
 *
 * @author RedNetty
 * @version 1.0.0
 */
public class SettingsRepository {

    private final DatabaseManager databaseManager;

    // Prepared SQL statements
    private static final String SELECT_SETTING =
            "SELECT setting_value, setting_type FROM plugin_settings WHERE setting_key = ?";

    private static final String INSERT_SETTING =
            "INSERT INTO plugin_settings (setting_key, setting_value, setting_type, description, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

    private static final String UPDATE_SETTING =
            "UPDATE plugin_settings SET setting_value = ?, setting_type = ?, updated_at = CURRENT_TIMESTAMP " +
                    "WHERE setting_key = ?";

    private static final String DELETE_SETTING =
            "DELETE FROM plugin_settings WHERE setting_key = ?";

    private static final String SELECT_ALL_SETTINGS =
            "SELECT setting_key, setting_value, setting_type FROM plugin_settings";

    public SettingsRepository(@NotNull DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Get setting value
     *
     * @param key Setting key
     * @param type Expected value type
     * @param <T> Value type
     * @return Setting value or null if not found
     * @throws SQLException If database error occurs
     */
    @Nullable
    public <T> T getSetting(@NotNull String key, @NotNull Class<T> type) throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(SELECT_SETTING)) {

            stmt.setString(1, key);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String value = rs.getString("setting_value");
                    String valueType = rs.getString("setting_type");

                    return parseSettingValue(value, valueType, type);
                }
            }
        }

        return null;
    }

    /**
     * Set setting value
     *
     * @param key Setting key
     * @param value Setting value
     * @param description Setting description
     * @throws SQLException If database error occurs
     */
    public void setSetting(@NotNull String key, @NotNull Object value, @Nullable String description) throws SQLException {
        // Check if setting exists
        Object existing = getSetting(key, Object.class);

        if (existing == null) {
            insertSetting(key, value, description);
        } else {
            updateSetting(key, value);
        }
    }

    /**
     * Insert new setting
     *
     * @param key Setting key
     * @param value Setting value
     * @param description Setting description
     * @throws SQLException If database error occurs
     */
    private void insertSetting(@NotNull String key, @NotNull Object value, @Nullable String description) throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(INSERT_SETTING)) {

            stmt.setString(1, key);
            stmt.setString(2, value.toString());
            stmt.setString(3, getValueType(value));
            stmt.setString(4, description);

            stmt.executeUpdate();
        }
    }

    /**
     * Update existing setting
     *
     * @param key Setting key
     * @param value New setting value
     * @throws SQLException If database error occurs
     */
    private void updateSetting(@NotNull String key, @NotNull Object value) throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(UPDATE_SETTING)) {

            stmt.setString(1, value.toString());
            stmt.setString(2, getValueType(value));
            stmt.setString(3, key);

            stmt.executeUpdate();
        }
    }

    /**
     * Delete setting
     *
     * @param key Setting key
     * @throws SQLException If database error occurs
     */
    public void deleteSetting(@NotNull String key) throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(DELETE_SETTING)) {

            stmt.setString(1, key);
            stmt.executeUpdate();
        }
    }

    /**
     * Get all settings
     *
     * @return Map of all settings
     * @throws SQLException If database error occurs
     */
    @NotNull
    public java.util.Map<String, Object> getAllSettings() throws SQLException {
        java.util.Map<String, Object> settings = new java.util.HashMap<>();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(SELECT_ALL_SETTINGS);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String key = rs.getString("setting_key");
                String value = rs.getString("setting_value");
                String type = rs.getString("setting_type");

                Object parsedValue = parseSettingValue(value, type, Object.class);
                settings.put(key, parsedValue);
            }
        }

        return settings;
    }

    /**
     * Parse setting value based on type
     *
     * @param value String value
     * @param valueType Value type
     * @param expectedType Expected return type
     * @param <T> Return type
     * @return Parsed value
     */
    @SuppressWarnings("unchecked")
    @Nullable
    private <T> T parseSettingValue(@NotNull String value, @NotNull String valueType, @NotNull Class<T> expectedType) {
        try {
            Object parsed = switch (valueType.toUpperCase()) {
                case "STRING" -> value;
                case "INTEGER" -> Integer.parseInt(value);
                case "LONG" -> Long.parseLong(value);
                case "DOUBLE" -> Double.parseDouble(value);
                case "BOOLEAN" -> Boolean.parseBoolean(value);
                default -> value;
            };

            if (expectedType.isInstance(parsed)) {
                return (T) parsed;
            }
        } catch (NumberFormatException e) {
            // Return null if parsing fails
        }

        return null;
    }

    /**
     * Get value type string
     *
     * @param value Value object
     * @return Type string
     */
    @NotNull
    private String getValueType(@NotNull Object value) {
        if (value instanceof String) return "STRING";
        if (value instanceof Integer) return "INTEGER";
        if (value instanceof Long) return "LONG";
        if (value instanceof Double) return "DOUBLE";
        if (value instanceof Boolean) return "BOOLEAN";
        return "STRING";
    }
}
