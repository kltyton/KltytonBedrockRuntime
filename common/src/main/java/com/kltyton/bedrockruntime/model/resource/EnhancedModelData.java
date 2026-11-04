package com.kltyton.bedrockruntime.model.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kltyton.bedrockruntime.geometry.mesh.BedrockMeshParser;
import com.kltyton.bedrockruntime.geometry.mesh.TriangleMesh;
import com.kltyton.bedrockruntime.geometry.mesh.MeshCaptureData;
import com.kltyton.bedrockruntime.animation.graph.GraphDefinition;
import com.kltyton.bedrockruntime.animation.graph.GraphJsonParser;
import com.kltyton.bedrockruntime.animation.graph.GraphParseException;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/** Immutable geometry and author declarations for a single baked resource revision. */
public record EnhancedModelData(String resource, long generation, Map<String, TriangleMesh> meshes,
                                Mode mode, Set<String> staticBones, Set<String> preservedBones, boolean doubleSided,
                                Map<String, MeshCaptureData> captureData, Optional<GraphDefinition> graph) {
    private static final AtomicLong GENERATIONS = new AtomicLong();

    public enum Mode { TREE, BAKED }

    public EnhancedModelData {
        meshes = Map.copyOf(meshes);
        staticBones = Set.copyOf(staticBones);
        preservedBones = Set.copyOf(preservedBones);
        captureData = Map.copyOf(captureData);
        graph = java.util.Objects.requireNonNull(graph);
    }

    public static EnhancedModelData parse(String resource, JsonObject json) {
        JsonArray geometries = json.getAsJsonArray("minecraft:geometry");
        if (geometries == null || geometries.isEmpty()) {
            throw new IllegalArgumentException(resource + ": missing minecraft:geometry");
        }
        JsonObject geometry = geometries.get(0).getAsJsonObject();
        JsonObject description = geometry.getAsJsonObject("description");
        float width = number(description, "texture_width", 16);
        float height = number(description, "texture_height", 16);
        Map<String, TriangleMesh> meshes = new LinkedHashMap<>();
        Set<String> locatorBones = new LinkedHashSet<>();
        Set<String> boneNames = new LinkedHashSet<>();
        JsonArray bones = geometry.getAsJsonArray("bones");
        if (bones != null) {
            for (JsonElement element : bones) {
                JsonObject bone = element.getAsJsonObject();
                String name = bone.get("name").getAsString();
                if (!boneNames.add(name)) {
                    throw new IllegalArgumentException(resource + ": duplicate bone '" + name + "'");
                }
                if (bone.has("locators") && !bone.getAsJsonObject("locators").entrySet().isEmpty()) {
                    locatorBones.add(name);
                }
                if (bone.has("poly_mesh")) {
                    TriangleMesh mesh = BedrockMeshParser.parse(resource, name, bone.getAsJsonObject("poly_mesh"), width, height);
                    if (meshes.putIfAbsent(name, mesh) != null) {
                        throw new IllegalArgumentException(resource + ": duplicate mesh bone '" + name + "'");
                    }
                }
            }
        }
        JsonObject options = json.has("kltyton_bedrock_runtime") ? json.getAsJsonObject("kltyton_bedrock_runtime") : new JsonObject();
        int version = options.has("format_version") ? options.get("format_version").getAsInt() : 1;
        if (version != 1) {
            throw new IllegalArgumentException(resource + ": unsupported kltyton_bedrock_runtime.format_version " + version);
        }
        String modeName = options.has("model_mode") ? options.get("model_mode").getAsString() : "tree";
        Mode mode = switch (modeName) {
            case "tree" -> Mode.TREE;
            case "baked" -> Mode.BAKED;
            default -> throw new IllegalArgumentException(resource + ": unknown model_mode '" + modeName + "'");
        };
        Map<String, MeshCaptureData> captureData = new LinkedHashMap<>();
        meshes.forEach((name, mesh) -> captureData.put(name, MeshCaptureData.of(mesh)));
        Set<String> preserved = new LinkedHashSet<>(names(options, "preserve_bones"));
        preserved.addAll(locatorBones);
        Set<String> staticBones = names(options, "static_bones");
        for (String name : staticBones) {
            if (!boneNames.contains(name)) {
                throw new IllegalArgumentException(resource + ": static_bones references missing bone '" + name + "'");
            }
        }
        for (String name : preserved) {
            if (!boneNames.contains(name)) {
                throw new IllegalArgumentException(resource + ": preserve_bones references missing bone '" + name + "'");
            }
        }
        return new EnhancedModelData(resource, GENERATIONS.incrementAndGet(), meshes, mode,
                staticBones, preserved,
                options.has("double_sided") && options.get("double_sided").getAsBoolean(), captureData,
                parseGraph(resource, options));
    }

    private static Optional<GraphDefinition> parseGraph(String resource, JsonObject options) {
        if (!options.has("graph")) {
            return Optional.empty();
        }
        try {
            return Optional.of(GraphJsonParser.parse(options.get("graph")));
        } catch (GraphParseException exception) {
            throw new IllegalArgumentException(resource + "#kltyton_bedrock_runtime.graph: " + exception.getMessage(), exception);
        }
    }

    private static float number(JsonObject object, String key, float fallback) {
        return object != null && object.has(key) ? object.get(key).getAsFloat() : fallback;
    }

    private static Set<String> names(JsonObject object, String key) {
        if (!object.has(key)) {
            return Set.of();
        }
        Set<String> names = new LinkedHashSet<>();
        for (JsonElement element : object.getAsJsonArray(key)) {
            names.add(element.getAsString());
        }
        return Set.copyOf(names);
    }
}
