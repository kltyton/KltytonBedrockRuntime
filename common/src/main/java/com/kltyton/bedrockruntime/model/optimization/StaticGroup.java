package com.kltyton.bedrockruntime.model.optimization;

import java.util.List;
import java.util.Objects;

public record StaticGroup(String name, List<String> boneNames, String anchorBone) {
    public StaticGroup {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("static group name must not be blank");
        }
        Objects.requireNonNull(boneNames, "boneNames");
        boneNames = List.copyOf(boneNames);
        if (boneNames.size() < 2) {
            throw new IllegalArgumentException("static group must contain at least two bones");
        }
        if (boneNames.stream().anyMatch(bone -> bone == null || bone.isBlank())) {
            throw new IllegalArgumentException("static group bone names must not be blank");
        }
        if (new java.util.LinkedHashSet<>(boneNames).size() != boneNames.size()) {
            throw new IllegalArgumentException("static group contains duplicate bone names");
        }
        if (anchorBone != null && anchorBone.isBlank()) {
            throw new IllegalArgumentException("anchor bone must be null or non-blank");
        }
    }

    public StaticGroup(String name, List<String> boneNames) {
        this(name, boneNames, null);
    }
}
