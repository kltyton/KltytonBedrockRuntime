package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.VariableTable;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;

import java.util.function.ToDoubleFunction;

/**
 * {@link MathValue} value supplier
 *
 * <p>
 * <b>Contract:</b>
 * <br>
 * Returns the currently stored value, which may be modified at any given time via {@link #set}. Values may be lazily evaluated to eliminate wasteful usage
 */
public final class Variable implements MathValue {
    private final String name;
    private ToDoubleFunction<MolangInstance> value;
    private Double constant;
    private String stringValue;
    private Object referenceValue;
    private boolean referenceSet;
    private boolean immutable;

    public Variable(String name, ToDoubleFunction<MolangInstance> value) {
        this.name = name;
        this.value = value;
    }

    public Variable(String name, double value) {
        this.name = name;
        this.constant = MathValue.normalize(value);
    }

    public Variable(String name, String value) {
        this.name = name;
        this.stringValue = value;
    }

    @Override
    public double get(MolangInstance instance) {
        if (referenceSet) return 0.0;
        double result;
        if (constant != null) {
            result = constant;
        } else if (stringValue != null) {
            result = 0.0;
        } else {
            result = value == null ? 0.0 : value.applyAsDouble(instance);
        }
        result = MathValue.normalize(result);
        if (immutable && stringValue == null) instance.getVars().setValue(name, result);
        return result;
    }

    @Override
    public Object getRaw(MolangInstance instance) {
        if (referenceSet) return referenceValue;
        if (stringValue != null) {
            return stringValue;
        }
        if (constant != null) {
            return MathValue.normalize(constant);
        }
        if (EvaluationScope.isTemporary(name)) {
            Object temporary = EvaluationScope.getTemporaryRaw(name);
            return temporary == null && !EvaluationScope.containsTemporary(name) ? 0.0 : temporary;
        }
        if (value == null) return 0.0;
        VariableTable vars = instance.getVars();
        if (vars != null && !name.startsWith("query.")) {
            Variable stored = vars.getVariable(name);
            if (stored == null) {
                Object raw = vars.getRawValue(name, instance);
                return raw == null ? 0.0 : raw;
            }
            if (stored != null && stored != this) {
                return vars.getRawValue(name, instance);
            }
        }
        return get(instance);
    }

    public Object getRawWithStructure(MolangInstance instance) {
        VariableTable vars = instance.getVars();
        Variable stored = vars.getVariable(name);
        if (name.startsWith("variable.") && stored == null && !vars.containsVariable(name)) {
            MolangStruct structure = vars.getStructureSnapshot(name, instance);
            if (structure != null) return structure;
        }
        Object raw = getRaw(instance);
        if (raw instanceof MolangStruct && !referenceSet && stringValue == null && constant == null
                && value != null && stored != this
                && (vars.hasLocalStructureOverlays(name) || vars.hasSharedStructureField(name))) return raw;
        if (name.startsWith("variable.") && (raw instanceof MolangStruct || raw instanceof java.util.Map<?, ?>
                || !vars.containsVariable(name))) {
            MolangStruct structure = vars.getStructureSnapshot(name, instance);
            if (structure != null) return structure;
        }
        return raw;
    }

    public void set(Double value) {
        this.constant = MathValue.normalize(java.util.Objects.requireNonNull(value));
        this.value = null;
        this.stringValue = null;
        this.referenceValue = null;
        this.referenceSet = false;
    }

    public void set(String value) {
        this.stringValue = java.util.Objects.requireNonNull(value);
        this.value = null;
        this.constant = null;
        this.referenceValue = null;
        this.referenceSet = false;
    }

    public void setRaw(Object value) {
        if (value instanceof Boolean bool) {
            set(bool ? 1.0 : 0.0);
            return;
        }
        this.referenceValue = value;
        this.referenceSet = true;
        this.stringValue = null;
        this.constant = null;
        this.value = null;
    }

    public MolangStruct structure() {
        return referenceSet && referenceValue instanceof MolangStruct structure ? structure : null;
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public Object rawReference() {
        return referenceSet ? referenceValue : null;
    }

    public boolean containsStructureField(String path) {
        if (!referenceSet) return false;
        Object value = referenceValue;
        for (String field : path.split("\\.")) {
            if (value instanceof java.util.Map<?, ?> values) value = MolangStruct.fromMap(values);
            if (!(value instanceof MolangStruct structure) || !structure.contains(field)) return false;
            value = structure.get(field);
        }
        return true;
    }

    @Override
    public boolean isMutable() {
        return !immutable;
    }

    @Override
    public void markImmutable() {
        if (!EvaluationScope.isTemporary(name)) this.immutable = true;
    }

    @Override
    public void set(ToDoubleFunction<MolangInstance> value) {
        this.value = java.util.Objects.requireNonNull(value);
        this.constant = null;
        this.stringValue = null;
        this.referenceValue = null;
        this.referenceSet = false;
    }

    public void set(Variable value) {
        this.constant = value.constant;
        this.value = value.value;
        this.stringValue = value.stringValue;
        this.referenceValue = value.referenceValue instanceof MolangStruct structure
                ? structure.copy() : value.referenceValue;
        this.referenceSet = value.referenceSet;
    }

    public String name() {
        return name;
    }

    public ToDoubleFunction<MolangInstance> value() {
        return value;
    }

    public Variable copy() {
        Variable variable;
        if (referenceSet) {
            variable = new Variable(name, 0.0);
            variable.setRaw(referenceValue instanceof MolangStruct structure ? structure.copy() : referenceValue);
        } else if (stringValue != null) {
            variable = new Variable(name, stringValue);
        } else {
            variable = constant == null ? new Variable(name, value) : new Variable(name, constant);
        }
        if (immutable) {
            variable.markImmutable();
        }
        return variable;
    }
}
