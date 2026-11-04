package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;

/**
 * {@link MathValue} value supplier
 *
 * <p>
 * <b>Contract:</b>
 * <br>
 * Negated equivalent of the stored value; returning a positive number if the stored value is negative, or a negative value if the stored value is positive
 */
public record Negative(MathValue value) implements MathValue {
    @Override
    public double get(MolangInstance instance) {
        return -this.value.get(instance);
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
        if (this.value instanceof Constant)
            return "-" + this.value;

        return "-" + "(" + this.value + ")";
    }
}
