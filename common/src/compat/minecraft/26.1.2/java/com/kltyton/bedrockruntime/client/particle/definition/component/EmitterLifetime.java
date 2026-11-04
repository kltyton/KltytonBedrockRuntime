package com.kltyton.bedrockruntime.client.particle.definition.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.IEmitterComponent;
import com.kltyton.bedrockruntime.client.particle.definition.molang.FloatMolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;

import java.util.List;

public abstract sealed class EmitterLifetime implements IEmitterComponent permits EmitterLifetime.Expression, EmitterLifetime.Looping, EmitterLifetime.Once {
    @Override
    public int order() {
        return 850;
    }

    /// Emitter will turn 'on' when the activation expression is non-zero, and will turn 'off' when it's zero.
    ///
    /// This is useful for situations like driving an entity-attached emitter from an entity variable.
    public static final class Expression extends EmitterLifetime {
        public static final net.minecraft.resources.Identifier ID = net.minecraft.resources.Identifier.withDefaultNamespace("emitter_lifetime_expression");
        public static final Codec<Expression> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FloatMolangExp.CODEC.lenientOptionalFieldOf("activation_expression", FloatMolangExp.ONE).forGetter(Expression::getActivationExpression),
                FloatMolangExp.CODEC.lenientOptionalFieldOf("expiration_expression", FloatMolangExp.ZERO).forGetter(Expression::getExpirationExpression)
        ).apply(instance, Expression::new));
        private final FloatMolangExp activationExpression;
        private final FloatMolangExp expirationExpression;

        public Expression(FloatMolangExp activationExpression, FloatMolangExp expirationExpression) {
            this.activationExpression = activationExpression;
            this.expirationExpression = expirationExpression;
        }

        /// When the expression is non-zero, the emitter will emit particles.
        ///
        /// Evaluated every frame
        public FloatMolangExp getActivationExpression() {
            return activationExpression;
        }

        /// Emitter will expire if the expression is non-zero.
        ///
        /// Evaluated every frame
        public FloatMolangExp getExpirationExpression() {
            return expirationExpression;
        }

        @Override
        public Codec<Expression> codec() {
            return CODEC;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(activationExpression, expirationExpression);
        }

        @Override
        public void update(ParticleEmitter emitter) {
            emitter.active = activationExpression.calculate(emitter) != 0.0 && !emitter.isExpired();
            if (expirationExpression.calculate(emitter) != 0.0) emitter.expire();
        }

        @Override
        public boolean requireUpdate() {
            return true;
        }

        @Override
        public String toString() {
            return "Expression{" +
                    "activationExpression=" + activationExpression +
                    ", expirationExpression=" + expirationExpression +
                    '}';
        }
    }

    /// Emitter will loop until it is removed.
    public static final class Looping extends EmitterLifetime {
        public static final net.minecraft.resources.Identifier ID = net.minecraft.resources.Identifier.withDefaultNamespace("emitter_lifetime_looping");
        public static final Codec<Looping> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BedrockParticles.lenientOptionalFieldOf(FloatMolangExp.CODEC, "active_time", () -> FloatMolangExp.ZERO).forGetter(Looping::getActiveTime),
                FloatMolangExp.CODEC.lenientOptionalFieldOf("sleep_time", FloatMolangExp.ZERO).forGetter(Looping::getSleepTime)
        ).apply(instance, Looping::new));
        private final FloatMolangExp activeTime;
        private final FloatMolangExp sleepTime;

        public Looping(FloatMolangExp activeTime, FloatMolangExp sleepTime) {
            this.activeTime = activeTime;
            this.sleepTime = sleepTime;
        }

        /// Emitter will emit particles for this time per loop
        ///
        /// Evaluated once per particle emitter loop
        public FloatMolangExp getActiveTime() {
            return activeTime;
        }

        /// Emitter will pause emitting particles for this time per loop
        ///
        /// Evaluated once per particle emitter loop
        public FloatMolangExp getSleepTime() {
            return sleepTime;
        }

        @Override
        public Codec<Looping> codec() {
            return CODEC;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(activeTime, sleepTime);
        }

        @Override
        public void apply(ParticleEmitter emitter) {
            float active = activeTime.calculate(emitter);
            emitter.setActiveTimeSeconds(active);
            emitter.setLoopTimes(active, sleepTime.calculate(emitter));

        }

        @Override
        public void update(ParticleEmitter emitter) {
            emitter.active = emitter.isInActiveLoopTime() && !emitter.isExpired();
        }

        @Override
        public boolean requireUpdate() {
            return true;
        }

        @Override
        public String toString() {
            return "Looping{" +
                    "activeTime=" + activeTime +
                    ", sleepTime=" + sleepTime +
                    '}';
        }
    }

    public static final class Once extends EmitterLifetime {
        public static final net.minecraft.resources.Identifier ID = net.minecraft.resources.Identifier.withDefaultNamespace("emitter_lifetime_once");
        public static final Codec<Once> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BedrockParticles.lenientOptionalFieldOf(FloatMolangExp.CODEC, "active_time", () -> FloatMolangExp.ZERO).forGetter(Once::getActiveTime)
        ).apply(instance, Once::new));
        private final FloatMolangExp activeTime;

        public Once(FloatMolangExp activeTime) {
            this.activeTime = activeTime;
        }

        public FloatMolangExp getActiveTime() {
            return activeTime;
        }

        @Override
        public Codec<Once> codec() {
            return CODEC;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(activeTime);
        }

        @Override
        public void update(ParticleEmitter emitter) {
            if (emitter.hasExceededLifetime()) {
                emitter.expire();
                emitter.active = false;
            }
        }

        @Override
        public void apply(ParticleEmitter emitter) {
            emitter.setLoopTimes(activeTime.calculate(emitter), 0.0F);
        }

        @Override
        public boolean requireUpdate() {
            return true;
        }

        @Override
        public String toString() {
            return "Once{" +
                    "activeTime=" + activeTime +
                    '}';
        }
    }
}
