package com.kltyton.bedrockruntime.model.pose;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.kltyton.bedrockruntime.model.definition.ModelBone;
import com.kltyton.bedrockruntime.model.definition.ModelGeometry;

import org.joml.Matrix4f;

public final class ModelPose {
    private final ModelGeometry geometry;
    private final Map<String, Matrix4f> localOverrides;

    private ModelPose(ModelGeometry geometry, Map<String, Matrix4f> localOverrides) {
        this.geometry = Objects.requireNonNull(geometry, "geometry");
        Map<String, Matrix4f> copy = new HashMap<>();
        for (Map.Entry<String, Matrix4f> entry : localOverrides.entrySet()) {
            copy.put(entry.getKey(), new Matrix4f(entry.getValue()));
        }
        this.localOverrides = Collections.unmodifiableMap(copy);
    }

    public static ModelPose bind(ModelGeometry geometry) {
        return new ModelPose(geometry, Map.of());
    }

    public ModelGeometry geometry() {
        return geometry;
    }

    public boolean hasOverride(String boneName) {
        return localOverrides.containsKey(boneName);
    }

    public Set<String> overriddenBones() {
        return localOverrides.keySet();
    }

    public ModelPose withLocalTransform(String boneName, Matrix4f transform) {
        int index = geometry.boneIndex(boneName);
        if (index < 0) {
            throw new IllegalArgumentException("unknown bone: " + boneName);
        }
        Objects.requireNonNull(transform, "transform");
        Map<String, Matrix4f> copy = new HashMap<>(localOverrides);
        copy.put(boneName, new Matrix4f(transform));
        return new ModelPose(geometry, copy);
    }

    public ModelPose withoutLocalTransform(String boneName) {
        if (!localOverrides.containsKey(boneName)) {
            return this;
        }
        Map<String, Matrix4f> copy = new HashMap<>(localOverrides);
        copy.remove(boneName);
        return new ModelPose(geometry, copy);
    }

    public Matrix4f localTransform(String boneName) {
        int index = geometry.boneIndex(boneName);
        if (index < 0) {
            throw new IllegalArgumentException("unknown bone: " + boneName);
        }
        return localTransform(index);
    }

    public Matrix4f globalTransform(String boneName) {
        int index = geometry.boneIndex(boneName);
        if (index < 0) {
            throw new IllegalArgumentException("unknown bone: " + boneName);
        }
        List<Integer> chain = new ArrayList<>();
        int current = index;
        while (current >= 0) {
            chain.add(current);
            current = geometry.parentIndex(current);
        }
        Matrix4f result = new Matrix4f();
        for (int i = chain.size() - 1; i >= 0; i--) {
            result.mul(localTransform(chain.get(i)));
        }
        return result;
    }

    private Matrix4f localTransform(int boneIndex) {
        ModelBone bone = geometry.bones().get(boneIndex);
        Matrix4f override = localOverrides.get(bone.name());
        return override == null ? bone.bindLocalTransform() : new Matrix4f(override);
    }
}
