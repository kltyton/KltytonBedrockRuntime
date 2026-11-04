package com.kltyton.bedrockruntime.mixin.client.particle;

import com.google.common.collect.EvictingQueue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.api.RegisterCustomParticleTypeEvent;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleManager;
import com.kltyton.bedrockruntime.client.particle.runtime.screen.NativePreviewParticleEngine;
import com.kltyton.bedrockruntime.client.particle.runtime.screen.PreviewParticleScope;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;

@Mixin(ParticleEngine.class)
public abstract class ParticleEnginePreviewMixin {
    @WrapOperation(
            method = "<init>",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/texture/TextureManager;register(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/renderer/texture/AbstractTexture;)V"))
    private void kltyton_bedrock_runtime$registerSharedParticleAtlas(TextureManager textureManager, ResourceLocation location,
                                                                     AbstractTexture createdAtlas, Operation<Void> original) {
        var sharedAtlas = PreviewParticleScope.constructionAtlas();
        original.call(textureManager, location, sharedAtlas == null ? createdAtlas : sharedAtlas);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void kltyton_bedrock_runtime$registerPreviewParticleTypes(net.minecraft.client.multiplayer.ClientLevel level,
                                                                      TextureManager textureManager, CallbackInfo ci) {
        if (PreviewParticleScope.isNativeEngineConstruction()) {
            RegisterCustomParticleTypeEvent.registerDefaults();
        }
    }

    @Inject(method = "add(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true)
    private void kltyton_bedrock_runtime$routePreviewParticle(Particle particle, CallbackInfo ci) {
        if (MolangParticleManager.routePreviewParticle((ParticleEngine) (Object) this, particle)) {
            ci.cancel();
        }
    }

    @WrapOperation(method = "add(Lnet/minecraft/client/particle/Particle;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/Queue;add(Ljava/lang/Object;)Z"))
    private boolean kltyton_bedrock_runtime$recordAdmission(Queue<Particle> queue, Object particle, Operation<Boolean> original) {
        boolean added = original.call(queue, particle);
        if (added && particle instanceof IMolangParticleInstance instance) {
            MolangParticleManager.onNativeParticleAdmitted((ParticleEngine) (Object) this, instance);
        }
        return added;
    }

    @WrapOperation(method = "tick",
            at = @At(value = "INVOKE", target = "Ljava/util/Queue;add(Ljava/lang/Object;)Z"))
    private boolean kltyton_bedrock_runtime$releaseEvictedParticle(Queue<Particle> queue, Object particle, Operation<Boolean> original) {
        Particle evicted = queue instanceof EvictingQueue<?> bounded && bounded.remainingCapacity() == 0 ? queue.peek() : null;
        boolean added = original.call(queue, particle);
        if (added && evicted instanceof IMolangParticleInstance) {
            evicted.getParticleGroup().ifPresent(group ->
                    ((ParticleEnginePreviewAccessor) this).kltyton_bedrock_runtime$updateGroupCount(group, -1));
            if (evicted != particle && evicted.isAlive()) {
                evicted.remove();
            }
        }
        return added;
    }

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V",
            at = @At("HEAD"))
    private void kltyton_bedrock_runtime$evaluateWorldFrameExpressions(PoseStack poseStack, MultiBufferSource.BufferSource buffers,
                                                                      LightTexture lightTexture, Camera camera, float partialTick,
                                                                      CallbackInfo ci) {
        MolangParticleManager.evaluateWorldFrameExpressions((ParticleEngine) (Object) this, partialTick);
    }

    @Inject(method = "reload", at = @At("RETURN"), cancellable = true)
    private void kltyton_bedrock_runtime$rebindPreviewSprites(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        cir.setReturnValue(cir.getReturnValue().thenRun(NativePreviewParticleEngine::rebindWorldAtlases));
    }
}
