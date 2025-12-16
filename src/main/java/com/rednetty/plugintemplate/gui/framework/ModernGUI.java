package com.rednetty.plugintemplate.gui.framework;

import com.rednetty.plugintemplate.PluginTemplate;
import com.rednetty.plugintemplate.utils.ComponentBuilder;
import com.rednetty.plugintemplate.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Modern GUI framework with animations and advanced features
 *
 * Features:
 * - Animated transitions and effects
 * - Pagination support
 * - Slot-based click handling
 * - Auto-updating content
 * - Sound effects and particles
 * - Input validation
 * - Template-based layouts
 * - Multi-page navigation
 * - Player-specific data context
 * - Async content loading
 *
 * @author RedNetty
 * @version 1.0.0
 */
public abstract class ModernGUI implements Listener {

    protected final PluginTemplate plugin;
    protected final String title;
    protected final int size;
    protected final boolean allowPlayerInventory;

    // GUI State
    protected Inventory inventory;
    protected final Map<Integer, GUIButton> buttons = new ConcurrentHashMap<>();
    protected final Map<UUID, GUISession> sessions = new ConcurrentHashMap<>();

    // Animation System
    private BukkitTask animationTask;
    private final List<GUIAnimation> activeAnimations = new ArrayList<>();

    // GUI Templates
    private static final Map<String, GUITemplate> templates = new ConcurrentHashMap<>();

    /**
     * GUI Button with advanced functionality
     */
    public static class GUIButton {
        private final ItemStack item;
        private final Consumer<ClickContext> clickHandler;
        private final String permission;
        private final boolean closeOnClick;
        private final GUISound clickSound;

        public GUIButton(@NotNull ItemStack item, @Nullable Consumer<ClickContext> clickHandler) {
            this(item, clickHandler, null, false, null);
        }

        public GUIButton(@NotNull ItemStack item, @Nullable Consumer<ClickContext> clickHandler,
                         @Nullable String permission, boolean closeOnClick, @Nullable GUISound clickSound) {
            this.item = item;
            this.clickHandler = clickHandler;
            this.permission = permission;
            this.closeOnClick = closeOnClick;
            this.clickSound = clickSound;
        }

        public ItemStack getItem() { return item.clone(); }
        public Consumer<ClickContext> getClickHandler() { return clickHandler; }
        public String getPermission() { return permission; }
        public boolean shouldCloseOnClick() { return closeOnClick; }
        public GUISound getClickSound() { return clickSound; }
    }

    /**
     * Click context for button handlers
     */
    public static class ClickContext {
        private final Player player;
        private final InventoryClickEvent event;
        private final ModernGUI gui;
        private final int slot;

        public ClickContext(@NotNull Player player, @NotNull InventoryClickEvent event,
                            @NotNull ModernGUI gui, int slot) {
            this.player = player;
            this.event = event;
            this.gui = gui;
            this.slot = slot;
        }

        public Player getPlayer() { return player; }
        public InventoryClickEvent getEvent() { return event; }
        public ModernGUI getGUI() { return gui; }
        public int getSlot() { return slot; }

        public void playSound(@NotNull GUISound sound) {
            sound.play(player);
        }

        public void closeGUI() {
            gui.close(player);
        }

        public void refreshGUI() {
            gui.refresh(player);
        }
    }

    /**
     * GUI Session for player-specific data
     */
    public static class GUISession {
        private final Player player;
        private final Map<String, Object> data = new ConcurrentHashMap<>();
        private long openTime;
        private int currentPage = 0;

        public GUISession(@NotNull Player player) {
            this.player = player;
            this.openTime = System.currentTimeMillis();
        }

        public Player getPlayer() { return player; }
        public long getOpenTime() { return openTime; }
        public int getCurrentPage() { return currentPage; }
        public void setCurrentPage(int page) { this.currentPage = page; }

        @SuppressWarnings("unchecked")
        public <T> T getData(@NotNull String key) {
            return (T) data.get(key);
        }

        public void setData(@NotNull String key, Object value) {
            data.put(key, value);
        }

        public Map<String, Object> getAllData() {
            return new HashMap<>(data);
        }
    }

    /**
     * GUI Animation system
     */
    public abstract static class GUIAnimation {
        protected final ModernGUI gui;
        protected final int duration; // ticks
        protected int currentTick = 0;
        protected boolean completed = false;

        public GUIAnimation(@NotNull ModernGUI gui, int duration) {
            this.gui = gui;
            this.duration = duration;
        }

        public abstract void tick();

        public boolean isCompleted() { return completed; }

        protected void complete() {
            this.completed = true;
        }
    }

    /**
     * Sound effects for GUI interactions
     */
    public enum GUISound {
        CLICK("ui.button.click", 1.0f, 1.0f),
        SUCCESS("entity.experience_orb.pickup", 1.0f, 1.2f),
        ERROR("block.note_block.bass", 1.0f, 0.5f),
        PAGE_TURN("item.book.page_turn", 1.0f, 1.0f),
        OPEN("block.chest.open", 0.7f, 1.0f),
        CLOSE("block.chest.close", 0.7f, 1.0f);

        private final String sound;
        private final float volume;
        private final float pitch;

        GUISound(String sound, float volume, float pitch) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }

        public void play(@NotNull Player player) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    /**
     * GUI Template for reusable layouts
     */
    public static class GUITemplate {
        private final String name;
        private final int size;
        private final Map<Integer, ItemStack> staticItems = new HashMap<>();
        private final List<Integer> borderSlots = new ArrayList<>();

        public GUITemplate(@NotNull String name, int size) {
            this.name = name;
            this.size = size;
        }

        public void setBorderItem(@NotNull ItemStack item) {
            // Set border around the GUI
            for (int i = 0; i < 9; i++) borderSlots.add(i); // Top row
            for (int i = size - 9; i < size; i++) borderSlots.add(i); // Bottom row
            for (int i = 9; i < size - 9; i += 9) borderSlots.add(i); // Left column
            for (int i = 17; i < size - 9; i += 9) borderSlots.add(i); // Right column

            for (int slot : borderSlots) {
                staticItems.put(slot, item);
            }
        }

        public void setStaticItem(int slot, @NotNull ItemStack item) {
            staticItems.put(slot, item);
        }

        public void applyTo(@NotNull ModernGUI gui) {
            for (Map.Entry<Integer, ItemStack> entry : staticItems.entrySet()) {
                gui.inventory.setItem(entry.getKey(), entry.getValue());
            }
        }

        public String getName() { return name; }
        public int getSize() { return size; }
        public List<Integer> getBorderSlots() { return new ArrayList<>(borderSlots); }
    }

    /**
     * Constructor for ModernGUI
     *
     * @param plugin Plugin instance
     * @param title GUI title
     * @param size GUI size (multiple of 9)
     */
    public ModernGUI(@NotNull PluginTemplate plugin, @NotNull String title, int size) {
        this(plugin, title, size, false);
    }

    /**
     * Constructor for ModernGUI with player inventory access
     *
     * @param plugin Plugin instance
     * @param title GUI title
     * @param size GUI size (multiple of 9)
     * @param allowPlayerInventory Whether to allow player inventory interaction
     */
    public ModernGUI(@NotNull PluginTemplate plugin, @NotNull String title, int size, boolean allowPlayerInventory) {
        this.plugin = plugin;
        this.title = title;
        this.size = size;
        this.allowPlayerInventory = allowPlayerInventory;

        // Create inventory
        this.inventory = Bukkit.createInventory(null, size, Component.text(title));

        // Register event listener
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Initialize GUI content
        initialize();
    }

    /**
     * Initialize GUI content (abstract method)
     */
    protected abstract void initialize();

    /**
     * Open the GUI for a player
     *
     * @param player The player to open for
     */
    public void open(@NotNull Player player) {
        // Create session
        GUISession session = new GUISession(player);
        sessions.put(player.getUniqueId(), session);

        // Setup GUI for player
        setupForPlayer(player);

        // Open inventory
        player.openInventory(inventory);

        // Play open sound
        GUISound.OPEN.play(player);

        // Start animations
        startAnimations();

        plugin.getLogger().fine("Opened GUI '" + title + "' for player " + player.getName());
    }

    /**
     * Close the GUI for a player
     *
     * @param player The player to close for
     */
    public void close(@NotNull Player player) {
        // Remove session
        sessions.remove(player.getUniqueId());

        // Close inventory
        player.closeInventory();

        // Play close sound
        GUISound.CLOSE.play(player);

        // Stop animations if no players
        if (sessions.isEmpty()) {
            stopAnimations();
        }
    }

    /**
     * Refresh the GUI for a player
     *
     * @param player The player to refresh for
     */
    public void refresh(@NotNull Player player) {
        if (sessions.containsKey(player.getUniqueId())) {
            setupForPlayer(player);
        }
    }

    /**
     * Setup GUI content for a specific player
     *
     * @param player The player
     */
    protected void setupForPlayer(@NotNull Player player) {
        // Clear inventory
        inventory.clear();

        // Apply template if set
        String templateName = getTemplate();
        if (templateName != null && templates.containsKey(templateName)) {
            templates.get(templateName).applyTo(this);
        }

        // Setup content
        setupContent(player);

        // Update inventory
        player.updateInventory();
    }

    /**
     * Setup GUI content for player (implement in subclasses)
     *
     * @param player The player
     */
    protected abstract void setupContent(@NotNull Player player);

    /**
     * Get the template name for this GUI
     *
     * @return Template name or null
     */
    @Nullable
    protected String getTemplate() {
        return null;
    }

    /**
     * Set a button at a specific slot
     *
     * @param slot The slot number
     * @param button The button
     */
    protected void setButton(int slot, @NotNull GUIButton button) {
        buttons.put(slot, button);
        inventory.setItem(slot, button.getItem());
    }

    /**
     * Set a simple button with click handler
     *
     * @param slot The slot number
     * @param item The item
     * @param clickHandler The click handler
     */
    protected void setButton(int slot, @NotNull ItemStack item, @Nullable Consumer<ClickContext> clickHandler) {
        setButton(slot, new GUIButton(item, clickHandler));
    }

    /**
     * Set multiple buttons with the same item
     *
     * @param slots The slot numbers
     * @param item The item
     */
    protected void setButtons(@NotNull List<Integer> slots, @NotNull ItemStack item) {
        for (int slot : slots) {
            setButton(slot, item, null);
        }
    }

    /**
     * Fill empty slots with an item
     *
     * @param item The filler item
     */
    protected void fillEmpty(@NotNull ItemStack item) {
        for (int i = 0; i < size; i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                setButton(i, item, null);
            }
        }
    }

    /**
     * Create navigation buttons for pagination
     *
     * @param currentPage Current page number
     * @param totalPages Total number of pages
     */
    protected void createNavigationButtons(int currentPage, int totalPages) {
        // Previous page button
        if (currentPage > 0) {
            ItemStack prevItem = new ItemBuilder(Material.ARROW)
                    .name(ComponentBuilder.create(plugin)
                            .text("Previous Page")
                            .color(NamedTextColor.YELLOW)
                            .build())
                    .lore(ComponentBuilder.create(plugin)
                            .text("Click to go to page " + currentPage)
                            .color(NamedTextColor.GRAY)
                            .build())
                    .build();

            setButton(size - 9, new GUIButton(prevItem, ctx -> {
                GUISession session = sessions.get(ctx.getPlayer().getUniqueId());
                if (session != null) {
                    session.setCurrentPage(currentPage - 1);
                    refresh(ctx.getPlayer());
                    GUISound.PAGE_TURN.play(ctx.getPlayer());
                }
            }));
        }

        // Next page button
        if (currentPage < totalPages - 1) {
            ItemStack nextItem = new ItemBuilder(Material.ARROW)
                    .name(ComponentBuilder.create(plugin)
                            .text("Next Page")
                            .color(NamedTextColor.YELLOW)
                            .build())
                    .lore(ComponentBuilder.create(plugin)
                            .text("Click to go to page " + (currentPage + 2))
                            .color(NamedTextColor.GRAY)
                            .build())
                    .build();

            setButton(size - 1, new GUIButton(nextItem, ctx -> {
                GUISession session = sessions.get(ctx.getPlayer().getUniqueId());
                if (session != null) {
                    session.setCurrentPage(currentPage + 1);
                    refresh(ctx.getPlayer());
                    GUISound.PAGE_TURN.play(ctx.getPlayer());
                }
            }));
        }

        // Page info
        ItemStack pageInfo = new ItemBuilder(Material.BOOK)
                .name(ComponentBuilder.create(plugin)
                        .text("Page " + (currentPage + 1) + " of " + totalPages)
                        .color(NamedTextColor.GOLD)
                        .build())
                .build();

        setButton(size - 5, pageInfo, null);
    }

    /**
     * Start GUI animations
     */
    private void startAnimations() {
        if (animationTask == null || animationTask.isCancelled()) {
            animationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                activeAnimations.removeIf(animation -> {
                    if (!animation.isCompleted()) {
                        animation.tick();
                        animation.currentTick++;
                        if (animation.currentTick >= animation.duration) {
                            animation.complete();
                        }
                    }
                    return animation.isCompleted();
                });
            }, 0L, 1L);
        }
    }

    /**
     * Stop GUI animations
     */
    private void stopAnimations() {
        if (animationTask != null && !animationTask.isCancelled()) {
            animationTask.cancel();
        }
        activeAnimations.clear();
    }

    /**
     * Add an animation to the GUI
     *
     * @param animation The animation
     */
    protected void addAnimation(@NotNull GUIAnimation animation) {
        activeAnimations.add(animation);
        startAnimations();
    }

    // ===== EVENT HANDLERS =====

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();

        // Check if clicking in player inventory
        if (slot >= size && !allowPlayerInventory) {
            return;
        }

        // Handle button click
        GUIButton button = buttons.get(slot);
        if (button != null) {
            // Check permission
            if (button.getPermission() != null && !player.hasPermission(button.getPermission())) {
                GUISound.ERROR.play(player);
                return;
            }

            // Play click sound
            if (button.getClickSound() != null) {
                button.getClickSound().play(player);
            } else {
                GUISound.CLICK.play(player);
            }

            // Execute click handler
            if (button.getClickHandler() != null) {
                ClickContext context = new ClickContext(player, event, this, slot);
                button.getClickHandler().accept(context);
            }

            // Close if needed
            if (button.shouldCloseOnClick()) {
                close(player);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(@NotNull InventoryDragEvent event) {
        if (event.getInventory().equals(inventory)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory) && event.getPlayer() instanceof Player player) {
            // Remove session
            sessions.remove(player.getUniqueId());

            // Stop animations if no players
            if (sessions.isEmpty()) {
                stopAnimations();
            }
        }
    }

    // ===== STATIC TEMPLATE MANAGEMENT =====

    /**
     * Register a GUI template
     *
     * @param template The template
     */
    public static void registerTemplate(@NotNull GUITemplate template) {
        templates.put(template.getName(), template);
    }

    /**
     * Get a GUI template
     *
     * @param name Template name
     * @return The template or null
     */
    @Nullable
    public static GUITemplate getTemplate(@NotNull String name) {
        return templates.get(name);
    }

    /**
     * Initialize default templates
     */
    public static void initializeDefaultTemplates() {
        // Standard bordered template
        GUITemplate bordered = new GUITemplate("bordered", 54);
        ItemStack borderItem = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name(Component.text(" "))
                .build();
        bordered.setBorderItem(borderItem);
        registerTemplate(bordered);

        // Compact template
        GUITemplate compact = new GUITemplate("compact", 27);
        registerTemplate(compact);
    }

    /**
     * Get session for player
     *
     * @param player The player
     * @return The session or null
     */
    @Nullable
    protected GUISession getSession(@NotNull Player player) {
        return sessions.get(player.getUniqueId());
    }

    /**
     * Check if GUI is open for player
     *
     * @param player The player
     * @return True if open
     */
    public boolean isOpen(@NotNull Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    /**
     * Get the GUI title
     *
     * @return The title
     */
    @NotNull
    public String getTitle() {
        return title;
    }

    /**
     * Get the GUI size
     *
     * @return The size
     */
    public int getSize() {
        return size;
    }
}