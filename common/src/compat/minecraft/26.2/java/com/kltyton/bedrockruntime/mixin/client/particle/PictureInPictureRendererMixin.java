package com.kltyton.bedrockruntime.mixin.client.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import com.kltyton.bedrockruntime.client.particle.runtime.screen.ParticlePreviewRenderState;
import com.kltyton.bedrockruntime.client.particle.runtime.screen.PreviewParticleScope;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PictureInPictureRenderer.class)
public abstract class PictureInPictureRendererMixin {
    @WrapOperation(method = "prepare", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/pip/PictureInPictureRenderer;renderToTexture(Lnet/minecraft/client/renderer/state/gui/pip/PictureInPictureRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V"))
    private void kltyton_bedrock_runtime$releaseOnRenderFailure(PictureInPictureRenderer<?> renderer,
            PictureInPictureRenderState state, PoseStack pose, SubmitNodeCollector collector, Operation<Void> original) {
        ParticlePreviewRenderState preview = state instanceof ParticlePreviewRenderState value ? value : null;
        boolean scoped = preview != null && preview.owner().isIndependentParticles() && preview.previewLevel() != null;
        if (scoped) PreviewParticleScope.enter(preview.owner(), preview.previewLevel(), preview.previewCamera());
        try {
            original.call(renderer, state, pose, collector);
        } catch (RuntimeException | Error exception) {
            if (preview != null) kltyton_bedrock_runtime$resetAfterFailure(preview, exception);
            throw exception;
        } finally {
            if (scoped) PreviewParticleScope.exit();
        }
    }

    @WrapOperation(method = "prepare", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderAllFeatures(Lnet/minecraft/client/renderer/SubmitNodeStorage;)V"))
    private void kltyton_bedrock_runtime$releaseAfterSubmit(FeatureRenderDispatcher dispatcher, SubmitNodeStorage storage,
            Operation<Void> original, PictureInPictureRenderState state, GuiRenderState gui,
            FeatureRenderDispatcher enclosingDispatcher, int guiScale) {
        ParticlePreviewRenderState preview = state instanceof ParticlePreviewRenderState value ? value : null;
        boolean scoped = preview != null && preview.owner().isIndependentParticles() && preview.previewLevel() != null;
        if (scoped) PreviewParticleScope.enter(preview.owner(), preview.previewLevel(), preview.previewCamera());
        try {
            try {
                original.call(dispatcher, storage);
            } catch (RuntimeException | Error exception) {
                if (preview != null) kltyton_bedrock_runtime$resetAfterFailure(preview, exception);
                throw exception;
            }
            if (preview != null) preview.nativeParticles().reset();
        } finally {
            if (scoped) PreviewParticleScope.exit();
        }
    }

    private static void kltyton_bedrock_runtime$resetAfterFailure(ParticlePreviewRenderState state, Throwable original) {
        try {
            state.nativeParticles().reset();
        } catch (RuntimeException | Error cleanup) {
            original.addSuppressed(cleanup);
        }
    }
}
