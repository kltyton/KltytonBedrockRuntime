package com.kltyton.bedrockruntime.mixin.client.particle;

import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleEngine;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Converts registered effect particles into attached emitters. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @WrapWithCondition(method = "tickEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private boolean kltyton_bedrock_runtime$modify(Level instance, ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        if (particleData instanceof MolangParticleOption(Identifier id)) {
            MolangParticleEngine.INSTANCE.addTrackedEmitter((LivingEntity) (Object) this, id);
            return false;
        }
        return true;
    }
}
