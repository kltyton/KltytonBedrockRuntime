package com.kltyton.bedrockruntime.model.optimization;

import java.util.List;
import java.util.Objects;

import com.kltyton.bedrockruntime.model.definition.ModelGeometry;

public final class OptimizationPlan {
    private final boolean optimized;
    private final String reason;
    private final List<MergedGroup> mergedGroups;
    private final List<SkippedGroup> skippedGroups;
    private final List<String> retainedBones;

    public OptimizationPlan(boolean optimized, String reason, List<MergedGroup> mergedGroups,
                            List<SkippedGroup> skippedGroups, List<String> retainedBones) {
        this.optimized = optimized;
        this.reason = reason == null ? "" : reason;
        this.mergedGroups = List.copyOf(Objects.requireNonNull(mergedGroups, "mergedGroups"));
        this.skippedGroups = List.copyOf(Objects.requireNonNull(skippedGroups, "skippedGroups"));
        this.retainedBones = List.copyOf(Objects.requireNonNull(retainedBones, "retainedBones"));
    }

    public static OptimizationPlan retained(ModelGeometry geometry, String reason, List<SkippedGroup> skipped) {
        return new OptimizationPlan(false, reason, List.of(), skipped, geometry.boneNames());
    }

    public boolean optimized() {
        return optimized;
    }

    public String reason() {
        return reason;
    }

    public List<MergedGroup> mergedGroups() {
        return mergedGroups;
    }

    public List<SkippedGroup> skippedGroups() {
        return skippedGroups;
    }

    public List<String> retainedBones() {
        return retainedBones;
    }

    public record MergedGroup(String groupName, String anchorBone, String material,
                              int sourcePartCount, int triangleCount) {
    }

    public record SkippedGroup(String groupName, String reason) {
    }
}
