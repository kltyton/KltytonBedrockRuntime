package com.kltyton.bedrockruntime.mixin.client.particle.geckolib;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import com.kltyton.bedrockruntime.client.particle.api.geckolib.GeckoLibHelper;
import com.kltyton.bedrockruntime.client.particle.api.geckolib.WithCurrentEntity;
import com.kltyton.bedrockruntime.client.particle.runtime.geckolib.ActorAnimationContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import software.bernie.geckolib.animatable.GeoAnimatable;

/** Keeps replaced actor identity valid through nested renders and exceptional exits. */
@Pseudo
@Mixin(targets = "software.bernie.geckolib.renderer.GeoReplacedEntityRenderer", remap = false)
public abstract class GeoReplacedEntityRendererMixin<E extends Entity, T extends GeoAnimatable> {
    @Shadow @Final protected T animatable;

    @WrapMethod(method = "render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    private void kltyton_bedrock_runtime$withActor(E entity, float yaw, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light, Operation<Void> original) {
        Entity previous = animatable instanceof WithCurrentEntity replaced ? replaced.getCurrentEntity() : null;
        try (var ignored = ActorAnimationContext.enter(animatable, entity)) {
            GeckoLibHelper.setCurrentEntity(animatable, entity);
            original.call(entity, yaw, partialTick, pose, buffers, light);
        } finally {
            GeckoLibHelper.setCurrentEntity(animatable, previous);
        }
    }
}
