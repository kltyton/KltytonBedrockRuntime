package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value;

import net.minecraft.world.entity.Entity;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangQueries;

/** Assigns through an actor reference when its integration exposes a writer. */
public record ActorVariableAssignment(MathValue actor, String name, MathValue value) implements MathValue {
    @Override
    public Object getRaw(MolangInstance instance) {
        Object target = actor.getRaw(instance);
        if (!(target instanceof Entity entity)) return 0.0;
        Object assigned = VariableAssignment.evaluateValue(value, instance);
        if (assigned instanceof MolangStruct structure) assigned = structure.copy();
        return MolangQueries.writePublicActorVariable(entity, name,
                VariableAssignment.storedValue(name, assigned, instance)) ? assigned : 0.0;
    }

    @Override
    public double get(MolangInstance instance) {
        Object assigned = getRaw(instance);
        return assigned instanceof Number number ? number.floatValue() : 0.0;
    }

    @Override
    public void markImmutable() {
        actor.markImmutable();
        value.markImmutable();
    }
}
