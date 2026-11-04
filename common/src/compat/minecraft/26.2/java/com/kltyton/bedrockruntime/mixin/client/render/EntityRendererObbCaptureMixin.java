package com.kltyton.bedrockruntime.mixin.client.render;

import com.kltyton.bedrockruntime.client.render.capture.CulledEntityCapture;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * CN: 附近模型独立采集 OBB，剔除范围包含真实模型边界，保留原版距离和视锥判断。
 * EN: Samples nearby OBBs independently and extends culling bounds to the model while preserving vanilla distance and frustum checks.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererObbCaptureMixin<T extends Entity> {
    @ModifyVariable(method = "shouldRender", at = @At("STORE"), ordinal = 0)
    private AABB kltyton_bedrock_runtime$includeModelBounds(AABB bounds, T entity, Frustum frustum,
            double cameraX, double cameraY, double cameraZ) {
        return CulledEntityCapture.extendCullingBounds(bounds, entity, frustum, cameraX, cameraY, cameraZ);
    }
}
