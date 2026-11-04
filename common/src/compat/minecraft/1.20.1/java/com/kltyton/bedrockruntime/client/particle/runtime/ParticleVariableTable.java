package com.kltyton.bedrockruntime.client.particle.runtime;

import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.VariableTable;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.Variable;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.UndefinedValueScope;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.MolangStruct;

public class ParticleVariableTable extends VariableTable {
    private final VariableTable emitter;

    public ParticleVariableTable(VariableTable preset, VariableTable emitter) {
        super(emitter.table, preset);
        this.emitter = emitter;
    }

    @Override
    public boolean containsVariable(String name) {
        return super.containsVariable(name) || emitter.containsVariable(name);
    }

    @Override
    public @Nullable Variable getVariable(String name) {
        Variable variable = super.getVariable(name);
        return variable != null ? variable : emitter.getVariable(name);
    }

    @Override
    public double getValue(String name, MolangInstance instance) {
        Object localField = getLocalStructureField(name, instance);
        if (localField != NO_LOCAL_FIELD) {
            if (localField == null && !containsVariable(name)) return UndefinedValueScope.missingNumber();
            return localField instanceof Number number ? number.doubleValue() : 0.0;
        }
        Variable variable = table.get(name);
        if (variable == null) {
            Object presetField = parent.getLocalStructureField(name, instance);
            if (presetField != NO_LOCAL_FIELD) {
                if (presetField == null && !parent.containsVariable(name)) return UndefinedValueScope.missingNumber();
                return presetField instanceof Number number ? number.doubleValue() : 0.0;
            }
            variable = parent.table.get(name);
            if (variable == null) {
                return emitter.getValue(name, instance);
            }
        }
        if (!beginResolve(name)) return emitter.getLocalValue(name, instance);
        try {
            return variable.get(instance);
        } finally {
            endResolve(name);
        }
    }

    @Override
    public @Nullable Object getRawValue(String name, MolangInstance instance) {
        Object localField = getLocalStructureField(name, instance);
        if (localField != NO_LOCAL_FIELD) {
            if (localField == null && !containsVariable(name)) return UndefinedValueScope.missing();
            return localField;
        }
        Variable variable = table.get(name);
        if (variable == null) {
            Object presetField = parent.getLocalStructureField(name, instance);
            if (presetField != NO_LOCAL_FIELD) {
                if (presetField == null && !parent.containsVariable(name)) return UndefinedValueScope.missing();
                return presetField;
            }
            variable = parent.table.get(name);
            if (variable == null) {
                return emitter.getRawValue(name, instance);
            }
        }
        if (!beginResolve(name)) return emitter.getLocalRawValue(name, instance);
        try {
            return variable.getRaw(instance);
        } finally {
            endResolve(name);
        }
    }

    @Override
    public boolean mergeStructureFields(String name, MolangInstance instance, MolangStruct result, boolean present) {
        present = emitter.mergeStructureFields(name, instance, result, present);
        present = parent.mergeLocalStructureFields(name, instance, result, present);
        return mergeLocalStructureFields(name, instance, result, present);
    }
}
