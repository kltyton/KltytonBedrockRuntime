package com.kltyton.bedrockruntime.client.particle.runtime.render;

import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.Nullable;

/** Identifies an active Iris program and its optional fallback alpha uniform. */
public interface IrisParticleShader {
    default @Nullable Uniform kltyton_bedrock_runtime$alphaTestUniform() {
        return ((ShaderInstance) this).getUniform("AlphaTestValue");
    }
}
