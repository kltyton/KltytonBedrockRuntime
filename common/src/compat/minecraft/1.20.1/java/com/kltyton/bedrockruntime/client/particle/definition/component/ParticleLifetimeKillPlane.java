package com.kltyton.bedrockruntime.client.particle.definition.component;

import com.mojang.serialization.Codec;
import org.joml.Vector3f;
import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.api.IParticleComponent;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;

import java.util.List;

/// Particles that cross this plane expire.
///
/// The equation uses the particle's simulation coordinates.
///
/// The four parameters are the usual 4 elements of a plane equation.
///
/// `A*x + B*y + C*z + D = 0` with the parameters being `[ A, B, C, D ]`
public final class ParticleLifetimeKillPlane implements IParticleComponent {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("particle_kill_plane");
    public static final Codec<ParticleLifetimeKillPlane> CODEC = com.kltyton.bedrockruntime.client.particle.definition.ParticleCodecs.list(Codec.FLOAT, 4, 4).xmap(
            floats -> new ParticleLifetimeKillPlane(floats.get(0), floats.get(1), floats.get(2), floats.get(3)),
            plane -> List.of(plane.A, plane.B, plane.C, plane.D)
    );
    public final float A;
    public final float B;
    public final float C;
    public final float D;

    private static final ThreadLocal<Vector3f> SIMULATION_POS = ThreadLocal.withInitial(Vector3f::new);
    private final float normalLengthSqr;

    public ParticleLifetimeKillPlane(float A, float B, float C, float D) {
        this.A = A;
        this.B = B;
        this.C = C;
        this.D = D;

        this.normalLengthSqr = A * A + B * B + C * C;
    }

    public boolean isDegenerate() {
        return normalLengthSqr < 1.0E-12F;
    }

    @Override
    public void update(IMolangParticleInstance instance) {
        float current = distanceToPlane(instance, instance.getX(), instance.getY(), instance.getZ());
        instance.setInsideKillPlane(current <= 0.0F);
        boolean previousValid = instance instanceof com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleInstance particle
                ? particle.hasPreviousMotionSample() : instance.getAge() > 1;
        if (!previousValid) return;
        float previous = distanceToPlane(instance, instance.getPreviousX(), instance.getPreviousY(), instance.getPreviousZ());
        if (previous <= 0.0F && current > 0.0F || previous > 0.0F && current <= 0.0F) instance.self().remove();
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    private float distanceToPlane(IMolangParticleInstance instance, double x, double y, double z) {
        Vector3f position = instance.toSimulationSpace(x, y, z, SIMULATION_POS.get());
        return A * position.x + B * position.y + C * position.z + D;
    }

    @Override
    public Codec<ParticleLifetimeKillPlane> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of();
    }

    @Override
    public String toString() {
        return "ParticleLifetimeKillPlane[" +
                "A=" + A + ", " +
                "B=" + B + ", " +
                "C=" + C + ", " +
                "D=" + D + ']';
    }
}
