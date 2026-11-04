package com.kltyton.bedrockruntime.client.particle.model.api;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Process-wide registry for model-particle geometry and {@code particle_models/*.json} descriptors.
 *
 * <p>Resource descriptors are published as one immutable snapshot by the client reload listener;
 * add-on mods may register programmatic models at any time. Reads never touch resources.</p>
 */
public final class ModelParticleModels {
    private static final Map<String, ModelParticleModel> PROGRAMMATIC = new ConcurrentHashMap<>();
    private static volatile Snapshot snapshot = Snapshot.EMPTY;
    private static final AtomicLong GENERATION = new AtomicLong();

    private ModelParticleModels() {
    }

    public static void register(String id, ModelParticleModel model) {
        String key = canonical(id);
        Objects.requireNonNull(model, "model");
        PROGRAMMATIC.put(key, model);
        GENERATION.incrementAndGet();
    }

    public static void register(String id, ModelParticleSource source) {
        register(id, Objects.requireNonNull(source, "source").resolve());
    }

    public static void unregister(String id) {
        String key = canonical(id);
        if (PROGRAMMATIC.remove(key) != null) {
            GENERATION.incrementAndGet();
        }
    }

    /** Atomically replaces all resource-derived models and descriptors. */
    public static void publishResourceSnapshot(Map<String, ModelParticleModel> models,
                                               Map<String, ModelParticleModelType> types) {
        snapshot = new Snapshot(copy(models), copy(types));
        GENERATION.incrementAndGet();
    }

    public static void clearResourceRegistrations() {
        snapshot = new Snapshot(Map.of(), Map.of());
        GENERATION.incrementAndGet();
    }

    public static long generation() {
        return GENERATION.get();
    }

    public static ModelParticleModelType type(String id) {
        return snapshot.types().get(canonical(id));
    }

    public static ModelParticleModel get(String id) {
        String key = canonical(id);
        ModelParticleModel model = PROGRAMMATIC.get(key);
        if (model == null) {
            model = snapshot.models().get(key);
        }
        if (model == null) {
            ModelParticleModelType type = snapshot.types().get(key);
            if (type != null) {
                String target = canonical(type.modelId());
                model = PROGRAMMATIC.get(target);
                if (model == null) {
                    model = snapshot.models().get(target);
                }
            }
        }
        return model;
    }

    public static ModelParticleModel require(String id) {
        ModelParticleModel model = get(id);
        if (model == null) {
            throw new IllegalArgumentException("Unknown model particle model: " + id);
        }
        return model;
    }

    public static Set<String> ids() {
        java.util.LinkedHashSet<String> result = new java.util.LinkedHashSet<>(snapshot.models().keySet());
        result.addAll(snapshot.types().keySet());
        result.addAll(PROGRAMMATIC.keySet());
        return Set.copyOf(result);
    }

    /** Model ids currently published by the last resource reload. */
    public static Set<String> resourceModelIds() {
        return snapshot.models().keySet();
    }

    /** Immutable view of explicit registrations, including overrides of resource model ids. */
    public static Map<String, ModelParticleModel> programmaticModels() {
        return Map.copyOf(PROGRAMMATIC);
    }

    public static Set<String> resourceTypeIds() {
        return snapshot.types().keySet();
    }

    public static String canonical(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("model particle id must not be blank");
        }
        String value = id.trim();
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        int separator = value.indexOf(':');
        if (separator <= 0) {
            return "minecraft:" + value;
        }
        if (separator == value.length() - 1) {
            throw new IllegalArgumentException("Invalid model particle id: " + id);
        }
        String namespace = value.substring(0, separator).toLowerCase(java.util.Locale.ROOT);
        if (!namespace.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException("Invalid model particle namespace: " + id);
        }
        return namespace + ":" + value.substring(separator + 1);
    }

    private static <K, V> Map<K, V> copy(Map<K, V> source) {
        return source == null || source.isEmpty() ? Map.of() : Map.copyOf(source);
    }

    private record Snapshot(Map<String, ModelParticleModel> models,
                            Map<String, ModelParticleModelType> types) {
        private static final Snapshot EMPTY = new Snapshot(Map.of(), Map.of());
    }
}

