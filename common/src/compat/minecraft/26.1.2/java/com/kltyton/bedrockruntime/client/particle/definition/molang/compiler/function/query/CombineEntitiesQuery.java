package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.world.entity.Entity;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** Combines actor references without duplicate identities. */
public final class CombineEntitiesQuery extends MathFunction {
    private final MathValue[] arguments;

    public CombineEntitiesQuery(MathValue... arguments) {
        super(arguments);
        this.arguments = arguments.clone();
    }

    @Override public Object getRaw(MolangInstance instance) {
        Set<Entity> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Entity> entities = new ArrayList<>();
        for (MathValue argument : arguments) {
            Object value = argument.getRaw(instance);
            if (value instanceof List<?> list) {
                for (Object element : list) add(entities, seen, element);
            } else add(entities, seen, value);
        }
        return List.copyOf(entities);
    }

    private static void add(List<Entity> entities, Set<Entity> seen, Object value) {
        if (value instanceof Entity entity && seen.add(entity)) entities.add(entity);
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
