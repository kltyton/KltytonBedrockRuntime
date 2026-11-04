package com.kltyton.bedrockruntime.client.particle.api;

import org.jetbrains.annotations.ApiStatus;
import com.kltyton.bedrockruntime.client.particle.definition.DefinedParticleEffect;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticlePreset;

public class ParticlePresetLoadedEvent {

    private final DefinedParticleEffect effect;
    private final ParticlePreset preset;

    @ApiStatus.Internal
    public ParticlePresetLoadedEvent(ParticlePreset preset) {
        this.effect = preset.effect;
        this.preset = preset;
    }

    public DefinedParticleEffect getEffect() {
        return effect;
    }

    public ParticlePreset getPreset() {
        return preset;
    }

    @FunctionalInterface
    public interface Callback {
        void onParticlePresetLoaded(ParticlePresetLoadedEvent event);
    }
}
