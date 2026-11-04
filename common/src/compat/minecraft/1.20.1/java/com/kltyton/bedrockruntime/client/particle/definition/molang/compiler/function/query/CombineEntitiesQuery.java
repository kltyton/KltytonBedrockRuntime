package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.world.entity.Entity;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Combines valid actor references without duplicates. */
public final class CombineEntitiesQuery extends MathFunction {
    private final MathValue[] arguments;

    public CombineEntitiesQuery(MathValue... arguments) {
        super(arguments);
        this.arguments = arguments.clone();
    }

    @Override public Object getRaw(MolangInstance instance) {
        Set<Entity> entities = new LinkedHashSet<>();
        for (MathValue argument : arguments) {
            Object value = argument.getRaw(instance);
            if (value instanceof List<?> list) {
                for (Object element : list) add(entities, element);
            } else add(entities, value);
        }
        return List.copyOf(entities);
    }

    private static void add(Set<Entity> entities, Object value) {
        if (value instanceof Entity entity && !entity.isRemoved()) entities.add(entity);
    }

    @Override public double compute(MolangInstance instance) {
        getRaw(instance);
        return 0.0;
    }
    @Override public boolean isMutable(MathValue... values) { return true; }
    @Override public String getName() { return "query.combine_entities"; }
    @Override public int getMinArgs() { return 0; }
    @Override public MathValue[] getArgs() { return arguments.clone(); }
}
