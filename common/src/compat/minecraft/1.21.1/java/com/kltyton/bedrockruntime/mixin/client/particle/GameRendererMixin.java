package com.kltyton.bedrockruntime.mixin.client.particle;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import com.kltyton.bedrockruntime.client.particle.definition.molang.RenderFrameContext;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Advances particle simulation before world and GUI particle extraction. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void kltyton_bedrock_runtime$beginRenderFrame(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        RenderFrameContext.beginFrame(deltaTracker.getGameTimeDeltaPartialTick(false));
        MolangParticleEngine.INSTANCE.advanceFrame();
    }
}
