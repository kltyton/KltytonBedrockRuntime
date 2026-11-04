package com.kltyton.bedrockruntime.client.particle.runtime.screen;

import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.resources.model.geometry.BakedQuad;

public record PreviewBlockQuad(float x, float y, float z, BakedQuad quad, QuadInstance instance) {
    public static PreviewBlockQuad snapshot(float x, float y, float z, BakedQuad quad, QuadInstance source) {
        QuadInstance copy = new QuadInstance();
        for (int i = 0; i < 4; i++) {
            copy.setColor(i, source.getColor(i));
            copy.setLightCoords(i, net.minecraft.util.LightCoordsUtil.FULL_BRIGHT);
        }
        copy.setOverlayCoords(source.overlayCoords());
        return new PreviewBlockQuad(x, y, z, quad, copy);
    }
}
