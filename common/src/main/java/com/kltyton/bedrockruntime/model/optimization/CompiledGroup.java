package com.kltyton.bedrockruntime.model.optimization;

import java.util.List;
import java.util.Objects;

import com.kltyton.bedrockruntime.geometry.mesh.TriangleMesh;

public record CompiledGroup(String name, String anchorBone, String material, TriangleMesh mesh,
                            List<String> sourceBones, List<String> dependencyBones) {
    public CompiledGroup {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("compiled group name must not be blank");
        }
        if (anchorBone == null || anchorBone.isBlank()) {
            throw new IllegalArgumentException("compiled group anchor must not be blank");
        }
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(mesh, "mesh");
        sourceBones = List.copyOf(Objects.requireNonNull(sourceBones, "sourceBones"));
        dependencyBones = List.copyOf(Objects.requireNonNull(dependencyBones, "dependencyBones"));
        if (sourceBones.isEmpty()) {
            throw new IllegalArgumentException("compiled group must have source bones");
        }
    }

    public boolean containsSource(String boneName) {
        return sourceBones.contains(boneName);
    }

    public boolean dependsOn(String boneName) {
        return dependencyBones.contains(boneName);
    }

    public int triangleCount() {
        return mesh.triangleCount();
    }
}
