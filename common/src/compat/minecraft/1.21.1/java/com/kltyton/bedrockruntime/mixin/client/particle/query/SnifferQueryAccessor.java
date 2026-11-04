package com.kltyton.bedrockruntime.mixin.client.particle.query;

import net.minecraft.world.entity.animal.sniffer.Sniffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Sniffer.class)
public interface SnifferQueryAccessor {
    @Invoker("getState")
    Sniffer.State kltyton_bedrock_runtime$getState();
}