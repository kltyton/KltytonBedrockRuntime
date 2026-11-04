package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

/** Reads a component of an entity position or displacement. */
public final class EntityAxisFunction extends MathFunction {
    public enum Kind { POSITION, POSITION_DELTA, MOVEMENT_DIRECTION }

    private final Kind kind;
    private final MathValue axis;

    public EntityAxisFunction(Kind kind, MathValue... values) {
        super(values);
        this.kind = kind;
        this.axis = values[0];
    }

    @Override
    public double compute(MolangInstance instance) {
        double selectedAxis = axis.get(instance);
        if (selectedAxis != 0 && selectedAxis != 1 && selectedAxis != 2) {
            throw new IllegalArgumentException("Entity position axis must be 0, 1, or 2");
        }
        Entity entity = instance.getAttachedEntity();
        if (entity == null) return 0.0;
        Vec3 vector = kind == Kind.POSITION ? entity.position()
                : new Vec3(entity.getX() - entity.xo, entity.getY() - entity.yo, entity.getZ() - entity.zo);
        if (kind == Kind.MOVEMENT_DIRECTION) vector = vector.normalize();
        return selectedAxis == 0 ? vector.x : selectedAxis == 1 ? vector.y : vector.z;
    }

    @Override public String getName() {
        return switch (kind) {
            case POSITION -> "query.position";
            case POSITION_DELTA -> "query.position_delta";
            case MOVEMENT_DIRECTION -> "query.movement_direction";
        };
    }
    @Override public int getMinArgs() { return 1; }
    @Override public void validate(MathValue... values) {
        if (values.length != 1) throw new IllegalArgumentException("Entity position query requires one axis");
    }
    @Override public MathValue[] getArgs() { return new MathValue[]{axis}; }
    @Override public boolean isMutable(MathValue... values) { return true; }
}
