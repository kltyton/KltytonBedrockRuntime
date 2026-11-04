package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;

import java.util.StringJoiner;

/**
 * {@link MathValue} value supplier
 *
 * <p>
 * <b>Contract:</b>
 * <br>
 * Contains a collection of sub-expressions that evaluate before returning the last expression, or 0 if no return is defined.
 * Sub-expressions have no bearing on the final return with exception for where they may be setting variable values
 */
public record CompoundValue(MathValue[] subValues) implements MathValue {
    @Override
    public double get(MolangInstance instance) {
        for (int i = 0; i < this.subValues.length - 1; i++) {
            this.subValues[i].get(instance);
        }

        return this.subValues[this.subValues.length - 1].get(instance);
    }

    @Override
    public Object getRaw(MolangInstance instance) {
        for (int i = 0; i < this.subValues.length - 1; i++) {
            this.subValues[i].get(instance);
        }
        return MathValue.resolveRaw(this.subValues[this.subValues.length - 1], instance);
    }

    @Override
    public void markImmutable() {
        for (MathValue subValue : subValues) {
            subValue.markImmutable();
        }
    }

    @Override
    public String toString() {
        final StringJoiner joiner = new StringJoiner("; ");

        for (MathValue subValue : this.subValues) {
            joiner.add(subValue.toString());
        }

        return joiner.toString();
    }
}
