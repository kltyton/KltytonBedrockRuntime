package com.kltyton.bedrockruntime.mixin.client.render;

import com.kltyton.bedrockruntime.client.render.item.HeldItemCapture;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {
    @WrapMethod(method = "renderArmWithItem")
    private void kltyton_bedrock_runtime$captureItem(LivingEntity entity, ItemStack stack, ItemDisplayContext display,
            HumanoidArm arm, PoseStack pose, MultiBufferSource buffers, int light, Operation<Void> original) {
        HeldItemCapture.render(entity, arm, () -> original.call(entity, stack, display, arm, pose, buffers, light));
    }
}
