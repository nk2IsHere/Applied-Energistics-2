package net.neoforged.neoforge.registries.callback;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public enum RegistryCallbackHolder {
    INSTANCE;

    private final Map<Registry<?>, List<RegistryCallback<?>>> callbacks = new ConcurrentHashMap<>();

    public void addCallback(Registry<?> registry, RegistryCallback<?> callback) {
        callbacks.computeIfAbsent(registry, key -> new CopyOnWriteArrayList<>()).add(callback);
    }

    public <T> void onAdd(Registry<T> registry, ResourceKey<T> key, T object) {
        var registryCallbacks = callbacks.get(registry);
        if (registryCallbacks != null) {
            registryCallbacks
                .stream()
                .filter(AddCallback.class::isInstance)
                .map(callback -> (AddCallback<T>) callback)
                .forEach(callback -> callback.onAdd(registry, key, object));
        }
    }

    public <T> void onBake(Registry<T> registry) {
        var registryCallbacks = callbacks.get(registry);
        if (registryCallbacks != null) {
            registryCallbacks
                .stream()
                .filter(BakeCallback.class::isInstance)
                .map(callback -> (BakeCallback<T>) callback)
                .forEach(callback -> callback.onBake(registry));
        }
    }

    public <T> void onClear(Registry<T> registry, boolean full) {
        var registryCallbacks = callbacks.get(registry);
        if (registryCallbacks != null) {
            registryCallbacks
                .stream()
                .filter(ClearCallback.class::isInstance)
                .map(callback -> (ClearCallback<T>) callback)
                .forEach(callback -> callback.onClear(registry, full));
        }
    }

    public void clear() {
        callbacks.clear();
    }

    public void clear(Registry<?> registry) {
        callbacks.remove(registry);
    }
}
