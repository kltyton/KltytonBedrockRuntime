package com.kltyton.bedrockruntime.client.particle.model.runtime;

import com.kltyton.bedrockruntime.client.particle.model.internal.ModelParticleRuntimeState;
import com.kltyton.bedrockruntime.client.particle.model.internal.ModelParticleStateAccess;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticlePreset;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.RandomSource;

/** 1.20.1 model particle: all simulation is inherited from the shared Molang particle runtime. */
public final class ModelMolangParticle extends MolangParticleInstance implements ModelParticleStateAccess {
    private final ModelParticleRuntimeState modelState = new ModelParticleRuntimeState();

    public ModelMolangParticle(ParticlePreset preset, ClientLevel level, double x, double y, double z,
                               RandomSource random) {
        super(preset, level, x, y, z, random);
    }

    @Override
    public ModelParticleRuntimeState kltyton_bedrock_runtime$modelParticleState() {
        return modelState;
    }

    /** Render bounds remain independent of the particle's collision box. */
    public AABB getModelRenderBounds(float partialTick) {
        Vector3f position = getRenderWorldPosition(new Vector3f(), partialTick);
        float scale = 0.0F;
        for (int axis = 0; axis < 3; axis++) {
            Float value = getBillboardSize(axis);
            float dimension = value == null ? getQuadSize(partialTick) : value;
            if (Float.isFinite(dimension)) scale = Math.max(scale, Math.abs(dimension));
        }
        double radius = modelState.boundingRadius() * scale;
        return new AABB(position.x - radius, position.y - radius, position.z - radius,
                position.x + radius, position.y + radius, position.z + radius);
    }

    @Override
    public boolean kltyton_bedrock_runtime$isVisible(Frustum frustum, float partialTick) {
        return frustum == null || frustum.isVisible(getModelRenderBounds(partialTick));
    }


    public int kltyton_bedrock_runtime$modelParticleLight(float partialTick) {
        return getLightColor(partialTick);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        ModelParticleRenderer.render(this, buffer, camera, partialTick);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }
}
