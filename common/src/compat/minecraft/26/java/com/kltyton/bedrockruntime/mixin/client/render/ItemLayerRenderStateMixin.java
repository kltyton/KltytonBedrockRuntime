package com.kltyton.bedrockruntime.mixin.client.render;

import java.util.function.Supplier;
import com.kltyton.bedrockruntime.client.render.item.HeldItemCapture;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.item.ItemStackRenderState$LayerRenderState")
public abstract class ItemLayerRenderStateMixin {
    @Shadow private Supplier<Vector3fc[]> extents;

    @Inject(method = "submit", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState$LayerRenderState;applyTransform(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;)V",
            shift = At.Shift.AFTER))
    private void kltyton_bedrock_runtime$captureLayer(PoseStack pose, SubmitNodeCollector output, int light, int overlay,
            int outline, CallbackInfo callback) {
        HeldItemCapture.layer(pose, output, extents.get());
    }
}
