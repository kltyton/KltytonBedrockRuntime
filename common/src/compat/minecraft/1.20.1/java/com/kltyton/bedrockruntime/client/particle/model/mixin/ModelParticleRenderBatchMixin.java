package com.kltyton.bedrockruntime.client.particle.model.mixin;

import com.kltyton.bedrockruntime.client.particle.model.runtime.ModelParticleBatch;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleManager;
import com.kltyton.bedrockruntime.client.particle.runtime.render.BedrockParticleRenderTypes;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/** Draws captured meshes after native buffers, while the particle view matrix is still active. */
@Mixin(ParticleEngine.class)
public abstract class ModelParticleRenderBatchMixin {
    @Shadow @Final private Map<ParticleRenderType, Queue<Particle>> particles;

    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/particle/ParticleEngine;RENDER_ORDER:Ljava/util/List;"))
    private List<ParticleRenderType> kltyton_bedrock_runtime$includeRegisteredRenderTypes(List<ParticleRenderType> original) {
        LinkedHashSet<ParticleRenderType> types = null;
        for (ParticleRenderType type : particles.keySet()) {
            if (type != ParticleRenderType.NO_RENDER && !original.contains(type)) {
                if (types == null) types = new LinkedHashSet<>(original);
                types.add(type);
            }
        }
        return BedrockParticleRenderTypes.renderOrder(types == null ? original : List.copyOf(types));
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V", at = @At("HEAD"))
    private void kltyton_bedrock_runtime$evaluateWorldFrameExpressions(PoseStack poseStack, MultiBufferSource.BufferSource buffers,
            LightTexture lightTexture, Camera camera, float partialTick, CallbackInfo ci) {
        MolangParticleManager.evaluateWorldFrameExpressions((ParticleEngine) (Object) this, partialTick);
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/particle/ParticleRenderType;end(Lcom/mojang/blaze3d/vertex/Tesselator;)V"))
    private void kltyton_bedrock_runtime$drawMaterial(ParticleRenderType type, Tesselator tesselator, Operation<Void> original) {
        try (var ignored = BedrockParticleRenderTypes.enterDraw(type)) {
            original.call(type, tesselator);
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"))
    private void kltyton_bedrock_runtime$drawModelBatches(CallbackInfo ci) {
        ModelParticleBatch.finish();
    }
}
