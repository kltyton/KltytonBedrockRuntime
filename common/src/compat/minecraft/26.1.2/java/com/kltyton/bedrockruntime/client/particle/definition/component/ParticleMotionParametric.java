package com.kltyton.bedrockruntime.client.particle.definition.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.api.IParticleComponent;
import com.kltyton.bedrockruntime.client.particle.definition.molang.FloatMolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.molang.FloatMolangExp3;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleFrameMotion;

import java.util.List;

/// This component directly controls the particle.
///
/// @param relativePosition Directly set the position relative to the emitter. Defaults to `[0, 0, 0]`<p>
///                         E.g. a spiral might be:
///                         ```
///                         "relative_position": ["Math.cos(Params.LifeTime)", 1.0, "Math.sin(Params.Lifetime)"]
///                         ```
///                         Evaluated every frame
/// @param direction        Directly set the 3d direction of the particle<p>
///                         Doesn't affect direction if not specified<p>
///                         Evaluated every frame
/// @param rotation         Directly set the rotation of the particle<p>
///                         Evaluated every frame
public record ParticleMotionParametric(FloatMolangExp3 relativePosition, FloatMolangExp3 direction, FloatMolangExp rotation) implements IParticleComponent {
    public static final net.minecraft.resources.Identifier ID = net.minecraft.resources.Identifier.withDefaultNamespace("particle_motion_parametric");
    public static final Codec<ParticleMotionParametric> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FloatMolangExp3.ZERO_COMPONENT_CODEC.lenientOptionalFieldOf("relative_position", FloatMolangExp3.ZERO).forGetter(ParticleMotionParametric::relativePosition),
            FloatMolangExp3.ZERO_COMPONENT_CODEC.lenientOptionalFieldOf("direction", FloatMolangExp3.ZERO).forGetter(ParticleMotionParametric::direction),
            FloatMolangExp.CODEC.lenientOptionalFieldOf("rotation", FloatMolangExp.ZERO).forGetter(ParticleMotionParametric::rotation)
    ).apply(instance, ParticleMotionParametric::new));

    @Override
    public Codec<ParticleMotionParametric> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of(
                relativePosition.exp1(), relativePosition.exp2(), relativePosition.exp3(),
                direction.exp1(), direction.exp2(), direction.exp3(), rotation
        );
    }

    private static final Vector3f vec = new Vector3f();

    public void sampleRender(IMolangParticleInstance instance, ParticleFrameMotion state, float partialTick) {
        state.reset();
        if (relativePosition != FloatMolangExp3.ZERO) {
            instance.getInterpolatedWorldPosition(state.positionBuffer(), partialTick);
        }
    }

    @Override
    public void update(IMolangParticleInstance instance) {
        if (relativePosition != FloatMolangExp3.ZERO) {
            vec.set(relativePosition.calculate(instance));
            ParticleEmitter emitter = instance.getEmitter();
            Vec3 position = emitter.fromParticleSimulationSpace(vec);
            instance.self().setPos(position.x, position.y, position.z);
        }
        if (direction != FloatMolangExp3.ZERO) {
            vec.set(direction.calculate(instance));
            float velocityScale = instance.getVelocityTimeScale();
            float vx = (float) (instance.getXd() / velocityScale);
            float vy = (float) (instance.getYd() / velocityScale);
            float vz = (float) (instance.getZd() / velocityScale);
            float speed = (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
            if (speed >= Math.ulp(1.0F)) {
                float length = vec.length();
                if (length < 1.0E-4F) vec.zero();
                else vec.div(length).mul(speed);
            }
            instance.self().setParticleSpeed((double) vec.x * velocityScale,
                    (double) vec.y * velocityScale, (double) vec.z * velocityScale);
        }
        if (rotation != FloatMolangExp.ZERO) {
            instance.setZRot(calculateRotation(instance));
        }
    }

    private float calculateRotation(IMolangParticleInstance instance) {
        return rotation.calculate(instance) * Mth.DEG_TO_RAD;
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    @Override
    public String toString() {
        return "ParticleMotionParametric{" +
                "relativePosition=" + relativePosition +
                ", direction=" + direction +
                ", rotation=" + rotation +
                '}';
    }
}
