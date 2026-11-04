package com.kltyton.bedrockruntime.animation.pose;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable skeleton layout used by animation poses.
 *
 * <p>Bones must be declared in parent-before-child order. Every bone has an implicit bind
 * translation of zero and an implicit bind scale of one; only the bind rotation is stored.</p>
 */
public final class SkeletonBinding {
    public record Bone(
            String name,
            String parent,
            float pivotX,
            float pivotY,
            float pivotZ,
            float bindRotX,
            float bindRotY,
            float bindRotZ) {
        public Bone {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Bone name must not be blank");
            }
            parent = parent == null || parent.isBlank() ? null : parent;
            requireFinite(pivotX, "pivotX");
            requireFinite(pivotY, "pivotY");
            requireFinite(pivotZ, "pivotZ");
            requireFinite(bindRotX, "bindRotX");
            requireFinite(bindRotY, "bindRotY");
            requireFinite(bindRotZ, "bindRotZ");
        }

        private static void requireFinite(float value, String field) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("Bone " + field + " must be finite");
            }
        }
    }

    private final String resource;
    private final long generation;
    private final List<Bone> bones;
    private final Map<String, Integer> indices;
    private final int[] parentIndices;

    public SkeletonBinding(String resource, long generation, List<Bone> bones) {
        this.resource = Objects.requireNonNull(resource, "resource");
        this.generation = generation;
        Objects.requireNonNull(bones, "bones");
        List<Bone> copied = new ArrayList<>(bones.size());
        Map<String, Integer> indexByName = new HashMap<>(Math.max(16, bones.size() * 2));
        for (int i = 0; i < bones.size(); i++) {
            Bone bone = Objects.requireNonNull(bones.get(i), "bones[" + i + "]");
            Integer previous = indexByName.putIfAbsent(bone.name(), i);
            if (previous != null) {
                throw error("duplicate bone name '" + bone.name() + "'");
            }
            copied.add(bone);
        }
        this.bones = Collections.unmodifiableList(copied);
        this.indices = Map.copyOf(indexByName);
        int[] parents = new int[copied.size()];
        Arrays.fill(parents, -1);
        for (int i = 0; i < copied.size(); i++) {
            String parent = copied.get(i).parent();
            if (parent == null) {
                continue;
            }
            Integer parentIndex = indexByName.get(parent);
            if (parentIndex == null) {
                throw error("bone '" + copied.get(i).name() + "' references missing parent '" + parent + "'");
            }
            if (parentIndex >= i) {
                throw error("parent '" + parent + "' must precede child '" + copied.get(i).name() + "'");
            }
            parents[i] = parentIndex;
        }
        this.parentIndices = parents;
    }

    public String resource() {
        return resource;
    }

    public long generation() {
        return generation;
    }

    public int size() {
        return bones.size();
    }

    public Bone bone(int index) {
        checkIndex(index);
        return bones.get(index);
    }

    public int indexOf(String name) {
        if (name == null) {
            return -1;
        }
        return indices.getOrDefault(name, -1);
    }

    public int indexOfOrThrow(String name) {
        int index = indexOf(name);
        if (index < 0) {
            throw error("unknown bone name '" + name + "'");
        }
        return index;
    }

    public int parentIndex(int boneIndex) {
        checkIndex(boneIndex);
        return parentIndices[boneIndex];
    }

    public boolean isDescendantOf(int boneIndex, int ancestorIndex) {
        checkIndex(boneIndex);
        checkIndex(ancestorIndex);
        int current = boneIndex;
        while (current >= 0) {
            if (current == ancestorIndex) {
                return true;
            }
            current = parentIndices[current];
        }
        return false;
    }

    public List<Bone> bones() {
        return bones;
    }

    public boolean sameSkeleton(SkeletonBinding other) {
        return this == other
                || other != null
                && generation == other.generation
                && resource.equals(other.resource)
                && bones.equals(other.bones);
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= bones.size()) {
            throw error("bone index " + index + " is outside [0, " + bones.size() + ")");
        }
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException("Skeleton '" + resource + "': " + message);
    }

    @Override
    public String toString() {
        return "SkeletonBinding[" + resource + "#" + generation + ", bones=" + bones.size() + "]";
    }
}
