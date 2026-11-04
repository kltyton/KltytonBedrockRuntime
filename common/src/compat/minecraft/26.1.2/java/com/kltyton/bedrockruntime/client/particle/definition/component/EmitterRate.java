package com.kltyton.bedrockruntime.client.particle.definition.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.IEmitterComponent;
import com.kltyton.bedrockruntime.client.particle.definition.molang.FloatMolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;
import com.kltyton.bedrockruntime.client.particle.runtime.MutableParticleGroup;

import java.util.List;

public abstract sealed class EmitterRate implements IEmitterComponent permits EmitterRate.Instant, EmitterRate.Steady, EmitterRate.Manual {
    @Override
    public int order() {
        return 900;
    }

    private static int clampCount(float value, int maximum) {
        return value > -1.0F && value < maximum ? (int) value : maximum;
    }

    public enum Type {
        INSTANT,
        STEADY,
        MANUAL
    }

    /// All particles come out at once, then no more unless the emitter loops.
    public static final class Instant extends EmitterRate {
        public static final net.minecraft.resources.Identifier ID = net.minecraft.resources.Identifier.withDefaultNamespace("emitter_rate_instant");
        public static final Codec<Instant> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BedrockParticles.lenientOptionalFieldOf(FloatMolangExp.CODEC, "num_particles", () -> FloatMolangExp.ZERO).forGetter(Instant::getNumParticles)
        ).apply(instance, Instant::new));
        private final FloatMolangExp numParticles;

        public Instant(FloatMolangExp numParticles) {
            this.numParticles = numParticles;
        }

        public FloatMolangExp getNumParticles() {
            return numParticles;
        }

        @Override
        public Codec<Instant> codec() {
            return CODEC;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(numParticles);
        }

        @Override
        public void apply(ParticleEmitter emitter) {
            emitter.spawned = false;
            emitter.spawnRate = 0;
            emitter.spawnChance = 0.0F;
            if (emitter.particleGroup == null) emitter.particleGroup = new MutableParticleGroup(0);
            else emitter.particleGroup.setLimit(0);
        }

        @Override
        public void update(ParticleEmitter emitter) {
            emitter.spawnRate = 0;
            emitter.spawnChance = 0.0F;
            if (emitter.spawned) return;
            emitter.spawnRate = clampCount(numParticles.calculate(emitter), 1000);
            if (emitter.particleGroup == null) emitter.particleGroup = new MutableParticleGroup(1000);
            else emitter.particleGroup.setLimit(1000);
        }

        @Override
        public boolean requireUpdate() {
            return true;
        }

        @Override
        public String toString() {
            return "Instant{" +
                    "numParticles=" + numParticles +
                    '}';
        }
    }

    /// Particles come out at a steady or Molang rate over time.
    public static final class Steady extends EmitterRate {
        public static final net.minecraft.resources.Identifier ID = net.minecraft.resources.Identifier.withDefaultNamespace("emitter_rate_steady");
        public static final Codec<Steady> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FloatMolangExp.CODEC.lenientOptionalFieldOf("spawn_rate", FloatMolangExp.ZERO).forGetter(Steady::getSpawnRate),
                BedrockParticles.lenientOptionalFieldOf(FloatMolangExp.CODEC, "max_particles", () -> FloatMolangExp.ZERO).forGetter(Steady::getMaxParticles)
        ).apply(instance, Steady::new));
        private final FloatMolangExp spawnRate;
        private final FloatMolangExp maxParticles;

        public Steady(FloatMolangExp spawnRate, FloatMolangExp maxParticles) {
            this.spawnRate = spawnRate;
            this.maxParticles = maxParticles;
        }

        public FloatMolangExp getSpawnRate() {
            return spawnRate;
        }

        public FloatMolangExp getMaxParticles() {
            return maxParticles;
        }

        @Override
        public Codec<Steady> codec() {
            return CODEC;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(spawnRate, maxParticles);
        }

        @Override
        public void apply(ParticleEmitter emitter) {
            emitter.spawned = false;
            emitter.steadyRate = this;
            emitter.spawnRate = 0;
            emitter.spawnChance = 0.0F;
            if (emitter.particleGroup == null) emitter.particleGroup = new MutableParticleGroup(0);
            else emitter.particleGroup.setLimit(0);
        }

        @Override
        public void update(ParticleEmitter emitter) {
            if (emitter.particleGroup == null || emitter.particleGroup.getLimit() == 0) {
                float value = maxParticles.calculate(emitter);
                int limit = clampCount(value, 1000);
                if (emitter.particleGroup == null) emitter.particleGroup = new MutableParticleGroup(limit);
                else emitter.particleGroup.setLimit(limit);
                sampleSpawnRate(emitter);
            }
            float accumulated = emitter.steadyEmissionAccumulator + emitter.invTickRate;
            float interval = 1.0F / emitter.steadyRatePerSecond;
            emitter.steadyEmissionAccumulator = accumulated;
            emitter.spawnRate = 0;
            emitter.spawnChance = 0.0F;
            if (accumulated >= interval) {
                float count = (float) Math.floor(accumulated / interval);
                emitter.spawnRate = count >= 0.0F ? (int) count : Integer.MAX_VALUE;
                emitter.steadyEmissionAccumulator = accumulated % interval;
                sampleSpawnRate(emitter);
            }
        }

        @Override
        public boolean requireUpdate() {
            return true;
        }

        public void sampleSpawnRate(ParticleEmitter emitter) {
            emitter.steadyRatePerSecond = spawnRate.calculate(emitter);
        }

        @Override
        public String toString() {
            return "Steady{" +
                    "spawnRate=" + spawnRate +
                    ", maxParticles=" + maxParticles +
                    '}';
        }
    }

    /// Particle emission will occur only when the emitter is told to emit via the game itself. This is mostly used by legacy particle effects.
    public static final class Manual extends EmitterRate {
        public static final net.minecraft.resources.Identifier ID = net.minecraft.resources.Identifier.withDefaultNamespace("emitter_rate_manual");
        public static final Codec<Manual> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BedrockParticles.lenientOptionalFieldOf(FloatMolangExp.CODEC, "max_particles", () -> FloatMolangExp.ZERO).forGetter(Manual::getMaxParticles)
        ).apply(instance, Manual::new));
        private final FloatMolangExp maxParticles;

        public Manual(FloatMolangExp maxParticles) {
            this.maxParticles = maxParticles;
        }

        public FloatMolangExp getMaxParticles() {
            return maxParticles;
        }

        @Override
        public Codec<Manual> codec() {
            return CODEC;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(maxParticles);
        }

        @Override
        public void apply(ParticleEmitter emitter) {
            emitter.manualRate = this;
            emitter.spawned = false;
            emitter.spawnRate = 0;
            emitter.spawnChance = 0.0F;
            prepareParticle(emitter);
        }

        public void prepareParticle(ParticleEmitter emitter) {
            int limit = clampCount(maxParticles.calculate(emitter), 2000);
            if (emitter.particleGroup == null) emitter.particleGroup = new MutableParticleGroup(limit);
            else emitter.particleGroup.setLimit(limit);
        }

        @Override
        public String toString() {
            return "Manual{" +
                    "maxParticles=" + maxParticles +
                    '}';
        }
    }
}