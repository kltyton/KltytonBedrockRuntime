package com.kltyton.bedrockruntime.client.compat.geckolib.render;

import com.kltyton.bedrockruntime.client.compat.geckolib.optimization.GeometryRenderScope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;

/** Marker and call bridge installed on GeckoLib 4.8.4's Forge renderer interface. */
public interface ForgeGeckoRenderHook {
    static void verifyInstalled() {
        if (!ForgeGeckoRenderHook.class.isAssignableFrom(GeoRenderer.class)) {
            throw new IllegalStateException("KBR could not install its Forge GeckoLib renderer transformer");
        }
    }

    static void renderCubes(GeoRenderer<?> renderer, PoseStack pose, GeoBone bone, VertexConsumer buffer,
            int light, int overlay, float red, float green, float blue, float alpha, boolean reRender) {
        boolean previous = GeometryRenderScope.enter(!reRender);
        try {
            renderer.renderCubesOfBone(pose, bone, buffer, light, overlay, red, green, blue, alpha);
        } finally {
            GeometryRenderScope.exit(previous);
        }
    }
}
