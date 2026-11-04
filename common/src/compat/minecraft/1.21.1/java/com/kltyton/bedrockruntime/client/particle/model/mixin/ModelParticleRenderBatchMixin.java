package com.kltyton.bedrockruntime.client.particle.model.mixin;

import com.kltyton.bedrockruntime.client.particle.model.runtime.ModelParticleBatch;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Flushes captured model meshes after the native particle buffers finish drawing. */
@Mixin(ParticleEngine.class)
public abstract class ModelParticleRenderBatchMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void kltyton_bedrock_runtime$drawModelBatches(CallbackInfo ci) {
        ModelParticleBatch.finish();
    }
}

