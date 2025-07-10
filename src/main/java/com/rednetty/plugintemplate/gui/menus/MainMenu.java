package com.rednetty.plugintemplate.gui.menus;

import com.rednetty.plugintemplate.PluginTemplate;
import com.rednetty.plugintemplate.gui.framework.ModernGUI;
import com.rednetty.plugintemplate.utils.ComponentBuilder;
import com.rednetty.plugintemplate.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Main menu GUI demonstrating the modern GUI framework
 *
 * Features:
 * - Player statistics display
 * - Quick access to plugin features
 * - Settings and configuration
 * - Admin tools (with permissions)
 * - Animated elements
 * - Sound effects
 *
 * @author RedNetty
 * @version 1.0.0
 */
public class MainMenu extends ModernGUI {

    public MainMenu(@NotNull PluginTemplate plugin) {
        super(plugin, "PluginTemplate Menu", 54, false);
    }

    @Override
    protected void initialize() {
        // Use bordered template
        ModernGUI.initializeDefaultTemplates();
    }

    @Override
    @Nullable
    protected String getTemplate() {
        return "bordered";
    }

    @Override
    protected void setupContent(@NotNull Player player) {
        // Title item
        ItemStack titleItem = new ItemBuilder(Material.NETHER_STAR)
                .name(ComponentBuilder.create(plugin)
                        .text("PluginTemplate")
                        .color(NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD)
                        .build())
                .lore(
                        ComponentBuilder.create(plugin)
                                .text("Version: " + plugin.getDescription().getVersion())
                                .color(NamedTextColor.GRAY)
                                .build(),
                        Component.empty(),
                        ComponentBuilder.create(plugin)
                                .text("Welcome, " + player.getName() + "!")
                                .color(NamedTextColor.YELLOW)
                                .build()
                )
                .enchanted(true)
                .build();

        setButton(4, titleItem, null);

        // Player Statistics
        setupPlayerStatistics(player);

        // Quick Actions
        setupQuickActions(player);

        // Settings
        setupSettings(player);

        // Admin Tools (if permission)
        if (player.hasPermission("plugintemplate.admin")) {
            setupAdminTools(player);
        }

        // Navigation
        setupNavigation(player);

        // Add entrance animation
        addEntranceAnimation();
    }

    /**
     * Setup player statistics section
     */
    private void setupPlayerStatistics(@NotNull Player player) {
        // Player head
        ItemStack playerHead = new ItemBuilder(Material.PLAYER_HEAD)
                .name(ComponentBuilder.create(plugin)
                        .text(player.getName())
                        .color(NamedTextColor.AQUA)
                        .decorate(TextDecoration.BOLD)
                        .build())
                .lore(
                        ComponentBuilder.create(plugin)
                                .text("Level: " + player.getLevel())
                                .color(NamedTextColor.GREEN)
                                .build(),
                        ComponentBuilder.create(plugin)
                                .text("Experience: " + player.getTotalExperience())
                                .color(NamedTextColor.GREEN)
                                .build(),
                        ComponentBuilder.create(plugin)
                                .text("Health: " + Math.round(player.getHealth()) + "/" + Math.round(player.getMaxHealth()))
                                .color(NamedTextColor.RED)
                                .build(),
                        Component.empty(),
                        ComponentBuilder.create(plugin)
                                .text("Click for detailed stats")
                                .color(NamedTextColor.GRAY)
                                .decorate(TextDecoration.ITALIC)
                                .build()
                )
                .skullOwner(player.getName())
                .build();

        setButton(20, new GUIButton(playerHead, ctx -> {
            // Open detailed stats menu
            new PlayerStatsMenu(plugin, player).open(ctx.getPlayer());
        }, null, false, GUISound.SUCCESS));

        // Play time (example)
        long playTime = player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) / 20; // Convert ticks to seconds
        ItemStack playTimeItem = new ItemBuilder(Material.CLOCK)
                .name(ComponentBuilder.create(plugin)
                        .text("Play Time")
                        .color(NamedTextColor.YELLOW)
                        .build())
                .lore(
                        ComponentBuilder.create(plugin)
                                .text("Total: " + formatTime(playTime))
                                .color(NamedTextColor.WHITE)
                                .build(),
                        ComponentBuilder.create(plugin)
                                .text("Session: " + formatTime((System.currentTimeMillis() - getSession(player).getOpenTime()) / 1000))
                                .color(NamedTextColor.WHITE)
                                .build()
                )
                .build();

        setButton(21, playTimeItem, null);

        // Location info
        ItemStack locationItem = new ItemBuilder(Material.COMPASS)
                .name(ComponentBuilder.create(plugin)
                        .text("Current Location")
                        .color(NamedTextColor.AQUA)
                        .build())
                .lore(
                        ComponentBuilder.create(plugin)
                                .text("World: " + player.getWorld().getName())
                                .color(NamedTextColor.WHITE)
                                .build(),
                        ComponentBuilder.create(plugin)
                                .text("X: " + Math.round(player.getLocation().getX()))
                                .color(NamedTextColor.WHITE)
                                .build(),
                        ComponentBuilder.create(plugin)
                                .text("Y: " + Math.round(player.getLocation().getY()))
                                .color(NamedTextColor.WHITE)
                                .build(),
                        ComponentBuilder.create(plugin)
                                .text("Z: " + Math.round(player.getLocation().getZ()))
                                .color(NamedTextColor.WHITE)
                                .build()
                )
                .build();

        setButton(22, locationItem, null);
    }

    /**
     * Setup quick actions section
     */
    private void setupQuickActions(@NotNull Player player) {
        // Features button
        ItemStack featuresItem = new ItemBuilder(Material.CHEST)
                .name(ComponentBuilder.create(plugin)
                        .text("Features")
                        .color(NamedTextColor.GREEN)
                        .decorate(TextDecoration.BOLD)
                        .build())
                .lore(
                        ComponentBuilder.create(plugin)
                                .text("Access plugin features")
                                .color(NamedTextColor.GRAY)
                                .build(),
                        Component.empty(),
                        ComponentBuilder.create(plugin)
                                .text("Click to explore!")
                                .color(NamedTextColor.YELLOW)
                                .decorate(TextDecoration.ITALIC)
                                .build()
                )
                .build();

        setButton(30, new GUIButton(featuresItem, ctx -> {
            new FeaturesMenu(plugin).open(ctx.getPlayer());
        }, null, false, GUISound.CLICK));

        // Tools button
        ItemStack toolsItem = new ItemBuilder(Material.DIAMOND_PICKAXE)
                .name(ComponentBuilder.create(plugin)
                        .text("Tools")
                        .color(NamedTextColor.BLUE)
                        .decorate(TextDecoration.BOLD)
                        .build())
                .lore(
                        ComponentBuilder.create(plugin)
                                .text("Useful tools and utilities")
                                .color(NamedTextColor.GRAY)
                                .build(),
                        Component.empty(),
                        ComponentBuilder.create(plugin)
                                .text("Click to access tools")
                                .color(NamedTextColor.YELLOW)
                                .decorate(TextDecoration.ITALIC)
                                .build()
                )
                .build();

        setButton(31, new GUIButton(toolsItem, ctx -> {
            new ToolsMenu(plugin).open(ctx.getPlayer());
        }, null, false, GUISound.CLICK));

        // Information button
        ItemStack infoItem = new ItemBuilder(Material.BOOK)
                .name(ComponentBuilder.create(plugin)
                        .text("Information")
                        .color(NamedTextColor.YELLOW)
                        .decorate(TextDecoration.BOLD)
                        .build())
                .lore(
                        ComponentBuilder.create(plugin)
                                .text("Plugin information and help")
                                .color(NamedTextColor.GRAY)
                                .build(),
                        Component.empty(),
                        ComponentBuilder.create(plugin)
                                .text("Click for more info")
                                .color(NamedTextColor.YELLOW)
                                .decorate(TextDecoration.ITALIC)
                                .build()
                )
                .build();

        setButton(32, new GUIButton(infoItem, ctx -> {
            ctx.getPlayer().performCommand("plugintemplate info");
            ctx.closeGUI();
        }, null, true, GUISound.PAGE_TURN));
    }

    /**
     * Setup settings section
     */
    private void setupSettings(@NotNull Player player) {
        // Settings button
        ItemStack settingsItem = new ItemBuilder(Material.REDSTONE)
                .name(ComponentBuilder.create(plugin)
                        .text("Settings")
                        .color(NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD)
                        .build())
                .lore(
                        ComponentBuilder.create(plugin)
                                .text("Configure your preferences")
                                .color(NamedTextColor.GRAY)
                                .build(),
                        Component.empty(),
                        ComponentBuilder.create(plugin)
                                .text("• Language settings")
                                .color(NamedTextColor.WHITE)
                                .build(),
                        ComponentBuilder.create(plugin)
                                .text("• Notification preferences")
                                .color(NamedTextColor.WHITE)
                                .build(),
                        ComponentBuilder.create(plugin)
                                .text("• Display options")
                                .color(NamedTextColor.WHITE)
                                .build(),
                        Component.empty(),
                        ComponentBuilder.create(plugin)
                                .text("Click to open settings")
                                .color(NamedTextColor.YELLOW)
                                .decorate(TextDecoration.ITALIC)
                                .build()
                )
                .build();

        setButton(40, new GUIButton(settingsItem, ctx -> {
            new SettingsMenu(plugin).open(ctx.getPlayer());
        }, null, false, GUISound.CLICK));

        // Language selector
        String currentLang = plugin.getLocalizationManager().getPlayerLanguage(player);
        String langDisplay = plugin.getLocalizationManager().getAvailableLanguages().get(currentLang);

        ItemStack languageItem = new ItemBuilder(Material.WRITABLE_BOOK)
                .name(ComponentBuilder.create(plugin)
                        .text("Language")
                        .color(NamedTextColor.LIGHT_PURPLE)
                        .build())
                .lore(
                        ComponentBuilder.create(plugin)
                                .text("Current: " + langDisplay)
                                .color(NamedTextColor.WHITE)
                                .build(),
                        Component.empty(),
                        ComponentBuilder.create(plugin)
                                .text("Click to change language")
                                .color(NamedTextColor.GRAY)
                                .decorate(TextDecoration.ITALIC)
                                .build()
                )
                .build();

        setButton(41, new GUIButton(languageItem, ctx -> {
            new LanguageMenu(plugin).open(ctx.getPlayer());
        }, null, false, GUISound.CLICK));
    }

    /**
     * Setup admin tools section (requires permission)
     */
    private void setupAdminTools(@NotNull Player player) {
        // Admin panel button
        ItemStack adminItem = new ItemBuilder(Material.COMMAND_BLOCK)
                .name(ComponentBuilder.create(plugin)
                        .text("Admin Panel")
                        .color(NamedTextColor.DARK_RED)
                        .decorate(TextDecoration.BOLD)
                        .build())
                .lore(
                        ComponentBuilder.create(plugin)
                                .text("Administrative tools")
                                .color(NamedTextColor.GRAY)
                                .build(),
                        Component.empty(),
                        ComponentBuilder.create(plugin)
                                .text("• Plugin management")
                                .color(NamedTextColor.WHITE)
                                .build(),
                        ComponentBuilder.create(plugin)
                                .text("• Database tools")
                                .color(NamedTextColor.WHITE)
                                .build(),
                        ComponentBuilder.create(plugin)
                                .text("• Debug utilities")
                                .color(NamedTextColor.WHITE)
                                .build(),
                        Component.empty(),
                        ComponentBuilder.create(plugin)
                                .text("⚠ Admin access required")
                                .color(NamedTextColor.RED)
                                .decorate(TextDecoration.ITALIC)
                                .build()
                )
                .enchanted(true)
                .build();

        setButton(49, new GUIButton(adminItem, ctx -> {
            new AdminMenu(plugin).open(ctx.getPlayer());
        }, "plugintemplate.admin", false, GUISound.SUCCESS));
    }

    /**
     * Setup navigation section
     */
    private void setupNavigation(@NotNull Player player) {
        // Close button
        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .name(ComponentBuilder.create(plugin)
                        .text("Close Menu")
                        .color(NamedTextColor.RED)
                        .build())
                .lore(
                        ComponentBuilder.create(plugin)
                                .text("Click to close this menu")
                                .color(NamedTextColor.GRAY)
                                .decorate(TextDecoration.ITALIC)
                                .build()
                )
                .build();

        setButton(53, new GUIButton(closeItem, ctx -> {
            ctx.closeGUI();
        }, null, true, GUISound.CLOSE));

        // Refresh button
        ItemStack refreshItem = new ItemBuilder(Material.EMERALD)
                .name(ComponentBuilder.create(plugin)
                        .text("Refresh")
                        .color(NamedTextColor.GREEN)
                        .build())
                .lore(
                        ComponentBuilder.create(plugin)
                                .text("Click to refresh the menu")
                                .color(NamedTextColor.GRAY)
                                .decorate(TextDecoration.ITALIC)
                                .build()
                )
                .build();

        setButton(45, new GUIButton(refreshItem, ctx -> {
            ctx.refreshGUI();
            ctx.playSound(GUISound.SUCCESS);
        }, null, false, null));
    }

    /**
     * Add entrance animation effect
     */
    private void addEntranceAnimation() {
        addAnimation(new GUIAnimation(this, 20) { // 1 second animation
            private int currentSlot = 0;
            private final int[] animationOrder = {
                    4, 20, 21, 22, 30, 31, 32, 40, 41, 49, 45, 53
            };

            @Override
            public void tick() {
                if (currentSlot < animationOrder.length && currentTick % 2 == 0) {
                    int slot = animationOrder[currentSlot];

                    // Create sparkle effect
                    ItemStack sparkle = new ItemBuilder(Material.FIREWORK_STAR)
                            .name(Component.text("✨"))
                            .build();

                    inventory.setItem(slot, sparkle);

                    // Restore original item after brief delay
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        GUIButton button = buttons.get(slot);
                        if (button != null) {
                            inventory.setItem(slot, button.getItem());
                        }
                    }, 3L);

                    currentSlot++;
                }

                if (currentSlot >= animationOrder.length) {
                    complete();
                }
            }
        });
    }

    /**
     * Format time duration
     *
     * @param seconds Time in seconds
     * @return Formatted time string
     */
    private String formatTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }
}

/**
 * Placeholder classes for other menus (implement as needed)
 */
class PlayerStatsMenu extends ModernGUI {
    private final Player targetPlayer;

    public PlayerStatsMenu(@NotNull PluginTemplate plugin, @NotNull Player targetPlayer) {
        super(plugin, targetPlayer.getName() + "'s Statistics", 54);
        this.targetPlayer = targetPlayer;
    }

    @Override
    protected void initialize() {}

    @Override
    protected void setupContent(@NotNull Player player) {
        // Implement detailed player statistics
        fillEmpty(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(Component.text(" ")).build());
    }
}

class FeaturesMenu extends ModernGUI {
    public FeaturesMenu(@NotNull PluginTemplate plugin) {
        super(plugin, "Plugin Features", 54);
    }

    @Override
    protected void initialize() {}

    @Override
    protected void setupContent(@NotNull Player player) {
        // Implement features menu
        fillEmpty(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(Component.text(" ")).build());
    }
}

class ToolsMenu extends ModernGUI {
    public ToolsMenu(@NotNull PluginTemplate plugin) {
        super(plugin, "Tools & Utilities", 54);
    }

    @Override
    protected void initialize() {}

    @Override
    protected void setupContent(@NotNull Player player) {
        // Implement tools menu
        fillEmpty(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(Component.text(" ")).build());
    }
}

class SettingsMenu extends ModernGUI {
    public SettingsMenu(@NotNull PluginTemplate plugin) {
        super(plugin, "Settings", 54);
    }

    @Override
    protected void initialize() {}

    @Override
    protected void setupContent(@NotNull Player player) {
        // Implement settings menu
        fillEmpty(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(Component.text(" ")).build());
    }
}

class LanguageMenu extends ModernGUI {
    public LanguageMenu(@NotNull PluginTemplate plugin) {
        super(plugin, "Language Selection", 27);
    }

    @Override
    protected void initialize() {}

    @Override
    protected void setupContent(@NotNull Player player) {
        // Implement language selection
        fillEmpty(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(Component.text(" ")).build());
    }
}

class AdminMenu extends ModernGUI {
    public AdminMenu(@NotNull PluginTemplate plugin) {
        super(plugin, "Admin Panel", 54);
    }

    @Override
    protected void initialize() {}

    @Override
    protected void setupContent(@NotNull Player player) {
        // Implement admin panel
        fillEmpty(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(Component.text(" ")).build());
    }
}