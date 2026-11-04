package com.kltyton.bedrockruntime.mixin.client.particle;

import net.minecraft.client.renderer.ShaderInstance;
import com.kltyton.bedrockruntime.client.particle.runtime.render.BedrockParticleRenderTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Restores the active Bedrock material blend after ShaderInstance applies its JSON blend. */
@Mixin(ShaderInstance.class)
public abstract class BedrockParticleShaderApplyMixin {
    @Inject(method = "apply", at = @At("TAIL"))
    private void kltyton_bedrock_runtime$applyMaterialBlend(CallbackInfo ci) {
        BedrockParticleRenderTypes.applyDrawState();
    }
}
