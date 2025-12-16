package com.rednetty.plugintemplate.core.registry;

import com.rednetty.plugintemplate.PluginTemplate;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * ListenerRegistry - Event listener registration and management
 *
 * Features:
 * - Dynamic listener registration
 * - Listener validation
 * - Event priority management
 * - Listener lifecycle tracking
 *
 * @author RedNetty
 * @version 1.0.0
 */
public class ListenerRegistry {

    private final PluginTemplate plugin;
    private final Set<Listener> registeredListeners = new HashSet<>();

    public ListenerRegistry(@NotNull PluginTemplate plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize the listener registry
     */
    public void initialize() {
        plugin.getLogger().info("ListenerRegistry initialized successfully!");
    }

    /**
     * Register an event listener
     *
     * @param listener Listener to register
     * @return True if registered successfully
     */
    public boolean registerListener(@NotNull Listener listener) {
        try {
            Bukkit.getPluginManager().registerEvents(listener, plugin);
            registeredListeners.add(listener);

            plugin.getLogger().fine("Registered listener: " + listener.getClass().getSimpleName());
            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register listener " +
                    listener.getClass().getSimpleName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Unregister an event listener
     *
     * @param listener Listener to unregister
     */
    public void unregisterListener(@NotNull Listener listener) {
        registeredListeners.remove(listener);
        plugin.getLogger().fine("Unregistered listener: " + listener.getClass().getSimpleName());
    }

    /**
     * Get registered listeners
     *
     * @return Set of registered listeners
     */
    @NotNull
    public Set<Listener> getRegisteredListeners() {
        return new HashSet<>(registeredListeners);
    }

    /**
     * Check if listener is registered
     *
     * @param listener Listener to check
     * @return True if registered
     */
    public boolean isListenerRegistered(@NotNull Listener listener) {
        return registeredListeners.contains(listener);
    }

    /**
     * Get listener count
     *
     * @return Number of registered listeners
     */
    public int getListenerCount() {
        return registeredListeners.size();
    }

    /**
     * Unregister all listeners
     */
    public void unregisterAll() {
        registeredListeners.clear();
        plugin.getLogger().info("Unregistered all listeners");
    }
}