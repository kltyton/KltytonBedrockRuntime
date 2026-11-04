package com.kltyton.bedrockruntime.client.particle.runtime.screen;

import net.minecraft.client.Camera;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.multiplayer.ClientLevel;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleManager;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;

import java.util.List;

public record ParticlePreviewRenderState(int x0, int y0, int x1, int y1, float scale,
                                         Matrix3x2fc pose, @Nullable ScreenRectangle scissorArea,
                                         MolangParticleManager owner, @Nullable ClientLevel previewLevel,
                                         Camera previewCamera, float partialTick, List<PreviewBlockQuad> blocks,
                                         Matrix4f projection, Matrix4f view, boolean perspective) {
    public @Nullable ScreenRectangle bounds() {
        if (scissorArea != null) {
            return new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).intersection(scissorArea);
        }
        return x1 > x0 && y1 > y0 ? new ScreenRectangle(x0, y0, x1 - x0, y1 - y0) : null;
    }
}
