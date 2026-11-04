package com.kltyton.bedrockruntime.mixin.client.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import com.kltyton.bedrockruntime.client.particle.runtime.render.BedrockParticleRenderTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Limits material-specific shader state to the native draw for that particle batch. */
@Mixin(ParticleEngine.class)
public abstract class BedrockParticleDrawMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/particle/ParticleRenderType;end(Lcom/mojang/blaze3d/vertex/Tesselator;)V"))
    private void kltyton_bedrock_runtime$drawMaterial(ParticleRenderType type, Tesselator tesselator, Operation<Void> original) {
        try (var ignored = BedrockParticleRenderTypes.enterDraw(type)) {
            original.call(type, tesselator);
        }
    }
}
