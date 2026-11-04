package com.kltyton.bedrockruntime.model.optimization;

import java.util.List;
import java.util.Objects;

public record OptimizationOptions(boolean enabled, List<StaticGroup> staticGroups) {
    public OptimizationOptions {
        Objects.requireNonNull(staticGroups, "staticGroups");
        staticGroups = List.copyOf(staticGroups);
    }

    public static OptimizationOptions defaults() {
        return new OptimizationOptions(false, List.of());
    }

    public static OptimizationOptions of(StaticGroup... groups) {
        return new OptimizationOptions(true, List.of(groups));
    }

    public static OptimizationOptions of(List<StaticGroup> groups) {
        return new OptimizationOptions(true, groups);
    }
}
