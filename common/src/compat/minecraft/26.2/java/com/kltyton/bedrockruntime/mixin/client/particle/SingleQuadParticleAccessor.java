package com.kltyton.bedrockruntime.mixin.client.particle;

import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes the layer used by a quad particle to the editor preview. */
@Mixin(SingleQuadParticle.class)
public interface SingleQuadParticleAccessor {
    @Invoker("getLayer")
    SingleQuadParticle.Layer kltyton_bedrock_runtime$getLayer();
}
