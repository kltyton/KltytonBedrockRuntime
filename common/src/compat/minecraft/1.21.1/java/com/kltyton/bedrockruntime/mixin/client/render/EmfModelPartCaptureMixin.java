package com.kltyton.bedrockruntime.mixin.client.render;

import com.kltyton.bedrockruntime.client.render.vanilla.EmfModelPartCapture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures EMF model parts whose rendering does not call vanilla ModelPart.render. */
@Pseudo
@Mixin(targets = "traben.entity_model_features.models.parts.EMFModelPart", remap = false)
public abstract class EmfModelPartCaptureMixin {
    @Inject(method = "renderLikeVanilla", at = @At(value = "INVOKE",
            target = "Ljava/util/Map;values()Ljava/util/Collection;", remap = false), remap = false)
    private void kltyton_bedrock_runtime$capture(PoseStack stack, VertexConsumer output, int light, int overlay,
            int color, CallbackInfo callback) {
        EmfModelPartCapture.afterDraw((ModelPart) (Object) this, stack);
    }
}
