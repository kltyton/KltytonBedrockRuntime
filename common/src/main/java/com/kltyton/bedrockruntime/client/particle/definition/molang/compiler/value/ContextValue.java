package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangQueries;

import java.util.Map;
import java.util.Locale;

/** Reads a context value supplied by the current expression owner. */
public record ContextValue(String name) implements MathValue {
    private static final Object MISSING = new Object();

    public ContextValue {
        name = MolangQueries.applyPrefixAliases(name.toLowerCase(Locale.ROOT), "context.", "c.");
    }

    public Object resolve(MolangInstance instance) {
        int separator = name.indexOf('.', "context.".length());
        if (separator >= 0) {
            String rootName = name.substring(0, separator);
            Object value = contextValue(instance, rootName);
            if (value != MISSING) {
                for (String field : name.substring(separator + 1).split("\\.")) {
                    if (value instanceof MolangStruct structure) value = structure.get(field);
                    else if (value instanceof Map<?, ?> map) value = findField(map, field);
                    else return null;
                }
                return value;
            }
        }
        Object value = contextValue(instance, name);
        if (value != MISSING) return value;
        String variable = "variable." + name.substring("context.".length());
        if (instance.getVars().containsVariable(variable)) return instance.getVars().getRawValue(variable, instance);
        return instance.getVars().getStructureSnapshot(variable, instance);
    }

    public Object resolveProvided(MolangInstance instance) {
        Object value = contextValue(instance, name);
        return value == MISSING ? null : value;
    }

    private static Object contextValue(MolangInstance instance, String name) {
        Map<String, ?> context = instance.getMolangContext();
        if (context.containsKey(name)) return context.get(name);
        Object exact = findContext(context, name);
        if (exact != MISSING) return exact;
        return MolangQueries.hasContext(name, instance) ? MolangQueries.getContext(name, instance) : MISSING;
    }

    private static Object findContext(Map<String, ?> context, String name) {
        for (Map.Entry<String, ?> entry : context.entrySet()) {
            String key = entry.getKey();
            if (key == null) continue;
            int prefixLength = key.regionMatches(true, 0, "context.", 0, 8) ? 8
                    : key.regionMatches(true, 0, "c.", 0, 2) ? 2 : -1;
            if (prefixLength >= 0 && key.length() - prefixLength == name.length() - 8
                    && key.regionMatches(true, prefixLength, name, 8, name.length() - 8)) {
                return entry.getValue();
            }
        }
        return MISSING;
    }

    private static Object findField(Map<?, ?> fields, String name) {
        if (fields.containsKey(name)) return fields.get(name);
        for (Map.Entry<?, ?> entry : fields.entrySet()) {
            if (entry.getKey() instanceof String key && key.equalsIgnoreCase(name)) return entry.getValue();
        }
        return null;
    }

    @Override
    public Object getRaw(MolangInstance instance) {
        Object value = resolve(instance);
        return value == null ? UndefinedValueScope.missingNumber() : MathValue.normalizeRaw(value);
    }

    @Override
    public double get(MolangInstance instance) {
        Object value = getRaw(instance);
        return value instanceof Number number ? MathValue.normalize(number.doubleValue()) : 0.0;
    }

    @Override
    public void markImmutable() {
    }
}
