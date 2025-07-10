package com.rednetty.plugintemplate.database.repositories;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rednetty.plugintemplate.core.managers.PlayerDataManager;
import com.rednetty.plugintemplate.database.DatabaseManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.UUID;

/**
 * PlayerDataRepository - Database operations for player data
 *
 * Features:
 * - CRUD operations for player data
 * - JSON serialization/deserialization
 * - Prepared statement optimization
 * - Transaction support
 * - Connection pooling
 *
 * @author RedNetty
 * @version 1.0.0
 */
public class PlayerDataRepository {

    private final DatabaseManager databaseManager;

    // Prepared SQL statements
    private static final String SELECT_PLAYER_DATA =
            "SELECT * FROM player_data WHERE uuid = ?";

    private static final String INSERT_PLAYER_DATA =
            "INSERT INTO player_data (uuid, username, first_join, last_join, play_time, data, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

    private static final String UPDATE_PLAYER_DATA =
            "UPDATE player_data SET username = ?, last_join = ?, play_time = ?, data = ?, updated_at = CURRENT_TIMESTAMP " +
                    "WHERE uuid = ?";

    private static final String DELETE_PLAYER_DATA =
            "DELETE FROM player_data WHERE uuid = ?";

    private static final String COUNT_PLAYERS =
            "SELECT COUNT(*) FROM player_data";

    public PlayerDataRepository(@NotNull DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Get player data by UUID
     *
     * @param playerUUID Player UUID
     * @return Player data or null if not found
     * @throws SQLException If database error occurs
     */
    @Nullable
    public PlayerDataManager.PlayerData getPlayerData(@NotNull UUID playerUUID) throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(SELECT_PLAYER_DATA)) {

            stmt.setString(1, playerUUID.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return parsePlayerData(rs);
                }
            }
        }

        return null;
    }

    /**
     * Save player data to database
     *
     * @param playerData Player data to save
     * @throws SQLException If database error occurs
     */
    public void savePlayerData(@NotNull PlayerDataManager.PlayerData playerData) throws SQLException {
        // Check if player exists
        PlayerDataManager.PlayerData existing = getPlayerData(playerData.getUUID());

        if (existing == null) {
            insertPlayerData(playerData);
        } else {
            updatePlayerData(playerData);
        }
    }

    /**
     * Insert new player data
     *
     * @param playerData Player data to insert
     * @throws SQLException If database error occurs
     */
    private void insertPlayerData(@NotNull PlayerDataManager.PlayerData playerData) throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(INSERT_PLAYER_DATA)) {

            stmt.setString(1, playerData.getUUID().toString());
            stmt.setString(2, playerData.getUsername());
            stmt.setLong(3, playerData.getFirstJoin());
            stmt.setLong(4, playerData.getLastJoin());
            stmt.setLong(5, playerData.getTotalPlayTime());
            stmt.setString(6, playerData.toJson().toString());

            stmt.executeUpdate();
        }
    }

    /**
     * Update existing player data
     *
     * @param playerData Player data to update
     * @throws SQLException If database error occurs
     */
    private void updatePlayerData(@NotNull PlayerDataManager.PlayerData playerData) throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(UPDATE_PLAYER_DATA)) {

            stmt.setString(1, playerData.getUsername());
            stmt.setLong(2, playerData.getLastJoin());
            stmt.setLong(3, playerData.getTotalPlayTime());
            stmt.setString(4, playerData.toJson().toString());
            stmt.setString(5, playerData.getUUID().toString());

            stmt.executeUpdate();
        }
    }

    /**
     * Delete player data
     *
     * @param playerUUID Player UUID
     * @throws SQLException If database error occurs
     */
    public void deletePlayerData(@NotNull UUID playerUUID) throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(DELETE_PLAYER_DATA)) {

            stmt.setString(1, playerUUID.toString());
            stmt.executeUpdate();
        }
    }

    /**
     * Get total player count
     *
     * @return Total number of players
     * @throws SQLException If database error occurs
     */
    public int getPlayerCount() throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(COUNT_PLAYERS);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        return 0;
    }

    /**
     * Parse player data from ResultSet
     *
     * @param rs ResultSet containing player data
     * @return Parsed PlayerData object
     * @throws SQLException If database error occurs
     */
    @NotNull
    private PlayerDataManager.PlayerData parsePlayerData(@NotNull ResultSet rs) throws SQLException {
        String uuidString = rs.getString("uuid");
        String jsonData = rs.getString("data");

        UUID uuid = UUID.fromString(uuidString);

        if (jsonData != null && !jsonData.isEmpty()) {
            JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();
            return PlayerDataManager.PlayerData.fromJson(jsonObject);
        } else {
            // Fallback for legacy data
            String username = rs.getString("username");
            PlayerDataManager.PlayerData data = new PlayerDataManager.PlayerData(uuid, username);
            data.setLastJoin(rs.getLong("last_join"));
            data.setTotalPlayTime(rs.getLong("play_time"));
            return data;
        }
    }
}
