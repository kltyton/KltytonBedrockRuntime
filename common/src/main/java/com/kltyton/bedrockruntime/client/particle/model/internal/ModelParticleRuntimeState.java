package com.kltyton.bedrockruntime.client.particle.model.internal;

import com.kltyton.bedrockruntime.client.particle.model.api.ModelMaterial;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelParticleModel;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelParticleModels;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Objects;

/** Per-particle model selection and cached geometry. Mutated only by the simulation thread. */
public final class ModelParticleRuntimeState {
    private String modelId = "";
    private ModelMaterial material = ModelMaterial.SOLID;
    private boolean doubleSided;
    private ModelParticleModel cached;
    private long cachedGeneration = Long.MIN_VALUE;
    private boolean seenAppearance;
    private ModelParticleModel boundsModel;
    private double boundingRadius;

    public void select(String modelId, ModelMaterial material, boolean doubleSided) {
        String key = modelId == null ? "" : modelId.trim();
        if (!key.equals(this.modelId)) {
            this.modelId = key;
            this.cached = null;
            this.cachedGeneration = Long.MIN_VALUE;
        }
        this.material = Objects.requireNonNullElse(material, ModelMaterial.SOLID);
        this.doubleSided = doubleSided;
        this.seenAppearance = true;
    }

    public boolean seenAppearance() {
        return seenAppearance;
    }

    public String modelId() {
        return modelId;
    }

    public ModelMaterial material() {
        return material;
    }

    public boolean doubleSided() {
        ModelParticleModel model = model();
        return model != null ? model.doubleSided() : doubleSided;
    }

    public ModelMaterial effectiveMaterial() {
        ModelParticleModel model = model();
        return model != null ? model.material() : material;
    }

    /** Model-space radius around the runtime's half-unit model origin, cached per resource model. */
    public double boundingRadius() {
        ModelParticleModel model = model();
        if (boundsModel != model) {
            double squaredRadius = 0.0;
            Vector3f point = new Vector3f();
            for (var part : model.parts()) {
                var mesh = part.mesh();
                if (mesh.triangleCount() == 0) continue;
                Matrix4f transform = part.appendTransform(new Matrix4f());
                for (int corner = 0; corner < 8; corner++) {
                    transform.transformPosition((corner & 1) == 0 ? mesh.minX() : mesh.maxX(),
                            (corner & 2) == 0 ? mesh.minY() : mesh.maxY(),
                            (corner & 4) == 0 ? mesh.minZ() : mesh.maxZ(), point);
                    double x = point.x - 0.5;
                    double y = point.y - 0.5;
                    double z = point.z - 0.5;
                    squaredRadius = Math.max(squaredRadius, x * x + y * y + z * z);
                }
            }
            boundingRadius = Math.sqrt(squaredRadius);
            boundsModel = model;
        }
        return boundingRadius;
    }

    public ModelParticleModel model() {
        if (modelId.isEmpty()) {
            cached = ModelParticleDiagnostics.missingModel("kltyton_bedrock_runtime:unspecified_model");
            return cached;
        }
        long generation = ModelParticleModels.generation();
        if (cached == null || cachedGeneration != generation) {
            cached = ModelParticleModels.get(modelId);
            if (cached == null) {
                cached = ModelParticleDiagnostics.missingModel(modelId);
            }
            cachedGeneration = generation;
        }
        return cached;
    }
}



