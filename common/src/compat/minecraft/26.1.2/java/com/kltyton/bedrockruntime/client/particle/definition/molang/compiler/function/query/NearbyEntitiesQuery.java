package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

import java.util.List;

/** Finds nearby entities, optionally restricted to a registered entity type. */
public final class NearbyEntitiesQuery extends MathFunction {
    private final MathValue[] arguments;
    private final boolean excludeSelf;

    public NearbyEntitiesQuery(boolean excludeSelf, MathValue... arguments) {
        super(arguments);
        if (arguments.length > 2) throw new IllegalArgumentException("Nearby entity query accepts at most two arguments");
        this.arguments = arguments.clone();
        this.excludeSelf = excludeSelf;
    }

    @Override
    public Object getRaw(MolangInstance instance) {
        double distance = arguments[0].get(instance);
        if (distance < 0) return List.of();
        Identifier type = null;
        if (arguments.length == 2) {
            String name = arguments[1].getString(instance);
            if (name == null) throw new IllegalArgumentException("Nearby entity type must be a string");
            type = Identifier.parse(name);
        }
        if (instance.getReferencedItem() != null) return List.of();
        Entity self = instance.getAttachedEntity();
        Vec3 origin = self == null ? instance.getPosition() : self.position();
        AABB area = new AABB(origin.subtract(distance, distance, distance), origin.add(distance, distance, distance));
        Identifier requestedType = type;
        double distanceSquared = distance * distance;
        return instance.getLevel().getEntities(excludeSelf ? self : null, area,
                entity -> !entity.isRemoved()
                        && entity.position().distanceToSqr(origin) <= distanceSquared
                        && (requestedType == null || BedrockEntityTypes.matches(requestedType, entity)));
    }

    @Override public double compute(MolangInstance instance) { getRaw(instance); return 0.0; }
    @Override public boolean isMutable(MathValue... values) { return true; }
    @Override public String getName() { return excludeSelf ? "query.get_nearby_entities_except_self" : "query.get_nearby_entities"; }
    @Override public int getMinArgs() { return 1; }
    @Override public MathValue[] getArgs() { return arguments.clone(); }
}
