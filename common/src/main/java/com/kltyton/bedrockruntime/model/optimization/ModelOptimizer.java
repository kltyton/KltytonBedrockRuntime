package com.kltyton.bedrockruntime.model.optimization;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.kltyton.bedrockruntime.geometry.mesh.TriangleMesh;
import com.kltyton.bedrockruntime.geometry.mesh.TriangleMeshBuilder;
import com.kltyton.bedrockruntime.model.binding.ModelConsumers;
import com.kltyton.bedrockruntime.model.definition.ModelBone;
import com.kltyton.bedrockruntime.model.definition.ModelGeometry;
import com.kltyton.bedrockruntime.model.definition.ModelGeometryPart;

import org.joml.Matrix4f;

public final class ModelOptimizer {
    private ModelOptimizer() {
    }

    public static OptimizationResult optimize(ModelGeometry geometry, ModelConsumers consumers,
                                              OptimizationOptions options) {
        Objects.requireNonNull(geometry, "geometry");
        Objects.requireNonNull(options, "options");
        ModelConsumers safeConsumers = consumers == null ? ModelConsumers.empty() : consumers;
        if (!options.enabled()) {
            return unchanged(geometry, "optimization disabled; original retained", List.of());
        }
        if (!geometry.staticGeometryDeclaredImmutable()) {
            return unchanged(geometry, "static geometry was not explicitly declared immutable", List.of());
        }
        if (options.staticGroups().isEmpty()) {
            return unchanged(geometry, "no certified static groups", List.of());
        }

        List<List<ModelGeometryPart>> partsByBone = new ArrayList<>(geometry.boneCount());
        for (ModelBone bone : geometry.bones()) {
            partsByBone.add(new ArrayList<>(bone.parts()));
        }
        List<OptimizationPlan.MergedGroup> merged = new ArrayList<>();
        List<OptimizationPlan.SkippedGroup> skipped = new ArrayList<>();
        Set<String> consumedBones = new HashSet<>();

        for (StaticGroup group : options.staticGroups()) {
            String unsafe = validateGroup(geometry, safeConsumers, group, consumedBones);
            if (unsafe != null) {
                skipped.add(new OptimizationPlan.SkippedGroup(group.name(), unsafe));
                continue;
            }
            String anchor = group.anchorBone() == null ? nearestCommonAncestor(geometry, group.boneNames()) : group.anchorBone();
            if (anchor == null) {
                skipped.add(new OptimizationPlan.SkippedGroup(group.name(), "no common anchor bone"));
                continue;
            }
            int anchorIndex = geometry.boneIndex(anchor);
            if (anchorIndex < 0) {
                skipped.add(new OptimizationPlan.SkippedGroup(group.name(), "unknown anchor bone: " + anchor));
                continue;
            }
            if (!group.boneNames().contains(anchor) && safeConsumers.references(anchor)) {
                skipped.add(new OptimizationPlan.SkippedGroup(group.name(), "anchor bone is consumer-visible"));
                continue;
            }
            Matrix4f anchorInverse = invert(geometry.globalBindTransform(anchor));
            if (anchorInverse == null) {
                skipped.add(new OptimizationPlan.SkippedGroup(group.name(), "anchor transform is not invertible"));
                continue;
            }

            Map<String, List<PartRef>> byMaterial = new LinkedHashMap<>();
            for (String boneName : group.boneNames()) {
                int boneIndex = geometry.boneIndex(boneName);
                for (ModelGeometryPart part : partsByBone.get(boneIndex)) {
                    byMaterial.computeIfAbsent(part.material(), ignored -> new ArrayList<>())
                            .add(new PartRef(boneIndex, part));
                }
            }

            boolean groupMerged = false;
            for (Map.Entry<String, List<PartRef>> entry : byMaterial.entrySet()) {
                List<PartRef> parts = entry.getValue();
                if (parts.size() < 2) {
                    continue;
                }
                TriangleMeshBuilder builder = new TriangleMeshBuilder();
                boolean transformFailed = false;
                for (PartRef part : parts) {
                    Matrix4f sourceToModel = new Matrix4f(geometry.globalBindTransform(geometry.bones().get(part.boneIndex).name()))
                            .mul(part.part.localTransform());
                    Matrix4f sourceToAnchor = new Matrix4f(anchorInverse).mul(sourceToModel);
                    try {
                        builder.addMesh(part.part.mesh(), sourceToAnchor);
                    } catch (RuntimeException exception) {
                        transformFailed = true;
                        break;
                    }
                }
                if (transformFailed) {
                    continue;
                }
                TriangleMesh mergedMesh = builder.build();
                if (mergedMesh.triangleCount() == 0) {
                    continue;
                }
                for (PartRef part : parts) {
                    partsByBone.get(part.boneIndex).remove(part.part);
                }
                partsByBone.get(anchorIndex).add(new ModelGeometryPart(entry.getKey(), mergedMesh, new Matrix4f()));
                merged.add(new OptimizationPlan.MergedGroup(group.name(), anchor, entry.getKey(),
                        parts.size(), mergedMesh.triangleCount()));
                groupMerged = true;
            }
            if (groupMerged) {
                consumedBones.addAll(group.boneNames());
            } else {
                skipped.add(new OptimizationPlan.SkippedGroup(group.name(),
                        "no two same-material parts in the certified group"));
            }
        }

        if (merged.isEmpty()) {
            return unchanged(geometry, "no safe same-material merge was possible", skipped);
        }
        List<ModelBone> newBones = new ArrayList<>(geometry.boneCount());
        for (int i = 0; i < geometry.boneCount(); i++) {
            newBones.add(geometry.bones().get(i).withParts(partsByBone.get(i)));
        }
        OptimizationPlan plan = new OptimizationPlan(true, "merged certified static geometry",
                merged, skipped, geometry.boneNames());
        return new OptimizationResult(geometry.withBones(newBones), plan);
    }

    private static String validateGroup(ModelGeometry geometry, ModelConsumers consumers, StaticGroup group,
                                        Set<String> consumedBones) {
        Set<String> names = new LinkedHashSet<>(group.boneNames());
        for (String bone : names) {
            if (geometry.boneIndex(bone) < 0) {
                return "unknown bone: " + bone;
            }
            if (consumedBones.contains(bone)) {
                return "bone already belongs to a merged group: " + bone;
            }
            if (consumers.references(bone)) {
                return "bone is referenced by a retained consumer: " + bone;
            }
        }
        return null;
    }

    private static String nearestCommonAncestor(ModelGeometry geometry, List<String> boneNames) {
        List<String> ancestors = ancestors(geometry, boneNames.get(0));
        for (int i = 1; i < boneNames.size(); i++) {
            Set<String> other = new HashSet<>(ancestors(geometry, boneNames.get(i)));
            ancestors.removeIf(bone -> !other.contains(bone));
        }
        return ancestors.isEmpty() ? null : ancestors.get(0);
    }

    private static List<String> ancestors(ModelGeometry geometry, String boneName) {
        List<String> result = new ArrayList<>();
        int index = geometry.boneIndex(boneName);
        while (index >= 0) {
            ModelBone bone = geometry.bones().get(index);
            result.add(bone.name());
            index = bone.parentIndex();
        }
        return result;
    }

    private static Matrix4f invert(Matrix4f matrix) {
        float determinant = matrix.determinant();
        if (!Float.isFinite(determinant) || Math.abs(determinant) <= 1.0E-12F) {
            return null;
        }
        Matrix4f result = new Matrix4f(matrix).invert();
        return finite(result) ? result : null;
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

    private static OptimizationResult unchanged(ModelGeometry geometry, String reason,
                                                List<OptimizationPlan.SkippedGroup> skipped) {
        return new OptimizationResult(geometry, OptimizationPlan.retained(geometry, reason, skipped));
    }

    private static final class PartRef {
        private final int boneIndex;
        private final ModelGeometryPart part;

        private PartRef(int boneIndex, ModelGeometryPart part) {
            this.boneIndex = boneIndex;
            this.part = part;
        }
    }
}
