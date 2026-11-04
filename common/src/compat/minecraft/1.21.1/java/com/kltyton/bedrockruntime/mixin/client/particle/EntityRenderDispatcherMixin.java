package com.kltyton.bedrockruntime.mixin.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import com.kltyton.bedrockruntime.client.particle.access.IEntity;
import com.kltyton.bedrockruntime.client.particle.definition.molang.RenderItemContext;
import org.spongepowered.asm.mixin.Mixin;

/** Samples actor animation time at the native entity render entry. */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @WrapMethod(method = "render")
    private void kltyton_bedrock_runtime$sampleActor(Entity entity, double x, double y, double z, float yaw,
            float partialTick, PoseStack pose, MultiBufferSource buffers, int light, Operation<Void> original) {
        IEntity.of(entity).kltyton_bedrock_runtime$sampleAnimationLifetime(partialTick);
        try (var ignored = RenderItemContext.enterOwner(RenderItemContext.Owner.of(entity))) {
            original.call(entity, x, y, z, yaw, partialTick, pose, buffers, light);
        }
    }
}
