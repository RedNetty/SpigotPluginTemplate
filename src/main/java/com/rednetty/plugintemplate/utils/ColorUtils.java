package com.rednetty.plugintemplate.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ColorUtils - Color parsing and formatting utility
 *
 * Features:
 * - Legacy color code support
 * - Hex color parsing
 * - MiniMessage integration
 * - Gradient support
 * - Color validation
 *
 * @author RedNetty
 * @version 1.0.0
 */
public class ColorUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern LEGACY_PATTERN = Pattern.compile("&([0-9a-fk-or])");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    /**
     * Colorize text with legacy and hex color support
     *
     * @param text Text to colorize
     * @return Colorized text
     */
    @NotNull
    public static String colorize(@NotNull String text) {
        // Handle hex colors
        text = translateHexColors(text);

        // Handle legacy colors
        text = ChatColor.translateAlternateColorCodes('&', text);

        return text;
    }

    /**
     * Translate hex colors to legacy format
     *
     * @param text Text with hex colors
     * @return Text with translated colors
     */
    @NotNull
    private static String translateHexColors(@NotNull String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hexColor = matcher.group(1);
            matcher.appendReplacement(buffer,
                    ChatColor.of("#" + hexColor).toString());
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }

    /**
     * Strip all color codes from text
     *
     * @param text Text with color codes
     * @return Plain text
     */
    @NotNull
    public static String stripColors(@NotNull String text) {
        text = LEGACY_PATTERN.matcher(text).replaceAll("");
        text = HEX_PATTERN.matcher(text).replaceAll("");
        return ChatColor.stripColor(text);
    }

    /**
     * Create a MiniMessage component from text
     *
     * @param text MiniMessage formatted text
     * @return Component
     */
    @NotNull
    public static Component parseMiniMessage(@NotNull String text) {
        return MINI_MESSAGE.deserialize(text);
    }

    /**
     * Serialize component to MiniMessage format
     *
     * @param component Component to serialize
     * @return MiniMessage string
     */
    @NotNull
    public static String serializeMiniMessage(@NotNull Component component) {
        return MINI_MESSAGE.serialize(component);
    }

    /**
     * Create gradient text
     *
     * @param text Text to apply gradient to
     * @param startColor Start color (hex)
     * @param endColor End color (hex)
     * @return Gradient formatted text
     */
    @NotNull
    public static String createGradient(@NotNull String text, @NotNull String startColor, @NotNull String endColor) {
        return "<gradient:" + startColor + ":" + endColor + ">" + text + "</gradient>";
    }

    /**
     * Validate hex color code
     *
     * @param hex Hex color code
     * @return True if valid
     */
    public static boolean isValidHexColor(@NotNull String hex) {
        return hex.matches("^#?[A-Fa-f0-9]{6}$");
    }

    /**
     * Convert RGB to hex
     *
     * @param r Red component (0-255)
     * @param g Green component (0-255)
     * @param b Blue component (0-255)
     * @return Hex color code
     */
    @NotNull
    public static String rgbToHex(int r, int g, int b) {
        return String.format("#%02x%02x%02x",
                Math.max(0, Math.min(255, r)),
                Math.max(0, Math.min(255, g)),
                Math.max(0, Math.min(255, b))
        );
    }

    /**
     * Parse hex color to RGB components
     *
     * @param hex Hex color code
     * @return RGB array [r, g, b] or null if invalid
     */
    @Nullable
    public static int[] hexToRgb(@NotNull String hex) {
        if (!isValidHexColor(hex)) {
            return null;
        }

        hex = hex.replace("#", "");
        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return new int[]{r, g, b};
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
