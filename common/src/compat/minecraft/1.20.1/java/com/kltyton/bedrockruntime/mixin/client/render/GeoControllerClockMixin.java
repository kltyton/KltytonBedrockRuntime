package com.kltyton.bedrockruntime.mixin.client.render;

import com.kltyton.bedrockruntime.client.compat.geckolib.skill.GeoControllerClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.core.animation.AnimationController", remap = false)
public abstract class GeoControllerClockMixin implements GeoControllerClock {
    @Shadow protected double tickOffset;
    @Unique private double kltyton_bedrock_runtime$timelineTick;

    @ModifyVariable(method = "process", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private double kltyton_bedrock_runtime$captureClock(double seekTime) {
        kltyton_bedrock_runtime$timelineTick = seekTime;
        return seekTime;
    }

    @Override
    public double kltyton_bedrock_runtime$getTimelineTick() { return kltyton_bedrock_runtime$timelineTick; }

    @Override
    public double kltyton_bedrock_runtime$getTickOffset() { return tickOffset; }
}
