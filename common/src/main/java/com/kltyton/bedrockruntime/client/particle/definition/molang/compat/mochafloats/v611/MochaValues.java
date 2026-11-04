package com.kltyton.bedrockruntime.client.particle.definition.molang.compat.mochafloats.v611;

import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.MolangStruct;
import com.kltyton.bedrockruntime.internal.mochafloats.runtime.value.*;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Converts language values without converting actor and item references to numbers. */
final class MochaValues {
    private MochaValues() {}

    static int arrayIndex(float requested, int size) {
        if (size == 0) return -1;
        long selected = requested > 0
                ? Integer.toUnsignedLong(requested >= 0x1.0p31F ? Integer.MIN_VALUE : (int) requested) : 0L;
        return (int) (selected % size);
    }

    static Value encode(Object raw) {
        if (raw instanceof Value value) return value;
        if (raw instanceof Boolean bool) return Value.of(bool);
        if (raw instanceof Number number) return NumberValue.of(number.floatValue());
        if (raw instanceof String string) return StringValue.of(string);
        if (raw instanceof MolangStruct.DynamicNumber number) return NumberValue.of(number.floatValue());
        if (raw instanceof MolangStruct structure) return new Structure(structure);
        if (raw instanceof Map<?, ?> map) return new Structure(MolangStruct.fromMap(map));
        if (raw instanceof Iterable<?> iterable) {
            List<Value> values = new ArrayList<>();
            iterable.forEach(item -> values.add(encode(item)));
            return ArrayValue.of(values.toArray(Value[]::new));
        }
        if (raw != null && raw.getClass().isArray()) {
            Value[] values = new Value[Array.getLength(raw)];
            for (int index = 0; index < values.length; index++) values[index] = encode(Array.get(raw, index));
            return ArrayValue.of(values);
        }
        return new JavaValue(raw);
    }

    static Object decode(Value value) {
        if (value instanceof NumberValue number) return (double) number.value();
        if (value instanceof StringValue string) return string.value();
        if (value instanceof JavaValue reference) return reference.value();
        if (value instanceof Structure structure) return structure.value.copy();
        if (value instanceof MochaScope.Path path) return path.raw();
        if (value instanceof ArrayValue array) {
            List<Object> values = new ArrayList<>(array.values().length);
            for (Value entry : array.values()) values.add(decode(entry));
            return values;
        }
        if (value instanceof ObjectValue object) {
            Map<String, Object> values = new LinkedHashMap<>();
            object.entries().forEach((name, property) -> values.put(name, decode(property.value())));
            return MolangStruct.fromMap(values);
        }
        return 0.0;
    }

    private record Structure(MolangStruct value) implements ObjectValue {
        @Override
        public ObjectProperty getProperty(String name) {
            return value.contains(name) ? ObjectProperty.property(encode(value.get(name)), false) : null;
        }

        @Override
        public boolean set(String name, Value entry) {
            value.put(name, decode(entry));
            return true;
        }

        @Override
        public Map<String, ObjectProperty> entries() {
            Map<String, ObjectProperty> result = new LinkedHashMap<>();
            for (String field : value.fieldNames()) result.put(field, getProperty(field));
            return result;
        }
    }
}
