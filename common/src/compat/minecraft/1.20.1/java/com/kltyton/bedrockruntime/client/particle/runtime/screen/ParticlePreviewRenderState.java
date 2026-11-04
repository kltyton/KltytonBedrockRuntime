package com.kltyton.bedrockruntime.client.particle.runtime.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleManager;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureManager;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.List;
import java.util.function.Predicate;

/** 1.20.1 screen-preview render input. */
public record ParticlePreviewRenderState(MolangParticleManager owner, @Nullable ClientLevel level,
                                         Camera camera, float partialTick, @Nullable Frustum frustum,
                                         LightTexture lightTexture, TextureManager textureManager,
                                         @Nullable PoseStack poseStack, Predicate<ParticleRenderType> layerPredicate,
                                         List<PreviewBlockQuad> blocks, Matrix4f projection, Matrix4f view,
                                         int x, int y, int width, int height) {
}
