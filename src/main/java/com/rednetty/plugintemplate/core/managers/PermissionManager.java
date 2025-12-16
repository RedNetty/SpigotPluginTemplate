package com.rednetty.plugintemplate.core.managers;


import com.rednetty.plugintemplate.PluginTemplate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PermissionManager - Permission management system
 *
 * Features:
 * - Dynamic permission assignment
 * - Permission caching
 * - Group-based permissions
 * - Temporary permissions
 * - Permission inheritance
 *
 * @author RedNetty
 * @version 1.0.0
 */
class PermissionManager {

    private final PluginTemplate plugin;
    private final Map<UUID, PermissionAttachment> attachments = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> tempPermissions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> groupPermissions = new ConcurrentHashMap<>();

    public PermissionManager(@NotNull PluginTemplate plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize the permission manager
     */
    public void initialize() {
        plugin.getLogger().info("Initializing PermissionManager...");

        // Load group permissions from config
        loadGroupPermissions();

        plugin.getLogger().info("PermissionManager initialized successfully!");
    }

    /**
     * Load group permissions from configuration
     */
    private void loadGroupPermissions() {
        var config = plugin.getConfigManager().getConfig();
        var groupsSection = config.getConfigurationSection("permissions.groups");

        if (groupsSection != null) {
            for (String group : groupsSection.getKeys(false)) {
                List<String> permissions = groupsSection.getStringList(group + ".permissions");
                groupPermissions.put(group, new HashSet<>(permissions));
            }
        }
    }

    /**
     * Apply permissions to player
     *
     * @param player Target player
     */
    public void applyPlayerPermissions(@NotNull Player player) {
        // Create permission attachment if not exists
        PermissionAttachment attachment = attachments.computeIfAbsent(player.getUniqueId(),
                uuid -> player.addAttachment(plugin));

        // Apply group permissions if Vault is available
        if (plugin.getVaultIntegration() != null && plugin.getVaultIntegration().isPermissionEnabled()) {
            String group = plugin.getVaultIntegration().getPrimaryGroup(player);
            applyGroupPermissions(player, group);
        }

        // Apply temporary permissions
        Set<String> tempPerms = tempPermissions.get(player.getUniqueId());
        if (tempPerms != null) {
            for (String permission : tempPerms) {
                attachment.setPermission(permission, true);
            }
        }
    }

    /**
     * Apply group permissions to player
     *
     * @param player Target player
     * @param group Group name
     */
    public void applyGroupPermissions(@NotNull Player player, @NotNull String group) {
        Set<String> permissions = groupPermissions.get(group);
        if (permissions == null) {
            return;
        }

        PermissionAttachment attachment = attachments.get(player.getUniqueId());
        if (attachment == null) {
            return;
        }

        for (String permission : permissions) {
            attachment.setPermission(permission, true);
        }
    }

    /**
     * Add temporary permission to player
     *
     * @param player Target player
     * @param permission Permission to add
     */
    public void addTempPermission(@NotNull Player player, @NotNull String permission) {
        tempPermissions.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(permission);

        PermissionAttachment attachment = attachments.get(player.getUniqueId());
        if (attachment != null) {
            attachment.setPermission(permission, true);
        }
    }

    /**
     * Remove temporary permission from player
     *
     * @param player Target player
     * @param permission Permission to remove
     */
    public void removeTempPermission(@NotNull Player player, @NotNull String permission) {
        Set<String> tempPerms = tempPermissions.get(player.getUniqueId());
        if (tempPerms != null) {
            tempPerms.remove(permission);
        }

        PermissionAttachment attachment = attachments.get(player.getUniqueId());
        if (attachment != null) {
            attachment.unsetPermission(permission);
        }
    }

    /**
     * Add temporary permission with duration
     *
     * @param player Target player
     * @param permission Permission to add
     * @param durationTicks Duration in ticks
     */
    public void addTempPermission(@NotNull Player player, @NotNull String permission, long durationTicks) {
        addTempPermission(player, permission);

        // Schedule removal
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                removeTempPermission(player, permission);
            }
        }, durationTicks);
    }

    /**
     * Check if player has temporary permission
     *
     * @param player Target player
     * @param permission Permission to check
     * @return True if has temporary permission
     */
    public boolean hasTempPermission(@NotNull Player player, @NotNull String permission) {
        Set<String> tempPerms = tempPermissions.get(player.getUniqueId());
        return tempPerms != null && tempPerms.contains(permission);
    }

    /**
     * Get all temporary permissions for player
     *
     * @param player Target player
     * @return Set of temporary permissions
     */
    @NotNull
    public Set<String> getTempPermissions(@NotNull Player player) {
        return new HashSet<>(tempPermissions.getOrDefault(player.getUniqueId(), Collections.emptySet()));
    }

    /**
     * Clear all temporary permissions for player
     *
     * @param player Target player
     */
    public void clearTempPermissions(@NotNull Player player) {
        tempPermissions.remove(player.getUniqueId());

        PermissionAttachment attachment = attachments.get(player.getUniqueId());
        if (attachment != null) {
            // Remove attachment and recreate to clear all permissions
            player.removeAttachment(attachment);
            attachments.remove(player.getUniqueId());
            applyPlayerPermissions(player);
        }
    }

    /**
     * Remove player from permission system
     *
     * @param player Target player
     */
    public void removePlayer(@NotNull Player player) {
        PermissionAttachment attachment = attachments.remove(player.getUniqueId());
        if (attachment != null) {
            player.removeAttachment(attachment);
        }

        tempPermissions.remove(player.getUniqueId());
    }

    /**
     * Reload group permissions
     */
    public void reloadGroupPermissions() {
        groupPermissions.clear();
        loadGroupPermissions();

        // Reapply permissions to online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyPlayerPermissions(player);
        }
    }

    /**
     * Shutdown the permission manager
     */
    public void shutdown() {
        // Remove all attachments
        for (Player player : Bukkit.getOnlinePlayers()) {
            removePlayer(player);
        }

        attachments.clear();
        tempPermissions.clear();
        groupPermissions.clear();

        plugin.getLogger().info("PermissionManager shutdown completed!");
    }
}