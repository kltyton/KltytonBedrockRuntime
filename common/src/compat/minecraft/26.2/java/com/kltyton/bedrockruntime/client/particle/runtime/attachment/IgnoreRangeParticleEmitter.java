package com.kltyton.bedrockruntime.client.particle.runtime.attachment;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.kltyton.bedrockruntime.client.particle.definition.event.ParticleEffect;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;

/// Emitter that may opt out of the distance based auto-removal handled by {@link EmitterAttachHandler}.
public abstract class IgnoreRangeParticleEmitter extends ParticleEmitter {
    public final boolean ignoreRange;

    public IgnoreRangeParticleEmitter(Identifier type, Level level, Vec3 pos, Identifier particleId, MolangExp expression, boolean ignoreRange) {
        this(type, level, pos, particleId, expression, ignoreRange, false);
    }

    protected IgnoreRangeParticleEmitter(Identifier type, Level level, Vec3 pos, Identifier particleId,
                                         MolangExp expression, boolean ignoreRange, boolean deferInitialization) {
        super(type, level, pos, particleId, expression, null, true);
        this.ignoreRange = ignoreRange;
        if (!deferInitialization) init();
    }

    public IgnoreRangeParticleEmitter(Level level, CompoundTag tag) {
        this(level, tag, false);
    }

    protected IgnoreRangeParticleEmitter(Level level, CompoundTag tag, boolean deferInitialization) {
        super(level, tag, true);
        this.ignoreRange = tag.getBoolean("ignoreRange").orElse(false);
        if (!deferInitialization) init();
    }

    public IgnoreRangeParticleEmitter(ParticleEmitter parent, ParticleEffect effect) {
        this(parent, effect, false);
    }

    protected IgnoreRangeParticleEmitter(ParticleEmitter parent, ParticleEffect effect, boolean deferInitialization) {
        super(parent, effect, null, null, true);
        this.ignoreRange = parent instanceof IgnoreRangeParticleEmitter irpe && irpe.ignoreRange;
        if (!deferInitialization) initializeEvent(parent, effect);
    }

    @Override
    public void serialize(CompoundTag tag) {
        super.serialize(tag);
        tag.putBoolean("ignoreRange", ignoreRange);
    }
}
