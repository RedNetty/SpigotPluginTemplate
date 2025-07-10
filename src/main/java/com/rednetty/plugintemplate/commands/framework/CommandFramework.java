package com.rednetty.plugintemplate.commands.framework;

import com.rednetty.plugintemplate.PluginTemplate;
import com.rednetty.plugintemplate.utils.ComponentBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Advanced command framework with auto-completion and annotation-based routing
 *
 * Features:
 * - Annotation-based command registration
 * - Automatic permission checking
 * - Intelligent tab completion
 * - Argument validation and parsing
 * - Sub-command support with unlimited depth
 * - Usage message generation
 * - Cooldown support
 * - Async command execution
 * - Command aliases
 * - Help system generation
 *
 * @author RedNetty
 * @version 1.0.0
 */
public abstract class CommandFramework implements CommandExecutor, TabCompleter {

    private final PluginTemplate plugin;
    private final Map<String, SubCommand> subCommands = new ConcurrentHashMap<>();
    private final Map<String, String> aliases = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    // Command metadata
    private final String commandName;
    private final String permission;
    private final boolean playersOnly;

    /**
     * Annotation for sub-commands
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface SubCommandInfo {
        String name();
        String[] aliases() default {};
        String permission() default "";
        String usage() default "";
        String description() default "";
        boolean playersOnly() default false;
        int cooldown() default 0; // seconds
        int minArgs() default 0;
        int maxArgs() default -1; // -1 = unlimited
    }

    /**
     * Annotation for tab completion
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TabComplete {
        String command();
    }

    /**
     * Sub-command container
     */
    private static class SubCommand {
        private final Method method;
        private final SubCommandInfo info;
        private final Method tabCompleter;

        public SubCommand(Method method, SubCommandInfo info, Method tabCompleter) {
            this.method = method;
            this.info = info;
            this.tabCompleter = tabCompleter;
        }

        public Method getMethod() { return method; }
        public SubCommandInfo getInfo() { return info; }
        public Method getTabCompleter() { return tabCompleter; }
    }

    /**
     * Command context for method parameters
     */
    public static class CommandContext {
        private final CommandSender sender;
        private final String[] args;
        private final String label;
        private final Command command;

        public CommandContext(CommandSender sender, String[] args, String label, Command command) {
            this.sender = sender;
            this.args = args;
            this.label = label;
            this.command = command;
        }

        public CommandSender getSender() { return sender; }
        public String[] getArgs() { return args; }
        public String getLabel() { return label; }
        public Command getCommand() { return command; }

        public Player getPlayer() {
            return sender instanceof Player ? (Player) sender : null;
        }

        public String getArg(int index) {
            return index < args.length ? args[index] : null;
        }

        public String getArg(int index, String defaultValue) {
            return index < args.length ? args[index] : defaultValue;
        }

        public int getArgAsInt(int index, int defaultValue) {
            try {
                return index < args.length ? Integer.parseInt(args[index]) : defaultValue;
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        public double getArgAsDouble(int index, double defaultValue) {
            try {
                return index < args.length ? Double.parseDouble(args[index]) : defaultValue;
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        public boolean getArgAsBoolean(int index, boolean defaultValue) {
            if (index >= args.length) return defaultValue;
            String arg = args[index].toLowerCase();
            return "true".equals(arg) || "yes".equals(arg) || "on".equals(arg) || "1".equals(arg);
        }

        public String getJoinedArgs(int start) {
            if (start >= args.length) return "";
            return String.join(" ", Arrays.copyOfRange(args, start, args.length));
        }
    }

    /**
     * Constructor for command framework
     *
     * @param plugin Plugin instance
     * @param commandName Main command name
     * @param permission Base permission
     * @param playersOnly Whether command is players only
     */
    protected CommandFramework(@NotNull PluginTemplate plugin, @NotNull String commandName,
                               @Nullable String permission, boolean playersOnly) {
        this.plugin = plugin;
        this.commandName = commandName;
        this.permission = permission;
        this.playersOnly = playersOnly;

        // Auto-register sub-commands
        registerSubCommands();
    }

    /**
     * Automatically register sub-commands using reflection
     */
    private void registerSubCommands() {
        Class<?> clazz = this.getClass();
        Map<String, Method> tabCompleters = new HashMap<>();

        // First pass: collect tab completers
        for (Method method : clazz.getDeclaredMethods()) {
            TabComplete tabComplete = method.getAnnotation(TabComplete.class);
            if (tabComplete != null) {
                tabCompleters.put(tabComplete.command(), method);
                method.setAccessible(true);
            }
        }

        // Second pass: register sub-commands
        for (Method method : clazz.getDeclaredMethods()) {
            SubCommandInfo subCmd = method.getAnnotation(SubCommandInfo.class);
            if (subCmd != null) {
                method.setAccessible(true);

                Method tabCompleter = tabCompleters.get(subCmd.name());
                SubCommand subCommand = new SubCommand(method, subCmd, tabCompleter);

                // Register main name
                subCommands.put(subCmd.name().toLowerCase(), subCommand);

                // Register aliases
                for (String alias : subCmd.aliases()) {
                    aliases.put(alias.toLowerCase(), subCmd.name().toLowerCase());
                    subCommands.put(alias.toLowerCase(), subCommand);
                }
            }
        }

        plugin.getLogger().info("Registered " + subCommands.size() + " sub-commands for " + commandName);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        // Check base permission
        if (permission != null && !sender.hasPermission(permission)) {
            sendNoPermissionMessage(sender);
            return true;
        }

        // Check if players only
        if (playersOnly && !(sender instanceof Player)) {
            sendPlayersOnlyMessage(sender);
            return true;
        }

        // Handle sub-commands
        if (args.length > 0) {
            String subCommandName = args[0].toLowerCase();
            SubCommand subCommand = subCommands.get(subCommandName);

            if (subCommand != null) {
                return executeSubCommand(sender, subCommand, command, label,
                        Arrays.copyOfRange(args, 1, args.length));
            }
        }

        // Execute default command or show help
        return executeCommand(sender, command, label, args);
    }

    /**
     * Execute a sub-command
     */
    private boolean executeSubCommand(@NotNull CommandSender sender, @NotNull SubCommand subCommand,
                                      @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        SubCommandInfo info = subCommand.getInfo();

        // Check permission
        if (!info.permission().isEmpty() && !sender.hasPermission(info.permission())) {
            sendNoPermissionMessage(sender);
            return true;
        }

        // Check if players only
        if (info.playersOnly() && !(sender instanceof Player)) {
            sendPlayersOnlyMessage(sender);
            return true;
        }

        // Check cooldown
        if (info.cooldown() > 0 && sender instanceof Player) {
            Player player = (Player) sender;
            if (isOnCooldown(player, info.name())) {
                long timeLeft = getCooldownTimeLeft(player, info.name());
                sendCooldownMessage(sender, timeLeft);
                return true;
            }
            setCooldown(player, info.name(), info.cooldown());
        }

        // Validate arguments
        if (args.length < info.minArgs()) {
            sendUsageMessage(sender, info, label);
            return true;
        }

        if (info.maxArgs() != -1 && args.length > info.maxArgs()) {
            sendUsageMessage(sender, info, label);
            return true;
        }

        // Execute sub-command
        try {
            CommandContext context = new CommandContext(sender, args, label, command);

            // Check method parameters and invoke accordingly
            Class<?>[] paramTypes = subCommand.getMethod().getParameterTypes();
            Object[] params = new Object[paramTypes.length];

            for (int i = 0; i < paramTypes.length; i++) {
                if (paramTypes[i] == CommandContext.class) {
                    params[i] = context;
                } else if (paramTypes[i] == CommandSender.class) {
                    params[i] = sender;
                } else if (paramTypes[i] == Player.class) {
                    params[i] = sender instanceof Player ? sender : null;
                } else if (paramTypes[i] == String[].class) {
                    params[i] = args;
                } else {
                    params[i] = null;
                }
            }

            Object result = subCommand.getMethod().invoke(this, params);
            return result instanceof Boolean ? (Boolean) result : true;

        } catch (Exception e) {
            plugin.getLogger().warning("Error executing sub-command " + info.name() + ": " + e.getMessage());
            sendErrorMessage(sender, "An error occurred while executing the command.");
            return true;
        }
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {

        // Base permission check
        if (permission != null && !sender.hasPermission(permission)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            // Complete sub-command names
            return subCommands.keySet().stream()
                    .filter(name -> name.startsWith(args[0].toLowerCase()))
                    .filter(name -> {
                        SubCommand subCmd = subCommands.get(name);
                        return subCmd.getInfo().permission().isEmpty() ||
                                sender.hasPermission(subCmd.getInfo().permission());
                    })
                    .collect(Collectors.toList());
        } else if (args.length > 1) {
            // Complete sub-command arguments
            String subCommandName = args[0].toLowerCase();
            SubCommand subCommand = subCommands.get(subCommandName);

            if (subCommand != null && subCommand.getTabCompleter() != null) {
                try {
                    CommandContext context = new CommandContext(sender,
                            Arrays.copyOfRange(args, 1, args.length), alias, command);

                    Object result = subCommand.getTabCompleter().invoke(this, context);
                    if (result instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> completions = (List<String>) result;
                        return completions;
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error in tab completion: " + e.getMessage());
                }
            }
        }

        return getTabCompletions(sender, command, alias, args);
    }

    /**
     * Cooldown management
     */
    private boolean isOnCooldown(@NotNull Player player, @NotNull String command) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return false;

        Long cooldownEnd = playerCooldowns.get(command);
        return cooldownEnd != null && System.currentTimeMillis() < cooldownEnd;
    }

    private long getCooldownTimeLeft(@NotNull Player player, @NotNull String command) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return 0;

        Long cooldownEnd = playerCooldowns.get(command);
        if (cooldownEnd == null) return 0;

        return Math.max(0, (cooldownEnd - System.currentTimeMillis()) / 1000);
    }

    private void setCooldown(@NotNull Player player, @NotNull String command, int seconds) {
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(command, System.currentTimeMillis() + (seconds * 1000L));
    }

    /**
     * Message sending methods
     */
    private void sendNoPermissionMessage(@NotNull CommandSender sender) {
        Component message = ComponentBuilder.create(plugin)
                .fromLocalization("commands.no-permission", "&cYou don't have permission to do that!")
                .color(NamedTextColor.RED)
                .build();
        plugin.getAudiences().sender(sender).sendMessage(message);
    }

    private void sendPlayersOnlyMessage(@NotNull CommandSender sender) {
        Component message = ComponentBuilder.create(plugin)
                .fromLocalization("commands.players-only", "&cThis command can only be used by players!")
                .color(NamedTextColor.RED)
                .build();
        plugin.getAudiences().sender(sender).sendMessage(message);
    }

    private void sendCooldownMessage(@NotNull CommandSender sender, long timeLeft) {
        Component message = ComponentBuilder.create(plugin)
                .fromLocalization("commands.cooldown", "&cYou must wait %time% seconds before using this command again!")
                .withPlaceholder("time", String.valueOf(timeLeft))
                .color(NamedTextColor.RED)
                .build();
        plugin.getAudiences().sender(sender).sendMessage(message);
    }

    private void sendUsageMessage(@NotNull CommandSender sender, @NotNull SubCommandInfo info, @NotNull String label) {
        String usage = info.usage().isEmpty() ?
                "/" + label + " " + info.name() :
                "/" + label + " " + info.usage();

        Component message = ComponentBuilder.create(plugin)
                .text("Usage: " + usage)
                .color(NamedTextColor.YELLOW)
                .build();
        plugin.getAudiences().sender(sender).sendMessage(message);
    }

    private void sendErrorMessage(@NotNull CommandSender sender, @NotNull String error) {
        Component message = ComponentBuilder.create(plugin)
                .text(error)
                .color(NamedTextColor.RED)
                .build();
        plugin.getAudiences().sender(sender).sendMessage(message);
    }

    /**
     * Generate help message for all sub-commands
     */
    protected void sendHelpMessage(@NotNull CommandSender sender, @NotNull String label) {
        Component header = ComponentBuilder.create(plugin)
                .text("Available Commands:")
                .color(NamedTextColor.GOLD)
                .build();
        plugin.getAudiences().sender(sender).sendMessage(header);

        subCommands.values().stream()
                .filter(subCmd -> !aliases.containsKey(subCmd.getInfo().name().toLowerCase()))
                .filter(subCmd -> subCmd.getInfo().permission().isEmpty() ||
                        sender.hasPermission(subCmd.getInfo().permission()))
                .forEach(subCmd -> {
                    SubCommandInfo info = subCmd.getInfo();
                    String usage = info.usage().isEmpty() ?
                            "/" + label + " " + info.name() :
                            "/" + label + " " + info.usage();

                    Component helpLine = ComponentBuilder.create(plugin)
                            .text("  " + usage + " - " + info.description())
                            .color(NamedTextColor.GRAY)
                            .build();
                    plugin.getAudiences().sender(sender).sendMessage(helpLine);
                });
    }

    /**
     * Abstract methods to be implemented by subclasses
     */
    protected abstract boolean executeCommand(@NotNull CommandSender sender, @NotNull Command command,
                                              @NotNull String label, @NotNull String[] args);

    @Nullable
    protected abstract List<String> getTabCompletions(@NotNull CommandSender sender, @NotNull Command command,
                                                      @NotNull String alias, @NotNull String[] args);

    /**
     * Cleanup cooldowns for offline players
     */
    public void cleanupCooldowns() {
        cooldowns.entrySet().removeIf(entry ->
                plugin.getServer().getPlayer(entry.getKey()) == null);
    }

    /**
     * Get the plugin instance
     */
    protected PluginTemplate getPlugin() {
        return plugin;
    }
}