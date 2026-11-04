package com.kltyton.bedrockruntime.mixin.client.render;

import com.kltyton.bedrockruntime.client.render.item.HeldItemCapture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererCaptureMixin {
    @Inject(method = "renderModelLists", at = @At("HEAD"))
    private void kltyton_bedrock_runtime$captureModel(BakedModel model, ItemStack stack, int light, int overlay,
            PoseStack pose, VertexConsumer output, CallbackInfo callback) {
        HeldItemCapture.model(pose.last(), model);
    }
}
