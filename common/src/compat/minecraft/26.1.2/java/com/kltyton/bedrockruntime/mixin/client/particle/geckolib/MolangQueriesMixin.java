package com.kltyton.bedrockruntime.mixin.client.particle.geckolib;

import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleEngine;

import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangQueries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.geckolib.loading.math.MolangQueries.setActorVariable;

@Pseudo
@Mixin(targets = "com.geckolib.loading.math.MolangQueries", remap = false)
public abstract class MolangQueriesMixin {
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void kltyton_bedrock_runtime$particleQueries(CallbackInfo ci) {
        setActorVariable("query.total_emitter_count", actor -> MolangParticleEngine.INSTANCE.totalEmitterCount());
        setActorVariable("query.total_particle_count", actor -> MolangQueries.totalParticleCount());
    }
}
