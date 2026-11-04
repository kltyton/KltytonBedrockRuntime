package com.kltyton.bedrockruntime.client.particle.model.api;

import java.util.List;
import java.util.Objects;

/**
 * Immutable model-particle geometry.
 *
 * @param id stable model identifier
 * @param texture default sprite identifier in the particle atlas
 * @param parts drawable sections in model space
 * @param material default material for sections that do not override it
 * @param doubleSided default two-sided flag
 */
public record ModelParticleModel(String id, String texture, List<ModelParticlePart> parts,
                                 ModelMaterial material, boolean doubleSided) {
    public ModelParticleModel {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("model particle id must not be blank");
        }
        texture = texture == null ? "" : texture.trim();
        parts = List.copyOf(Objects.requireNonNull(parts, "parts"));
        material = Objects.requireNonNull(material, "material");
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("model particle " + id + " has no drawable parts");
        }
    }

    public ModelParticleModel(String id, String texture, List<ModelParticlePart> parts) {
        this(id, texture, parts, ModelMaterial.SOLID, false);
    }

    public boolean hasDrawableGeometry() {
        for (ModelParticlePart part : parts) {
            if (part.mesh().triangleCount() > 0) {
                return true;
            }
        }
        return false;
    }

    public int triangleCount() {
        int count = 0;
        for (ModelParticlePart part : parts) {
            count += part.mesh().triangleCount();
        }
        return count;
    }
}
