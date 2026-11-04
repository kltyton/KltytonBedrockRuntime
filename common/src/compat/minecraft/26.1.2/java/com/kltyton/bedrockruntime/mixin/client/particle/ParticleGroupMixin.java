package com.kltyton.bedrockruntime.mixin.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleGroup;
import com.kltyton.bedrockruntime.client.particle.access.ParticleAdmission;
import com.kltyton.bedrockruntime.client.particle.access.ParticleGroupAdmission;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Queue;

/** Releases admitted particles when the group rejects, removes or clears them. */
@Mixin(ParticleGroup.class)
public abstract class ParticleGroupMixin implements ParticleGroupAdmission {
    @Shadow @Final protected Queue<Particle> particles;

    @Inject(method = "add", at = @At("RETURN"))
    private void kltyton_bedrock_runtime$onGroupAdmission(Particle particle, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && particle instanceof ParticleAdmission tracked) tracked.kltyton_bedrock_runtime$released();
    }

    @Inject(method = "tickParticle", at = @At("TAIL"))
    private void kltyton_bedrock_runtime$onParticleTick(Particle particle, CallbackInfo ci) {
        if (!particle.isAlive() && particle instanceof ParticleAdmission tracked) tracked.kltyton_bedrock_runtime$released();
    }

    @Override
    public void kltyton_bedrock_runtime$releaseParticles() {
        for (Particle particle : particles) {
            if (particle instanceof ParticleAdmission tracked) tracked.kltyton_bedrock_runtime$released();
        }
    }
}
