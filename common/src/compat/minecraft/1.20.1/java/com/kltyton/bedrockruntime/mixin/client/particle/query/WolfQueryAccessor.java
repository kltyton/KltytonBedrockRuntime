package com.kltyton.bedrockruntime.mixin.client.particle.query;

import net.minecraft.world.entity.animal.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Wolf.class)
public interface WolfQueryAccessor {
    @Accessor("isShaking")
    boolean kltyton_bedrock_runtime$isShakingWetness();
}