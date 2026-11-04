package com.kltyton.bedrockruntime.client.compat.geckolib.loading;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.ResourceLocation;
import com.kltyton.bedrockruntime.api.model.EnhancedModels;
import com.kltyton.bedrockruntime.animation.graph.GraphDefinition;
import com.kltyton.bedrockruntime.client.compat.geckolib.model.MeshGeoBone;
import software.bernie.geckolib.cache.GeckoLibCache;

public final class EnhancedGeometryLoader {
    private static final String AUTO_NAMESPACE = "kltyton_bedrock_runtime";
    private static final Set<String> RESOURCES = ConcurrentHashMap.newKeySet();
    private static final Set<String> DOUBLE_SIDED = ConcurrentHashMap.newKeySet();
    private static volatile boolean active;

    private EnhancedGeometryLoader() {
    }

    public static synchronized void registerResource(String resource) {
        registerResource(resource, false);
    }

    public static synchronized void registerResource(String resource, boolean doubleSided) {
        String canonical = canonical(resource);
        RESOURCES.add(canonical);
        if (doubleSided) {
            DOUBLE_SIDED.add(canonical);
        } else {
            DOUBLE_SIDED.remove(canonical);
        }
    }

    public static synchronized void register() {
        EnhancedModels.installClientGraphResolver(EnhancedGeometryLoader::graph);
        active = true;
    }

    private static GraphDefinition graph(String resource) {
        ResourceLocation id = ResourceLocation.tryParse(resource);
        if (id == null) {
            throw new IllegalArgumentException("Invalid graph model resource: " + resource);
        }
        var model = GeckoLibCache.getBakedModels().get(id);
        var data = model == null ? null : MeshGeoBone.modelData(model);
        if (data == null) {
            throw new IllegalStateException(resource + ": graph model has not been baked with the enhanced loader");
        }
        return data.graph().orElseThrow(
                () -> new IllegalArgumentException(resource + ": missing kltyton_bedrock_runtime.graph"));
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isRegisteredResource(String resource) {
        String canonical = canonicalOrNull(resource);
        return canonical != null && RESOURCES.contains(canonical);
    }

    public static boolean shouldUse(ResourceLocation resource) {
        if (!active || resource == null) {
            return false;
        }
        return AUTO_NAMESPACE.equals(resource.getNamespace()) || RESOURCES.contains(resource.toString())
                || com.kltyton.bedrockruntime.api.model.EnhancedModels.isRegistered(resource.toString());
    }

    public static boolean shouldUse(String resource) {
        if (!active || resource == null) {
            return false;
        }
        String canonical = canonicalOrNull(resource);
        return canonical != null && (canonical.startsWith(AUTO_NAMESPACE + ":") || RESOURCES.contains(canonical)
                || com.kltyton.bedrockruntime.api.model.EnhancedModels.isRegistered(canonical));
    }

    public static boolean doubleSided(ResourceLocation resource) {
        return resource != null && DOUBLE_SIDED.contains(resource.toString());
    }

    public static boolean doubleSided(String resource) {
        String canonical = canonicalOrNull(resource);
        return canonical != null && DOUBLE_SIDED.contains(canonical);
    }

    private static String canonical(String resource) {
        if (resource == null || resource.isBlank()) {
            throw new IllegalArgumentException("resource must not be blank");
        }
        String value = resource.trim();
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        int separator = value.indexOf(':');
        if (separator <= 0 || separator == value.length() - 1) {
            throw new IllegalArgumentException("resource must be namespaced: " + resource);
        }
        String namespace = value.substring(0, separator).toLowerCase(Locale.ROOT);
        if (!namespace.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException("invalid resource namespace: " + resource);
        }
        return namespace + ":" + value.substring(separator + 1);
    }

    private static String canonicalOrNull(String resource) {
        if (resource == null || resource.isBlank()) {
            return null;
        }
        try {
            return canonical(resource);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
