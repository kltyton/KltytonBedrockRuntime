package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

public record EvaluationScope(MathValue expression) implements MathValue {
    private static final ThreadLocal<ArrayDeque<Map<String, Object>>> TEMPORARIES =
            ThreadLocal.withInitial(ArrayDeque::new);
    private record ThisValue(@org.jetbrains.annotations.Nullable MolangInstance owner, float value) {}
    private static final ThreadLocal<ArrayDeque<ThisValue>> THIS_VALUES = new ThreadLocal<>();

    public static void pushThis(float value) {
        pushThis(null, value);
    }

    public static void pushThis(@org.jetbrains.annotations.Nullable MolangInstance owner, float value) {
        ArrayDeque<ThisValue> values = THIS_VALUES.get();
        if (values == null) {
            values = new ArrayDeque<>();
            THIS_VALUES.set(values);
        }
        values.push(new ThisValue(owner, value));
    }

    public static void popThis() {
        ArrayDeque<ThisValue> values = THIS_VALUES.get();
        values.pop();
        if (values.isEmpty()) THIS_VALUES.remove();
    }

    public static double getThis(MolangInstance instance) {
        ArrayDeque<ThisValue> values = THIS_VALUES.get();
        if (values != null) {
            for (ThisValue value : values) {
                if (value.owner() == null || value.owner() == instance) return value.value();
            }
        }
        Object contextual = MathValue.normalizeRaw(instance.getMolangContext().get("this"));
        return contextual instanceof Number number ? number.floatValue() : 0.0;
    }

    public static boolean isTemporary(String name) {
        return name.startsWith("temp.");
    }

    public static double getTemporary(String name) {
        Object value = getTemporaryRaw(name);
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }

    public static Object getTemporaryRaw(String name) {
        Map<String, Object> scope = TEMPORARIES.get().peek();
        if (scope == null) return UndefinedValueScope.missing();
        String[] parts = name.substring("temp.".length()).split("\\.");
        if (!scope.containsKey(parts[0])) return UndefinedValueScope.missing();
        Object value = scope.get(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (value instanceof Map<?, ?> map) value = MolangStruct.fromMap(map);
            if (!(value instanceof MolangStruct structure) || !structure.contains(parts[i])) return UndefinedValueScope.missing();
            value = structure.get(parts[i]);
        }
        return value;
    }

    public static boolean containsTemporary(String name) {
        Map<String, Object> scope = TEMPORARIES.get().peek();
        if (scope == null) return false;
        String[] parts = name.substring("temp.".length()).split("\\.");
        if (parts.length == 1) return scope.containsKey(parts[0]);
        Object value = scope.get(parts[0]);
        for (int i = 1; i < parts.length - 1; i++) {
            if (value instanceof Map<?, ?> map) value = MolangStruct.fromMap(map);
            if (!(value instanceof MolangStruct structure)) return false;
            value = structure.get(parts[i]);
        }
        if (value instanceof Map<?, ?> map) value = MolangStruct.fromMap(map);
        return value instanceof MolangStruct structure && structure.contains(parts[parts.length - 1]);
    }

    public static void setTemporary(String name, double value) {
        setTemporaryValue(name, value);
    }

    public static void setTemporary(String name, String value) {
        setTemporaryValue(name, value);
    }

    public static void setTemporaryValue(String name, Object value) {
        value = MathValue.normalizeRaw(value);
        Map<String, Object> scope = TEMPORARIES.get().peek();
        if (scope == null) throw new IllegalStateException("Temporary variable used outside an evaluation");
        String[] parts = name.substring("temp.".length()).split("\\.");
        if (parts.length == 1) {
            scope.put(parts[0], value instanceof MolangStruct structure ? structure.copy() : value);
            return;
        }
        Object root = scope.get(parts[0]);
        MolangStruct structure;
        if (root instanceof MolangStruct existing) structure = existing;
        else {
            structure = root instanceof Map<?, ?> map ? MolangStruct.fromMap(map) : new MolangStruct();
            scope.put(parts[0], structure);
        }
        for (int i = 1; i < parts.length - 1; i++) structure = structure.child(parts[i]);
        structure.put(parts[parts.length - 1], value);
    }

    @Override
    public double get(MolangInstance instance) {
        ArrayDeque<Map<String, Object>> stack = TEMPORARIES.get();
        stack.push(new HashMap<>());
        try {
            return expression.get(instance);
        } finally {
            stack.pop();
            if (stack.isEmpty()) TEMPORARIES.remove();
        }
    }

    @Override
    public Object getRaw(MolangInstance instance) {
        ArrayDeque<Map<String, Object>> stack = TEMPORARIES.get();
        stack.push(new HashMap<>());
        try {
            return expression.getRaw(instance);
        } finally {
            stack.pop();
            if (stack.isEmpty()) TEMPORARIES.remove();
        }
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public void markImmutable() {
        expression.markImmutable();
    }
}
