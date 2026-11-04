package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;

import java.util.Map;

/** Reads fields following an evaluated structure expression. */
public record StructureMember(MathValue owner, String path) implements MathValue {
    @Override
    public Object getRaw(MolangInstance instance) {
        Object value = MathValue.resolveRaw(owner, instance);
        for (String field : path.split("\\.")) {
            if (value instanceof MolangStruct structure) {
                if (!structure.contains(field)) return UndefinedValueScope.missing();
                value = structure.get(field);
            }
            else if (value instanceof Map<?, ?> map) value = mapField(map, field);
            else return UndefinedValueScope.missing();
        }
        return MathValue.normalizeRaw(value);
    }

    private static Object mapField(Map<?, ?> map, String field) {
        if (map.containsKey(field)) return map.get(field);
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() instanceof String key && key.equalsIgnoreCase(field)) return entry.getValue();
        }
        return UndefinedValueScope.missing();
    }

    @Override
    public double get(MolangInstance instance) {
        Object value = getRaw(instance);
        return value instanceof Number number ? MathValue.normalize(number.doubleValue()) : 0.0;
    }

    @Override
    public void markImmutable() {
        owner.markImmutable();
    }
}
