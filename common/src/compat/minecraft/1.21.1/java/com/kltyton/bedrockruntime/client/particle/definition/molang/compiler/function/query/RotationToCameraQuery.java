package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

public final class RotationToCameraQuery extends MathFunction {
    private static final java.util.concurrent.atomic.AtomicBoolean REPORTED_ARGUMENTS = new java.util.concurrent.atomic.AtomicBoolean();
    private final MathValue[] arguments;

    public RotationToCameraQuery(MathValue... arguments) {
        super(arguments);
        this.arguments = arguments.clone();
    }

    @Override
    public double compute(MolangInstance instance) {
        if (arguments.length != 1) return invalidAxis();
        float axis = (float) arguments[0].get(instance);
        if (!(axis > -1.0F && axis < 2.0F)) return 0.0;
        Camera camera = instance.getReferencedItem() == null ? CameraRotationQuery.camera(instance) : null;
        Vec3 cameraPosition = camera == null ? Vec3.ZERO : camera.getPosition();
        Entity entity = instance.getAttachedEntity();
        Vec3 entityPosition = entity == null ? Vec3.ZERO : entity.position();
        float x = (float) cameraPosition.x - (float) entityPosition.x;
        float y = (float) cameraPosition.y - (float) entityPosition.y;
        float z = (float) cameraPosition.z - (float) entityPosition.z;
        float length = (float) Math.sqrt(z * z + (x * x + y * y));
        if (length < 1.0E-4F) x = y = z = 0.0F;
        else {
            x /= length;
            y /= length;
            z /= length;
        }
        return (int) axis == 0
                ? (float) Math.atan2(y, (float) Math.sqrt(z * z + x * x)) * -57.295776F
                : (float) Math.atan2(z, x) * 57.295776F - 90.0F;
    }

    private double invalidAxis() {
        if (REPORTED_ARGUMENTS.compareAndSet(false, true)) {
            BedrockParticles.LOGGER.warn("query.rotation_to_camera requires one axis in the range 0 to 1");
        }
        return 0.0;
    }

    @Override public String getName() { return "query.rotation_to_camera"; }
    @Override public int getMinArgs() { return 0; }
    @Override public MathValue[] getArgs() { return arguments.clone(); }
    @Override public boolean isMutable(MathValue... values) { return true; }
}
