package com.kltyton.bedrockruntime.model.optimization;

import java.util.Objects;

import com.kltyton.bedrockruntime.model.definition.ModelGeometry;

public record OptimizationResult(ModelGeometry geometry, OptimizationPlan plan) {
    public OptimizationResult {
        Objects.requireNonNull(geometry, "geometry");
        Objects.requireNonNull(plan, "plan");
    }
}
