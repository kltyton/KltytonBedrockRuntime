package com.kltyton.bedrockruntime.client.particle.api;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleOption;

public class MolangParticleMobEffect extends MobEffect {
    public MolangParticleMobEffect(MobEffectCategory category, int color, Identifier particleId) {
        super(category, color, new MolangParticleOption(particleId));
    }
}
