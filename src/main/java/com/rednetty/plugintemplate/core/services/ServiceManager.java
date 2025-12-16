package com.rednetty.plugintemplate.core.services;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Service locator and dependency injection manager
 */
public class ServiceManager {

    private final Map<Class<?>, Object> services = new HashMap<>();

    public <T> void registerService(@NotNull Class<T> serviceClass, @NotNull T service) {
        services.put(serviceClass, service);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getService(@NotNull Class<T> serviceClass) {
        return (T) services.get(serviceClass);
    }

    public void unregisterService(@NotNull Class<?> serviceClass) {
        services.remove(serviceClass);
    }
}
