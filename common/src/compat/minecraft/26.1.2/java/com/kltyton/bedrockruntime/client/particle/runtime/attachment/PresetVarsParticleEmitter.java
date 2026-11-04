package com.kltyton.bedrockruntime.client.particle.runtime.attachment;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.definition.event.ParticleEffect;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.Variable;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;

/// Emitter created with caller supplied Molang variables, e.g. from {@link EmitterAttachHandler#addEmitter}.
public class PresetVarsParticleEmitter extends IgnoreRangeParticleEmitter {
    public static final Identifier TYPE = BedrockParticles.asResource("preset_vars");

    private @Nullable Runnable createVarsCallback;

    public PresetVarsParticleEmitter(Level level, Vec3 pos, Identifier particleId, boolean ignoreRange, Variable... variables) {
        super(TYPE, level, pos, particleId, MolangExp.EMPTY, ignoreRange);
        this.createVarsCallback = () -> {
            for (Variable var : variables) {
                vars.table.put(var.name(), var);
            }
        };
        super.init();
    }

    public PresetVarsParticleEmitter(Level level, CompoundTag tag) {
        super(level, tag, true);
        super.init();
    }

    public PresetVarsParticleEmitter(ParticleEmitter parent, ParticleEffect effect) {
        super(parent, effect);
    }

    @Override
    protected void init() {}

    @Override
    protected void createVars() {
        super.createVars();
        if (createVarsCallback != null) {
            createVarsCallback.run();
            this.createVarsCallback = null;
        }
    }
}
