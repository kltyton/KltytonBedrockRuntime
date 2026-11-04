package com.kltyton.bedrockruntime.client.particle.model.api;

import java.util.Locale;

/** The four material passes supported by model particles. */
public enum ModelMaterial {
    SOLID,
    CUTOUT,
    CUTOUT_MIPPED,
    TRANSLUCENT;

    public boolean translucent() {
        return this == TRANSLUCENT;
    }

    public static ModelMaterial byName(String value, ModelMaterial fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "solid", "opaque" -> SOLID;
            case "cutout", "alpha" -> CUTOUT;
            case "cutout_mipped", "cutout-mipped", "mipped" -> CUTOUT_MIPPED;
            case "translucent", "blend", "blended" -> TRANSLUCENT;
            default -> fallback;
        };
    }
}
