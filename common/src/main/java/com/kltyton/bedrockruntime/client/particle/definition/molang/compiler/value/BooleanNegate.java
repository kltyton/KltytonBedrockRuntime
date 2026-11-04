package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;

/**
 * {@link MathValue} value supplier
 *
 * <p>
 * <b>Contract:</b>
 * <br>
 * Returns <b>1</b> if the contained value is equal to <b>0</b>, otherwise returns <b>0</b>
 */
public record BooleanNegate(MathValue value) implements MathValue {
    @Override
    public double get(MolangInstance instance) {
        return this.value.get(instance) == 0 ? 1 : 0;
    }

    @Override
    public boolean isMutable() {
        return this.value.isMutable();
    }

    @Override
    public void markImmutable() {
        this.value.markImmutable();
    }

    @Override
    public String toString() {
        return "!" + this.value.toString();
    }
}
