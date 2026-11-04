package com.kltyton.bedrockruntime.mixin.client.render;

import com.kltyton.bedrockruntime.api.entity.BoneHitboxEntity;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * CN: 强制捕获附近 opt-in 实体的模型，即使其原版小 AABB 已离开视锥；否则 OBB 接触会在贴近/穿入模型时消失。
 * EN: Captures nearby opt-in models even when their small vanilla AABB leaves the frustum; otherwise OBB contacts disappear at close range.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererObbCaptureMixin<T extends Entity> {
    private static final double KLTYTON_BEDROCK_RUNTIME_CAPTURE_RANGE_SQR = 32.0 * 32.0;

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void kltyton_bedrock_runtime$forceNearbyObbCapture(T entity, Frustum frustum,
            double cameraX, double cameraY, double cameraZ, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (entity instanceof BoneHitboxEntity
                && entity.distanceToSqr(cameraX, cameraY, cameraZ) <= KLTYTON_BEDROCK_RUNTIME_CAPTURE_RANGE_SQR) {
            callbackInfo.setReturnValue(true);
        }
    }
}
