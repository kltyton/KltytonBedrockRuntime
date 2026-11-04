package com.kltyton.bedrockruntime.model.definition;

import java.util.Objects;

import org.joml.Vector3f;

public record ModelLocator(String name, Vector3f localPosition, Vector3f rotationEuler) {
    public ModelLocator {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("locator name must not be blank");
        }
        Objects.requireNonNull(localPosition, "localPosition");
        Objects.requireNonNull(rotationEuler, "rotationEuler");
        localPosition = new Vector3f(localPosition);
        rotationEuler = new Vector3f(rotationEuler);
        if (!finite(localPosition) || !finite(rotationEuler)) {
            throw new IllegalArgumentException("locator coordinates must be finite");
        }
    }

    @Override
    public Vector3f localPosition() {
        return new Vector3f(localPosition);
    }

    @Override
    public Vector3f rotationEuler() {
        return new Vector3f(rotationEuler);
    }

    private static boolean finite(Vector3f value) {
        return Float.isFinite(value.x()) && Float.isFinite(value.y()) && Float.isFinite(value.z());
    }
}
