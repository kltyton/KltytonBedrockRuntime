package com.kltyton.bedrockruntime.client.particle.api;

import com.kltyton.bedrockruntime.client.particle.definition.DefinedParticleEffect;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticlePreset;

public class ParticlePresetLoadedEvent {
    private final ParticlePreset preset;

    public ParticlePresetLoadedEvent(ParticlePreset preset) {
        this.preset = preset;
    }

    /// The effect that produced the preset, kept for API compatibility.
    public DefinedParticleEffect getEffect() {
        return preset.effect;
    }

    public ParticlePreset getPreset() {
        return preset;
    }
}
