package com.kltyton.bedrockruntime.mixin.client.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import com.kltyton.bedrockruntime.client.particle.access.ParticleAdmission;
import com.kltyton.bedrockruntime.client.particle.access.ParticleGroupAdmission;
import com.kltyton.bedrockruntime.client.particle.access.ParticleEnginePreviewOwner;
import com.kltyton.bedrockruntime.client.particle.access.IPSParticleEngine;
import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.api.RegisterCustomParticleTypeEvent;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleEngine;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleManager;
import com.kltyton.bedrockruntime.client.particle.runtime.screen.PreviewParticleScope;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Queue;

/** Tracks actual admission and clearing without changing the engine's limits. */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin implements ParticleEnginePreviewOwner, IPSParticleEngine {
    @Shadow @Final private Queue<Particle> particlesToAdd;
    @Shadow @Final private Map<ParticleRenderType, ParticleGroup<?>> particles;
    @Unique private @Nullable MolangParticleManager kltyton_bedrock_runtime$previewOwner;

    @Override
    public void kltyton_bedrock_runtime$setPreviewOwner(MolangParticleManager owner) {
        this.kltyton_bedrock_runtime$previewOwner = owner;
    }

    @Override
    public void kltyton_bedrock_runtime$bindSprites() {
        RegisterCustomParticleTypeEvent.rebindSprites();
    }

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void kltyton_bedrock_runtime$routePreviewAdd(Particle particle, CallbackInfo ci) {
        PreviewParticleScope.Context context = PreviewParticleScope.current();
        if (context != null && (Object) this == Minecraft.getInstance().particleEngine) {
            context.manager().addNativePreviewParticle(particle, context.level());
            ci.cancel();
        }
    }

    @WrapOperation(method = "add", at = @At(value = "INVOKE", target = "Ljava/util/Queue;add(Ljava/lang/Object;)Z"))
    private boolean kltyton_bedrock_runtime$admit(Queue<Particle> queue, Object value, Operation<Boolean> original) {
        boolean added = original.call(queue, value);
        if (added && value instanceof ParticleAdmission tracked
                && (kltyton_bedrock_runtime$previewOwner != null || value instanceof IMolangParticleInstance)) {
            tracked.kltyton_bedrock_runtime$admitted(kltyton_bedrock_runtime$previewOwner == null
                    ? MolangParticleEngine.INSTANCE : kltyton_bedrock_runtime$previewOwner);
        }
        return added;
    }

    @Inject(method = "extract", at = @At("HEAD"))
    private void kltyton_bedrock_runtime$evaluatePerRender(ParticlesRenderState state, Frustum frustum, Camera camera,
                                                  float partialTick, CallbackInfo ci) {
        if (kltyton_bedrock_runtime$previewOwner == null) {
            MolangParticleEngine.INSTANCE.evaluatePerRenderExpressions(partialTick);
        }
    }

    @Inject(method = "clearParticles", at = @At("HEAD"))
    private void kltyton_bedrock_runtime$clearAdmissions(CallbackInfo ci) {
        for (Particle particle : particlesToAdd) {
            if (particle instanceof ParticleAdmission tracked) tracked.kltyton_bedrock_runtime$released();
        }
        for (ParticleGroup<?> group : particles.values()) {
            ((ParticleGroupAdmission) group).kltyton_bedrock_runtime$releaseParticles();
        }
    }
}
