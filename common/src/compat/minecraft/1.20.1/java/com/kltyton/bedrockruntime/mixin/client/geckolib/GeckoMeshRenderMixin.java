package com.kltyton.bedrockruntime.mixin.client.geckolib;

import com.kltyton.bedrockruntime.client.compat.geckolib.model.MeshGeoBone;
import com.kltyton.bedrockruntime.client.model.mesh.MeshVertexEmitter;
import com.kltyton.bedrockruntime.geometry.mesh.TriangleMesh;
import com.kltyton.bedrockruntime.client.compat.geckolib.optimization.GL4CompiledGeometry;
import com.kltyton.bedrockruntime.client.compat.geckolib.optimization.GeometryRenderScope;
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
        if (GeometryRenderScope.primary()
                && GL4CompiledGeometry.intercept(bone, poseStack, buffer, red, green, blue, alpha, light, overlay)) {
            callback.cancel();
        }
    }

    @Inject(method = "renderCubesOfBone", at = @At("TAIL"))
    private void kltyton_bedrock_runtime$renderPolyMesh(PoseStack poseStack, GeoBone bone, VertexConsumer buffer,
                                              int packedLight, int packedOverlay,
                                              float red, float green, float blue, float alpha,
                                              CallbackInfo callback) {
        TriangleMesh mesh = MeshGeoBone.mesh(bone);
        if (mesh == null || bone.isHidden()) {
            return;
        }
        int color = (Math.max(0, Math.min(255, Math.round(alpha * 255.0F))) << 24)
                | (Math.max(0, Math.min(255, Math.round(red * 255.0F))) << 16)
                | (Math.max(0, Math.min(255, Math.round(green * 255.0F))) << 8)
                | Math.max(0, Math.min(255, Math.round(blue * 255.0F)));
        MeshVertexEmitter.emit(mesh, poseStack.last().pose(), poseStack.last().normal(),
                color, packedLight, packedOverlay, MeshGeoBone.doubleSided(bone),
                (x, y, z, packedColor, u, v, overlay, light, normalX, normalY, normalZ) ->
                        buffer.vertex(x, y, z)
                                .color(red, green, blue, alpha)
                                .uv(u, v)
                                .overlayCoords(overlay)
                                .uv2(light)
                                .normal(normalX, normalY, normalZ)
                                .endVertex());
    }
}
