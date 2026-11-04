package com.kltyton.bedrockruntime.client.particle.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleOption;

public class MolangParticleMobEffect extends MobEffect {
    public MolangParticleMobEffect(MobEffectCategory category, int color, ResourceLocation particleId) {
        super(category, color, new MolangParticleOption(particleId));
    }
}
