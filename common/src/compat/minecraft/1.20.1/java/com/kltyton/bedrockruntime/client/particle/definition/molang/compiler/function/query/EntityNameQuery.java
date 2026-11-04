package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.world.entity.Entity;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

public final class EntityNameQuery extends MathFunction {
    public enum Kind { GET_NAME, IS_NAME_ANY }

    private final Kind kind;
    private final MathValue[] arguments;

    public EntityNameQuery(Kind kind, MathValue... arguments) {
        super(arguments);
        if (kind == Kind.GET_NAME && arguments.length != 0 || kind == Kind.IS_NAME_ANY && arguments.length == 0) {
            throw new IllegalArgumentException("Invalid argument count for entity name query");
        }
        this.kind = kind;
        this.arguments = arguments.clone();
    }

    @Override
    public Object getRaw(MolangInstance instance) {
        if (kind == Kind.GET_NAME) {
            Entity entity = instance.getAttachedEntity();
            return entity == null ? "" : entity.getName().getString();
        }
        return get(instance);
    }

    @Override
    public double compute(MolangInstance instance) {
        if (kind == Kind.GET_NAME) return 0.0;
        Entity entity = instance.getAttachedEntity();
        if (entity == null) return 0.0;
        String name = entity.getName().getString();
        for (MathValue argument : arguments) {
            String candidate = argument.getString(instance);
            if (candidate == null) throw new IllegalArgumentException("query.is_name_any requires string arguments");
            if (name.equals(candidate)) return 1.0;
        }
        return 0.0;
    }

    @Override public String getName() { return kind == Kind.GET_NAME ? "query.get_name" : "query.is_name_any"; }
    @Override public int getMinArgs() { return 0; }
    @Override public MathValue[] getArgs() { return arguments.clone(); }
    @Override public boolean isMutable(MathValue... values) { return true; }
}
