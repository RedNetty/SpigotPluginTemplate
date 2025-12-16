package com.rednetty.plugintemplate.exceptions;

/**
 * Base exception class for plugin-specific errors
 */
public class PluginException extends Exception {

    public PluginException(String message) {
        super(message);
    }

    public PluginException(String message, Throwable cause) {
        super(message, cause);
    }
}
