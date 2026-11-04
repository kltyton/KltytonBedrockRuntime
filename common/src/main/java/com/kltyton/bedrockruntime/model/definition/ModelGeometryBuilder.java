package com.kltyton.bedrockruntime.model.definition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.kltyton.bedrockruntime.geometry.mesh.TriangleMesh;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class ModelGeometryBuilder {
    private final String id;
    private final boolean staticGeometryDeclaredImmutable;
    private final Map<String, BoneDraft> bones = new LinkedHashMap<>();

    public ModelGeometryBuilder(String id, boolean staticGeometryDeclaredImmutable) {
        this.id = id == null ? "" : id;
        this.staticGeometryDeclaredImmutable = staticGeometryDeclaredImmutable;
    }

    public static ModelGeometryBuilder from(ModelGeometry geometry) {
        Objects.requireNonNull(geometry, "geometry");
        ModelGeometryBuilder builder = new ModelGeometryBuilder(geometry.id(), geometry.staticGeometryDeclaredImmutable());
        for (ModelBone bone : geometry.bones()) {
            builder.addBone(bone.name(), bone.parentName(), bone.bindLocalTransform(), bone.pivot());
            for (ModelGeometryPart part : bone.parts()) {
                builder.addPart(bone.name(), part.material(), part.mesh(), part.localTransform());
            }
            for (ModelLocator locator : bone.locators()) {
                builder.addLocator(bone.name(), locator.name(), locator.localPosition(), locator.rotationEuler());
            }
        }
        return builder;
    }

    public ModelGeometryBuilder addBone(String name, String parentName, Matrix4f bindLocalTransform, Vector3f pivot) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("bone name must not be blank");
        }
        if (bones.containsKey(name)) {
            throw new IllegalArgumentException("duplicate bone name: " + name);
        }
        bones.put(name, new BoneDraft(parentName, new Matrix4f(Objects.requireNonNull(bindLocalTransform, "bindLocalTransform")),
                new Vector3f(Objects.requireNonNull(pivot, "pivot"))));
        return this;
    }

    public ModelGeometryBuilder addBone(String name, String parentName, Vector3f pivot) {
        return addBone(name, parentName, new Matrix4f(), pivot);
    }

    public ModelGeometryBuilder addBone(String name, String parentName) {
        return addBone(name, parentName, new Matrix4f(), new Vector3f());
    }

    public ModelGeometryBuilder addPart(String boneName, String material, TriangleMesh mesh, Matrix4f localTransform) {
        BoneDraft bone = requireDraft(boneName);
        bone.parts.add(new ModelGeometryPart(material, mesh, localTransform));
        return this;
    }

    public ModelGeometryBuilder addLocator(String boneName, String name, Vector3f position) {
        return addLocator(boneName, name, position, new Vector3f());
    }

    public ModelGeometryBuilder addLocator(String boneName, String name, Vector3f position, Vector3f rotation) {
        BoneDraft bone = requireDraft(boneName);
        bone.locators.add(new ModelLocator(name, position, rotation));
        return this;
    }

    public ModelGeometry build() {
        Map<String, Integer> indices = new LinkedHashMap<>();
        int index = 0;
        for (String name : bones.keySet()) {
            indices.put(name, index++);
        }
        List<ModelBone> built = new ArrayList<>(bones.size());
        int boneIndex = 0;
        for (Map.Entry<String, BoneDraft> entry : bones.entrySet()) {
            BoneDraft draft = entry.getValue();
            int parentIndex = -1;
            if (draft.parentName != null && !draft.parentName.isBlank()) {
                Integer parent = indices.get(draft.parentName);
                if (parent == null) {
                    throw new IllegalArgumentException("unknown parent bone '" + draft.parentName + "' for " + entry.getKey());
                }
                parentIndex = parent;
            }
            built.add(new ModelBone(entry.getKey(), boneIndex++, parentIndex, draft.parentName,
                    draft.pivot, draft.bindLocalTransform, draft.parts, draft.locators));
        }
        return new ModelGeometry(id, staticGeometryDeclaredImmutable, built);
    }

    private BoneDraft requireDraft(String boneName) {
        BoneDraft draft = bones.get(boneName);
        if (draft == null) {
            throw new IllegalArgumentException("unknown bone: " + boneName);
        }
        return draft;
    }

    private static final class BoneDraft {
        private final String parentName;
        private final Matrix4f bindLocalTransform;
        private final Vector3f pivot;
        private final List<ModelGeometryPart> parts = new ArrayList<>();
        private final List<ModelLocator> locators = new ArrayList<>();

        private BoneDraft(String parentName, Matrix4f bindLocalTransform, Vector3f pivot) {
            this.parentName = parentName;
            this.bindLocalTransform = bindLocalTransform;
            this.pivot = pivot;
        }
    }

}

