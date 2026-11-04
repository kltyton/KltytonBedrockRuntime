package com.kltyton.bedrockruntime.model.optimization;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.kltyton.bedrockruntime.geometry.mesh.TriangleMesh;
import com.kltyton.bedrockruntime.geometry.mesh.TriangleMeshBuilder;
import com.kltyton.bedrockruntime.model.binding.ModelConsumer;
import com.kltyton.bedrockruntime.model.binding.ModelConsumers;
import com.kltyton.bedrockruntime.model.definition.ModelBone;
import com.kltyton.bedrockruntime.model.definition.ModelGeometry;
import com.kltyton.bedrockruntime.model.definition.ModelGeometryPart;
import com.kltyton.bedrockruntime.model.resource.EnhancedModelData;

import org.joml.Matrix4f;

public final class CompiledModelPlanner {
    private CompiledModelPlanner() {
    }

    public static CompiledModelPlan plan(String resource, long generation, EnhancedModelData.Mode mode,
                                         ModelGeometry logical, EnhancedModelData data,
                                         ModelConsumers consumers) {
        if (data == null) {
            return CompiledModelPlan.retained(resource, generation, mode, logical, "no enhanced model data");
        }
        if (mode != EnhancedModelData.Mode.BAKED) {
            return retained(resource, generation, mode, logical, data, "model mode is not BAKED");
        }
        if (data.staticBones().isEmpty()) {
            return retained(resource, generation, mode, logical, data, "no certified static bones");
        }
        if (!logical.staticGeometryDeclaredImmutable()) {
            return retained(resource, generation, mode, logical, data,
                    "geometry was not explicitly declared immutable");
        }

        ModelConsumers effectiveConsumers = effectiveConsumers(data, consumers);
        List<StaticGroup> groups = staticGroups(logical, data, effectiveConsumers);
        if (groups.isEmpty()) {
            return retained(resource, generation, mode, logical, data,
                    "no safe multi-bone static groups");
        }
        OptimizationResult result = ModelOptimizer.optimize(logical, effectiveConsumers,
                OptimizationOptions.of(groups));
        if (!result.plan().optimized()) {
            return new CompiledModelPlan(resource, generation, mode, logical, result.geometry(), result.plan(),
                    List.of(), data.captureData(), data.meshes(), data.doubleSided());
        }
        Map<String, StaticGroup> byName = new LinkedHashMap<>();
        groups.forEach(group -> byName.put(group.name(), group));
        List<CompiledGroup> compiled = new ArrayList<>();
        for (OptimizationPlan.MergedGroup merged : result.plan().mergedGroups()) {
            StaticGroup group = byName.get(merged.groupName());
            if (group == null) {
                continue;
            }
            List<String> dependencies = dependencyBones(logical, merged.anchorBone(), group.boneNames());
            if (dependencies == null) {
                continue;
            }
            TriangleMesh mesh = mergeGroup(logical, group, merged.anchorBone(), merged.material());
            if (mesh == null || mesh.triangleCount() == 0) {
                continue;
            }
            compiled.add(new CompiledGroup(merged.groupName(), merged.anchorBone(), merged.material(), mesh,
                    group.boneNames(), dependencies));
        }
        if (compiled.isEmpty()) {
            return new CompiledModelPlan(resource, generation, mode, logical, result.geometry(), result.plan(),
                    List.of(), data.captureData(), data.meshes(), data.doubleSided());
        }
        return new CompiledModelPlan(resource, generation, mode, logical, result.geometry(), result.plan(),
                compiled, data.captureData(), data.meshes(), data.doubleSided());
    }

    private static CompiledModelPlan retained(String resource, long generation, EnhancedModelData.Mode mode,
                                               ModelGeometry logical, EnhancedModelData data, String reason) {
        OptimizationPlan plan = OptimizationPlan.retained(logical, reason, List.of());
        return new CompiledModelPlan(resource, generation, mode, logical, logical, plan, List.of(),
                data.captureData(), data.meshes(), data.doubleSided());
    }

    private static ModelConsumers effectiveConsumers(EnhancedModelData data, ModelConsumers consumers) {
        ModelConsumers.Builder builder = ModelConsumers.builder();
        if (consumers != null) {
            for (ModelConsumer consumer : consumers.activeConsumers()) {
                builder.addAll(consumer, consumers.bones(consumer));
            }
        }
        for (String preserved : data.preservedBones()) {
            builder.add(ModelConsumer.PER_BONE_CALLBACK, preserved);
        }
        return builder.build();
    }

    private static List<StaticGroup> staticGroups(ModelGeometry geometry, EnhancedModelData data,
                                                  ModelConsumers consumers) {
        Set<String> staticBones = new LinkedHashSet<>(data.staticBones());
        Set<String> preserved = data.preservedBones();
        Map<String, List<String>> byTop = new LinkedHashMap<>();
        for (ModelBone bone : geometry.bones()) {
            String name = bone.name();
            if (!staticBones.contains(name) || preserved.contains(name) || consumers.references(name)
                    || !bone.hasGeometry()) {
                continue;
            }
            String top = name;
            String parent = bone.parentName();
            while (parent != null && staticBones.contains(parent) && !preserved.contains(parent)
                    && !consumers.references(parent) && geometry.boneIndex(parent) >= 0) {
                top = parent;
                parent = geometry.bones().get(geometry.boneIndex(parent)).parentName();
            }
            byTop.computeIfAbsent(top, ignored -> new ArrayList<>()).add(name);
        }
        List<StaticGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : byTop.entrySet()) {
            if (entry.getValue().size() < 2) {
                continue;
            }
            groups.add(new StaticGroup("compiled:" + entry.getKey(), entry.getValue(), entry.getKey()));
        }
        return groups;
    }

    private static TriangleMesh mergeGroup(ModelGeometry geometry, StaticGroup group, String anchor,
                                           String material) {
        Matrix4f anchorInverse = invert(geometry.globalBindTransform(anchor));
        if (anchorInverse == null) {
            return null;
        }
        TriangleMeshBuilder builder = new TriangleMeshBuilder();
        for (String boneName : group.boneNames()) {
            ModelBone bone = geometry.requireBone(boneName);
            Matrix4f boneGlobal = geometry.globalBindTransform(boneName);
            for (ModelGeometryPart part : bone.parts()) {
                if (!part.material().equals(material)) {
                    continue;
                }
                Matrix4f sourceToModel = new Matrix4f(boneGlobal).mul(part.localTransform());
                Matrix4f sourceToAnchor = new Matrix4f(anchorInverse).mul(sourceToModel);
                builder.addMesh(part.mesh(), sourceToAnchor);
            }
        }
        return builder.build();
    }

    private static List<String> dependencyBones(ModelGeometry geometry, String anchor, List<String> sources) {
        LinkedHashSet<String> dependencies = new LinkedHashSet<>();
        for (String source : sources) {
            int index = geometry.boneIndex(source);
            if (index < 0) {
                return null;
            }
            int current = index;
            boolean foundAnchor = false;
            while (current >= 0) {
                String name = geometry.bones().get(current).name();
                if (name.equals(anchor)) {
                    foundAnchor = true;
                    break;
                }
                dependencies.add(name);
                current = geometry.bones().get(current).parentIndex();
            }
            if (!foundAnchor) {
                return null;
            }
        }
        return List.copyOf(dependencies);
    }

    private static Matrix4f invert(Matrix4f matrix) {
        float determinant = matrix.determinant();
        if (!Float.isFinite(determinant) || Math.abs(determinant) <= 1.0E-12F) {
            return null;
        }
        Matrix4f inverse = new Matrix4f(matrix).invert();
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                if (!Float.isFinite(inverse.get(column, row))) {
                    return null;
                }
            }
        }
        return inverse;
    }
}


