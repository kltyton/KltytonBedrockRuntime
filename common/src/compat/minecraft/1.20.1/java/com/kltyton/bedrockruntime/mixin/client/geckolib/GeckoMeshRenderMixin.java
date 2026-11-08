package com.kltyton.bedrockruntime.mixin.client.geckolib;

import com.kltyton.bedrockruntime.client.compat.geckolib.optimization.GeometryRenderScope;
import com.kltyton.bedrockruntime.client.compat.geckolib.render.GeckoGeometryHooks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;

/**
 * Minimal bridge for GeckoLib 4: emit the optional authored poly_mesh immediately after the
 * renderer's normal cubes for the same bone, using the same pose, vertex consumer and material pass.
 * GeckoRenderer already applies RenderUtils.prepMatrixForBone before renderCubesOfBone, and mesh
 * coordinates are absolute model coordinates, so no additional pivot translation is applied here.
 */
@Mixin(value = GeoRenderer.class, remap = false)
public interface GeckoMeshRenderMixin {
    @WrapOperation(method = "renderRecursively", at = @At(value = "INVOKE",
            target = "Lsoftware/bernie/geckolib/renderer/GeoRenderer;renderCubesOfBone"))
    private void kltyton_bedrock_runtime$scopeGeometry(GeoRenderer<?> renderer, PoseStack pose, GeoBone bone, VertexConsumer buffer,
                                            int light, int overlay, float red, float green, float blue, float alpha,
                                            Operation<Void> original, @Local(argsOnly = true) boolean isReRender) {
        boolean previous = GeometryRenderScope.enter(!isReRender);
        try {
            original.call(renderer, pose, bone, buffer, light, overlay, red, green, blue, alpha);
        } finally {
            GeometryRenderScope.exit(previous);
        }
    }

    @Inject(method = "renderCubesOfBone", at = @At("HEAD"), cancellable = true)
    private void kltyton_bedrock_runtime$renderCompiledGeometry(PoseStack poseStack, GeoBone bone, VertexConsumer buffer,
                                                     int light, int overlay, float red, float green, float blue,
                                                     float alpha, CallbackInfo callback) {
        if (GeckoGeometryHooks.intercept(poseStack, bone, buffer, light, overlay, red, green, blue, alpha)) {
            callback.cancel();
        }
    }

    @Inject(method = "renderCubesOfBone", at = @At("TAIL"))
    private void kltyton_bedrock_runtime$renderPolyMesh(PoseStack poseStack, GeoBone bone, VertexConsumer buffer,
                                              int packedLight, int packedOverlay,
                                              float red, float green, float blue, float alpha,
                                              CallbackInfo callback) {
        GeckoGeometryHooks.emit(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
