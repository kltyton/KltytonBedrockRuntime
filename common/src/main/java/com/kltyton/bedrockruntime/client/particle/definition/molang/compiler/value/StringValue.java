package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;

public record StringValue(String value) implements MathValue {
    @Override
    public double get(MolangInstance instance) {
        return 0.0;
    }

    @Override
    public Object getRaw(MolangInstance instance) {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public void markImmutable() {
    }

    @Override
    public String toString() {
        return "'" + value + "'";
    }
}
