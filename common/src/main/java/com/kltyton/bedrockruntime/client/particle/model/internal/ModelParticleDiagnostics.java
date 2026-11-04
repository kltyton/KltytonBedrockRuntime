package com.kltyton.bedrockruntime.client.particle.model.internal;

import com.kltyton.bedrockruntime.client.particle.model.api.ModelMaterial;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelParticleModel;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelParticlePart;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelParticleModels;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelUvSpace;
import com.kltyton.bedrockruntime.geometry.mesh.TriangleMesh;
import com.kltyton.bedrockruntime.geometry.mesh.TriangleMeshBuilder;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Missing-resource diagnostics and a visible fallback model. Fallbacks keep particles alive while
 * naming the exact resource that failed to load.
 */
public final class ModelParticleDiagnostics {
    private static final String FALLBACK_TEXTURE = "minecraft:missingno";
    private static final Map<String, ModelParticleModel> FALLBACKS = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> REPORTED = new ConcurrentHashMap<>();
    private static volatile TriangleMesh fallbackMesh;

    private ModelParticleDiagnostics() {
    }

    public static ModelParticleModel missingModel(String modelId) {
        String canonical = ModelParticleModels.canonical(modelId);
        reportOnce("model:" + canonical, () -> BedrockParticles.LOGGER.error(
                "Model particle '{}' is not published. Expected particle_models/{}.json or a programmatic registration",
                canonical, canonical.substring(canonical.indexOf(':') + 1)));
        return FALLBACKS.computeIfAbsent(canonical, id -> new ModelParticleModel(id, FALLBACK_TEXTURE,
                java.util.List.of(new ModelParticlePart(FALLBACK_TEXTURE, ModelMaterial.CUTOUT,
                        ModelUvSpace.NORMALIZED, fallbackMesh(), new Matrix4f())),
                ModelMaterial.CUTOUT, false));
    }

    public static void descriptorMissing(String descriptorId, String expectedResource) {
        reportOnce("descriptor:" + descriptorId + ":" + expectedResource, () -> BedrockParticles.LOGGER.error(
                "Model particle descriptor '{}' is missing resource '{}'", descriptorId, expectedResource));
    }

    public static void invalidModelSelection(String identity, String selected) {
        reportOnce("selection:" + identity + ":" + selected, () -> BedrockParticles.LOGGER.error(
                "Model particle '{}' selected invalid model id '{}'; using a visible missing-model placeholder",
                identity, selected));
    }

    public static void descriptorFailure(String descriptorId, String resource, Throwable failure) {
        reportOnce("prepare:" + descriptorId + ":" + resource, () -> BedrockParticles.LOGGER.error(
                "Failed to read model particle descriptor '{}' at resource '{}'; publishing a visible missing-model placeholder",
                descriptorId, resource, failure));
    }

    public static void bakeFailure(String descriptorId, String resource, Throwable failure) {
        reportOnce("bake:" + descriptorId + ":" + resource, () -> BedrockParticles.LOGGER.error(
                "Failed to bake model particle '{}' from resource '{}'; using a visible missing-model placeholder",
                descriptorId, resource, failure));
    }

    private static synchronized TriangleMesh fallbackMesh() {
        if (fallbackMesh != null) {
            return fallbackMesh;
        }
        TriangleMeshBuilder builder = new TriangleMeshBuilder();
        float low = -0.5F;
        float high = 0.5F;
        quad(builder, low, low, low, low, low, high, high, low, high, high, low, low);
        quad(builder, low, high, low, high, high, low, high, high, high, low, high, high);
        quad(builder, low, low, low, low, high, low, high, high, low, high, low, low);
        quad(builder, high, low, high, low, high, high, high, high, high, high, low, high);
        quad(builder, low, low, high, low, high, high, high, high, high, high, low, high);
        quad(builder, low, low, low, high, low, low, high, low, high, low, low, high);
        fallbackMesh = builder.build();
        return fallbackMesh;
    }

    private static void quad(TriangleMeshBuilder builder, float x0, float y0, float z0,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3) {
        int p0 = builder.addPosition(x0, y0, z0);
        int p1 = builder.addPosition(x1, y1, z1);
        int p2 = builder.addPosition(x2, y2, z2);
        int p3 = builder.addPosition(x3, y3, z3);
        int u0 = builder.addUv(0.0F, 0.0F);
        int u1 = builder.addUv(1.0F, 0.0F);
        int u2 = builder.addUv(1.0F, 1.0F);
        int u3 = builder.addUv(0.0F, 1.0F);
        builder.addQuad(p0, p1, p2, p3, -1, -1, -1, -1, u0, u1, u2, u3);
    }

    private static void reportOnce(String key, Runnable action) {
        if (REPORTED.putIfAbsent(key, Boolean.TRUE) == null) {
            action.run();
        }
    }
}
