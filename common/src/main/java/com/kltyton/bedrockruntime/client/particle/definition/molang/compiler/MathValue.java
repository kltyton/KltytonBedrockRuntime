package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.Group;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.Ternary;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.Variable;

import java.util.function.ToDoubleFunction;

/**
 * Base interface for all computational values in the math system
 * <p>
 * All mathematical objects are an extension of this interface, allowing for an indefinitely-nestable
 * mathematical system that can be accessed via this one access point
 */
public interface MathValue extends ToDoubleFunction<MolangInstance> {
    /**
     * Get computed or stored value
     */
    double get(MolangInstance instance);

    /** Molang numeric operations use single-precision values. */
    static double normalize(double value) {
        return (float) value;
    }

    /**
     * Get the computed or stored value without converting it to a number.
     * <p>
     * Numeric values are returned as {@link Double} and Molang strings as {@link String}.
     */
    default Object getRaw(MolangInstance instance) {
        if (this instanceof Group group) {
            return resolveRaw(group.contents(), instance);
        }
        if (this instanceof Ternary ternary) {
            return ternary.condition().get(instance) != 0
                    ? resolveRaw(ternary.trueValue(), instance)
                    : resolveRaw(ternary.falseValue(), instance);
        }
        return get(instance);
    }

    static Object resolveRaw(MathValue value, MolangInstance instance) {
        while (value instanceof Group group) value = group.contents();
        Object raw = value instanceof Variable variable ? variable.getRawWithStructure(instance) : value.getRaw(instance);
        return normalizeRaw(raw);
    }

    static Object normalizeRaw(Object value) {
        if (value instanceof Boolean bool) return bool ? 1.0 : 0.0;
        return value instanceof Number number ? normalize(number.doubleValue()) : value;
    }

    /**
     * Get the computed or stored string value, or {@code null} if this value is not a string.
     */
    default String getString(MolangInstance instance) {
        Object raw = getRaw(instance);
        return raw instanceof String string ? string : null;
    }

    void markImmutable();

    default void set(ToDoubleFunction<MolangInstance> function) {}

    /**
     * Return whether this type of MathValue should be considered mutable; its value could change.
     * <br>
     * This is used to cache calculated values, optimising computational work
     */
    default boolean isMutable() {
        return true;
    }

    /**
     * Use {@link #get}
     */
    @Deprecated()
    @Override
    default double applyAsDouble(MolangInstance instance) {
        return get(instance);
    }
}
