package com.kltyton.bedrockruntime.client.particle.runtime.screen;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.kltyton.bedrockruntime.client.particle.definition.event.ParticleEffect;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;
import com.kltyton.bedrockruntime.client.particle.runtime.EmitterPreset;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticlePreset;
import org.jetbrains.annotations.Nullable;

/// Marks an emitter as belonging to an independent screen preview.
public class ScreenParticleEmitter extends ParticleEmitter {
    public ScreenParticleEmitter(ResourceLocation type, Level level, Vec3 pos, ResourceLocation particleId, MolangExp expression) {
        super(type, level, pos, particleId, expression);
    }

    protected ScreenParticleEmitter(ResourceLocation type, Level level, Vec3 pos, ResourceLocation particleId, MolangExp expression,
                                    boolean deferInitialization) {
        super(type, level, pos, particleId, expression, null, deferInitialization);
    }

    public ScreenParticleEmitter(ParticleEmitter parent, ParticleEffect effect) {
        super(parent, effect);
    }

    protected ScreenParticleEmitter(ParticleEmitter parent, ParticleEffect effect,
                                    @Nullable EmitterPreset preset, @Nullable ParticlePreset particlePreset) {
        super(parent, effect, preset, particlePreset);
    }
}
