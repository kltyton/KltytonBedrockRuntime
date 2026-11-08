package com.kltyton.bedrockruntime.client.particle.model.mixin;

import com.kltyton.bedrockruntime.client.particle.model.runtime.ModelMolangParticle;
import com.kltyton.bedrockruntime.client.particle.model.runtime.ModelParticleBatch;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleManager;
import com.kltyton.bedrockruntime.client.particle.runtime.render.BedrockParticleRenderTypes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Integrates model particles with Forge's frustum-aware particle render pass. */
@Mixin(ParticleEngine.class)
public abstract class ForgeModelParticleRenderBatchMixin {
    private static final String RENDER = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V";

    @Inject(method = RENDER, at = @At("HEAD"), remap = false)
    private void kltyton_bedrock_runtime$evaluateWorldFrameExpressions(PoseStack poseStack, MultiBufferSource.BufferSource buffers,
            LightTexture lightTexture, Camera camera, float partialTick, Frustum frustum, CallbackInfo ci) {
        MolangParticleManager.evaluateWorldFrameExpressions((ParticleEngine) (Object) this, partialTick);
    }

    @WrapOperation(method = RENDER, at = @At(value = "INVOKE", target = "Ljava/util/Map;keySet()Ljava/util/Set;", remap = false), remap = false)
    private Set<ParticleRenderType> kltyton_bedrock_runtime$orderRenderTypes(Map<ParticleRenderType, ?> particles,
            Operation<Set<ParticleRenderType>> original) {
        Set<ParticleRenderType> available = original.call(particles);
        LinkedHashSet<ParticleRenderType> ordered = new LinkedHashSet<>(BedrockParticleRenderTypes.renderOrder(List.copyOf(available)));
        ordered.retainAll(available);
        return ordered;
    }

    @WrapOperation(method = RENDER, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/particle/Particle;getBoundingBox()Lnet/minecraft/world/phys/AABB;", remap = true), remap = false)
    private AABB kltyton_bedrock_runtime$modelRenderBounds(Particle particle, Operation<AABB> original,
            @Local(argsOnly = true) float partialTick) {
        return particle instanceof ModelMolangParticle model
                ? model.getModelRenderBounds(partialTick) : original.call(particle);
    }

    @WrapOperation(method = RENDER, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/particle/ParticleRenderType;end(Lcom/mojang/blaze3d/vertex/Tesselator;)V", remap = true), remap = false)
    private void kltyton_bedrock_runtime$drawMaterial(ParticleRenderType type, Tesselator tesselator, Operation<Void> original) {
        try (var ignored = BedrockParticleRenderTypes.enterDraw(type)) {
            original.call(type, tesselator);
        }
    }

    @Inject(method = RENDER, at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", remap = true), remap = false)
    private void kltyton_bedrock_runtime$drawModelBatches(CallbackInfo ci) {
        ModelParticleBatch.finish();
    }
}
