package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Reads a component of an entity position or displacement. */
public final class EntityAxisFunction extends MathFunction {
    public enum Kind { POSITION, POSITION_DELTA, MOVEMENT_DIRECTION }

    private static final Set<Kind> REPORTED_INVALID_AXES = ConcurrentHashMap.newKeySet();
    private final Kind kind;
    private final MathValue[] arguments;

    public EntityAxisFunction(Kind kind, MathValue... values) {
        super(values);
        this.kind = kind;
        this.arguments = values.clone();
    }

    @Override
    public double compute(MolangInstance instance) {
        if (kind != Kind.MOVEMENT_DIRECTION && instance.getAttachedEntity() == null) return 0.0;
        if (arguments.length != 1) return invalidAxis();
        float requested = (float) arguments[0].get(instance);
        if (!(requested > -1.0F && requested < 3.0F)) return invalidAxis();
        int selectedAxis = (int) requested;
        Entity entity = instance.getAttachedEntity();
        if (entity == null) return 0.0;
        if (kind == Kind.POSITION) {
            return selectedAxis == 0 ? ActorQueryPosition.x(entity)
                    : selectedAxis == 1 ? ActorQueryPosition.y(entity) : ActorQueryPosition.z(entity);
        }
        Vec3 vector = new Vec3(entity.getX() - entity.xo, entity.getY() - entity.yo, entity.getZ() - entity.zo);
        if (kind == Kind.MOVEMENT_DIRECTION) {
            float x = (float) vector.x;
            float y = (float) vector.y;
            float z = (float) vector.z;
            float length = (float) Math.sqrt(z * z + x * x + y * y);
            if (length < 0.0001F) return 0.0;
            return selectedAxis == 0 ? x / length : selectedAxis == 1 ? y / length : z / length;
        }
        return selectedAxis == 0 ? vector.x : selectedAxis == 1 ? vector.y : vector.z;
    }

    private double invalidAxis() {
        if (REPORTED_INVALID_AXES.add(kind)) {
            BedrockParticles.LOGGER.warn("Molang {} requires one axis in the range 0 to 2", getName());
        }
        return 0.0;
    }

    @Override public String getName() {
        return switch (kind) {
            case POSITION -> "query.position";
            case POSITION_DELTA -> "query.position_delta";
            case MOVEMENT_DIRECTION -> "query.movement_direction";
        };
    }
    @Override public int getMinArgs() { return 0; }
    @Override public MathValue[] getArgs() { return arguments.clone(); }
    @Override public boolean isMutable(MathValue... values) { return true; }
}
