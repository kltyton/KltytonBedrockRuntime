package com.kltyton.bedrockruntime.client.particle.runtime.attachment;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.kltyton.bedrockruntime.client.particle.definition.event.ParticleEffect;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;

/// Emitter that may opt out of the distance based auto-removal handled by {@link EmitterAttachHandler}.
public abstract class IgnoreRangeParticleEmitter extends ParticleEmitter {
    public final boolean ignoreRange;

    public IgnoreRangeParticleEmitter(ResourceLocation type, Level level, Vec3 pos, ResourceLocation particleId, MolangExp expression, boolean ignoreRange) {
        super(type, level, pos, particleId, expression);
        this.ignoreRange = ignoreRange;
    }

    public IgnoreRangeParticleEmitter(Level level, CompoundTag tag) {
        this(level, tag, false);
    }

    protected IgnoreRangeParticleEmitter(Level level, CompoundTag tag, boolean deferInitialization) {
        super(level, tag, deferInitialization);
        this.ignoreRange = tag.getBoolean("ignoreRange");
    }

    public IgnoreRangeParticleEmitter(ParticleEmitter parent, ParticleEffect effect) {
        super(parent, effect);
        this.ignoreRange = parent instanceof IgnoreRangeParticleEmitter irpe && irpe.ignoreRange;
    }

    @Override
    public void serialize(CompoundTag tag) {
        super.serialize(tag);
        tag.putBoolean("ignoreRange", ignoreRange);
    }
}
