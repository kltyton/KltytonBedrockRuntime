package com.kltyton.bedrockruntime.client.particle.model.internal;

import com.google.gson.JsonObject;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelParticleModelType;

import java.util.Map;

/** Immutable background reload result; baking and snapshot publication happen in apply. */
public record ModelParticlePrepared(Map<String, PendingModel> descriptors, Map<String, String> failures) {
    public static final ModelParticlePrepared EMPTY = new ModelParticlePrepared(Map.of(), Map.of());

    public ModelParticlePrepared {
        descriptors = Map.copyOf(descriptors);
        failures = Map.copyOf(failures);
    }

    public record PendingModel(String descriptorId, ModelParticleModelType type,
                               String modelResource, String nativeModelId, JsonObject vanillaModel) {
        public PendingModel {
            modelResource = modelResource == null ? "" : modelResource;
            nativeModelId = nativeModelId == null ? "" : nativeModelId;
            vanillaModel = vanillaModel == null ? null : vanillaModel.deepCopy();
        }

        @Override
        public JsonObject vanillaModel() {
            return vanillaModel == null ? null : vanillaModel.deepCopy();
        }
    }
}
