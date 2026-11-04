package com.kltyton.bedrockruntime.client.particle.model.resource;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelParticleModel;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelParticleModelType;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelParticleModels;
import com.kltyton.bedrockruntime.client.particle.model.bake.ModelParticleBakery;
import com.kltyton.bedrockruntime.client.particle.model.bake.NativeGeometryParticleBakery;
import com.kltyton.bedrockruntime.client.particle.model.internal.ModelParticleDiagnostics;
import com.kltyton.bedrockruntime.client.particle.model.internal.ModelParticlePrepared;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Background prepare and game-thread apply for {@code particle_models/*.json} on 1.20.1. */
public final class ModelParticleLoader {
    private static final FileToIdConverter MODELS = FileToIdConverter.json("particle_models");

    private ModelParticleLoader() {
    }

    public static ModelParticlePrepared prepare(ResourceManager resources) {
        Map<String, ModelParticlePrepared.PendingModel> descriptors = new LinkedHashMap<>();
        Map<String, String> failures = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Resource> entry : MODELS.listMatchingResources(resources).entrySet()) {
            ResourceLocation fileId = MODELS.fileToId(entry.getKey());
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                ModelParticleModelType type = ModelParticleModelType.parse(json);
                String modelResource = modelResource(type);
                String nativeModelId = type.variant() == ModelParticleModelType.Variant.GEOMETRY
                        ? nativeModelId(type) : type.modelId();
                JsonObject vanilla = type.variant() == ModelParticleModelType.Variant.GEOMETRY
                        ? readTextureModel(resources, nativeModelId, fileId.toString()) : null;
                descriptors.put(fileId.toString(), new ModelParticlePrepared.PendingModel(
                        fileId.toString(), type, modelResource, nativeModelId, vanilla));
            } catch (Exception exception) {
                failures.put(fileId.toString(), entry.getKey().toString());
                ModelParticleDiagnostics.descriptorFailure(fileId.toString(), entry.getKey().toString(), exception);
            }
        }
        return new ModelParticlePrepared(descriptors, failures);
    }

    public static void apply(ModelParticlePrepared prepared) {
        Map<String, ModelParticleModel> models = new LinkedHashMap<>();
        Map<String, ModelParticleModelType> types = new LinkedHashMap<>();
        for (ModelParticlePrepared.PendingModel pending : prepared.descriptors().values()) {
            ModelParticleModelType type = pending.type();
            String id = pending.descriptorId();
            try {
                ModelParticleModel model = switch (type.variant()) {
                    case HARDCODE -> ModelParticleBakery.hardcoded(type, id);
                    case GEOMETRY -> NativeGeometryParticleBakery.bake(id, pending.nativeModelId(), type);
                    case GECKOLIB, POLY_MESH -> ModelParticleBakery.gecko(type, id);
                };
                models.put(id, model);
                types.put(id, type);
                models.put(ModelParticleModels.canonical(type.modelId()), model);
            } catch (Exception exception) {
                ModelParticleDiagnostics.bakeFailure(id, pending.modelResource(), exception);
                ModelParticleModel fallback = ModelParticleDiagnostics.missingModel(id);
                models.put(id, fallback);
                types.put(id, type);
                models.put(ModelParticleModels.canonical(type.modelId()), fallback);
            }
        }
        prepared.failures().forEach((id, resource) -> {
            ModelParticleModel fallback = ModelParticleDiagnostics.missingModel(id);
            models.put(id, fallback);
        });
        ModelParticleModels.publishResourceSnapshot(models, types);
    }

    public static Map<String, String> geometryModelIds(ResourceManager resources) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Resource> entry : MODELS.listMatchingResources(resources).entrySet()) {
            ResourceLocation fileId = MODELS.fileToId(entry.getKey());
            try (Reader reader = entry.getValue().openAsReader()) {
                ModelParticleModelType type = ModelParticleModelType.parse(
                        JsonParser.parseReader(reader).getAsJsonObject());
                if (type.variant() == ModelParticleModelType.Variant.GEOMETRY) {
                    result.put(fileId.toString(), nativeModelId(type));
                }
            } catch (Exception exception) {
                ModelParticleDiagnostics.descriptorFailure(fileId.toString(), entry.getKey().toString(), exception);
            }
        }
        return result;
    }

    private static String modelResource(ModelParticleModelType type) {
        ResourceLocation location = ResourceLocation.tryParse(type.modelId());
        if (location == null) {
            return type.modelId();
        }
        if (type.variant() != ModelParticleModelType.Variant.GEOMETRY) {
            return location.toString();
        }
        String path = location.getPath();
        if (!path.startsWith("models/")) {
            path = "models/" + path;
        }
        if (!path.endsWith(".json")) {
            path = path + ".json";
        }
        return location.getNamespace() + ":" + path;
    }

    private static String nativeModelId(ModelParticleModelType type) {
        ResourceLocation location = ResourceLocation.tryParse(type.modelId());
        if (location == null) {
            throw new IllegalArgumentException("Invalid geometry model id " + type.modelId());
        }
        String path = location.getPath();
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - 5);
        }
        if (path.startsWith("models/")) {
            path = path.substring("models/".length());
        }
        if (path.isBlank()) {
            throw new IllegalArgumentException("Invalid geometry model id " + type.modelId());
        }
        return location.getNamespace() + ":" + path;
    }

    private static JsonObject readTextureModel(ResourceManager resources, String nativeModelId, String owner) {
        JsonObject merged = new JsonObject();
        mergeTextureModel(resources, nativeModelId, merged, new LinkedHashSet<>(), owner, true);
        resolveTextureReferences(merged);
        return merged;
    }

    private static void mergeTextureModel(ResourceManager resources, String modelId, JsonObject merged,
                                          Set<String> visited, String owner, boolean required) {
        if (!visited.add(modelId)) {
            return;
        }
        JsonObject model = readModelFile(resources, modelId, owner);
        if (model == null) {
            if (required) {
                throw new IllegalArgumentException(owner + ": missing geometry model " + modelId);
            }
            return;
        }
        if (model.has("parent") && model.get("parent").isJsonPrimitive()) {
            String parent = model.get("parent").getAsString();
            mergeTextureModel(resources, qualifyParent(parent), merged, visited, owner, false);
        }
        if (model.has("textures") && model.get("textures").isJsonObject()) {
            JsonObject textures = merged.has("textures") && merged.get("textures").isJsonObject()
                    ? merged.getAsJsonObject("textures") : new JsonObject();
            for (Map.Entry<String, JsonElement> entry : model.getAsJsonObject("textures").entrySet()) {
                textures.add(entry.getKey(), entry.getValue().deepCopy());
            }
            merged.add("textures", textures);
        }
        if (model.has("elements")) {
            merged.add("elements", model.get("elements").deepCopy());
        }
        if (model.has("texture_size")) {
            merged.add("texture_size", model.get("texture_size").deepCopy());
        }
    }

    private static JsonObject readModelFile(ResourceManager resources, String modelId, String owner) {
        ResourceLocation location = ResourceLocation.tryParse(modelId);
        if (location == null) {
            throw new IllegalArgumentException(owner + ": invalid geometry model id " + modelId);
        }
        ResourceLocation file = new ResourceLocation(location.getNamespace(),
                "models/" + location.getPath() + ".json");
        Resource resource = resources.getResource(file).orElse(null);
        if (resource == null) {
            return null;
        }
        try (Reader reader = resource.openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception exception) {
            throw new IllegalArgumentException(owner + ": cannot read geometry model " + modelId, exception);
        }
    }

    private static String qualifyParent(String parent) {
        return parent.indexOf(':') >= 0 ? parent : "minecraft:" + parent;
    }

    private static void resolveTextureReferences(JsonObject merged) {
        if (!merged.has("textures") || !merged.get("textures").isJsonObject()) {
            return;
        }
        JsonObject textures = merged.getAsJsonObject("textures");
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                values.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        for (String key : new ArrayList<>(values.keySet())) {
            String resolved = resolveTextureReference(key, values, new LinkedHashSet<>());
            if (resolved != null) {
                textures.addProperty(key, resolved);
            }
        }
    }

    private static String resolveTextureReference(String key, Map<String, String> values, Set<String> visited) {
        String value = values.get(key);
        if (value == null || !value.startsWith("#")) {
            return value;
        }
        String target = value.substring(1);
        if (!visited.add(target)) {
            return null;
        }
        return resolveTextureReference(target, values, visited);
    }
}
