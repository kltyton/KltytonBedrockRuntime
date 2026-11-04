package com.kltyton.bedrockruntime.model.optimization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.kltyton.bedrockruntime.geometry.mesh.MeshCaptureData;
import com.kltyton.bedrockruntime.geometry.mesh.TriangleMesh;
import com.kltyton.bedrockruntime.model.definition.ModelGeometry;
import com.kltyton.bedrockruntime.model.resource.EnhancedModelData;

public final class CompiledModelPlan {
    private final String resource;
    private final long generation;
    private final EnhancedModelData.Mode mode;
    private final ModelGeometry logicalGeometry;
    private final ModelGeometry optimizedGeometry;
    private final OptimizationPlan optimizationPlan;
    private final List<CompiledGroup> groups;
    private final Map<String, MeshCaptureData> captureData;
    private final Map<String, TriangleMesh> originalMeshes;
    private final boolean doubleSided;
    private final Map<String, List<CompiledGroup>> groupsByAnchor;
    private final Map<String, List<CompiledGroup>> groupsBySource;

    public CompiledModelPlan(String resource, long generation, EnhancedModelData.Mode mode,
                             ModelGeometry logicalGeometry, ModelGeometry optimizedGeometry,
                             OptimizationPlan optimizationPlan, List<CompiledGroup> groups,
                             Map<String, MeshCaptureData> captureData, Map<String, TriangleMesh> originalMeshes,
                             boolean doubleSided) {
        this.resource = resource == null ? "" : resource;
        this.generation = generation;
        this.mode = mode == null ? EnhancedModelData.Mode.TREE : mode;
        this.logicalGeometry = Objects.requireNonNull(logicalGeometry, "logicalGeometry");
        this.optimizedGeometry = Objects.requireNonNull(optimizedGeometry, "optimizedGeometry");
        this.optimizationPlan = Objects.requireNonNull(optimizationPlan, "optimizationPlan");
        this.groups = List.copyOf(Objects.requireNonNull(groups, "groups"));
        this.captureData = Map.copyOf(Objects.requireNonNull(captureData, "captureData"));
        this.originalMeshes = Map.copyOf(Objects.requireNonNull(originalMeshes, "originalMeshes"));
        this.doubleSided = doubleSided;
        Map<String, List<CompiledGroup>> byAnchor = new LinkedHashMap<>();
        Map<String, List<CompiledGroup>> bySource = new LinkedHashMap<>();
        for (CompiledGroup group : this.groups) {
            byAnchor.computeIfAbsent(group.anchorBone(), ignored -> new ArrayList<>()).add(group);
            for (String source : group.sourceBones()) {
                bySource.computeIfAbsent(source, ignored -> new ArrayList<>()).add(group);
            }
        }
        Map<String, List<CompiledGroup>> immutableByAnchor = new LinkedHashMap<>();
        byAnchor.forEach((key, value) -> immutableByAnchor.put(key, List.copyOf(value)));
        Map<String, List<CompiledGroup>> immutableBySource = new LinkedHashMap<>();
        bySource.forEach((key, value) -> immutableBySource.put(key, List.copyOf(value)));
        this.groupsByAnchor = Collections.unmodifiableMap(immutableByAnchor);
        this.groupsBySource = Collections.unmodifiableMap(immutableBySource);
    }

    public static CompiledModelPlan retained(String resource, long generation, EnhancedModelData.Mode mode,
                                             ModelGeometry geometry, String reason) {
        OptimizationPlan plan = OptimizationPlan.retained(geometry, reason, List.of());
        return new CompiledModelPlan(resource, generation, mode, geometry, geometry, plan, List.of(),
                Map.of(), Map.of(), false);
    }

    public String resource() {
        return resource;
    }

    public long generation() {
        return generation;
    }

    public EnhancedModelData.Mode mode() {
        return mode;
    }

    public ModelGeometry logicalGeometry() {
        return logicalGeometry;
    }

    public ModelGeometry optimizedGeometry() {
        return optimizedGeometry;
    }

    public OptimizationPlan optimizationPlan() {
        return optimizationPlan;
    }

    public List<CompiledGroup> groups() {
        return groups;
    }

    public boolean compiled() {
        return !groups.isEmpty();
    }

    public Map<String, MeshCaptureData> captureData() {
        return captureData;
    }

    public MeshCaptureData captureData(String boneName) {
        return captureData.get(boneName);
    }

    public Map<String, TriangleMesh> originalMeshes() {
        return originalMeshes;
    }

    public TriangleMesh originalMesh(String boneName) {
        return originalMeshes.get(boneName);
    }

    public boolean doubleSided() {
        return doubleSided;
    }

    public List<CompiledGroup> groupsAtAnchor(String anchorBone) {
        List<CompiledGroup> result = groupsByAnchor.get(anchorBone);
        return result == null ? List.of() : result;
    }

    public CompiledGroup groupForSource(String boneName) {
        List<CompiledGroup> groups = groupsForSource(boneName);
        return groups.isEmpty() ? null : groups.get(0);
    }

    public List<CompiledGroup> groupsForSource(String boneName) {
        List<CompiledGroup> result = groupsBySource.get(boneName);
        return result == null ? List.of() : result;
    }

    public Set<String> sourceBones() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(groupsBySource.keySet()));
    }

    public Set<String> anchorBones() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(groupsByAnchor.keySet()));
    }
}


