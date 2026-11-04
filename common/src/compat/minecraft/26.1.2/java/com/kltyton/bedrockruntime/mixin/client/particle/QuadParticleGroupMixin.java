package com.kltyton.bedrockruntime.mixin.client.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Vector3f;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Culls Molang particles with their rendered world bounds and extraction camera. */
@Mixin(QuadParticleGroup.class)
public abstract class QuadParticleGroupMixin {
    @Unique private static final ThreadLocal<Vector3f> PARTICLESTORM_POSITION = ThreadLocal.withInitial(Vector3f::new);

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/culling/Frustum;pointInFrustum(DDD)Z"))
    private boolean kltyton_bedrock_runtime$worldSpaceCulling(Frustum frustum, double x, double y, double z,
                                                    Operation<Boolean> original,
                                                    @Local SingleQuadParticle particle,
                                                    @Local(argsOnly = true) Camera camera,
                                                    @Local(argsOnly = true) float partialTick) {
        if (particle instanceof MolangParticleInstance molang) {
            Vector3f position = molang.getRenderWorldPosition(PARTICLESTORM_POSITION.get(), partialTick);
            return original.call(frustum, (double) position.x, (double) position.y, (double) position.z)
                    || molang.kltyton_bedrock_runtime$isVisible(frustum, camera, partialTick);
        }
        return original.call(frustum, x, y, z);
    }
}