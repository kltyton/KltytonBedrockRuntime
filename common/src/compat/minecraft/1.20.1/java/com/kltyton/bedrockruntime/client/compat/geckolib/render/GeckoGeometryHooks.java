package com.kltyton.bedrockruntime.client.compat.geckolib.render;

import com.kltyton.bedrockruntime.client.compat.geckolib.model.MeshGeoBone;
import com.kltyton.bedrockruntime.client.compat.geckolib.optimization.GL4CompiledGeometry;
import com.kltyton.bedrockruntime.client.compat.geckolib.optimization.GeometryRenderScope;
import com.kltyton.bedrockruntime.client.model.mesh.MeshVertexEmitter;
import com.kltyton.bedrockruntime.geometry.mesh.TriangleMesh;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import software.bernie.geckolib.cache.object.GeoBone;

/** Applies KBR geometry at GeckoLib's per-bone cube boundary. */
public final class GeckoGeometryHooks {
    private GeckoGeometryHooks() { }

    public static boolean intercept(PoseStack pose, GeoBone bone, VertexConsumer buffer, int light, int overlay,
            float red, float green, float blue, float alpha) {
        return GeometryRenderScope.primary()
                && GL4CompiledGeometry.intercept(bone, pose, buffer, red, green, blue, alpha, light, overlay);
    }

    public static void emit(PoseStack pose, GeoBone bone, VertexConsumer buffer, int light, int overlay,
            float red, float green, float blue, float alpha) {
        TriangleMesh mesh = MeshGeoBone.mesh(bone);
        if (mesh == null || bone.isHidden()) { return; }
        int color = (Math.max(0, Math.min(255, Math.round(alpha * 255.0F))) << 24)
                | (Math.max(0, Math.min(255, Math.round(red * 255.0F))) << 16)
                | (Math.max(0, Math.min(255, Math.round(green * 255.0F))) << 8)
                | Math.max(0, Math.min(255, Math.round(blue * 255.0F)));
        MeshVertexEmitter.emit(mesh, pose.last().pose(), pose.last().normal(), color, light, overlay,
                MeshGeoBone.doubleSided(bone),
                (x, y, z, packedColor, u, v, packedOverlay, packedLight, normalX, normalY, normalZ) ->
                        buffer.vertex(x, y, z)
                                .color(red, green, blue, alpha)
                                .uv(u, v)
                                .overlayCoords(packedOverlay)
                                .uv2(packedLight)
                                .normal(normalX, normalY, normalZ)
                                .endVertex());
    }
}
