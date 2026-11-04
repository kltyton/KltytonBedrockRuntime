package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

/** A Molang structure whose fields can contain values or further structures. */
public final class MolangStruct {
    /** A stored value whose numeric representation follows a replaceable resource. */
    public interface DynamicNumber {
        float floatValue();
    }

    private final Map<String, Object> fields = new LinkedHashMap<>();

    public static MolangStruct fromMap(Map<?, ?> values) {
        MolangStruct structure = new MolangStruct();
        values.forEach((key, value) -> {
            if (!(key instanceof String field)) throw new IllegalArgumentException("Molang structure field must be a string");
            String normalized = field.toLowerCase(Locale.ROOT);
            if (structure.contains(normalized)) throw new IllegalArgumentException("Duplicate Molang structure field: " + field);
            Object converted = value instanceof Map<?, ?> nestedMap ? fromMap(nestedMap)
                    : value instanceof MolangStruct nestedStructure ? nestedStructure.copy()
                    : value instanceof Boolean bool ? (bool ? 1.0 : 0.0) : value;
            structure.fields.put(normalized, converted);
        });
        return structure;
    }

    public @Nullable Object get(String field) {
        Object value = fields.get(field.toLowerCase(Locale.ROOT));
        return value instanceof DynamicNumber number ? number.floatValue() : value;
    }

    public boolean contains(String field) {
        return fields.containsKey(field.toLowerCase(Locale.ROOT));
    }

    public Set<String> fieldNames() {
        return Set.copyOf(fields.keySet());
    }

    public void put(String field, @Nullable Object value) {
        fields.put(field.toLowerCase(Locale.ROOT), value instanceof MolangStruct structure ? structure.copy()
                : value instanceof Boolean bool ? (bool ? 1.0 : 0.0) : value);
    }

    public MolangStruct child(String field) {
        field = field.toLowerCase(Locale.ROOT);
        Object existing = fields.get(field);
        if (existing instanceof MolangStruct structure) return structure;
        MolangStruct structure = existing instanceof Map<?, ?> map ? fromMap(map) : new MolangStruct();
        fields.put(field, structure);
        return structure;
    }

    public void clear() {
        fields.clear();
    }

    public void merge(MolangStruct source) {
        source.fields.forEach((field, value) -> {
            Object existing = fields.get(field);
            if (existing instanceof MolangStruct nested && value instanceof MolangStruct incoming) nested.merge(incoming);
            else put(field, value);
        });
    }

    public void putPath(String path, @Nullable Object value) {
        String[] parts = path.split("\\.");
        MolangStruct target = this;
        for (int i = 0; i < parts.length - 1; i++) target = target.child(parts[i]);
        target.put(parts[parts.length - 1], value);
    }

    public MolangStruct copy() {
        MolangStruct copy = new MolangStruct();
        fields.forEach((field, value) -> copy.fields.put(field, copyValue(value)));
        return copy;
    }

    private static @Nullable Object copyValue(@Nullable Object value) {
        if (value instanceof MolangStruct structure) return structure.copy();
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> copy = new LinkedHashMap<>();
            map.forEach((key, item) -> copy.put(key, copyValue(item)));
            return copy;
        }
        if (value instanceof java.util.List<?> list) {
            java.util.ArrayList<Object> copy = new java.util.ArrayList<>(list.size());
            list.forEach(item -> copy.add(copyValue(item)));
            return copy;
        }
        return value;
    }

    MolangStruct comparisonSnapshot() {
        MolangStruct copy = new MolangStruct();
        fields.forEach((field, value) -> copy.fields.put(field, MolangValueEquality.snapshot(value)));
        return copy;
    }

    boolean sameMembers(MolangStruct other) {
        if (fields.size() != other.fields.size()) return false;
        var left = fields.entrySet().iterator();
        var right = other.fields.entrySet().iterator();
        while (left.hasNext()) {
            var a = left.next();
            var b = right.next();
            if (!MolangValueEquality.sameString(a.getKey(), b.getKey())
                    || !MolangValueEquality.same(a.getValue(), b.getValue())) return false;
        }
        return true;
    }
}
