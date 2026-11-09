package com.kltyton.bedrockruntime.mixin.client.render;

import com.kltyton.bedrockruntime.api.entity.BoneHitboxEntity;
import com.kltyton.bedrockruntime.client.selection.model.VisualPartEntityRenderState;
import com.kltyton.bedrockruntime.client.selection.service.BonePartSelectionClient;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Collects a complete world-model submission before replacing the entity's previous parts. */
@Mixin(EntityRenderDispatcher.class)
abstract class EntityRenderDispatcherObbCaptureMixin {
    @WrapOperation(method = "submit", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"))
    private void kltyton_bedrock_runtime$captureModel(EntityRenderer<?, ?> renderer, EntityRenderState state,
            PoseStack pose, SubmitNodeCollector output, CameraRenderState camera, Operation<Void> original) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && camera == minecraft.gameRenderer.gameRenderState().levelRenderState.cameraRenderState
                && state instanceof VisualPartEntityRenderState source) {
            Entity owner = minecraft.level.getEntity(source.kltyton_bedrock_runtime$getEntityId());
            if (owner instanceof BoneHitboxEntity && owner != minecraft.player && !owner.isRemoved()) {
                BonePartSelectionClient.captureEntityModel(owner, camera.pos,
                        () -> original.call(renderer, state, pose, output, camera));
                return;
            }
        }
        original.call(renderer, state, pose, output, camera);
    }
}
