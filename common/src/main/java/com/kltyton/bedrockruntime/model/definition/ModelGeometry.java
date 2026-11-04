package com.kltyton.bedrockruntime.model.definition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class ModelGeometry {
    private final String id;
    private final boolean staticGeometryDeclaredImmutable;
    private final List<ModelBone> bones;
    private final Map<String, Integer> boneIndices;
    private final int[][] children;
    private final Matrix4f[] globalBindTransforms;
    private final Map<String, LocatorBinding> locators;

    ModelGeometry(String id, boolean staticGeometryDeclaredImmutable, List<ModelBone> bones) {
        this.id = id == null ? "" : id;
        this.staticGeometryDeclaredImmutable = staticGeometryDeclaredImmutable;
        this.bones = List.copyOf(Objects.requireNonNull(bones, "bones"));
        Map<String, Integer> byName = new LinkedHashMap<>();
        for (int i = 0; i < this.bones.size(); i++) {
            ModelBone bone = this.bones.get(i);
            if (bone.index() != i) {
                throw new IllegalArgumentException("bone index does not match position: " + bone.name());
            }
            if (byName.put(bone.name(), i) != null) {
                throw new IllegalArgumentException("duplicate bone name: " + bone.name());
            }
        }
        this.boneIndices = Map.copyOf(byName);
        List<List<Integer>> childLists = new ArrayList<>(this.bones.size());
        for (int i = 0; i < this.bones.size(); i++) {
            childLists.add(new ArrayList<>());
        }
        for (int i = 0; i < this.bones.size(); i++) {
            ModelBone bone = this.bones.get(i);
            int parent = bone.parentIndex();
            if (parent < -1 || parent >= this.bones.size() || parent == i) {
                throw new IllegalArgumentException("invalid parent index for bone: " + bone.name());
            }
            if (parent >= 0) {
                childLists.get(parent).add(i);
            }
        }
        this.children = new int[childLists.size()][];
        for (int i = 0; i < childLists.size(); i++) {
            List<Integer> list = childLists.get(i);
            int[] values = new int[list.size()];
            for (int j = 0; j < list.size(); j++) {
                values[j] = list.get(j);
            }
            this.children[i] = values;
        }
        this.globalBindTransforms = new Matrix4f[this.bones.size()];
        int[] states = new int[this.bones.size()];
        for (int i = 0; i < this.bones.size(); i++) {
            globalBind(i, states);
        }
        Map<String, LocatorBinding> locatorMap = new LinkedHashMap<>();
        for (ModelBone bone : this.bones) {
            for (ModelLocator locator : bone.locators()) {
                if (locatorMap.put(locator.name(), new LocatorBinding(bone.name(), locator)) != null) {
                    throw new IllegalArgumentException("duplicate locator name: " + locator.name());
                }
            }
        }
        this.locators = Map.copyOf(locatorMap);
    }

    public String id() {
        return id;
    }

    public boolean staticGeometryDeclaredImmutable() {
        return staticGeometryDeclaredImmutable;
    }

    public List<ModelBone> bones() {
        return bones;
    }

    public int boneCount() {
        return bones.size();
    }

    public List<String> boneNames() {
        List<String> names = new ArrayList<>(bones.size());
        for (ModelBone bone : bones) {
            names.add(bone.name());
        }
        return List.copyOf(names);
    }

    public int boneIndex(String name) {
        Integer index = boneIndices.get(name);
        return index == null ? -1 : index;
    }

    public Optional<ModelBone> findBone(String name) {
        int index = boneIndex(name);
        return index < 0 ? Optional.empty() : Optional.of(bones.get(index));
    }

    public ModelBone requireBone(String name) {
        int index = boneIndex(name);
        if (index < 0) {
            throw new IllegalArgumentException("unknown bone: " + name);
        }
        return bones.get(index);
    }

    public int parentIndex(int boneIndex) {
        checkBoneIndex(boneIndex);
        return bones.get(boneIndex).parentIndex();
    }

    public List<ModelBone> childrenOf(String name) {
        int index = boneIndex(name);
        if (index < 0) {
            throw new IllegalArgumentException("unknown bone: " + name);
        }
        int[] childIndices = children[index];
        List<ModelBone> result = new ArrayList<>(childIndices.length);
        for (int child : childIndices) {
            result.add(bones.get(child));
        }
        return List.copyOf(result);
    }

    public List<ModelBone> rootBones() {
        List<ModelBone> result = new ArrayList<>();
        for (ModelBone bone : bones) {
            if (bone.parentIndex() < 0) {
                result.add(bone);
            }
        }
        return List.copyOf(result);
    }

    public Matrix4f localBindTransform(String name) {
        return requireBone(name).bindLocalTransform();
    }

    public Matrix4f globalBindTransform(String name) {
        int index = boneIndex(name);
        if (index < 0) {
            throw new IllegalArgumentException("unknown bone: " + name);
        }
        return new Matrix4f(globalBindTransforms[index]);
    }

    public Vector3f pivot(String name) {
        return requireBone(name).pivot();
    }

    public List<ModelLocator> locators(String boneName) {
        return requireBone(boneName).locators();
    }

    public Optional<ModelLocator> findLocator(String locatorName) {
        LocatorBinding binding = locators.get(locatorName);
        return binding == null ? Optional.empty() : Optional.of(binding.locator());
    }

    public String locatorBone(String locatorName) {
        LocatorBinding binding = locators.get(locatorName);
        return binding == null ? null : binding.boneName();
    }

    public int triangleCount() {
        int total = 0;
        for (ModelBone bone : bones) {
            total += bone.triangleCount();
        }
        return total;
    }

    public ModelGeometry withBones(List<ModelBone> newBones) {
        return new ModelGeometry(id, staticGeometryDeclaredImmutable, newBones);
    }

    private Matrix4f globalBind(int index, int[] states) {
        if (states[index] == 2) {
            return globalBindTransforms[index];
        }
        if (states[index] == 1) {
            throw new IllegalArgumentException("bone hierarchy contains a cycle at: " + bones.get(index).name());
        }
        states[index] = 1;
        ModelBone bone = bones.get(index);
        Matrix4f result = new Matrix4f();
        if (bone.parentIndex() >= 0) {
            result.mul(globalBind(bone.parentIndex(), states));
        }
        result.mul(bone.bindLocalTransform());
        globalBindTransforms[index] = result;
        states[index] = 2;
        return result;
    }

    private void checkBoneIndex(int index) {
        if (index < 0 || index >= bones.size()) {
            throw new IndexOutOfBoundsException("bone index " + index);
        }
    }

    private record LocatorBinding(String boneName, ModelLocator locator) {
    }
}

