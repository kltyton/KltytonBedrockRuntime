package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;

/**
 * {@link MathValue} value supplier
 *
 * <p>
 * <b>Contract:</b>
 * <br>
 * An immutable Molang numeric value
 */
public record Constant(double value) implements MathValue {
    public Constant {
        value = MathValue.normalize(value);
    }
    @Override
    public double get(MolangInstance instance) {
        return (float) this.value;
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
        return String.valueOf(this.value);
    }
}
