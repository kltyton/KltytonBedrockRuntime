package com.kltyton.bedrockruntime.client.particle.runtime.screen;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleManager;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import java.util.List;

public record ParticlePreviewRenderState(int x0, int y0, int x1, int y1, float scale,
                                         Matrix3x2f pose, @Nullable ScreenRectangle scissorArea,
                                         QuadParticleRenderState particles, ParticlesRenderState nativeParticles,
                                         CameraRenderState camera, MolangParticleManager owner, @Nullable ClientLevel previewLevel,
                                         Camera previewCamera, List<PreviewBlockQuad> blocks, Matrix4f projection,
                                         Matrix4f view, boolean perspective) implements PictureInPictureRenderState {
    @Override
    public @Nullable ScreenRectangle bounds() {
        return PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea);
    }
}
