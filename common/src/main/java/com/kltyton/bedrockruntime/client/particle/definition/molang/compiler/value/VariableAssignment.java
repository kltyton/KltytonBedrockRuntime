package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.particle.BedrockParticles;

/**
 * {@link MathValue} value supplier
 *
 * <p>
 * <b>Contract:</b>
 * <br>
 * Assigns a variable to the given value and returns the assigned value.
 */
public record VariableAssignment(String name, MathValue value) implements MathValue {
    private static final java.util.Set<String> REPORTED_NONFINITE = java.util.concurrent.ConcurrentHashMap.newKeySet();
    @Override
    public double get(MolangInstance instance) {
        return toDouble(evaluate(instance));
    }

    @Override
    public Object getRaw(MolangInstance instance) {
        return evaluate(instance);
    }

    private Object evaluate(MolangInstance instance) {
        Object calculated = evaluateValue(value, instance);
        Object stored = calculated instanceof java.util.Map<?, ?> fields ? MolangStruct.fromMap(fields) : calculated;
        if (EvaluationScope.isTemporary(name)) {
            EvaluationScope.setTemporaryValue(name, stored);
            return calculated;
        }
        instance.getVars().setRawValue(name, storedValue(name, stored, instance), instance);
        return calculated;
    }

    static Object storedValue(String name, Object value, MolangInstance instance) {
        if (name.startsWith("variable.") && name.indexOf('.', "variable.".length()) < 0
                && value instanceof Number number && !Float.isFinite(number.floatValue())) {
            String key = instance.getIdentity() + ":" + name;
            if (REPORTED_NONFINITE.add(key)) {
                BedrockParticles.LOGGER.warn("Non-finite Molang variable {} in effect {}", name, instance.getIdentity());
            }
            return 0.0;
        }
        return value;
    }

    static Object evaluateValue(MathValue source, MolangInstance instance) {
        Object calculated = MathValue.resolveRaw(source, instance);
        if (calculated instanceof Number number) calculated = (double) number.floatValue();
        return calculated;
    }

    private static double toDouble(Object value) {
        return value instanceof Number number ? number.floatValue() : 0.0;
    }

    @Override
    public void markImmutable() {
        value.markImmutable();
    }

    @Override
    public String toString() {
        return name + "=" + value.toString();
    }
}
