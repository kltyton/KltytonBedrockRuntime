package com.kltyton.bedrockruntime.client.particle.api;

import org.jetbrains.annotations.ApiStatus;
import com.kltyton.bedrockruntime.client.particle.runtime.EmitterPreset;

public class EmitterPresetLoadedEvent {

    private final EmitterPreset preset;

    @ApiStatus.Internal
    public EmitterPresetLoadedEvent(EmitterPreset preset) {
        this.preset = preset;
    }

    public EmitterPreset getPreset() {
        return preset;
    }

    @FunctionalInterface
    public interface Callback {
        void onEmitterPresetLoaded(EmitterPresetLoadedEvent event);
    }
}
