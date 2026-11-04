package com.kltyton.bedrockruntime.mixin.client.geckolib;

import com.geckolib.cache.model.cuboid.CuboidGeoBone;
import com.geckolib.renderer.base.RenderPassInfo;
import com.kltyton.bedrockruntime.client.compat.geckolib.model.EnhancedBoneMeshes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Emits attached poly_mesh geometry after the native cuboid bone has rendered its cubes.
 * The render matrix has already been prepared for the bone; mesh coordinates are absolute model coordinates.
 */
@Mixin(value = CuboidGeoBone.class, remap = false)
public class GeckoPolyMeshRenderMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void kltyton_bedrock_runtime$renderPolyMesh(RenderPassInfo<?> renderPassInfo, PoseStack poseStack,
                                              VertexConsumer consumer, int packedLight, int packedOverlay,
                                              int renderColor, CallbackInfo callback) {
        EnhancedBoneMeshes.render((CuboidGeoBone) (Object) this, poseStack, consumer, packedLight, packedOverlay, renderColor);
    }
}
