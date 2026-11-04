package com.kltyton.bedrockruntime.model.definition;

import java.util.List;
import java.util.Objects;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class ModelBone {
    private final String name;
    private final int index;
    private final int parentIndex;
    private final String parentName;
    private final Vector3f pivot;
    private final Matrix4f bindLocalTransform;
    private final List<ModelGeometryPart> parts;
    private final List<ModelLocator> locators;

    ModelBone(String name, int index, int parentIndex, String parentName, Vector3f pivot,
              Matrix4f bindLocalTransform, List<ModelGeometryPart> parts, List<ModelLocator> locators) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("bone name must not be blank");
        }
        if (index < 0 || parentIndex < -1) {
            throw new IllegalArgumentException("invalid bone indices");
        }
        this.name = name;
        this.index = index;
        this.parentIndex = parentIndex;
        this.parentName = parentName;
        this.pivot = new Vector3f(Objects.requireNonNull(pivot, "pivot"));
        this.bindLocalTransform = new Matrix4f(Objects.requireNonNull(bindLocalTransform, "bindLocalTransform"));
        this.parts = List.copyOf(Objects.requireNonNull(parts, "parts"));
        this.locators = List.copyOf(Objects.requireNonNull(locators, "locators"));
        if (!finite(pivot) || !finite(bindLocalTransform)) {
            throw new IllegalArgumentException("bone transform must be finite");
        }
    }

    public String name() {
        return name;
    }

    public int index() {
        return index;
    }

    public int parentIndex() {
        return parentIndex;
    }

    public String parentName() {
        return parentName;
    }

    public Vector3f pivot() {
        return new Vector3f(pivot);
    }

    public Matrix4f bindLocalTransform() {
        return new Matrix4f(bindLocalTransform);
    }

    public List<ModelGeometryPart> parts() {
        return parts;
    }

    public List<ModelLocator> locators() {
        return locators;
    }

    public boolean hasGeometry() {
        return !parts.isEmpty();
    }

    public int triangleCount() {
        int total = 0;
        for (ModelGeometryPart part : parts) {
            total += part.triangleCount();
        }
        return total;
    }

    public ModelBone withParts(List<ModelGeometryPart> newParts) {
        return new ModelBone(name, index, parentIndex, parentName, pivot, bindLocalTransform, newParts, locators);
    }

    private static boolean finite(Vector3f value) {
        return Float.isFinite(value.x()) && Float.isFinite(value.y()) && Float.isFinite(value.z());
    }

    private static boolean finite(Matrix4f matrix) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                if (!Float.isFinite(matrix.get(column, row))) {
                    return false;
                }
            }
        }
        return true;
    }
}

