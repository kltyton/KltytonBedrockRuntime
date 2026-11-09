package com.kltyton.bedrockruntime.mixin.client.particle;

import com.google.common.collect.EvictingQueue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import com.kltyton.bedrockruntime.client.particle.access.ParticleAdmission;
import com.kltyton.bedrockruntime.client.particle.access.ParticleGroupAdmission;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;

/** Releases admitted particles when 26.1.2's bounded queue evicts, removes or clears them. */
@Mixin(ParticleGroup.class)
public abstract class ParticleGroupMixin implements ParticleGroupAdmission {
    @Shadow @Final protected Queue<Particle> particles;
    @Shadow @Final protected ParticleEngine engine;

    @WrapOperation(method = "add", at = @At(value = "INVOKE", target = "Ljava/util/Queue;add(Ljava/lang/Object;)Z"))
    private boolean kltyton_bedrock_runtime$releaseEvicted(Queue<Particle> queue, Object particle, Operation<Boolean> original) {
        Particle evicted = queue instanceof EvictingQueue<?> bounded && bounded.remainingCapacity() == 0 ? queue.peek() : null;
        boolean added = original.call(queue, particle);
        if (added && evicted != null && evicted != particle) {
            evicted.getParticleLimit().ifPresent(limit ->
                    ((ParticleEngineAccessor) engine).kltyton_bedrock_runtime$getTrackedParticleCounts().addTo(limit, -1));
            if (evicted instanceof ParticleAdmission tracked) tracked.kltyton_bedrock_runtime$released();
            if (evicted.isAlive()) evicted.remove();
        }
        return added;
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
