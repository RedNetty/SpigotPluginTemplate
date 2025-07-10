package com.rednetty.plugintemplate.utils;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map; /**
 * PlayerUtils - Player-related utility methods
 *
 * Features:
 * - Player validation
 * - Name formatting
 * - Permission checking
 * - Location utilities
 * - Inventory management
 *
 * @author RedNetty
 * @version 1.0.0
 */
public class PlayerUtils {

    /**
     * Check if player name is valid
     *
     * @param name Player name
     * @return True if valid
     */
    public static boolean isValidPlayerName(@NotNull String name) {
        return name.matches("^[a-zA-Z0-9_]{3,16}$");
    }

    /**
     * Format player name with proper capitalization
     *
     * @param name Player name
     * @return Formatted name
     */
    @NotNull
    public static String formatPlayerName(@NotNull String name) {
        if (name.length() == 0) {
            return name;
        }

        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }

    /**
     * Get display name or regular name
     *
     * @param player OfflinePlayer
     * @return Display name
     */
    @NotNull
    public static String getDisplayName(@NotNull OfflinePlayer player) {
        if (player.getName() == null) {
            return "Unknown";
        }

        if (player.isOnline() && player.getPlayer() != null) {
            return player.getPlayer().getDisplayName();
        }

        return player.getName();
    }

    /**
     * Check if player has any of the specified permissions
     *
     * @param player Player to check
     * @param permissions Permissions to check
     * @return True if player has any permission
     */
    public static boolean hasAnyPermission(@NotNull org.bukkit.entity.Player player, @NotNull String... permissions) {
        for (String permission : permissions) {
            if (player.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if player has all specified permissions
     *
     * @param player Player to check
     * @param permissions Permissions to check
     * @return True if player has all permissions
     */
    public static boolean hasAllPermissions(@NotNull org.bukkit.entity.Player player, @NotNull String... permissions) {
        for (String permission : permissions) {
            if (!player.hasPermission(permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get free inventory slots count
     *
     * @param player Player to check
     * @return Number of free slots
     */
    public static int getFreeInventorySlots(@NotNull org.bukkit.entity.Player player) {
        int freeSlots = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType() == Material.AIR) {
                freeSlots++;
            }
        }
        return freeSlots;
    }

    /**
     * Check if player inventory has space for item
     *
     * @param player Player to check
     * @param item Item to check
     * @return True if player has space
     */
    public static boolean hasInventorySpace(@NotNull org.bukkit.entity.Player player, @NotNull ItemStack item) {
        return player.getInventory().firstEmpty() != -1 ||
                canAddToExistingStack(player, item);
    }

    /**
     * Check if item can be added to existing stacks
     *
     * @param player Player to check
     * @param item Item to check
     * @return True if can be added
     */
    private static boolean canAddToExistingStack(@NotNull org.bukkit.entity.Player player, @NotNull ItemStack item) {
        for (ItemStack invItem : player.getInventory().getStorageContents()) {
            if (invItem != null && invItem.isSimilar(item)) {
                int space = invItem.getMaxStackSize() - invItem.getAmount();
                if (space >= item.getAmount()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Give item to player with overflow protection
     *
     * @param player Player to give item to
     * @param item Item to give
     * @return Items that couldn't fit
     */
    @NotNull
    public static Collection<ItemStack> giveItem(@NotNull org.bukkit.entity.Player player, @NotNull ItemStack item) {
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);

        // Drop overflow items at player location
        for (ItemStack overflowItem : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), overflowItem);
        }

        return overflow.values();
    }

    /**
     * Format location as readable string
     *
     * @param location Location to format
     * @return Formatted location string
     */
    @NotNull
    public static String formatLocation(@NotNull org.bukkit.Location location) {
        return String.format("%s: %.1f, %.1f, %.1f",
                location.getWorld() != null ? location.getWorld().getName() : "unknown",
                location.getX(),
                location.getY(),
                location.getZ()
        );
    }

    /**
     * Calculate distance between two players
     *
     * @param player1 First player
     * @param player2 Second player
     * @return Distance or -1 if in different worlds
     */
    public static double getDistance(@NotNull org.bukkit.entity.Player player1, @NotNull org.bukkit.entity.Player player2) {
        if (!player1.getWorld().equals(player2.getWorld())) {
            return -1;
        }

        return player1.getLocation().distance(player2.getLocation());
    }

    /**
     * Get player ping (reflection-based, may not work on all versions)
     *
     * @param player Player to check
     * @return Ping in milliseconds or -1 if unavailable
     */
    public static int getPing(@NotNull org.bukkit.entity.Player player) {
        try {
            return player.getPing();
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Check if player is in creative or spectator mode
     *
     * @param player Player to check
     * @return True if in creative or spectator mode
     */
    public static boolean isInCreativeMode(@NotNull org.bukkit.entity.Player player) {
        return player.getGameMode() == org.bukkit.GameMode.CREATIVE ||
                player.getGameMode() == org.bukkit.GameMode.SPECTATOR;
    }

    /**
     * Get health percentage
     *
     * @param player Player to check
     * @return Health percentage (0-100)
     */
    public static double getHealthPercentage(@NotNull org.bukkit.entity.Player player) {
        return (player.getHealth() / player.getMaxHealth()) * 100.0;
    }

    /**
     * Check if player is at full health
     *
     * @param player Player to check
     * @return True if at full health
     */
    public static boolean isAtFullHealth(@NotNull org.bukkit.entity.Player player) {
        return player.getHealth() >= player.getMaxHealth();
    }
}
