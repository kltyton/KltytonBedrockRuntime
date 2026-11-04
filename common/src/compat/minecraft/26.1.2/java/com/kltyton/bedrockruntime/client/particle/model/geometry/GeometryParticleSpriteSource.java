package com.kltyton.bedrockruntime.client.particle.model.geometry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelParticleModelType;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class GeometryParticleSpriteSource implements SpriteSource {
    public static final MapCodec<GeometryParticleSpriteSource> CODEC = MapCodec.unit(new GeometryParticleSpriteSource());
    private static final FileToIdConverter MODELS = FileToIdConverter.json("particle_models");

    @Override
    public void run(ResourceManager resources, Output output) {
        Set<Identifier> added = new LinkedHashSet<>();
        for (Map.Entry<Identifier, Resource> entry : MODELS.listMatchingResources(resources).entrySet()) {
            Identifier descriptorId = MODELS.fileToId(entry.getKey());
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject descriptor = JsonParser.parseReader(reader).getAsJsonObject();
                ModelParticleModelType descriptorType = ModelParticleModelType.parse(descriptor);
                if (descriptorType.variant() != ModelParticleModelType.Variant.GEOMETRY) {
                    continue;
                }
                String modelId = nativeModelId(descriptorType.modelId());
                if (modelId == null) {
                    BedrockParticles.LOGGER.error("Geometry particle descriptor '{}' has invalid model_id '{}'",
                            descriptorId, descriptorType.modelId());
                    continue;
                }
                ModelData data = new ModelData();
                if (!mergeModel(resources, modelId, data, new LinkedHashSet<>(), new LinkedHashSet<>(), descriptorId.toString())) {
                    continue;
                }
                resolveTextureReferences(data.textures);
                if (!descriptorType.texture().isBlank()) {
                    addTexture(resources, output, added, descriptorType.texture(), descriptorId.toString());
                }
                String particleTexture = data.textures.get("particle");
                if (particleTexture != null) {
                    addTexture(resources, output, added, particleTexture, descriptorId.toString());
                }
                collectFaceTextures(resources, output, added, data, descriptorId.toString());
            } catch (Exception exception) {
                BedrockParticles.LOGGER.error("Cannot read geometry particle descriptor '{}' while collecting block-atlas sprites",
                        descriptorId, exception);
            }
        }
    }

    private static boolean mergeModel(ResourceManager resources, String modelId, ModelData data,
                                      Set<String> visited, Set<String> path, String owner) {
        Identifier location = Identifier.tryParse(modelId);
        if (location == null) {
            BedrockParticles.LOGGER.error("Geometry model '{}' referenced by '{}' is not a valid model id", modelId, owner);
            return false;
        }
        String key = location.toString();
        if (!path.add(key)) {
            BedrockParticles.LOGGER.error("Geometry model '{}' referenced by '{}' has a parent loop", modelId, owner);
            return false;
        }
        if (!visited.add(key)) {
            path.remove(key);
            return true;
        }
        Resource resource = resources.getResource(modelFile(location)).orElse(null);
        if (resource == null) {
            BedrockParticles.LOGGER.error("Geometry model '{}' referenced by '{}' is missing", modelId, owner);
            path.remove(key);
            return false;
        }
        try (Reader reader = resource.openAsReader()) {
            JsonObject model = JsonParser.parseReader(reader).getAsJsonObject();
            if (model.has("parent") && model.get("parent").isJsonPrimitive()) {
                mergeModel(resources, qualifyParent(model.get("parent").getAsString()), data, visited, path, owner);
            }
            if (model.has("textures") && model.get("textures").isJsonObject()) {
                for (Map.Entry<String, JsonElement> texture : model.getAsJsonObject("textures").entrySet()) {
                    if (texture.getValue().isJsonPrimitive()) {
                        data.textures.put(texture.getKey(), texture.getValue().getAsString());
                    }
                }
            }
            if (model.has("elements") && model.get("elements").isJsonArray()) {
                data.elements = model.getAsJsonArray("elements").deepCopy();
            }
            path.remove(key);
            return true;
        } catch (Exception exception) {
            BedrockParticles.LOGGER.error("Cannot read geometry model '{}' referenced by '{}'", modelId, owner, exception);
            path.remove(key);
            return false;
        }
    }

    private static void collectFaceTextures(ResourceManager resources, Output output, Set<Identifier> added,
                                            ModelData data, String owner) {
        if (data.elements == null) {
            return;
        }
        for (JsonElement element : data.elements) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            if (!object.has("faces") || !object.get("faces").isJsonObject()) {
                continue;
            }
            for (Map.Entry<String, JsonElement> face : object.getAsJsonObject("faces").entrySet()) {
                if (!face.getValue().isJsonObject()) {
                    continue;
                }
                JsonElement texture = face.getValue().getAsJsonObject().get("texture");
                if (texture == null || !texture.isJsonPrimitive()) {
                    continue;
                }
                String resolved = resolveTextureReference(texture.getAsString(), data.textures);
                if (resolved == null) {
                    BedrockParticles.LOGGER.error("Geometry model '{}' face '{}' has an unresolved texture reference '{}'",
                            owner, face.getKey(), texture.getAsString());
                    continue;
                }
                addTexture(resources, output, added, resolved, owner);
            }
        }
    }

    private static void addTexture(ResourceManager resources, Output output, Set<Identifier> added,
                                   String texture, String owner) {
        Identifier id = Identifier.tryParse(texture);
        if (id == null) {
            BedrockParticles.LOGGER.error("Geometry model '{}' references invalid texture '{}'", owner, texture);
            return;
        }
        if (!added.add(id)) {
            return;
        }
        Resource resource = resources.getResource(TEXTURE_ID_CONVERTER.idToFile(id)).orElse(null);
        if (resource == null) {
            BedrockParticles.LOGGER.error("Geometry model '{}' references missing texture '{}'", owner, id);
            return;
        }
        output.add(id, resource);
    }

    private static void resolveTextureReferences(Map<String, String> textures) {
        for (String key : new LinkedHashSet<>(textures.keySet())) {
            String resolved = resolveTextureReference(textures.get(key), textures);
            if (resolved != null) {
                textures.put(key, resolved);
            }
        }
    }

    private static String resolveTextureReference(String texture, Map<String, String> textures) {
        String current = texture;
        Set<String> seen = new LinkedHashSet<>();
        while (current != null && current.startsWith("#")) {
            String target = current.substring(1);
            if (!seen.add(target)) {
                return null;
            }
            current = textures.get(target);
        }
        return current;
    }

    private static String nativeModelId(String modelId) {
        Identifier location = Identifier.tryParse(modelId);
        if (location == null) {
            return null;
        }
        String path = location.getPath();
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - 5);
        }
        if (path.startsWith("models/")) {
            path = path.substring("models/".length());
        }
        return path.isBlank() ? null : location.getNamespace() + ":" + path;
    }

    private static Identifier modelFile(Identifier location) {
        return Identifier.fromNamespaceAndPath(location.getNamespace(), "models/" + location.getPath() + ".json");
    }

    private static String qualifyParent(String parent) {
        return parent.indexOf(':') >= 0 ? parent : "minecraft:" + parent;
    }

    @Override
    public MapCodec<? extends SpriteSource> codec() {
        return CODEC;
    }

    private static final class ModelData {
        private final Map<String, String> textures = new LinkedHashMap<>();
        private JsonArray elements;
    }
}
