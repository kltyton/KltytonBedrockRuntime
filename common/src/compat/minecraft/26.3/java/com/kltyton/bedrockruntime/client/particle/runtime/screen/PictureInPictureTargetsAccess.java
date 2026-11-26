package com.kltyton.bedrockruntime.client.particle.runtime.screen;

import com.mojang.renderpearl.api.textures.GpuTextureView;
import org.jetbrains.annotations.Nullable;

/** Exposes the active PictureInPictureRenderer color and depth attachments on MC 26.3. */
public interface PictureInPictureTargetsAccess {
    @Nullable GpuTextureView kltyton_bedrock_runtime$colorTextureView();

    @Nullable GpuTextureView kltyton_bedrock_runtime$depthTextureView();
}
