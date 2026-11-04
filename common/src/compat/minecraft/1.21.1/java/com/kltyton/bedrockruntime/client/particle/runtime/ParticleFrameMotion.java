package com.kltyton.bedrockruntime.client.particle.runtime;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/** Interpolated particle state supplied to the current render pass. */
public final class ParticleFrameMotion {
    private final Vector3f position = new Vector3f();
    private final Vector3f direction = new Vector3f();
    private boolean hasPosition;
    private boolean hasDirection;
    private boolean hasRotation;
    private float rotation;

    public void reset() {
        hasPosition = false;
        hasDirection = false;
        hasRotation = false;
    }

    public Vector3f positionBuffer() {
        hasPosition = true;
        return position;
    }

    public Vector3f directionBuffer() {
        hasDirection = true;
        return direction;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
        hasRotation = true;
    }

    public @Nullable Vector3f position() { return hasPosition ? position : null; }
    public @Nullable Vector3f direction() { return hasDirection ? direction : null; }
    public boolean hasRotation() { return hasRotation; }
    public float rotation() { return rotation; }
}
