package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.MolangValueEquality;

/** Matches an entity associated with the current Molang scope by type. */
public final class EntityTypeRelationQuery extends MathFunction {
    public enum Kind { RIDING, OWNER, OWNER_IDENTIFIER }

    private final Kind kind;
    private final MathValue[] arguments;

    public EntityTypeRelationQuery(MathValue... arguments) {
        this(Kind.RIDING, arguments);
    }

    public EntityTypeRelationQuery(Kind kind, MathValue... arguments) {
        super(arguments);
        this.kind = kind;
        this.arguments = arguments.clone();
    }

    @Override
    public Object getRaw(MolangInstance instance) {
        if (kind == Kind.OWNER_IDENTIFIER) {
            Entity entity = instance.getAttachedEntity();
            return entity == null ? MolangValueEquality.defaultIdentifier() : BedrockEntityTypes.identifier(entity).toString();
        }
        return get(instance);
    }

    @Override
    public double compute(MolangInstance instance) {
        if (kind == Kind.OWNER_IDENTIFIER) return 0.0;
        if (arguments.length == 0) return contentError("requires at least one type");
        Entity entity = instance.getAttachedEntity();
        if (entity == null) return kind == Kind.OWNER ? contentError("requires an entity") : 0.0;

        Entity target = kind == Kind.RIDING ? entity.getVehicle() : entity;
        if (target == null) return 0.0;

        boolean matched = false;
        for (MathValue argument : arguments) {
            String name = argument.getString(instance);
            if (name == null) {
                return contentError("requires string arguments");
            }
            if (kind == Kind.OWNER) {
                if (MolangValueEquality.sameString(BedrockEntityTypes.identifier(target).toString(), name)) return 1.0;
            } else {
                Identifier identifier = Identifier.tryParse(name);
                if (identifier != null && BedrockEntityTypes.matches(identifier, target)) matched = true;
            }
        }
        return matched ? 1.0 : 0.0;
    }

    private double contentError(String message) {
        if (REPORTED_ERRORS.add(getName() + ':' + message)) BedrockParticles.LOGGER.warn("{}: {}", getName(), message);
        return 0.0;
    }

    private static final java.util.Set<String> REPORTED_ERRORS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    @Override public String getName() {
        if (kind == Kind.OWNER_IDENTIFIER) return "query.owner_identifier";
        return kind == Kind.OWNER ? "query.is_owner_identifier_any" : "query.is_riding_any_entity_of_type";
    }
    @Override public int getMinArgs() { return 0; }
    @Override public MathValue[] getArgs() { return arguments.clone(); }
    @Override public boolean isMutable(MathValue... values) { return true; }
}
