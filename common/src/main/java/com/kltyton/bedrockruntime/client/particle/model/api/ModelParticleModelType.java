package com.kltyton.bedrockruntime.client.particle.model.api;

import com.google.gson.JsonObject;

import java.util.Locale;

/**
 * Descriptor parsed from {@code particle_models/<id>.json}.
 *
 * <pre>{@code
 * {"variant":"geometry","model_id":"example:model","texture":"example:model_texture",
 *  "material":"cutout","double_sided":false}
 * }</pre>
 */
public record ModelParticleModelType(Variant variant, String modelId, String texture,
                                     ModelMaterial material, boolean doubleSided) {
    public enum Variant {
        HARDCODE,
        GEOMETRY,
        GECKOLIB,
        POLY_MESH;

        public static Variant byName(String value, Variant fallback) {
            if (value == null) {
                return fallback;
            }
            return switch (value.trim().toLowerCase(Locale.ROOT)) {
                case "hardcode", "hardcoded", "modelpart" -> HARDCODE;
                case "geometry", "vanilla", "model" -> GEOMETRY;
                case "geckolib", "gecko" -> GECKOLIB;
                case "poly_mesh", "polymesh", "mesh" -> POLY_MESH;
                default -> fallback;
            };
        }
    }

    public ModelParticleModelType {
        if (variant == null) {
            throw new IllegalArgumentException("model particle variant is required");
        }
        if (modelId == null || modelId.isBlank()) {
            throw new IllegalArgumentException("model particle model_id is required");
        }
        modelId = modelId.trim();
        texture = texture == null ? "" : texture.trim();
        material = material == null ? ModelMaterial.SOLID : material;
    }

    public static ModelParticleModelType parse(JsonObject json) {
        if (json == null) {
            throw new IllegalArgumentException("model particle descriptor is missing");
        }
        String variantName = json.has("variant") && json.get("variant").isJsonPrimitive()
                ? json.get("variant").getAsString() : "geometry";
        String modelId = json.has("model_id") && json.get("model_id").isJsonPrimitive()
                ? json.get("model_id").getAsString() : null;
        String texture = json.has("texture") && json.get("texture").isJsonPrimitive()
                ? json.get("texture").getAsString() : "";
        String materialName = json.has("material") && json.get("material").isJsonPrimitive()
                ? json.get("material").getAsString() : "solid";
        boolean doubleSided = json.has("double_sided") && json.get("double_sided").isJsonPrimitive()
                && json.get("double_sided").getAsBoolean();
        return new ModelParticleModelType(Variant.byName(variantName, Variant.GEOMETRY), modelId, texture,
                ModelMaterial.byName(materialName, ModelMaterial.SOLID), doubleSided);
    }

    @Override
    public String toString() {
        return "ModelParticleModelType{" + variant + " " + modelId + " texture=" + texture
                + " material=" + material + " doubleSided=" + doubleSided + '}';
    }
}
