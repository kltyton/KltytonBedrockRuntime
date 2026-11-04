package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;

/** The current value of the expression's destination. */
public enum ThisValue implements MathValue {
    INSTANCE;

    @Override
    public double get(MolangInstance instance) {
        return EvaluationScope.getThis(instance);
    }

    @Override
    public void markImmutable() {
    }
}
