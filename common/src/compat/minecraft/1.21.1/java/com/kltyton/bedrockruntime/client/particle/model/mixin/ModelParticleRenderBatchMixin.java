package com.kltyton.bedrockruntime.client.particle.model.mixin;

import com.kltyton.bedrockruntime.client.particle.model.runtime.ModelParticleBatch;
import com.kltyton.bedrockruntime.client.particle.runtime.render.BedrockParticleRenderTypes;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.MeshData;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Flushes captured model meshes after the native particle buffers finish drawing. */
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
        return types == null ? original : List.copyOf(types);
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/MeshData;)V"))
    private void kltyton_bedrock_runtime$drawMaterial(MeshData mesh, Operation<Void> original, @Local ParticleRenderType type) {
        try (var ignored = BedrockParticleRenderTypes.enterDraw(type)) {
            original.call(mesh);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void kltyton_bedrock_runtime$drawModelBatches(CallbackInfo ci) {
        ModelParticleBatch.finish();
    }
}

