package com.kltyton.bedrockruntime.mixin.client.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import com.kltyton.bedrockruntime.client.particle.runtime.render.BedrockParticleRenderTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Limits material-specific shader state to the native draw for that particle batch. */
@Mixin(ParticleEngine.class)
public abstract class BedrockParticleDrawMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/MeshData;)V"))
    private void kltyton_bedrock_runtime$drawMaterial(MeshData mesh, Operation<Void> original, @Local ParticleRenderType type) {
        try (var ignored = BedrockParticleRenderTypes.enterDraw(type)) {
            original.call(mesh);
        }
    }
}
