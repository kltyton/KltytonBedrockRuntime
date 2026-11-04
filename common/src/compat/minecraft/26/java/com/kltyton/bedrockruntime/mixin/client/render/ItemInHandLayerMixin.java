package com.kltyton.bedrockruntime.mixin.client.render;

import com.kltyton.bedrockruntime.client.render.item.HeldItemCapture;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {
    @Redirect(method = "submitArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void kltyton_bedrock_runtime$captureItem(ItemStackRenderState item, PoseStack pose, SubmitNodeCollector output,
            int light, int overlay, int outline, ArmedEntityRenderState state, ItemStackRenderState held,
            ItemStack stack, HumanoidArm arm, PoseStack armPose, SubmitNodeCollector armOutput, int armLight) {
        HeldItemCapture.submit(item, pose, output, light, overlay, outline, state, arm);
    }
}
