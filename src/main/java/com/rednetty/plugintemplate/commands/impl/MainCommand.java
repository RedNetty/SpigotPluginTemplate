package com.rednetty.plugintemplate.commands.impl;

import com.rednetty.plugintemplate.PluginTemplate;
import com.rednetty.plugintemplate.commands.framework.CommandFramework;
import com.rednetty.plugintemplate.database.DatabaseManager;
import com.rednetty.plugintemplate.gui.menus.MainMenu;
import com.rednetty.plugintemplate.utils.ComponentBuilder;
import com.rednetty.plugintemplate.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main command implementation for PluginTemplate
 *
 * This command serves as the central hub for all plugin functionality
 * and demonstrates the advanced command framework capabilities.
 *
 * Available sub-commands:
 * - help: Show command help
 * - reload: Reload plugin configuration
 * - info: Show plugin information
 * - stats: Show plugin statistics
 * - debug: Toggle debug mode
 * - gui: Open main GUI menu
 * - test: Test various plugin features
 *
 * @author RedNetty
 * @version 1.0.0
 */
public class MainCommand extends CommandFramework {

    public MainCommand(@NotNull PluginTemplate plugin) {
        super(plugin, "plugintemplate", "plugintemplate.use", false);
    }

    @Override
    protected boolean executeCommand(@NotNull CommandSender sender, @NotNull Command command,
                                     @NotNull String label, @NotNull String[] args) {
        // Default command behavior - show info and help
        sendWelcomeMessage(sender, label);
        return true;
    }

    @Override
    @Nullable
    protected List<String> getTabCompletions(@NotNull CommandSender sender, @NotNull Command command,
                                             @NotNull String alias, @NotNull String[] args) {
        // Default tab completions (empty since sub-commands handle their own)
        return List.of();
    }

    /**
     * Send welcome message with plugin info
     */
    private void sendWelcomeMessage(@NotNull CommandSender sender, @NotNull String label) {
        Component header = ComponentBuilder.create(getPlugin())
                .text("═══════════════════════════════════")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
                .build();

        Component title = ComponentBuilder.create(getPlugin())
                .text("PluginTemplate v" + getPlugin().getDescription().getVersion())
                .color(NamedTextColor.AQUA)
                .decorate(TextDecoration.BOLD)
                .build();

        Component subtitle = ComponentBuilder.create(getPlugin())
                .text("A modern, comprehensive Spigot plugin template")
                .color(NamedTextColor.GRAY)
                .decorate(TextDecoration.ITALIC)
                .build();

        Component helpCommand = ComponentBuilder.create(getPlugin())
                .text("Use /" + label + " help for available commands")
                .color(NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.runCommand("/" + label + " help"))
                .hoverEvent(HoverEvent.showText(Component.text("Click to show help", NamedTextColor.GOLD)))
                .build();

        getPlugin().getAudiences().sender(sender).sendMessage(header);
        getPlugin().getAudiences().sender(sender).sendMessage(title);
        getPlugin().getAudiences().sender(sender).sendMessage(subtitle);
        getPlugin().getAudiences().sender(sender).sendMessage(Component.empty());
        getPlugin().getAudiences().sender(sender).sendMessage(helpCommand);
        getPlugin().getAudiences().sender(sender).sendMessage(header);
    }

    // ===== SUB-COMMANDS =====

    /**
     * Help command - shows all available commands
     */
    @SubCommandInfo(
            name = "help",
            aliases = {"?", "h"},
            description = "Show available commands",
            usage = "help [command]"
    )
    public boolean helpCommand(@NotNull CommandContext context) {
        CommandSender sender = context.getSender();
        String[] args = context.getArgs();

        if (args.length > 0) {
            // Show specific command help
            String commandName = args[0];
            // Implementation for specific command help would go here
            Component message = ComponentBuilder.create(getPlugin())
                    .text("Help for command: " + commandName)
                    .color(NamedTextColor.GREEN)
                    .build();
            getPlugin().getAudiences().sender(sender).sendMessage(message);
        } else {
            // Show general help
            sendHelpMessage(sender, context.getLabel());
        }

        return true;
    }

    /**
     * Reload command - reloads plugin configuration
     */
    @SubCommandInfo(
            name = "reload",
            aliases = {"rl"},
            permission = "plugintemplate.reload",
            description = "Reload plugin configuration",
            cooldown = 5
    )
    public boolean reloadCommand(@NotNull CommandContext context) {
        CommandSender sender = context.getSender();

        try {
            long startTime = System.currentTimeMillis();

            // Reload the plugin
            getPlugin().reload();

            long duration = System.currentTimeMillis() - startTime;

            Component message = ComponentBuilder.create(getPlugin())
                    .fromLocalization("commands.reload.success",
                            "&aConfiguration reloaded successfully! (took %time%ms)")
                    .withPlaceholder("time", String.valueOf(duration))
                    .color(NamedTextColor.GREEN)
                    .build();

            getPlugin().getAudiences().sender(sender).sendMessage(message);

        } catch (Exception e) {
            Component errorMessage = ComponentBuilder.create(getPlugin())
                    .fromLocalization("commands.reload.error",
                            "&cFailed to reload configuration: %error%")
                    .withPlaceholder("error", e.getMessage())
                    .color(NamedTextColor.RED)
                    .build();

            getPlugin().getAudiences().sender(sender).sendMessage(errorMessage);
            getPlugin().getLogger().warning("Reload command failed: " + e.getMessage());
        }

        return true;
    }

    /**
     * Info command - shows plugin information
     */
    @SubCommandInfo(
            name = "info",
            aliases = {"information", "about"},
            description = "Show plugin information"
    )
    public boolean infoCommand(@NotNull CommandContext context) {
        CommandSender sender = context.getSender();

        Component header = ComponentBuilder.create(getPlugin())
                .text("Plugin Information")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
                .build();

        Component version = ComponentBuilder.create(getPlugin())
                .text("Version: " + getPlugin().getDescription().getVersion())
                .color(NamedTextColor.AQUA)
                .build();

        Component authors = ComponentBuilder.create(getPlugin())
                .text("Authors: " + String.join(", ", getPlugin().getDescription().getAuthors()))
                .color(NamedTextColor.AQUA)
                .build();

        Component description = ComponentBuilder.create(getPlugin())
                .text("Description: " + getPlugin().getDescription().getDescription())
                .color(NamedTextColor.GRAY)
                .build();

        Component website = ComponentBuilder.create(getPlugin())
                .text("Website: " + getPlugin().getDescription().getWebsite())
                .color(NamedTextColor.BLUE)
                .clickEvent(ClickEvent.openUrl(getPlugin().getDescription().getWebsite()))
                .hoverEvent(HoverEvent.showText(Component.text("Click to open website", NamedTextColor.GOLD)))
                .build();

        getPlugin().getAudiences().sender(sender).sendMessage(header);
        getPlugin().getAudiences().sender(sender).sendMessage(version);
        getPlugin().getAudiences().sender(sender).sendMessage(authors);
        getPlugin().getAudiences().sender(sender).sendMessage(description);
        getPlugin().getAudiences().sender(sender).sendMessage(website);

        return true;
    }

    /**
     * Stats command - shows plugin statistics
     */
    @SubCommandInfo(
            name = "stats",
            aliases = {"statistics", "status"},
            permission = "plugintemplate.admin",
            description = "Show plugin statistics"
    )
    public boolean statsCommand(@NotNull CommandContext context) {
        CommandSender sender = context.getSender();

        // Get database statistics
        DatabaseManager.DatabaseStats dbStats = getPlugin().getDatabaseManager().getStatistics();

        // Get runtime statistics
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMemory = runtime.maxMemory() / 1024 / 1024;

        Component header = ComponentBuilder.create(getPlugin())
                .text("Plugin Statistics")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
                .build();

        Component separator = ComponentBuilder.create(getPlugin())
                .text("─────────────────────")
                .color(NamedTextColor.GRAY)
                .build();

        Component dbInfo = ComponentBuilder.create(getPlugin())
                .text("Database:")
                .color(NamedTextColor.YELLOW)
                .build();

        Component dbQueries = ComponentBuilder.create(getPlugin())
                .text("  Total Queries: " + dbStats.totalQueries())
                .color(NamedTextColor.WHITE)
                .build();

        Component dbSuccess = ComponentBuilder.create(getPlugin())
                .text("  Success Rate: " + String.format("%.2f%%", dbStats.getSuccessRate()))
                .color(NamedTextColor.WHITE)
                .build();

        Component dbHealth = ComponentBuilder.create(getPlugin())
                .text("  Health: " + (dbStats.isHealthy() ? "Healthy" : "Unhealthy"))
                .color(dbStats.isHealthy() ? NamedTextColor.GREEN : NamedTextColor.RED)
                .build();

        Component memoryInfo = ComponentBuilder.create(getPlugin())
                .text("Memory:")
                .color(NamedTextColor.YELLOW)
                .build();

        Component memoryUsage = ComponentBuilder.create(getPlugin())
                .text("  Used: " + usedMemory + "MB / " + maxMemory + "MB")
                .color(NamedTextColor.WHITE)
                .build();

        Component playersOnline = ComponentBuilder.create(getPlugin())
                .text("Players Online: " + Bukkit.getOnlinePlayers().size())
                .color(NamedTextColor.AQUA)
                .build();

        getPlugin().getAudiences().sender(sender).sendMessage(header);
        getPlugin().getAudiences().sender(sender).sendMessage(separator);
        getPlugin().getAudiences().sender(sender).sendMessage(dbInfo);
        getPlugin().getAudiences().sender(sender).sendMessage(dbQueries);
        getPlugin().getAudiences().sender(sender).sendMessage(dbSuccess);
        getPlugin().getAudiences().sender(sender).sendMessage(dbHealth);
        getPlugin().getAudiences().sender(sender).sendMessage(separator);
        getPlugin().getAudiences().sender(sender).sendMessage(memoryInfo);
        getPlugin().getAudiences().sender(sender).sendMessage(memoryUsage);
        getPlugin().getAudiences().sender(sender).sendMessage(separator);
        getPlugin().getAudiences().sender(sender).sendMessage(playersOnline);

        return true;
    }

    /**
     * Debug command - toggles debug mode
     */
    @SubCommandInfo(
            name = "debug",
            permission = "plugintemplate.admin",
            description = "Toggle debug mode",
            usage = "debug [on|off]"
    )
    public boolean debugCommand(@NotNull CommandContext context) {
        CommandSender sender = context.getSender();
        String[] args = context.getArgs();

        boolean debugMode;

        if (args.length > 0) {
            String arg = args[0].toLowerCase();
            debugMode = "on".equals(arg) || "true".equals(arg) || "enable".equals(arg);
        } else {
            // Toggle current state
            debugMode = !getPlugin().getConfigManager().getConfig().getBoolean("plugin.debug", false);
        }

        // Update config
        getPlugin().getConfigManager().getConfig().set("plugin.debug", debugMode);
        getPlugin().getConfigManager().saveConfig();

        Component message = ComponentBuilder.create(getPlugin())
                .text("Debug mode " + (debugMode ? "enabled" : "disabled"))
                .color(debugMode ? NamedTextColor.GREEN : NamedTextColor.RED)
                .build();

        getPlugin().getAudiences().sender(sender).sendMessage(message);

        return true;
    }

    /**
     * GUI command - opens the main GUI menu
     */
    @SubCommandInfo(
            name = "gui",
            aliases = {"menu", "ui"},
            playersOnly = true,
            description = "Open the main GUI menu"
    )
    public boolean guiCommand(@NotNull CommandContext context) {
        Player player = context.getPlayer();

        if (player == null) {
            return false; // Should not happen due to playersOnly = true
        }

        MainMenu mainMenu = new MainMenu(getPlugin());
        mainMenu.open(player);

        Component message = ComponentBuilder.create(getPlugin())
                .text("Opening main menu...")
                .color(NamedTextColor.GREEN)
                .build();

        getPlugin().getAudiences().player(player).sendMessage(message);

        return true;
    }

    /**
     * Test command - tests various plugin features
     */
    @SubCommandInfo(
            name = "test",
            permission = "plugintemplate.admin",
            description = "Test plugin features",
            usage = "test <feature> [args...]",
            minArgs = 1
    )
    public boolean testCommand(@NotNull CommandContext context) {
        CommandSender sender = context.getSender();
        String[] args = context.getArgs();
        String feature = args[0].toLowerCase();

        switch (feature) {
            case "database" -> {
                return testDatabase(sender);
            }
            case "localization", "lang" -> {
                return testLocalization(sender);
            }
            case "permissions", "perms" -> {
                return testPermissions(sender);
            }
            case "notifications", "notif" -> {
                return testNotifications(sender);
            }
            default -> {
                Component message = ComponentBuilder.create(getPlugin())
                        .text("Available test features: database, localization, permissions, notifications")
                        .color(NamedTextColor.YELLOW)
                        .build();
                getPlugin().getAudiences().sender(sender).sendMessage(message);
                return true;
            }
        }
    }

    /**
     * Tab completion for test command
     */
    @TabComplete(command = "test")
    public List<String> testTabComplete(@NotNull CommandContext context) {
        String[] args = context.getArgs();

        if (args.length == 1) {
            return Arrays.asList("database", "localization", "permissions", "notifications")
                    .stream()
                    .filter(feature -> feature.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    /**
     * Tab completion for debug command
     */
    @TabComplete(command = "debug")
    public List<String> debugTabComplete(@NotNull CommandContext context) {
        String[] args = context.getArgs();

        if (args.length == 1) {
            return Arrays.asList("on", "off", "enable", "disable")
                    .stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    // ===== TEST METHODS =====

    private boolean testDatabase(@NotNull CommandSender sender) {
        Component message = ComponentBuilder.create(getPlugin())
                .text("Testing database connection...")
                .color(NamedTextColor.YELLOW)
                .build();
        getPlugin().getAudiences().sender(sender).sendMessage(message);

        boolean healthy = getPlugin().getDatabaseManager().isHealthy();

        Component result = ComponentBuilder.create(getPlugin())
                .text("Database test " + (healthy ? "passed" : "failed"))
                .color(healthy ? NamedTextColor.GREEN : NamedTextColor.RED)
                .build();
        getPlugin().getAudiences().sender(sender).sendMessage(result);

        return true;
    }

    private boolean testLocalization(@NotNull CommandSender sender) {
        Component message = ComponentBuilder.create(getPlugin())
                .text("Testing localization system...")
                .color(NamedTextColor.YELLOW)
                .build();
        getPlugin().getAudiences().sender(sender).sendMessage(message);

        // Test message retrieval
        String testMessage = getPlugin().getLocalizationManager().getMessage("test.message", "Test message works!");

        Component result = ComponentBuilder.create(getPlugin())
                .text("Localization test: " + testMessage)
                .color(NamedTextColor.GREEN)
                .build();
        getPlugin().getAudiences().sender(sender).sendMessage(result);

        return true;
    }

    private boolean testPermissions(@NotNull CommandSender sender) {
        Component message = ComponentBuilder.create(getPlugin())
                .text("Testing permission system...")
                .color(NamedTextColor.YELLOW)
                .build();
        getPlugin().getAudiences().sender(sender).sendMessage(message);

        boolean hasPermission = sender.hasPermission("plugintemplate.test");

        Component result = ComponentBuilder.create(getPlugin())
                .text("Permission test (plugintemplate.test): " + (hasPermission ? "GRANTED" : "DENIED"))
                .color(hasPermission ? NamedTextColor.GREEN : NamedTextColor.RED)
                .build();
        getPlugin().getAudiences().sender(sender).sendMessage(result);

        return true;
    }

    private boolean testNotifications(@NotNull CommandSender sender) {
        Component message = ComponentBuilder.create(getPlugin())
                .text("Testing notification system...")
                .color(NamedTextColor.YELLOW)
                .build();
        getPlugin().getAudiences().sender(sender).sendMessage(message);

        // Send test notification
        Component notification = ComponentBuilder.create(getPlugin())
                .text("This is a test notification!")
                .color(NamedTextColor.LIGHT_PURPLE)
                .decorate(TextDecoration.ITALIC)
                .build();

        getPlugin().getAudiences().sender(sender).sendMessage(notification);

        return true;
    }
}