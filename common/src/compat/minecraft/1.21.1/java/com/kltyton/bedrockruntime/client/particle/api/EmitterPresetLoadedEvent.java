package com.kltyton.bedrockruntime.client.particle.api;

import com.kltyton.bedrockruntime.client.particle.runtime.EmitterPreset;

/// Fired on the mod event bus after an {@link EmitterPreset} has been fully constructed and its
/// Molang expressions compiled. Listeners may attach extra data through {@link EmitterPreset#setTicket}.
public class EmitterPresetLoadedEvent {
    private final EmitterPreset preset;

    public EmitterPresetLoadedEvent(EmitterPreset preset) {
        this.preset = preset;
    }

    public EmitterPreset getPreset() {
        return preset;
    }
}
