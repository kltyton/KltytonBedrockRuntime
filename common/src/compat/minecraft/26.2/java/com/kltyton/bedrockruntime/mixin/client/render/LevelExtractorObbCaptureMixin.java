package com.kltyton.bedrockruntime.mixin.client.render;

import com.kltyton.bedrockruntime.api.entity.BoneHitboxEntity;
import com.kltyton.bedrockruntime.client.render.capture.CulledEntityCapture;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Samples nearby collision models that world extraction omits, including section culling. */
@Mixin(LevelExtractor.class)
abstract class LevelExtractorObbCaptureMixin {
    @WrapOperation(method = "extractVisibleEntities", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/extract/LevelExtractor;isEntityVisible(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/culling/Frustum;DDD)Z"))
    private boolean kltyton_bedrock_runtime$captureOmittedModel(LevelExtractor extractor, Entity entity,
            Frustum frustum, double cameraX, double cameraY, double cameraZ, Operation<Boolean> original) {
        boolean visible = original.call(extractor, entity, frustum, cameraX, cameraY, cameraZ);
        if (!visible && entity instanceof BoneHitboxEntity && !(entity instanceof LocalPlayer)
                && entity.shouldRender(cameraX, cameraY, cameraZ)) {
            CulledEntityCapture.sampleIfNearby(entity, cameraX, cameraY, cameraZ);
        }
        return visible;
    }
}
