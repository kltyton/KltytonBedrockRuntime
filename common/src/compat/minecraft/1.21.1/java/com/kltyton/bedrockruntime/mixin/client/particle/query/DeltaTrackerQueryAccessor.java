package com.kltyton.bedrockruntime.mixin.client.particle.query;

import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the unclamped frame interval for the Molang delta-time query. */
@Mixin(DeltaTracker.Timer.class)
public interface DeltaTrackerQueryAccessor {
    @Accessor("realtimeDeltaTicks")
    float kltyton_bedrock_runtime$getRealtimeDeltaTicks();
}