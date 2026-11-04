package com.kltyton.bedrockruntime.client.particle.model.api;

import com.google.gson.JsonObject;
import com.kltyton.bedrockruntime.client.particle.model.bake.ModelParticleBakery;
import com.kltyton.bedrockruntime.geometry.mesh.TriangleMesh;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;

import java.util.List;

/**
 * 1.20.1 convenience registration for model-particle inputs. Add-on mods may call these methods
 * after client setup (or from a resource reload) without touching the particle runtime.
 */
public final class ModelParticleSources {
    private ModelParticleSources() {
    }

    public static void register(String id, ModelParticleModel model) {
        ModelParticleModels.register(id, model);
    }

    public static ModelParticleModel registerHardcoded(String id, ModelPart root, String texture,
                                                       ModelMaterial material, boolean doubleSided) {
        ModelParticleModel model = ModelParticleBakery.hardcodedRoot(id, root, texture, material, doubleSided);
        ModelParticleModels.register(id, model);
        return model;
    }

    public static ModelParticleModel registerGeometry(String id, JsonObject modelJson, String texture,
                                                      ModelMaterial material, boolean doubleSided) {
        ModelParticleModel model = ModelParticleBakery.vanilla(id, modelJson,
                new ModelParticleModelType(ModelParticleModelType.Variant.GEOMETRY, id, texture, material, doubleSided));
        ModelParticleModels.register(id, model);
        return model;
    }

    public static ModelParticleModel registerGecko(String id, String geckoModelId, String texture,
                                                   ModelMaterial material, boolean doubleSided) {
        ModelParticleModel model = ModelParticleBakery.gecko(
                new ModelParticleModelType(ModelParticleModelType.Variant.GECKOLIB, geckoModelId,
                        texture, material, doubleSided), id);
        ModelParticleModels.register(id, model);
        return model;
    }

    public static ModelParticleModel registerPolyMesh(String id, TriangleMesh mesh, String texture,
                                                      ModelMaterial material, boolean doubleSided) {
        ModelParticleModel model = new ModelParticleModel(id, texture,
                List.of(new ModelParticlePart(texture, material, ModelUvSpace.NORMALIZED, mesh,
                        new Matrix4f(), doubleSided)), material, doubleSided);
        ModelParticleModels.register(id, model);
        return model;
    }
}

