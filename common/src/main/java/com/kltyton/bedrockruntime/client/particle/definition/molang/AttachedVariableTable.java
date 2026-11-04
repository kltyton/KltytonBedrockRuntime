package com.kltyton.bedrockruntime.client.particle.definition.molang;

import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.Variable;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.MolangStruct;

/** Resolves attached-object variables before this emitter's preset without changing the attached object. */
public final class AttachedVariableTable extends VariableTable {
    private final VariableTable attached;

    public AttachedVariableTable(VariableTable attached, VariableTable preset) {
        super(attached.table, preset);
        this.attached = attached;
    }

    @Override
    public double getValue(String name, MolangInstance instance) {
        return usesAttachedRoot(name) ? attached.getValue(name, instance) : parent.getValue(name, instance);
    }

    @Override
    public @Nullable Object getRawValue(String name, MolangInstance instance) {
        return usesAttachedRoot(name) ? attached.getRawValue(name, instance) : parent.getRawValue(name, instance);
    }

    private boolean usesAttachedRoot(String name) {
        if (attached.containsVariable(name)) return true;
        int rootEnd = name.indexOf('.', name.indexOf('.') + 1);
        return name.startsWith("variable.") && rootEnd >= 0
                && attached.containsVariable(name.substring(0, rootEnd));
    }

    @Override
    public boolean mergeStructureFields(String name, MolangInstance instance, MolangStruct result, boolean present) {
        present = parent.mergeStructureFields(name, instance, result, present);
        return attached.mergeStructureFields(name, instance, result, present);
    }

    @Override
    public boolean hasStructureFields(String name) {
        int rootEnd = name.startsWith("variable.") ? name.indexOf('.', "variable.".length()) : -1;
        String root = rootEnd < 0 ? name : name.substring(0, rootEnd);
        if (attached.containsVariable(root)) return attached.hasStructureFields(name);
        return parent.hasStructureFields(name) || attached.hasStructureFields(name);
    }

    @Override
    public @Nullable Variable getVariable(String name) {
        return usesAttachedRoot(name) ? attached.getVariable(name) : parent.getVariable(name);
    }

    @Override
    public boolean containsVariable(String name) {
        return usesAttachedRoot(name) ? attached.containsVariable(name) : parent.containsVariable(name);
    }
}
