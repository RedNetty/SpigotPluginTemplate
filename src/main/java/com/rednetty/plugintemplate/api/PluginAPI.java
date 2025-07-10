package com.rednetty.plugintemplate.api;

/**
 * Public API for external plugin integration
 */
public class PluginAPI {

    private static PluginAPI instance;

    private PluginAPI() {}

    public static PluginAPI getInstance() {
        if (instance == null) {
            instance = new PluginAPI();
        }
        return instance;
    }
}
