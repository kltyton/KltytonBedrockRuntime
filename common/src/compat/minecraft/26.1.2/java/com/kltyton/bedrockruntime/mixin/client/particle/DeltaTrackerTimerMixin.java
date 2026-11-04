package com.kltyton.bedrockruntime.mixin.client.particle;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import com.kltyton.bedrockruntime.client.particle.molang.FrameTimeHistory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records the main client's frame intervals for Molang frame-history queries. */
@Mixin(DeltaTracker.Timer.class)
public abstract class DeltaTrackerTimerMixin {
    @Shadow private float realtimeDeltaTicks;
    @Shadow @Final private float msPerTick;

    @Inject(method = "advanceRealTime", at = @At("TAIL"))
    private void kltyton_bedrock_runtime$recordFrameTime(long currentMs, CallbackInfo ci) {
        if ((Object) this == Minecraft.getInstance().getDeltaTracker()) {
            FrameTimeHistory.record(realtimeDeltaTicks * msPerTick / 1000.0F);
        }
    }
}
