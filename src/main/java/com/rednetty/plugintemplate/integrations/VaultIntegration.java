package com.rednetty.plugintemplate.integrations;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

/**
 * Comprehensive Vault integration for economy, permissions, and chat
 *
 * Features:
 * - Economy management (balance, transactions)
 * - Permission system integration
 * - Chat/prefix/suffix management
 * - Error handling and validation
 * - Balance formatting utilities
 * - Transaction logging
 * - Group management
 * - Multi-world support
 *
 * @author RedNetty
 * @version 1.0.0
 */
public class VaultIntegration {

    private static final Logger LOGGER = Logger.getLogger(VaultIntegration.class.getName());

    // Vault service providers
    private Economy economy;
    private Permission permission;
    private Chat chat;

    // Integration status
    private boolean economyEnabled = false;
    private boolean permissionEnabled = false;
    private boolean chatEnabled = false;

    /**
     * Transaction result enumeration
     */
    public enum TransactionResult {
        SUCCESS,
        INSUFFICIENT_FUNDS,
        ACCOUNT_NOT_FOUND,
        ECONOMY_DISABLED,
        UNKNOWN_ERROR
    }

    /**
     * Transaction record for logging and tracking
     */
    public record Transaction(
            String playerName,
            TransactionType type,
            double amount,
            double balanceBefore,
            double balanceAfter,
            String reason,
            long timestamp,
            boolean successful
    ) {
        public enum TransactionType {
            DEPOSIT, WITHDRAW, SET, TRANSFER_SEND, TRANSFER_RECEIVE
        }
    }

    /**
     * Initialize Vault integration
     *
     * @return True if at least one service was initialized
     */
    public boolean initialize() {
        LOGGER.info("Initializing Vault integration...");

        boolean anyServiceLoaded = false;

        // Setup economy
        if (setupEconomy()) {
            economyEnabled = true;
            anyServiceLoaded = true;
            LOGGER.info("Economy service loaded: " + economy.getName());
        } else {
            LOGGER.warning("Economy service not found!");
        }

        // Setup permissions
        if (setupPermissions()) {
            permissionEnabled = true;
            anyServiceLoaded = true;
            LOGGER.info("Permission service loaded: " + permission.getName());
        } else {
            LOGGER.warning("Permission service not found!");
        }

        // Setup chat
        if (setupChat()) {
            chatEnabled = true;
            anyServiceLoaded = true;
            LOGGER.info("Chat service loaded: " + chat.getName());
        } else {
            LOGGER.warning("Chat service not found!");
        }

        if (anyServiceLoaded) {
            LOGGER.info("Vault integration initialized successfully!");
        } else {
            LOGGER.severe("Failed to initialize any Vault services!");
        }

        return anyServiceLoaded;
    }

    /**
     * Setup economy service
     *
     * @return True if successful
     */
    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider =
                Bukkit.getServer().getServicesManager().getRegistration(Economy.class);

        if (economyProvider != null) {
            economy = economyProvider.getProvider();
            return economy != null;
        }

        return false;
    }

    /**
     * Setup permission service
     *
     * @return True if successful
     */
    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> permissionProvider =
                Bukkit.getServer().getServicesManager().getRegistration(Permission.class);

        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
            return permission != null;
        }

        return false;
    }

    /**
     * Setup chat service
     *
     * @return True if successful
     */
    private boolean setupChat() {
        RegisteredServiceProvider<Chat> chatProvider =
                Bukkit.getServer().getServicesManager().getRegistration(Chat.class);

        if (chatProvider != null) {
            chat = chatProvider.getProvider();
            return chat != null;
        }

        return false;
    }

    // ===== ECONOMY METHODS =====

    /**
     * Check if economy is enabled
     *
     * @return True if economy is available
     */
    public boolean isEconomyEnabled() {
        return economyEnabled && economy != null;
    }

    /**
     * Get player balance
     *
     * @param player The player
     * @return Balance amount
     */
    public double getBalance(@NotNull OfflinePlayer player) {
        if (!isEconomyEnabled()) {
            return 0.0;
        }

        return economy.getBalance(player);
    }

    /**
     * Get player balance in specific world
     *
     * @param player The player
     * @param world World name
     * @return Balance amount
     */
    public double getBalance(@NotNull OfflinePlayer player, @NotNull String world) {
        if (!isEconomyEnabled()) {
            return 0.0;
        }

        return economy.getBalance(player, world);
    }

    /**
     * Check if player has enough money
     *
     * @param player The player
     * @param amount Amount to check
     * @return True if player has enough money
     */
    public boolean hasEnoughMoney(@NotNull OfflinePlayer player, double amount) {
        return isEconomyEnabled() && economy.has(player, amount);
    }

    /**
     * Deposit money to player account
     *
     * @param player The player
     * @param amount Amount to deposit
     * @param reason Reason for transaction
     * @return Transaction result
     */
    @NotNull
    public TransactionResult depositMoney(@NotNull OfflinePlayer player, double amount, @Nullable String reason) {
        if (!isEconomyEnabled()) {
            return TransactionResult.ECONOMY_DISABLED;
        }

        if (amount <= 0) {
            return TransactionResult.UNKNOWN_ERROR;
        }

        double balanceBefore = getBalance(player);
        EconomyResponse response = economy.depositPlayer(player, amount);

        if (response.transactionSuccess()) {
            double balanceAfter = getBalance(player);
            logTransaction(new Transaction(
                    player.getName(),
                    Transaction.TransactionType.DEPOSIT,
                    amount,
                    balanceBefore,
                    balanceAfter,
                    reason != null ? reason : "Deposit",
                    System.currentTimeMillis(),
                    true
            ));

            return TransactionResult.SUCCESS;
        } else {
            return mapEconomyResponseType(response.type);
        }
    }

    /**
     * Withdraw money from player account
     *
     * @param player The player
     * @param amount Amount to withdraw
     * @param reason Reason for transaction
     * @return Transaction result
     */
    @NotNull
    public TransactionResult withdrawMoney(@NotNull OfflinePlayer player, double amount, @Nullable String reason) {
        if (!isEconomyEnabled()) {
            return TransactionResult.ECONOMY_DISABLED;
        }

        if (amount <= 0) {
            return TransactionResult.UNKNOWN_ERROR;
        }

        if (!hasEnoughMoney(player, amount)) {
            return TransactionResult.INSUFFICIENT_FUNDS;
        }

        double balanceBefore = getBalance(player);
        EconomyResponse response = economy.withdrawPlayer(player, amount);

        if (response.transactionSuccess()) {
            double balanceAfter = getBalance(player);
            logTransaction(new Transaction(
                    player.getName(),
                    Transaction.TransactionType.WITHDRAW,
                    amount,
                    balanceBefore,
                    balanceAfter,
                    reason != null ? reason : "Withdrawal",
                    System.currentTimeMillis(),
                    true
            ));

            return TransactionResult.SUCCESS;
        } else {
            return mapEconomyResponseType(response.type);
        }
    }

    /**
     * Set player balance
     *
     * @param player The player
     * @param amount New balance amount
     * @param reason Reason for transaction
     * @return Transaction result
     */
    @NotNull
    public TransactionResult setBalance(@NotNull OfflinePlayer player, double amount, @Nullable String reason) {
        if (!isEconomyEnabled()) {
            return TransactionResult.ECONOMY_DISABLED;
        }

        if (amount < 0) {
            return TransactionResult.UNKNOWN_ERROR;
        }

        double balanceBefore = getBalance(player);
        double difference = amount - balanceBefore;

        EconomyResponse response;
        if (difference > 0) {
            response = economy.depositPlayer(player, difference);
        } else if (difference < 0) {
            response = economy.withdrawPlayer(player, Math.abs(difference));
        } else {
            return TransactionResult.SUCCESS; // No change needed
        }

        if (response.transactionSuccess()) {
            double balanceAfter = getBalance(player);
            logTransaction(new Transaction(
                    player.getName(),
                    Transaction.TransactionType.SET,
                    amount,
                    balanceBefore,
                    balanceAfter,
                    reason != null ? reason : "Balance set",
                    System.currentTimeMillis(),
                    true
            ));

            return TransactionResult.SUCCESS;
        } else {
            return mapEconomyResponseType(response.type);
        }
    }

    /**
     * Transfer money between players
     *
     * @param from Source player
     * @param to Target player
     * @param amount Amount to transfer
     * @param reason Reason for transfer
     * @return Transaction result
     */
    @NotNull
    public TransactionResult transferMoney(@NotNull OfflinePlayer from, @NotNull OfflinePlayer to,
                                           double amount, @Nullable String reason) {
        if (!isEconomyEnabled()) {
            return TransactionResult.ECONOMY_DISABLED;
        }

        if (amount <= 0) {
            return TransactionResult.UNKNOWN_ERROR;
        }

        if (!hasEnoughMoney(from, amount)) {
            return TransactionResult.INSUFFICIENT_FUNDS;
        }

        // Withdraw from sender
        TransactionResult withdrawResult = withdrawMoney(from, amount,
                "Transfer to " + to.getName() + (reason != null ? " (" + reason + ")" : ""));

        if (withdrawResult != TransactionResult.SUCCESS) {
            return withdrawResult;
        }

        // Deposit to receiver
        TransactionResult depositResult = depositMoney(to, amount,
                "Transfer from " + from.getName() + (reason != null ? " (" + reason + ")" : ""));

        if (depositResult != TransactionResult.SUCCESS) {
            // Rollback - deposit back to sender
            depositMoney(from, amount, "Transfer rollback");
            return depositResult;
        }

        return TransactionResult.SUCCESS;
    }

    /**
     * Format balance for display
     *
     * @param amount Amount to format
     * @return Formatted balance string
     */
    @NotNull
    public String formatBalance(double amount) {
        if (!isEconomyEnabled()) {
            return String.valueOf(amount);
        }

        return economy.format(amount);
    }

    /**
     * Get currency name (singular)
     *
     * @return Currency name
     */
    @NotNull
    public String getCurrencyNameSingular() {
        if (!isEconomyEnabled()) {
            return "Dollar";
        }

        return economy.currencyNameSingular();
    }

    /**
     * Get currency name (plural)
     *
     * @return Currency name
     */
    @NotNull
    public String getCurrencyNamePlural() {
        if (!isEconomyEnabled()) {
            return "Dollars";
        }

        return economy.currencyNamePlural();
    }

    /**
     * Map economy response type to transaction result
     *
     * @param responseType Economy response type
     * @return Transaction result
     */
    @NotNull
    private TransactionResult mapEconomyResponseType(@NotNull EconomyResponse.ResponseType responseType) {
        return switch (responseType) {
            case NOT_IMPLEMENTED -> TransactionResult.ECONOMY_DISABLED;
            case FAILURE -> TransactionResult.UNKNOWN_ERROR;
            case SUCCESS -> TransactionResult.SUCCESS;
        };
    }

    /**
     * Log transaction for auditing
     *
     * @param transaction Transaction details
     */
    private void logTransaction(@NotNull Transaction transaction) {
        LOGGER.info(String.format("Transaction: %s %s %.2f (%s) - Balance: %.2f -> %.2f",
                transaction.playerName(),
                transaction.type().name().toLowerCase(),
                transaction.amount(),
                transaction.reason(),
                transaction.balanceBefore(),
                transaction.balanceAfter()
        ));
    }

    // ===== PERMISSION METHODS =====

    /**
     * Check if permissions are enabled
     *
     * @return True if permissions are available
     */
    public boolean isPermissionEnabled() {
        return permissionEnabled && permission != null;
    }

    /**
     * Check if player has permission
     *
     * @param player The player
     * @param perm Permission node
     * @return True if player has permission
     */
    public boolean hasPermission(@NotNull Player player, @NotNull String perm) {
        if (!isPermissionEnabled()) {
            return player.hasPermission(perm);
        }

        return permission.has(player, perm);
    }

    /**
     * Check if player has permission in specific world
     *
     * @param player The player
     * @param world World name
     * @param perm Permission node
     * @return True if player has permission
     */
    public boolean hasPermission(@NotNull Player player, @NotNull String world, @NotNull String perm) {
        if (!isPermissionEnabled()) {
            return player.hasPermission(perm);
        }

        return permission.has(player, world, perm);
    }

    /**
     * Get player's primary group
     *
     * @param player The player
     * @return Primary group name
     */
    @NotNull
    public String getPrimaryGroup(@NotNull Player player) {
        if (!isPermissionEnabled()) {
            return "default";
        }

        return permission.getPrimaryGroup(player);
    }

    /**
     * Get player's groups
     *
     * @param player The player
     * @return Array of group names
     */
    @NotNull
    public String[] getPlayerGroups(@NotNull Player player) {
        if (!isPermissionEnabled()) {
            return new String[]{"default"};
        }

        return permission.getPlayerGroups(player);
    }

    /**
     * Check if player is in group
     *
     * @param player The player
     * @param group Group name
     * @return True if player is in group
     */
    public boolean isInGroup(@NotNull Player player, @NotNull String group) {
        if (!isPermissionEnabled()) {
            return false;
        }

        return permission.playerInGroup(player, group);
    }

    // ===== CHAT METHODS =====

    /**
     * Check if chat is enabled
     *
     * @return True if chat is available
     */
    public boolean isChatEnabled() {
        return chatEnabled && chat != null;
    }

    /**
     * Get player's prefix
     *
     * @param player The player
     * @return Player prefix
     */
    @NotNull
    public String getPlayerPrefix(@NotNull Player player) {
        if (!isChatEnabled()) {
            return "";
        }

        String prefix = chat.getPlayerPrefix(player);
        return prefix != null ? prefix : "";
    }

    /**
     * Get player's suffix
     *
     * @param player The player
     * @return Player suffix
     */
    @NotNull
    public String getPlayerSuffix(@NotNull Player player) {
        if (!isChatEnabled()) {
            return "";
        }

        String suffix = chat.getPlayerSuffix(player);
        return suffix != null ? suffix : "";
    }

    /**
     * Get group prefix
     *
     * @param world World name
     * @param group Group name
     * @return Group prefix
     */
    @NotNull
    public String getGroupPrefix(@NotNull String world, @NotNull String group) {
        if (!isChatEnabled()) {
            return "";
        }

        String prefix = chat.getGroupPrefix(world, group);
        return prefix != null ? prefix : "";
    }

    /**
     * Get group suffix
     *
     * @param world World name
     * @param group Group name
     * @return Group suffix
     */
    @NotNull
    public String getGroupSuffix(@NotNull String world, @NotNull String group) {
        if (!isChatEnabled()) {
            return "";
        }

        String suffix = chat.getGroupSuffix(world, group);
        return suffix != null ? suffix : "";
    }

    /**
     * Set player prefix
     *
     * @param player The player
     * @param prefix New prefix
     */
    public void setPlayerPrefix(@NotNull Player player, @NotNull String prefix) {
        if (isChatEnabled()) {
            chat.setPlayerPrefix(player, prefix);
        }
    }

    /**
     * Set player suffix
     *
     * @param player The player
     * @param suffix New suffix
     */
    public void setPlayerSuffix(@NotNull Player player, @NotNull String suffix) {
        if (isChatEnabled()) {
            chat.setPlayerSuffix(player, suffix);
        }
    }

    // ===== STATUS AND UTILITIES =====

    /**
     * Get integration status
     *
     * @return Integration status information
     */
    @NotNull
    public VaultStatus getStatus() {
        return new VaultStatus(
                economyEnabled,
                permissionEnabled,
                chatEnabled,
                economyEnabled ? economy.getName() : null,
                permissionEnabled ? permission.getName() : null,
                chatEnabled ? chat.getName() : null
        );
    }

    /**
     * Vault integration status record
     */
    public record VaultStatus(
            boolean economyEnabled,
            boolean permissionEnabled,
            boolean chatEnabled,
            String economyProvider,
            String permissionProvider,
            String chatProvider
    ) {
        public boolean isAnyEnabled() {
            return economyEnabled || permissionEnabled || chatEnabled;
        }

        public int getEnabledCount() {
            int count = 0;
            if (economyEnabled) count++;
            if (permissionEnabled) count++;
            if (chatEnabled) count++;
            return count;
        }
    }
}