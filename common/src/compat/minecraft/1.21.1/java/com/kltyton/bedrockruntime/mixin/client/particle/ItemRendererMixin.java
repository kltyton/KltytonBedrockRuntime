package com.kltyton.bedrockruntime.mixin.client.particle;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.definition.molang.RenderItemContext;
import com.kltyton.bedrockruntime.client.particle.definition.molang.RenderFrameContext;
import com.kltyton.bedrockruntime.client.particle.access.IEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Carries the real item stack, display context, and entity owner through rendering. */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @WrapMethod(method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V")
    private void kltyton_bedrock_runtime$renderStaticWithOwner(@Nullable LivingEntity entity, ItemStack stack,
            ItemDisplayContext display, boolean leftHand, PoseStack poseStack, MultiBufferSource bufferSource,
            @Nullable Level level, int light, int overlay, int seed, Operation<Void> original) {
        RenderItemContext.Owner owner = display == ItemDisplayContext.GUI ? null
                : entity == null ? RenderItemContext.currentOwner() : RenderItemContext.Owner.of(entity);
        kltyton_bedrock_runtime$sampleOwner(owner, display);
        try (RenderItemContext ignored = RenderItemContext.enter(stack, owner, display)) {
            original.call(entity, stack, display, leftHand, poseStack, bufferSource, level, light, overlay, seed);
        }
    }

    @WrapMethod(method = "renderStatic(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;IILcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;I)V")
    private void kltyton_bedrock_runtime$renderStatic(ItemStack stack, ItemDisplayContext display, int light, int overlay,
            PoseStack poseStack, MultiBufferSource bufferSource, @Nullable Level level, int seed,
            Operation<Void> original) {
        RenderItemContext.Owner owner = display == ItemDisplayContext.GUI ? null : RenderItemContext.currentOwner();
        kltyton_bedrock_runtime$sampleOwner(owner, display);
        try (RenderItemContext ignored = RenderItemContext.enter(stack, owner, display)) {
            original.call(stack, display, light, overlay, poseStack, bufferSource, level, seed);
        }
    }

    @WrapMethod(method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V")
    private void kltyton_bedrock_runtime$render(ItemStack stack, ItemDisplayContext display, boolean leftHand,
            PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay, BakedModel model,
            Operation<Void> original) {
        RenderItemContext.Owner owner = display == ItemDisplayContext.GUI ? null : RenderItemContext.currentOwner();
        kltyton_bedrock_runtime$sampleOwner(owner, display);
        try (RenderItemContext ignored = RenderItemContext.enter(stack, owner, display)) {
            original.call(stack, display, leftHand, poseStack, bufferSource, light, overlay, model);
        }
    }
    @Unique
    private static void kltyton_bedrock_runtime$sampleOwner(@Nullable RenderItemContext.Owner owner, ItemDisplayContext display) {
        if (owner == null || display == ItemDisplayContext.GUI) return;
        var entity = owner.entity();
        if (entity != null) IEntity.of(entity).kltyton_bedrock_runtime$sampleAnimationLifetime(RenderFrameContext.framePartialTick());
    }

}
