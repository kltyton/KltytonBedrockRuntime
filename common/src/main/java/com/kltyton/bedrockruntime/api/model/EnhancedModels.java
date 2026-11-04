package com.kltyton.bedrockruntime.api.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kltyton.bedrockruntime.api.bone.key.ObbBoneKey;
import com.kltyton.bedrockruntime.geometry.mesh.MeshCaptureData;
import com.kltyton.bedrockruntime.geometry.obb.ObbGeometry;
import com.kltyton.bedrockruntime.model.resource.EnhancedModelData;
import com.kltyton.bedrockruntime.model.binding.ModelConsumers;
import com.kltyton.bedrockruntime.animation.graph.GraphDefinition;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Registers immutable model geometry on both logical sides during mod setup or resource reload. */
public final class EnhancedModels {
    private static final Map<String, Asset> ASSETS = new ConcurrentHashMap<>();
    private static final Map<EntityType<?>, Function<Entity, String>> ENTITY_MODELS = new ConcurrentHashMap<>();
    private static final Map<String, ModelConsumers> CONSUMERS = new ConcurrentHashMap<>();
    private static volatile Function<String, GraphDefinition> graphResolver = EnhancedModels::registeredGraph;

    private EnhancedModels() { }

    public static EnhancedModelData registerResource(String resource, Class<?> owner) {
        validateResource(resource);
        String[] parts = resource.split(":", 2);
        String path = "/assets/" + parts[0] + "/" + parts[1];
        try (InputStream stream = owner.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalArgumentException("Missing model resource " + resource + " in " + owner.getName());
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return registerResource(resource, JsonParser.parseReader(reader).getAsJsonObject());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read model resource " + resource, exception);
        }
    }

    public static EnhancedModelData registerResource(String resource, JsonObject json) {
        validateResource(resource);
        EnhancedModelData definition = EnhancedModelData.parse(resource, json);
        Map<String, Integer> cubes = new LinkedHashMap<>();
        var geometry = json.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        if (geometry.has("bones")) {
            for (var element : geometry.getAsJsonArray("bones")) {
                var bone = element.getAsJsonObject();
                cubes.put(bone.get("name").getAsString(), bone.has("cubes") ? bone.getAsJsonArray("cubes").size() : 0);
            }
        }
        ASSETS.put(resource, new Asset(definition, Map.copyOf(cubes)));
        return definition;
    }

    public static boolean isRegistered(String resource) { return ASSETS.containsKey(resource); }

    /** Uses the currently baked client model; dedicated servers read the registered immutable asset. */
    public static GraphDefinition graph(String resource) {
        return graphResolver.apply(resource);
    }

    public static void installClientGraphResolver(Function<String, GraphDefinition> resolver) {
        graphResolver = java.util.Objects.requireNonNull(resolver);
    }

    private static GraphDefinition registeredGraph(String resource) {
        return requireAsset(resource).definition.graph().orElseThrow(
                () -> new IllegalArgumentException(resource + ": missing kltyton_bedrock_runtime.graph"));
    }

    /** Declare programmatic, mask, attachment and rendering consumers before the resource is baked. */
    public static void registerConsumers(String resource, ModelConsumers consumers) {
        validateResource(resource);
        CONSUMERS.put(resource, java.util.Objects.requireNonNull(consumers));
    }

    public static ModelConsumers consumers(String resource) {
        return CONSUMERS.getOrDefault(resource, ModelConsumers.empty());
    }

    public static void bindEntity(EntityType<?> type, String resource) {
        requireAsset(resource);
        bindEntity(type, entity -> resource);
    }

    /** The resolver reads authoritative entity state; it must return a previously registered resource. */
    public static void bindEntity(EntityType<?> type, Function<Entity, String> resource) {
        ENTITY_MODELS.put(type, resource);
    }

    public static Optional<ObbGeometry> resolveGeometry(Entity entity, ObbBoneKey key, ObbGeometry reported) {
        Function<Entity, String> resolver = ENTITY_MODELS.get(entity.getType());
        if (!"gecko".equals(key.source()) || resolver == null) {
            return Optional.of(reported);
        }
        Asset asset = requireAsset(resolver.apply(entity));
        MeshCaptureData mesh = asset.definition.captureData().get(key.name());
        int cubeCount = asset.cubes.getOrDefault(key.name(), 0);
        if (key.cubeIndex() < cubeCount) {
            return Optional.of(reported);
        }
        int region = key.cubeIndex() - cubeCount;
        if (mesh == null || region >= mesh.regions().size()) {
            return Optional.empty();
        }
        AABB bounds = mesh.regions().get(region).localBounds();
        if (!sameBounds(bounds, reported.localBounds())) {
            return Optional.empty();
        }
        return Optional.of(ObbGeometry.fromLocalBounds(bounds, reported.localToWorld()).withMesh(mesh.mesh()));
    }

    private static Asset requireAsset(String resource) {
        Asset asset = ASSETS.get(resource);
        if (asset == null) {
            throw new IllegalArgumentException("Enhanced model has not been registered: " + resource);
        }
        return asset;
    }

    private static boolean sameBounds(AABB expected, AABB reported) {
        double epsilon = 1.0E-3;
        return Math.abs(expected.minX - reported.minX) <= epsilon && Math.abs(expected.minY - reported.minY) <= epsilon
                && Math.abs(expected.minZ - reported.minZ) <= epsilon && Math.abs(expected.maxX - reported.maxX) <= epsilon
                && Math.abs(expected.maxY - reported.maxY) <= epsilon && Math.abs(expected.maxZ - reported.maxZ) <= epsilon;
    }

    private static void validateResource(String resource) {
        if (resource == null || !resource.matches("[a-z0-9_.-]+:[a-z0-9/._-]+") || resource.contains("..")) {
            throw new IllegalArgumentException("Invalid enhanced model resource: " + resource);
        }
    }

    private record Asset(EnhancedModelData definition, Map<String, Integer> cubes) { }
}
