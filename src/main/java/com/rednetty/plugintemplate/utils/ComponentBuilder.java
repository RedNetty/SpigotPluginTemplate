package com.rednetty.plugintemplate.utils;

import com.rednetty.plugintemplate.PluginTemplate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ComponentBuilder - Modern text component creation utility
 *
 * Features:
 * - Adventure API component building
 * - MiniMessage support
 * - Placeholder replacement
 * - Localization integration
 * - Color parsing and formatting
 * - Style chaining
 * - Click and hover events
 *
 * @author RedNetty
 * @version 1.0.0
 */
public class ComponentBuilder {

    private final PluginTemplate plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private Component component;

    private ComponentBuilder(@NotNull PluginTemplate plugin) {
        this.plugin = plugin;
        this.component = Component.empty();
    }

    /**
     * Create a new ComponentBuilder instance
     *
     * @param plugin Plugin instance
     * @return New ComponentBuilder
     */
    @NotNull
    public static ComponentBuilder create(@NotNull PluginTemplate plugin) {
        return new ComponentBuilder(plugin);
    }

    /**
     * Set text content
     *
     * @param text The text
     * @return This builder
     */
    @NotNull
    public ComponentBuilder text(@NotNull String text) {
        this.component = Component.text(text);
        return this;
    }

    /**
     * Create component from localization key
     *
     * @param key      Localization key
     * @param fallback Fallback text
     * @return This builder
     */
    @NotNull
    public ComponentBuilder fromLocalization(@NotNull String key, @NotNull String fallback) {
        String localizedText = plugin.getLocalizationManager().getMessage(key, fallback);
        this.component = miniMessage.deserialize(localizedText);
        return this;
    }

    /**
     * Create component from MiniMessage format
     *
     * @param miniMessageText MiniMessage formatted text
     * @return This builder
     */
    @NotNull
    public ComponentBuilder fromMiniMessage(@NotNull String miniMessageText) {
        this.component = this.miniMessage.deserialize(miniMessageText);
        return this;
    }

    /**
     * Set color
     *
     * @param color The color
     * @return This builder
     */
    @NotNull
    public ComponentBuilder color(@NotNull NamedTextColor color) {
        this.component = this.component.color(color);
        return this;
    }

    /**
     * Set color from hex
     *
     * @param hex Hex color code
     * @return This builder
     */
    @NotNull
    public ComponentBuilder color(@NotNull String hex) {
        TextColor color = TextColor.fromHexString(hex);
        if (color != null) {
            this.component = this.component.color(color);
        }
        return this;
    }

    /**
     * Add decoration
     *
     * @param decoration The decoration
     * @return This builder
     */
    @NotNull
    public ComponentBuilder decorate(@NotNull TextDecoration decoration) {
        this.component = this.component.decorate(decoration);
        return this;
    }

    /**
     * Remove decoration
     *
     * @param decoration The decoration to remove
     * @return This builder
     */
    @NotNull
    public ComponentBuilder removeDecoration(@NotNull TextDecoration decoration) {
        this.component = this.component.decoration(decoration, false);
        return this;
    }

    /**
     * Add style
     *
     * @param style The style
     * @return This builder
     */
    @NotNull
    public ComponentBuilder style(@NotNull Style style) {
        this.component = this.component.style(style);
        return this;
    }

    /**
     * Add click event
     *
     * @param clickEvent Click event
     * @return This builder
     */
    @NotNull
    public ComponentBuilder clickEvent(@NotNull net.kyori.adventure.text.event.ClickEvent clickEvent) {
        this.component = this.component.clickEvent(clickEvent);
        return this;
    }

    /**
     * Add hover event
     *
     * @param hoverEvent Hover event
     * @return This builder
     */
    @NotNull
    public ComponentBuilder hoverEvent(@NotNull net.kyori.adventure.text.event.HoverEvent<?> hoverEvent) {
        this.component = this.component.hoverEvent(hoverEvent);
        return this;
    }

    /**
     * Replace placeholder in the component
     *
     * @param placeholder Placeholder name
     * @param value       Replacement value
     * @return This builder
     */
    @NotNull
    public ComponentBuilder withPlaceholder(@NotNull String placeholder, @NotNull String value) {
        // Convert component to string, replace placeholder, then back to component
        String componentText = miniMessage.serialize(this.component);
        componentText = componentText.replace("%" + placeholder + "%", value);
        componentText = componentText.replace("{" + placeholder + "}", value);
        this.component = miniMessage.deserialize(componentText);
        return this;
    }

    /**
     * Replace multiple placeholders
     *
     * @param placeholders Map of placeholder -> value
     * @return This builder
     */
    @NotNull
    public ComponentBuilder withPlaceholders(@NotNull java.util.Map<String, String> placeholders) {
        String componentText = miniMessage.serialize(this.component);

        for (java.util.Map.Entry<String, String> entry : placeholders.entrySet()) {
            String placeholder = entry.getKey();
            String value = entry.getValue();
            componentText = componentText.replace("%" + placeholder + "%", value);
            componentText = componentText.replace("{" + placeholder + "}", value);
        }

        this.component = miniMessage.deserialize(componentText);
        return this;
    }

    /**
     * Append another component
     *
     * @param other Component to append
     * @return This builder
     */
    @NotNull
    public ComponentBuilder append(@NotNull Component other) {
        this.component = this.component.append(other);
        return this;
    }

    /**
     * Append text
     *
     * @param text Text to append
     * @return This builder
     */
    @NotNull
    public ComponentBuilder append(@NotNull String text) {
        this.component = this.component.append(Component.text(text));
        return this;
    }

    /**
     * Append a space
     *
     * @return This builder
     */
    @NotNull
    public ComponentBuilder space() {
        return append(" ");
    }

    /**
     * Append a newline
     *
     * @return This builder
     */
    @NotNull
    public ComponentBuilder newline() {
        this.component = this.component.append(Component.newline());
        return this;
    }

    /**
     * Build the final component
     *
     * @return The built component
     */
    @NotNull
    public Component build() {
        return this.component;
    }
}