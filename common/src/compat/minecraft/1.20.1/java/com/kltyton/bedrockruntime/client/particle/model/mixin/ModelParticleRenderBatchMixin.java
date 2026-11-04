package com.kltyton.bedrockruntime.client.particle.model.mixin;

import com.kltyton.bedrockruntime.client.particle.model.runtime.ModelParticleBatch;
import com.kltyton.bedrockruntime.client.particle.model.runtime.ModelMolangParticle;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.AABB;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws captured meshes after native buffers, while the particle view matrix is still active. */
@Mixin(ParticleEngine.class)
public abstract class ModelParticleRenderBatchMixin {
    // Forge adds a frustum filter to render; vanilla Fabric has no such bounding-box call.
    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/particle/Particle;getBoundingBox()Lnet/minecraft/world/phys/AABB;"),
            require = 0)
    private AABB kltyton_bedrock_runtime$modelRenderBounds(Particle particle, Operation<AABB> original,
                                                         @Local(argsOnly = true) float partialTick) {
        return particle instanceof ModelMolangParticle model
                ? model.getModelRenderBounds(partialTick) : original.call(particle);
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"))
    private void kltyton_bedrock_runtime$drawModelBatches(CallbackInfo ci) {
        ModelParticleBatch.finish();
    }
}
