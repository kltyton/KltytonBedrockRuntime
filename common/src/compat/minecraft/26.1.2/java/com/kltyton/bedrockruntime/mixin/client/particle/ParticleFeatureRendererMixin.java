package com.kltyton.bedrockruntime.mixin.client.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.renderer.feature.ParticleFeatureRenderer;
import com.kltyton.bedrockruntime.client.particle.runtime.screen.PreviewParticleScope;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Keeps native particle feature draws inside the active editor framebuffer. */
@Mixin(ParticleFeatureRenderer.class)
public abstract class ParticleFeatureRendererMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;getColorTextureView()Lcom/mojang/blaze3d/textures/GpuTextureView;"))
    private GpuTextureView kltyton_bedrock_runtime$previewColor(RenderTarget target, Operation<GpuTextureView> original) {
        GpuTextureView view = original.call(target);
        PreviewParticleScope.Context context = PreviewParticleScope.current();
        return context != null && context.manager().isIndependentParticles() && RenderSystem.outputColorTextureOverride != null
                ? RenderSystem.outputColorTextureOverride : view;
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;getDepthTextureView()Lcom/mojang/blaze3d/textures/GpuTextureView;"))
    private GpuTextureView kltyton_bedrock_runtime$previewDepth(RenderTarget target, Operation<GpuTextureView> original) {
        GpuTextureView view = original.call(target);
        PreviewParticleScope.Context context = PreviewParticleScope.current();
        return context != null && context.manager().isIndependentParticles() && RenderSystem.outputDepthTextureOverride != null
                ? RenderSystem.outputDepthTextureOverride : view;
    }
}
