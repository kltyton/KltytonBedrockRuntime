package com.kltyton.bedrockruntime.client.particle.api.event;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Loader-neutral synchronous event bus for particle API registration and lifecycle events.
 */
public final class ParticleEvents {
    private static final Map<Class<?>, List<Consumer<?>>> LISTENERS = new ConcurrentHashMap<>();

    private ParticleEvents() {
    }

    public static <T> void addListener(Class<T> eventType, Consumer<? super T> listener) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(listener, "listener");
        LISTENERS.computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public static <T> void removeListener(Class<T> eventType, Consumer<? super T> listener) {
        List<Consumer<?>> listeners = LISTENERS.get(eventType);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    public static void clear() {
        LISTENERS.clear();
    }

    @SuppressWarnings("unchecked")
    public static <T> T post(T event) {
        Objects.requireNonNull(event, "event");
        for (Map.Entry<Class<?>, List<Consumer<?>>> entry : LISTENERS.entrySet()) {
            if (!entry.getKey().isInstance(event)) {
                continue;
            }
            for (Consumer<?> listener : entry.getValue()) {
                ((Consumer<T>) listener).accept(event);
            }
        }
        return event;
    }
}
