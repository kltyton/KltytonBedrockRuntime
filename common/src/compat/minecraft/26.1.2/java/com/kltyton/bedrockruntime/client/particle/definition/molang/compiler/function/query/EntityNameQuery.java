package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.MolangValueEquality;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class EntityNameQuery extends MathFunction {
    public enum Kind { GET_NAME, IS_NAME_ANY }
    private static final Set<String> REPORTED_ERRORS = ConcurrentHashMap.newKeySet();

    private final Kind kind;
    private final MathValue[] arguments;

    public EntityNameQuery(Kind kind, MathValue... arguments) {
        super(arguments);
        this.kind = kind;
        this.arguments = arguments.clone();
    }

    @Override
    public Object getRaw(MolangInstance instance) {
        if (kind == Kind.GET_NAME) {
            Entity entity = instance.getAttachedEntity();
            return entity == null ? 0.0 : nameTag(entity);
        }
        return get(instance);
    }

    @Override
    public double compute(MolangInstance instance) {
        if (kind == Kind.GET_NAME) return 0.0;
        if (arguments.length == 0) return contentError("requires at least one name");
        Entity entity = instance.getAttachedEntity();
        if (entity == null) return contentError("requires an entity");
        String name = nameTag(entity);
        for (MathValue argument : arguments) {
            String candidate = argument.getString(instance);
            if (candidate == null) return contentError("requires string names");
            if (MolangValueEquality.sameString(name, candidate)) return 1.0;
        }
        return 0.0;
    }

    private static String nameTag(Entity entity) {
        if (entity instanceof Player player) return player.getName().getString();
        var customName = entity.getCustomName();
        return customName == null ? "" : customName.getString();
    }

    private double contentError(String reason) {
        if (REPORTED_ERRORS.add(getName() + ':' + reason)) BedrockParticles.LOGGER.warn("Molang {} {}", getName(), reason);
        return 0.0;
    }

    @Override public String getName() { return kind == Kind.GET_NAME ? "query.get_name" : "query.is_name_any"; }
    @Override public int getMinArgs() { return 0; }
    @Override public MathValue[] getArgs() { return arguments.clone(); }
    @Override public boolean isMutable(MathValue... values) { return true; }
}
