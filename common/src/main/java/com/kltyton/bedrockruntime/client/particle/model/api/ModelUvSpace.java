package com.kltyton.bedrockruntime.client.particle.model.api;

/** How the UV values stored in a {@code TriangleMesh} must be interpreted. */
public enum ModelUvSpace {
    /** UVs are normalized texels in the model texture; the renderer maps them through the sprite. */
    NORMALIZED,
    /** UVs are already atlas coordinates (vanilla baked models and similar sources). */
    ATLAS
}
