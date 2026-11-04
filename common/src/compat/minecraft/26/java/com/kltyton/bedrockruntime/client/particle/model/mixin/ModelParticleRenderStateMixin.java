package com.kltyton.bedrockruntime.client.particle.model.mixin;

import com.kltyton.bedrockruntime.client.particle.model.runtime.ModelParticleGeometry;
import com.kltyton.bedrockruntime.client.particle.model.runtime.ModelParticleSubmissionAccess;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

/** Keeps model mesh snapshots beside the native billboard state without changing its particle count. */
@Mixin(QuadParticleRenderState.class)
public abstract class ModelParticleRenderStateMixin implements ModelParticleSubmissionAccess {
    @Unique
    private final ArrayList<ModelParticleGeometry> kltyton_bedrock_runtime$modelGeometry = new ArrayList<>();

    @Override
    public void kltyton_bedrock_runtime$addModelGeometry(ModelParticleGeometry geometry) {
        kltyton_bedrock_runtime$modelGeometry.add(geometry);
    }

    @Override
    public void kltyton_bedrock_runtime$submitModelGeometry(SubmitNodeCollector collector) {
        if (kltyton_bedrock_runtime$modelGeometry.isEmpty()) return;
        PoseStack identity = new PoseStack();
        for (ModelParticleGeometry geometry : kltyton_bedrock_runtime$modelGeometry) {
            geometry.submit(collector, identity);
        }
    }

    @Inject(method = "submit", at = @At("TAIL"))
    private void kltyton_bedrock_runtime$submitModels(SubmitNodeCollector collector,
                                                    CameraRenderState camera, CallbackInfo ci) {
        kltyton_bedrock_runtime$submitModelGeometry(collector);
    }

    @Inject(method = "isEmpty", at = @At("RETURN"), cancellable = true)
    private void kltyton_bedrock_runtime$includeModels(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && !kltyton_bedrock_runtime$modelGeometry.isEmpty()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "clear", at = @At("TAIL"))
    private void kltyton_bedrock_runtime$clearModels(CallbackInfo ci) {
        kltyton_bedrock_runtime$modelGeometry.clear();
    }
}

