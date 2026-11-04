package com.kltyton.bedrockruntime.client.particle.model.runtime;

import com.kltyton.bedrockruntime.client.particle.model.internal.ModelParticleRuntimeState;
import com.kltyton.bedrockruntime.client.particle.model.internal.ModelParticleStateAccess;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticlePreset;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

/** 26.1.2 model particle: simulation stays in the shared Molang particle runtime. */
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

    public int kltyton_bedrock_runtime$modelParticleLight(float partialTick) {
        return getLightCoords(partialTick);
    }

    @Override
    public void extract(@NotNull QuadParticleRenderState state, @NotNull Camera camera, float partialTicks) {
        ModelParticleRenderer.extract(this, state, camera, partialTicks);
    }

    @Override
    public boolean kltyton_bedrock_runtime$isVisible(Frustum frustum, Camera camera, float partialTick) {
        Vector3f position = getRenderWorldPosition(new Vector3f(), partialTick);
        double scale = Math.max(Math.abs(size(0)), Math.max(Math.abs(size(1)), Math.abs(size(2))));
        double size = modelState.boundingRadius() * scale;
        if (size == 0.0) {
            return false;
        }
        return frustum.isVisible(new AABB(
                position.x - size, position.y - size, position.z - size,
                position.x + size, position.y + size, position.z + size));
    }

    @Override
    public boolean kltyton_bedrock_runtime$isVisible(Frustum frustum, float partialTick) {
        return kltyton_bedrock_runtime$isVisible(frustum,
                net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera(), partialTick);
    }

    private float size(int axis) {
        Float value = getBillboardSize(axis);
        return value == null ? getQuadSize(1.0F) : value;
    }
}



