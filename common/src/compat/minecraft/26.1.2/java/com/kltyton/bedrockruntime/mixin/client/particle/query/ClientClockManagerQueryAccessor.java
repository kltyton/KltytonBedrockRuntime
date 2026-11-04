package com.kltyton.bedrockruntime.mixin.client.particle.query;

import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/** Exposes the synchronized client clock rate used by particle item frames. */
@Mixin(ClientClockManager.class)
public interface ClientClockManagerQueryAccessor {
    @Accessor("clocks")
    Map<Holder<WorldClock>, ?> kltyton_bedrock_runtime$getClocks();

    @Mixin(targets = "net.minecraft.client.ClientClockManager$ClockInstance")
    interface ClockInstanceAccessor {
        @Accessor("rate")
        float kltyton_bedrock_runtime$getRate();
    }
}