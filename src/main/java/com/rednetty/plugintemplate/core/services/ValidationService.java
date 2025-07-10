package com.rednetty.plugintemplate.core.services;


import com.rednetty.plugintemplate.PluginTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * ValidationService - Data validation and sanitization
 *
 * Features:
 * - Input validation
 * - Data sanitization
 * - Format checking
 * - Security validation
 * - Custom validators
 *
 * @author RedNetty
 * @version 1.0.0
 */
public class ValidationService {

    private final PluginTemplate plugin;
    private final Map<String, Predicate<String>> customValidators = new ConcurrentHashMap<>();

    public ValidationService(@NotNull PluginTemplate plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize the validation service
     */
    public void initialize() {
        // Register default validators
        registerDefaultValidators();

        plugin.getLogger().info("ValidationService initialized successfully!");
    }

    /**
     * Register default validators
     */
    private void registerDefaultValidators() {
        // Player name validator
        customValidators.put("player_name", name ->
                name != null && name.matches("^[a-zA-Z0-9_]{3,16}$"));

        // Email validator
        customValidators.put("email", email ->
                email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"));

        // URL validator
        customValidators.put("url", url -> {
            if (url == null) return false;
            try {
                new java.net.URL(url);
                return true;
            } catch (java.net.MalformedURLException e) {
                return false;
            }
        });

        // UUID validator
        customValidators.put("uuid", uuid -> {
            if (uuid == null) return false;
            try {
                UUID.fromString(uuid);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        });
    }

    /**
     * Validate string input
     *
     * @param input Input to validate
     * @param validatorName Validator name
     * @return True if valid
     */
    public boolean validate(@Nullable String input, @NotNull String validatorName) {
        Predicate<String> validator = customValidators.get(validatorName);
        return validator != null && validator.test(input);
    }

    /**
     * Register custom validator
     *
     * @param name Validator name
     * @param validator Validator function
     */
    public void registerValidator(@NotNull String name, @NotNull Predicate<String> validator) {
        customValidators.put(name, validator);
    }

    /**
     * Sanitize string input
     *
     * @param input Input to sanitize
     * @return Sanitized input
     */
    @NotNull
    public String sanitizeInput(@Nullable String input) {
        if (input == null) {
            return "";
        }

        // Remove potential harmful characters
        return input.replaceAll("[<>\"'&]", "")
                .trim()
                .substring(0, Math.min(input.length(), 255)); // Limit length
    }

    /**
     * Validate number range
     *
     * @param value Value to check
     * @param min Minimum value
     * @param max Maximum value
     * @return True if in range
     */
    public boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    /**
     * Validate string length
     *
     * @param input Input string
     * @param minLength Minimum length
     * @param maxLength Maximum length
     * @return True if length is valid
     */
    public boolean isValidLength(@Nullable String input, int minLength, int maxLength) {
        if (input == null) {
            return minLength <= 0;
        }

        int length = input.length();
        return length >= minLength && length <= maxLength;
    }

    /**
     * Check if string contains only allowed characters
     *
     * @param input Input string
     * @param allowedPattern Regex pattern for allowed characters
     * @return True if valid
     */
    public boolean containsOnlyAllowed(@Nullable String input, @NotNull String allowedPattern) {
        return input != null && input.matches(allowedPattern);
    }

    /**
     * Validate IP address format
     *
     * @param ip IP address string
     * @return True if valid IP
     */
    public boolean isValidIPAddress(@Nullable String ip) {
        if (ip == null) return false;

        String ipv4Pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        return ip.matches(ipv4Pattern);
    }

    /**
     * Validate port number
     *
     * @param port Port number
     * @return True if valid port
     */
    public boolean isValidPort(int port) {
        return port >= 1 && port <= 65535;
    }

    /**
     * Check if validation service is healthy
     *
     * @return True if healthy
     */
    public boolean isHealthy() {
        return !customValidators.isEmpty();
    }

    /**
     * Shutdown the validation service
     */
    public void shutdown() {
        customValidators.clear();
        plugin.getLogger().info("ValidationService shutdown completed!");
    }
}