package com.kltyton.bedrockruntime.mixin.client.particle;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor {
    @Accessor("x")
    double kltyton_bedrock_runtime$getX();

    @Accessor("y")
    double kltyton_bedrock_runtime$getY();

    @Accessor("z")
    double kltyton_bedrock_runtime$getZ();
}
