package com.kltyton.bedrockruntime.client.particle.definition.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.api.IParticleComponent;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.FloatMolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.molang.FloatMolangExp3;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;

import java.util.List;

/// This component specifies the dynamic properties of the particle, from a simulation standpoint what forces act upon the particle?
///
/// These dynamics alter the velocity of the particle, which is a combination of the direction of the particle and the speed.
///
/// Particle direction will always be in the direction of the velocity of the particle.
///
/// @param linerAcceleration       The linear acceleration applied to the particle, defaults to `[0, 0, 0]`.<p>
///                                Units are blocks/sec/sec<p>
///                                An example would be gravity which is `[0, -9.8, 0]`<p>
///                                Evaluated every frame
/// @param linearDragCoefficient   Equation is `acceleration = -linear_drag_coefficient*velocity`<p>
///                                Where velocity is the current direction times speed<p>
///                                Think of this as air-drag. The higher the exp, the more drag<p>
///                                Evaluated every frame
/// @param rotationAcceleration    Acceleration applies to the rotation speed of the particle<p>
///                                Think of a disc spinning up or a smoke puff that starts rotating but slows down over time<p>
///                                Evaluated every frame<p>
///                                Acceleration is in degrees/sec/sec
/// @param rotationDragCoefficient Drag applied to slow down rotation<p>
///                                Equation is `rotation_acceleration += -rotation_rate*rotation_drag_coefficient`<p>
///                                Useful to slow a rotation, or to limit the rotation acceleration<p>
///                                Think of a disc that speeds up (acceleration) but reaches a terminal speed (drag)<p>
///                                Another use is if you have a particle growing in size, having the rotation slow down due to drag can add "weight" to the particle's motion
public record ParticleMotionDynamic(FloatMolangExp3 linerAcceleration, FloatMolangExp linearDragCoefficient, FloatMolangExp rotationAcceleration,
                                    FloatMolangExp rotationDragCoefficient) implements IParticleComponent {
    public static final ResourceLocation ID = new ResourceLocation("particle_motion_dynamic");
    public static final Codec<ParticleMotionDynamic> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FloatMolangExp3.CODEC.optionalFieldOf("linear_acceleration", FloatMolangExp3.ZERO).forGetter(ParticleMotionDynamic::linerAcceleration),
            FloatMolangExp.CODEC.optionalFieldOf("linear_drag_coefficient", FloatMolangExp.ZERO).forGetter(ParticleMotionDynamic::linearDragCoefficient),
            FloatMolangExp.CODEC.optionalFieldOf("rotation_acceleration", FloatMolangExp.ZERO).forGetter(ParticleMotionDynamic::rotationAcceleration),
            FloatMolangExp.CODEC.optionalFieldOf("rotation_drag_coefficient", FloatMolangExp.ZERO).forGetter(ParticleMotionDynamic::rotationDragCoefficient)
    ).apply(instance, ParticleMotionDynamic::new));

    @Override
    public Codec<ParticleMotionDynamic> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of(
                linerAcceleration.exp1(), linerAcceleration.exp2(), linerAcceleration.exp3(),
                linearDragCoefficient, rotationAcceleration, rotationDragCoefficient
        );
    }

    @Override
    public void update(IMolangParticleInstance instance) {
        float dt = instance.getInvTickRate();
        float velocityScale = instance.getVelocityTimeScale();
        Vector3f acceleration = instance.getAcceleration().set(linerAcceleration.calculate(instance));
        float drag = linearDragCoefficient.calculate(instance);
        float vx = (float) (instance.getXd() / velocityScale);
        float vy = (float) (instance.getYd() / velocityScale);
        float vz = (float) (instance.getZd() / velocityScale);
        if (Math.abs(drag) <= Math.ulp(1.0F)) {
            vx += acceleration.x * dt;
            vy += acceleration.y * dt;
            vz += acceleration.z * dt;
        } else {
            float exponent = (float) Math.exp(drag * dt);
            if (exponent >= Float.MAX_VALUE) {
                vx = vy = vz = 0.0F;
            } else {
                float ax = acceleration.x / drag;
                float ay = acceleration.y / drag;
                float az = acceleration.z / drag;
                vx = (ax * exponent + (vx - ax)) / exponent;
                vy = (ay * exponent + (vy - ay)) / exponent;
                vz = (az * exponent + (vz - az)) / exponent;
            }
        }
        instance.self().setParticleSpeed((double) vx * velocityScale, (double) vy * velocityScale, (double) vz * velocityScale);
        if (instance instanceof MolangParticleInstance particle) particle.moveSimulation(vx * dt, vy * dt, vz * dt);

        float angularAcceleration = rotationAcceleration.calculate(instance) * Mth.DEG_TO_RAD;
        float angularDrag = rotationDragCoefficient.calculate(instance);
        float angularSpeed = instance.getZRotD() / velocityScale;
        angularSpeed += (angularAcceleration - angularDrag * angularSpeed) * dt;
        instance.setZRotD(angularSpeed * velocityScale);
        if (instance instanceof MolangParticleInstance particle) {
            Float rotation = particle.getRotation();
            if (rotation != null) particle.setZRot(rotation + angularSpeed * dt);
        }
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    @Override
    public String toString() {
        return "ParticleMotionDynamic{" +
                "linerAcceleration=" + linerAcceleration +
                ", linearDragCoefficient=" + linearDragCoefficient +
                ", rotationAcceleration=" + rotationAcceleration +
                ", rotationDragCoefficient=" + rotationDragCoefficient +
                '}';
    }
}
