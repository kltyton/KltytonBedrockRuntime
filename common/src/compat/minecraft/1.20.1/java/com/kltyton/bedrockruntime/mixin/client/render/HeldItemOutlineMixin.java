package com.kltyton.bedrockruntime.mixin.client.render;

import com.kltyton.bedrockruntime.client.render.item.HeldItemCapture;
import com.kltyton.bedrockruntime.client.render.outline.PartOutlineRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class HeldItemOutlineMixin {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void kltyton_bedrock_runtime$beginOutlines(com.mojang.blaze3d.vertex.PoseStack pose, float partialTick,
            long finishNanoTime, boolean renderBlockOutline, net.minecraft.client.Camera camera,
            net.minecraft.client.renderer.GameRenderer renderer, net.minecraft.client.renderer.LightTexture light,
            org.joml.Matrix4f projection, CallbackInfo callback) {
        com.kltyton.bedrockruntime.client.render.WorldRenderPose.begin(pose.last());
        PartOutlineRenderer.beginFrame();
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void kltyton_bedrock_runtime$endWorldCapture(CallbackInfo callback) {
        com.kltyton.bedrockruntime.client.render.WorldRenderPose.end();
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endLastBatch()V"))
    private void kltyton_bedrock_runtime$heldItemOutlines(CallbackInfo callback) {
        Minecraft minecraft = Minecraft.getInstance();
        HeldItemCapture.submitLocalPlayerOutlines(minecraft);
        PartOutlineRenderer.flush(minecraft.renderBuffers().bufferSource());
    }
}
