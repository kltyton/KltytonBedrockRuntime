package com.kltyton.bedrockruntime.client.particle.definition.component;

import com.mojang.serialization.Codec;
import org.joml.Vector3f;
import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.api.IParticleComponent;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;

import java.util.List;

/// Particles that cross this plane expire.
///
/// The plane moves with the emitter and keeps its world-space orientation.
///
/// The four parameters are the usual 4 elements of a plane equation.
///
/// `A*x + B*y + C*z + D = 0` with the parameters being `[ A, B, C, D ]`
public final class ParticleLifetimeKillPlane implements IParticleComponent {
    public static final net.minecraft.resources.Identifier ID = net.minecraft.resources.Identifier.withDefaultNamespace("particle_kill_plane");
    public static final Codec<ParticleLifetimeKillPlane> CODEC = Codec.list(Codec.FLOAT, 4, 4).xmap(
            floats -> new ParticleLifetimeKillPlane(floats.getFirst(), floats.get(1), floats.get(2), floats.get(3)),
            plane -> List.of(plane.A, plane.B, plane.C, plane.D)
    );
    public final float A;
    public final float B;
    public final float C;
    public final float D;

    private static final ThreadLocal<Vector3f> OFFSET = ThreadLocal.withInitial(Vector3f::new);
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
    public void apply(IMolangParticleInstance instance) {
        instance.setInsideKillPlane(inside(instance));
    }

    @Override
    public void update(IMolangParticleInstance instance) {
        boolean previous = instance.isInsideKillPlane();
        boolean current = inside(instance);
        instance.setInsideKillPlane(current);
        if (previous != current) instance.self().remove();
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    private boolean inside(IMolangParticleInstance instance) {
        Vector3f offset = instance.getWorldOffsetFromEmitter(OFFSET.get());
        return A * offset.x + B * offset.y + C * offset.z + D <= 0;
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
