package com.rednetty.plugintemplate.core.registry;

import com.rednetty.plugintemplate.PluginTemplate;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * CommandRegistry - Command registration and management
 *
 * Features:
 * - Dynamic command registration
 * - Command validation
 * - Permission checking
 * - Tab completion support
 *
 * @author RedNetty
 * @version 1.0.0
 */
class CommandRegistry {

    private final PluginTemplate plugin;
    private final Map<String, CommandExecutor> registeredCommands = new HashMap<>();
    private final Map<String, TabCompleter> registeredTabCompleters = new HashMap<>();

    public CommandRegistry(@NotNull PluginTemplate plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize the command registry
     */
    public void initialize() {
        plugin.getLogger().info("CommandRegistry initialized successfully!");
    }

    /**
     * Register a command
     *
     * @param commandName Command name
     * @param executor Command executor
     * @return True if registered successfully
     */
    public boolean registerCommand(@NotNull String commandName, @NotNull CommandExecutor executor) {
        return registerCommand(commandName, executor, null);
    }

    /**
     * Register a command with tab completer
     *
     * @param commandName Command name
     * @param executor Command executor
     * @param tabCompleter Tab completer
     * @return True if registered successfully
     */
    public boolean registerCommand(@NotNull String commandName, @NotNull CommandExecutor executor,
                                   @org.jetbrains.annotations.Nullable TabCompleter tabCompleter) {
        try {
            PluginCommand command = plugin.getCommand(commandName);
            if (command == null) {
                plugin.getLogger().warning("Command not found in plugin.yml: " + commandName);
                return false;
            }

            command.setExecutor(executor);
            registeredCommands.put(commandName, executor);

            if (tabCompleter != null) {
                command.setTabCompleter(tabCompleter);
                registeredTabCompleters.put(commandName, tabCompleter);
            }

            plugin.getLogger().fine("Registered command: " + commandName);
            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register command " + commandName + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Unregister a command
     *
     * @param commandName Command name
     */
    public void unregisterCommand(@NotNull String commandName) {
        PluginCommand command = plugin.getCommand(commandName);
        if (command != null) {
            command.setExecutor(null);
            command.setTabCompleter(null);
        }

        registeredCommands.remove(commandName);
        registeredTabCompleters.remove(commandName);

        plugin.getLogger().fine("Unregistered command: " + commandName);
    }

    /**
     * Get registered commands
     *
     * @return Set of registered command names
     */
    @NotNull
    public Set<String> getRegisteredCommands() {
        return new HashSet<>(registeredCommands.keySet());
    }

    /**
     * Check if command is registered
     *
     * @param commandName Command name
     * @return True if registered
     */
    public boolean isCommandRegistered(@NotNull String commandName) {
        return registeredCommands.containsKey(commandName);
    }

    /**
     * Get command executor
     *
     * @param commandName Command name
     * @return Command executor or null
     */
    @org.jetbrains.annotations.Nullable
    public CommandExecutor getCommandExecutor(@NotNull String commandName) {
        return registeredCommands.get(commandName);
    }
}
