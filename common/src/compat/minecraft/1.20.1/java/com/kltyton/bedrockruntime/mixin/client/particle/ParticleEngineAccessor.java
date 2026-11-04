package com.kltyton.bedrockruntime.mixin.client.particle;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import java.util.Map;
import java.util.Queue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ParticleEngine.class)
public interface ParticleEngineAccessor {
    @Accessor("particles")
    Map<ParticleRenderType, Queue<Particle>> kltyton_bedrock_runtime$getParticles();

    @Accessor("textureAtlas")
    TextureAtlas kltyton_bedrock_runtime$getTextureAtlas();
}
