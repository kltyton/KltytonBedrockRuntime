package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;

import java.util.List;

/** Accesses an array using Molang's clamped and wrapped index rules. */
public record ArrayAccess(MathValue array, MathValue index) implements MathValue {
    @Override
    public Object getRaw(MolangInstance instance) {
        Object value = array.getRaw(instance);
        float requested = (float) index.get(instance);
        long selected = requested > 0.0F
                ? Integer.toUnsignedLong(requested >= 0x1.0p31F ? Integer.MIN_VALUE : (int) requested) : 0L;
        if (!(value instanceof List<?> entries)) return null;
        if (entries.isEmpty()) return null;
        return MathValue.normalizeRaw(entries.get((int) (selected % entries.size())));
    }

    @Override
    public double get(MolangInstance instance) {
        Object value = getRaw(instance);
        return value instanceof Number number ? MathValue.normalize(number.doubleValue()) : 0.0;
    }

    @Override public void markImmutable() {
    }
}
