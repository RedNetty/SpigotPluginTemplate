package com.rednetty.plugintemplate.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ItemBuilder - Advanced ItemStack creation utility with proper Adventure API support
 *
 * Features:
 * - Fluent API for item creation
 * - Adventure Component support with legacy fallback
 * - Enchantment handling
 * - Item flags management
 * - Player head support
 * - Custom model data
 * - Automatic Adventure/Legacy compatibility
 *
 * @author RedNetty
 * @version 1.0.0
 */
public class ItemBuilder {

    private final ItemStack itemStack;
    private final ItemMeta itemMeta;
    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.legacySection();

    /**
     * Create ItemBuilder from material
     *
     * @param material The material
     */
    public ItemBuilder(@NotNull Material material) {
        this(material, 1);
    }

    /**
     * Create ItemBuilder from material and amount
     *
     * @param material The material
     * @param amount Item amount
     */
    public ItemBuilder(@NotNull Material material, int amount) {
        this.itemStack = new ItemStack(material, amount);
        this.itemMeta = itemStack.getItemMeta();
    }

    /**
     * Create ItemBuilder from existing ItemStack
     *
     * @param itemStack Existing ItemStack
     */
    public ItemBuilder(@NotNull ItemStack itemStack) {
        this.itemStack = itemStack.clone();
        this.itemMeta = this.itemStack.getItemMeta();
    }

    /**
     * Set display name using Component (Adventure API)
     *
     * @param name Display name component
     * @return This builder
     */
    @NotNull
    public ItemBuilder name(@NotNull Component name) {
        if (itemMeta != null) {
            try {
                // Try Adventure API method first (Paper/newer Spigot)
                itemMeta.getClass().getMethod("displayName", Component.class).invoke(itemMeta, name);
            } catch (Exception e) {
                // Fallback to legacy method
                String legacyText = LEGACY_SERIALIZER.serialize(name);
                itemMeta.setDisplayName(legacyText);
            }
        }
        return this;
    }

    /**
     * Set display name using string
     *
     * @param name Display name
     * @return This builder
     */
    @NotNull
    public ItemBuilder name(@NotNull String name) {
        if (itemMeta != null) {
            itemMeta.setDisplayName(name);
        }
        return this;
    }

    /**
     * Set lore using Components (Adventure API)
     *
     * @param lore Lore components
     * @return This builder
     */
    @NotNull
    public ItemBuilder lore(@NotNull Component... lore) {
        return lore(Arrays.asList(lore));
    }

    /**
     * Set lore using Component list (Adventure API)
     *
     * @param lore Lore component list
     * @return This builder
     */
    @NotNull
    public ItemBuilder lore(@NotNull List<Component> lore) {
        if (itemMeta != null) {
            try {
                // Try Adventure API method first (Paper/newer Spigot)
                itemMeta.getClass().getMethod("lore", List.class).invoke(itemMeta, lore);
            } catch (Exception e) {
                // Fallback to legacy method
                List<String> legacyLore = lore.stream()
                        .map(LEGACY_SERIALIZER::serialize)
                        .toList();
                itemMeta.setLore(legacyLore);
            }
        }
        return this;
    }

    /**
     * Set lore using strings
     *
     * @param lore Lore strings
     * @return This builder
     */
    @NotNull
    public ItemBuilder lore(@NotNull String... lore) {
        if (itemMeta != null) {
            itemMeta.setLore(Arrays.asList(lore));
        }
        return this;
    }

    /**
     * Add lore line using Component
     *
     * @param line Lore line component
     * @return This builder
     */
    @NotNull
    public ItemBuilder addLore(@NotNull Component line) {
        if (itemMeta != null) {
            List<String> currentLore = itemMeta.getLore();
            if (currentLore == null) {
                currentLore = new ArrayList<>();
            } else {
                currentLore = new ArrayList<>(currentLore);
            }
            currentLore.add(LEGACY_SERIALIZER.serialize(line));
            itemMeta.setLore(currentLore);
        }
        return this;
    }

    /**
     * Add lore line using string
     *
     * @param line Lore line
     * @return This builder
     */
    @NotNull
    public ItemBuilder addLore(@NotNull String line) {
        if (itemMeta != null) {
            List<String> currentLore = itemMeta.getLore();
            if (currentLore == null) {
                currentLore = new ArrayList<>();
            } else {
                currentLore = new ArrayList<>(currentLore);
            }
            currentLore.add(line);
            itemMeta.setLore(currentLore);
        }
        return this;
    }

    /**
     * Add multiple lore lines using Components
     *
     * @param lines Lore lines
     * @return This builder
     */
    @NotNull
    public ItemBuilder addLore(@NotNull Component... lines) {
        for (Component line : lines) {
            addLore(line);
        }
        return this;
    }

    /**
     * Add multiple lore lines using strings
     *
     * @param lines Lore lines
     * @return This builder
     */
    @NotNull
    public ItemBuilder addLore(@NotNull String... lines) {
        for (String line : lines) {
            addLore(line);
        }
        return this;
    }

    /**
     * Clear all lore
     *
     * @return This builder
     */
    @NotNull
    public ItemBuilder clearLore() {
        if (itemMeta != null) {
            itemMeta.setLore(null);
        }
        return this;
    }

    /**
     * Set item amount
     *
     * @param amount Item amount
     * @return This builder
     */
    @NotNull
    public ItemBuilder amount(int amount) {
        itemStack.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }

    /**
     * Add enchantment
     *
     * @param enchantment Enchantment type
     * @param level Enchantment level
     * @return This builder
     */
    @NotNull
    public ItemBuilder enchant(@NotNull Enchantment enchantment, int level) {
        if (itemMeta != null) {
            itemMeta.addEnchant(enchantment, level, true);
        }
        return this;
    }

    /**
     * Add enchanted effect (fake enchantment glow)
     *
     * @return This builder
     */
    @NotNull
    public ItemBuilder enchanted() {
        return enchanted(true);
    }

    /**
     * Set enchanted effect
     *
     * @param enchanted Whether to add glow effect
     * @return This builder
     */
    @NotNull
    public ItemBuilder enchanted(boolean enchanted) {
        if (enchanted) {
            // Add a harmless enchantment and hide it to create glow effect
            enchant(Enchantment.LOOTING, 1);
            addFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    /**
     * Remove enchantment
     *
     * @param enchantment Enchantment to remove
     * @return This builder
     */
    @NotNull
    public ItemBuilder removeEnchant(@NotNull Enchantment enchantment) {
        if (itemMeta != null) {
            itemMeta.removeEnchant(enchantment);
        }
        return this;
    }

    /**
     * Remove all enchantments
     *
     * @return This builder
     */
    @NotNull
    public ItemBuilder clearEnchants() {
        if (itemMeta != null) {
            itemMeta.getEnchants().keySet().forEach(itemMeta::removeEnchant);
        }
        return this;
    }

    /**
     * Add item flags
     *
     * @param flags Item flags to add
     * @return This builder
     */
    @NotNull
    public ItemBuilder addFlags(@NotNull ItemFlag... flags) {
        if (itemMeta != null) {
            itemMeta.addItemFlags(flags);
        }
        return this;
    }

    /**
     * Remove item flags
     *
     * @param flags Item flags to remove
     * @return This builder
     */
    @NotNull
    public ItemBuilder removeFlags(@NotNull ItemFlag... flags) {
        if (itemMeta != null) {
            itemMeta.removeItemFlags(flags);
        }
        return this;
    }

    /**
     * Hide all flags
     *
     * @return This builder
     */
    @NotNull
    public ItemBuilder hideAllFlags() {
        return addFlags(ItemFlag.values());
    }

    /**
     * Show all flags (remove all flags)
     *
     * @return This builder
     */
    @NotNull
    public ItemBuilder showAllFlags() {
        return removeFlags(ItemFlag.values());
    }

    /**
     * Set custom model data
     *
     * @param customModelData Custom model data value
     * @return This builder
     */
    @NotNull
    public ItemBuilder customModelData(int customModelData) {
        if (itemMeta != null) {
            itemMeta.setCustomModelData(customModelData);
        }
        return this;
    }

    /**
     * Remove custom model data
     *
     * @return This builder
     */
    @NotNull
    public ItemBuilder removeCustomModelData() {
        if (itemMeta != null && itemMeta.hasCustomModelData()) {
            try {
                // Use reflection to set to null (not all versions support this directly)
                java.lang.reflect.Method method = itemMeta.getClass().getMethod("setCustomModelData", Integer.class);
                method.invoke(itemMeta, (Integer) null);
            } catch (Exception e) {
                // Ignore if not supported
            }
        }
        return this;
    }

    /**
     * Set unbreakable
     *
     * @param unbreakable Whether item is unbreakable
     * @return This builder
     */
    @NotNull
    public ItemBuilder unbreakable(boolean unbreakable) {
        if (itemMeta != null) {
            itemMeta.setUnbreakable(unbreakable);
        }
        return this;
    }

    /**
     * Set unbreakable (default true)
     *
     * @return This builder
     */
    @NotNull
    public ItemBuilder unbreakable() {
        return unbreakable(true);
    }

    /**
     * Set skull owner (for player heads)
     *
     * @param owner Player name
     * @return This builder
     */
    @NotNull
    public ItemBuilder skullOwner(@NotNull String owner) {
        if (itemMeta instanceof SkullMeta skullMeta) {
            skullMeta.setOwner(owner);
        }
        return this;
    }

    /**
     * Set skull owner (for player heads)
     *
     * @param owner OfflinePlayer
     * @return This builder
     */
    @NotNull
    public ItemBuilder skullOwner(@NotNull OfflinePlayer owner) {
        if (itemMeta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(owner);
        }
        return this;
    }

    /**
     * Set durability/damage
     *
     * @param damage Damage value
     * @return This builder
     */
    @NotNull
    public ItemBuilder damage(int damage) {
        if (itemMeta instanceof org.bukkit.inventory.meta.Damageable damageable) {
            damageable.setDamage(Math.max(0, damage));
        }
        return this;
    }

    /**
     * Set item as glowing (adds enchantment glow without actual enchantment)
     *
     * @return This builder
     */
    @NotNull
    public ItemBuilder glow() {
        return glow(true);
    }

    /**
     * Set item glow state
     *
     * @param glow Whether to glow
     * @return This builder
     */
    @NotNull
    public ItemBuilder glow(boolean glow) {
        if (glow) {
            enchant(Enchantment.LOOTING, 1);
            addFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            removeEnchant(Enchantment.LOOTING);
            removeFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    /**
     * Apply a consumer function to the ItemMeta
     *
     * @param metaConsumer Consumer function
     * @return This builder
     */
    @NotNull
    public ItemBuilder meta(@NotNull java.util.function.Consumer<ItemMeta> metaConsumer) {
        if (itemMeta != null) {
            metaConsumer.accept(itemMeta);
        }
        return this;
    }

    /**
     * Apply a consumer function to the ItemStack
     *
     * @param stackConsumer Consumer function
     * @return This builder
     */
    @NotNull
    public ItemBuilder stack(@NotNull java.util.function.Consumer<ItemStack> stackConsumer) {
        stackConsumer.accept(itemStack);
        return this;
    }

    /**
     * Clone this builder
     *
     * @return New ItemBuilder with cloned ItemStack
     */
    @NotNull
    public ItemBuilder clone() {
        return new ItemBuilder(build());
    }

    /**
     * Build the final ItemStack
     *
     * @return The built ItemStack
     */
    @NotNull
    public ItemStack build() {
        if (itemMeta != null) {
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack.clone();
    }

    /**
     * Create a new ItemBuilder from an existing ItemStack
     *
     * @param itemStack ItemStack to copy
     * @return New ItemBuilder
     */
    @NotNull
    public static ItemBuilder from(@NotNull ItemStack itemStack) {
        return new ItemBuilder(itemStack);
    }

    /**
     * Create a new ItemBuilder from a material
     *
     * @param material Material
     * @return New ItemBuilder
     */
    @NotNull
    public static ItemBuilder of(@NotNull Material material) {
        return new ItemBuilder(material);
    }

    /**
     * Create a new ItemBuilder from a material and amount
     *
     * @param material Material
     * @param amount Amount
     * @return New ItemBuilder
     */
    @NotNull
    public static ItemBuilder of(@NotNull Material material, int amount) {
        return new ItemBuilder(material, amount);
    }
}