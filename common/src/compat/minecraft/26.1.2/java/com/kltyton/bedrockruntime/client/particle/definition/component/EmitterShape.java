package com.kltyton.bedrockruntime.client.particle.definition.component;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.particle.Particle;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;
import com.kltyton.bedrockruntime.client.particle.PSDiagnostics;
import com.kltyton.bedrockruntime.client.particle.api.*;
import com.kltyton.bedrockruntime.client.particle.definition.molang.FloatMolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.molang.FloatMolangExp3;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.runtime.EmitterPreset;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleEngine;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticlePreset;

import java.util.List;
import java.util.Map;

public abstract sealed class EmitterShape implements IEmitterComponent permits EmitterShape.Disc, EmitterShape.Box, EmitterShape.EntityAABB, EmitterShape.Point, EmitterShape.Custom, EmitterShape.Sphere {
    static final Map<Identifier, Codec<? extends EmitterShape>> MAP = new Object2ObjectOpenHashMap<>();

    protected final boolean surfaceOnly;

    protected EmitterShape(boolean surfaceOnly) {
        this.surfaceOnly = surfaceOnly;
    }

    /// Emit only from the edge of the shape
    public boolean isSurfaceOnly() {
        return surfaceOnly;
    }

    @Override
    public void update(ParticleEmitter emitter) {
        List<EmitterShape> shapes = emitter.getPreset().shapes;
        if (!shapes.isEmpty() && shapes.getFirst() != this && shapes.contains(this)) return;
        if (emitter.spawned && !emitter.getPreset().hasSteadyRate && !emitter.isManual) return;
        int count = emitter.isManual ? 1 : emitter.spawnRate;
        if (!emitter.isManual && emitter.spawnChance > 0.0F && emitter.level.getRandom().nextFloat() < emitter.spawnChance) {
            count++;
        }
        boolean attempted = false;
        for (int num = 0; num < count; num++) {
            if (emitter.isManual && emitter.manualRate != null) {
                emitter.manualRate.prepareParticle(emitter);
            }
            if (!hasSpaceInParticleLimit(emitter)) break;
            attempted = true;
            emitReserved(emitter);
        }
        if (attempted && emitter.getPreset().hasInstantRate) {
            emitter.spawned = true;
        }
    }


    private boolean emitReserved(ParticleEmitter emitter) {
        int previousPending = emitter.reserveParticleSlot();
        try {
            return emittingParticle(emitter);
        } finally {
            emitter.releaseParticleSlot(previousPending);
        }
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    private static void sampleBox(RandomSource random, Vector3f position, float x, float y, float z, boolean surface) {
        int axis = -1;
        float boundary = 0.0F;
        float yz = y * z;
        float xz = x * z;
        float area = x * y + xz + yz;
        if (surface && area > Math.ulp(1.0F)) {
            float selection = random.nextFloat() * area;
            axis = selection < yz ? 0 : selection < yz + xz ? 1 : 2;
            float extent = axis == 0 ? x : axis == 1 ? y : z;
            boundary = random.nextBoolean() ? extent : -extent;
        }
        position.set(random.nextFloat() * (x + x) - x,
                random.nextFloat() * (y + y) - y,
                random.nextFloat() * (z + z) - z);
        if (axis >= 0) position.setComponent(axis, boundary);
    }

    private static void normalizeDirection(Vector3f direction) {
        float length = direction.length();
        if (length < 1.0E-4F) direction.zero();
        else direction.div(length);
    }

    private static void sampleRadialDirection(RandomSource random, Vector3f direction) {
        direction.set(random.nextFloat() - random.nextFloat(),
                random.nextFloat() - random.nextFloat(), random.nextFloat() - random.nextFloat());
    }

    private static void normalizeRadialDirection(Vector3f direction) {
        normalizeDirection(direction);
        if (direction.x == 0.0F && direction.y == 0.0F && direction.z == 0.0F) direction.set(1.0F, 0.0F, 0.0F);
    }

    protected abstract void initializeParticle(MolangInstance instance, Vector3f position, Vector3f speed);

    protected boolean isPoint() {
        return false;
    }

    protected abstract Identifier id();

    public final Identifier dispatchId() {
        return id();
    }

    private static Identifier registerId(String name, Codec<? extends EmitterShape> codec) {
        Identifier id = Identifier.withDefaultNamespace(name);
        MAP.put(id, codec);
        return id;
    }

    private <T extends Particle & IMolangParticleInstance> boolean emittingParticle(ParticleEmitter emitter) {
        try {
            T instance = RegisterCustomParticleTypeEvent.createParticle(emitter);
            instance.setEmitter(emitter);
            if (instance instanceof MolangParticleInstance particle && emitter.getManualEmissionPosition() == null) {
                particle.includeInCurrentUpdate();
            }

            ParticlePreset particlePreset = instance.getPreset();

            Vector3f position = new Vector3f();
            Vector3f speed = new Vector3f();
            particlePreset.prepareVariables(instance);
            EmitterPreset emitterPreset = emitter.getPreset();
            if (!emitterPreset.shapes.isEmpty() && emitterPreset.shapes.getFirst() == this) {
                for (EmitterShape shape : emitterPreset.shapes) shape.initializeParticle(instance, position, speed);
            } else {
                initializeParticle(instance, position, speed);
            }
            Vec3 manualPosition = emitter.getManualEmissionPosition();
            if (manualPosition != null) {
                position.add((float) manualPosition.x, (float) manualPosition.y, (float) manualPosition.z);
            }
            if (emitter.inheritedParticleSpeed != null) speed.add(emitter.inheritedParticleSpeed);
            emitter.transformParticleBirth(position, speed);
            instance.getInitialSpeed().set(speed);
            float velocityScale = instance.getVelocityTimeScale();
            if (emitter.isParticleSimulationWorldSpace()) {
                Vec3 emitterPos = emitter.getPosition();
                position.add((float) emitterPos.x, (float) emitterPos.y, (float) emitterPos.z);
            }
            Vec3 storedPosition = emitter.fromParticleSimulationSpace(position);
            instance.setPos(storedPosition.x, storedPosition.y, storedPosition.z);
            instance.setPosO(storedPosition.x, storedPosition.y, storedPosition.z);
            instance.setParticleSpeed((double) speed.x * velocityScale,
                    (double) speed.y * velocityScale, (double) speed.z * velocityScale);

            for (IParticleComponent component : particlePreset.effect.orderedParticleEarlyComponents) {
                component.apply(instance);
            }
            speed.set(instance.getInitialSpeed());
            if (emitterPreset.localVelocity) speed.add(emitter.getMovementVelocity());
            instance.setParticleSpeed((double) speed.x * velocityScale,
                    (double) speed.y * velocityScale, (double) speed.z * velocityScale);
            instance.setParticleGroup(emitter.particleGroup == null ? null : emitter.particleGroup.asParticleLimit());

            for (IParticleComponent component : particlePreset.effect.orderedParticleComponents) {
                if (component instanceof ParticleAppearanceBillboard || component instanceof ParticleAppearanceTinting
                        || component instanceof ParticleAppearanceCameraDistanceFade) continue;
                component.apply(instance);
            }
            if (instance.isDiscarded()) return false;
            instance.setComponents(particlePreset.effect.orderedParticleComponentsWhichRequireUpdate);
            MolangParticleEngine.addParticle(instance);
            if (instance instanceof MolangParticleInstance molang) {
                PSDiagnostics.infoFirstN("particle-created:" + emitter.id, 12, "particle created runtimeId={} particle={} state={}", emitter.id, emitter.particleId, molang.diagnosticSummary());
            } else {
                PSDiagnostics.infoFirstN("particle-created:" + emitter.id, 12, "particle created runtimeId={} particle={} instance={} pos=({}, {}, {}) speed=({}, {}, {}) lifetime={}",
                        emitter.id,
                        emitter.particleId,
                        instance.getClass().getName(),
                        instance.getX(),
                        instance.getY(),
                        instance.getZ(),
                        instance.getXd(),
                        instance.getYd(),
                        instance.getZd(),
                        instance.self().getLifetime()
                );
            }
            return true;
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Failed to create particle " + emitter.particleId
                    + " for emitter " + emitter.id + " using " + getClass().getSimpleName(), exception);
        }
    }

    private static boolean hasSpaceInParticleLimit(ParticleEmitter emitter) {
        return emitter.hasSpace();
    }

    /// This component spawns particles using a disc shape, particles can be spawned inside the shape or on its outer perimeter.
    public static final class Disc extends EmitterShape {
        public static final Codec<Disc> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FloatMolangExp3.ZERO_COMPONENT_CODEC.lenientOptionalFieldOf("offset", FloatMolangExp3.ZERO).forGetter(disc -> disc.offset),
                FloatMolangExp.CODEC.lenientOptionalFieldOf("radius", FloatMolangExp.ONE).forGetter(disc -> disc.radius),
                PlaneNormal.CODEC.lenientOptionalFieldOf("plane_normal", PlaneNormal.Y).forGetter(disc -> disc.planeNormal),
                Direction.CODEC.lenientOptionalFieldOf("direction", Direction.OUTWARDS).forGetter(disc -> disc.direction),
                Codec.BOOL.lenientOptionalFieldOf("surface_only", false).forGetter(EmitterShape::isSurfaceOnly)
        ).apply(instance, Disc::new));
        public static final Identifier ID = registerId("emitter_shape_disc", CODEC);
        /// Specifies the offset from the emitter to emit the particles
        ///
        /// Evaluated once per particle emitted
        public final FloatMolangExp3 offset;
        /// Disc radius
        ///
        /// Evaluated once per particle emitted
        public final FloatMolangExp radius;
        /// Specifies the normal of the disc plane, the disc will be perpendicular to this direction
        public final PlaneNormal planeNormal;
        /// Specifies the direction of particles.
        public final Direction direction;

        public Disc(FloatMolangExp3 offset, FloatMolangExp radius, PlaneNormal planeNormal, Direction direction, boolean surfaceOnly) {
            super(surfaceOnly);
            this.offset = offset;
            this.radius = radius;
            this.planeNormal = planeNormal;
            this.direction = direction;
        }

        @Override
        public Codec<Disc> codec() {
            return CODEC;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(
                    offset.exp1(), offset.exp2(), offset.exp3(), radius,
                    planeNormal.plane.exp1(), planeNormal.plane.exp2(), planeNormal.plane.exp3(),
                    direction.direct.exp1(), direction.direct.exp2(), direction.direct.exp3()
            );
        }

        @Override
        protected void initializeParticle(MolangInstance instance, Vector3f position, Vector3f speed) {
            float[] shapeOffset = offset.calculate(instance);
            RandomSource random = instance.getLevel().getRandom();
            sampleRadialDirection(random, speed);
            Vector3f normal = new Vector3f(planeNormal.plane.calculate(instance));
            normalizeDirection(normal);
            speed.fma(-speed.dot(normal), normal);
            normalizeRadialDirection(speed);
            float distance = radius.calculate(instance);
            if (!surfaceOnly) distance *= (random.nextFloat() * 0.5F + 0.5F) - random.nextFloat() * 0.5F;
            position.set(speed).mul(distance);
            if (direction == Direction.INWARDS) speed.negate();
            else if (direction != Direction.OUTWARDS) speed.set(direction.direct.calculate(instance));
            position.add(shapeOffset[0], shapeOffset[1], shapeOffset[2]);
        }

        @Override
        protected Identifier id() {
            return ID;
        }

        @Override
        public String toString() {
            return "Disc{" +
                    "offset=" + offset +
                    ", radius=" + radius +
                    ", planeNormal=" + planeNormal +
                    ", direction=" + direction +
                    ", surfaceOnly=" + surfaceOnly +
                    '}';
        }

        /// Custom direction for the normal
        public record PlaneNormal(String name, FloatMolangExp3 plane) {
            public static final PlaneNormal X = new PlaneNormal("x", FloatMolangExp3.X);
            public static final PlaneNormal Y = new PlaneNormal("y", FloatMolangExp3.Y);
            public static final PlaneNormal Z = new PlaneNormal("z", FloatMolangExp3.Z);
            public static final float[] YN = new float[]{0.0F, 1.0F, 0.0F};
            public static final Codec<PlaneNormal> CODEC = Codec.either(Codec.STRING, FloatMolangExp3.ZERO_COMPONENT_CODEC).xmap(
                    either -> either.map(d -> switch (d) {
                        case "x" -> X;
                        case "z" -> Z;
                        default -> Y;
                    }, list -> new PlaneNormal("custom", list)),
                    plane -> Either.right(plane.plane)
            );

            public boolean isCustom() {
                return "custom".equals(name);
            }

            @Override
            public String toString() {
                return "PlaneNormal{" +
                        "name='" + name + '\'' +
                        ", plane=" + plane +
                        '}';
            }
        }
    }

    /// All particles come out of a box of the specified size from the emitter.
    public static final class Box extends EmitterShape {
        public static final Codec<Box> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FloatMolangExp3.ZERO_COMPONENT_CODEC.lenientOptionalFieldOf("offset", FloatMolangExp3.ZERO).forGetter(box -> box.offset),
                FloatMolangExp3.ZERO_COMPONENT_CODEC.lenientOptionalFieldOf("half_dimensions", FloatMolangExp3.ZERO).forGetter(box -> box.halfDimensions),
                Direction.CODEC.lenientOptionalFieldOf("direction", Direction.OUTWARDS).forGetter(box -> box.direction),
                Codec.BOOL.lenientOptionalFieldOf("surface_only", false).forGetter(EmitterShape::isSurfaceOnly)
        ).apply(instance, Box::new));
        public static final Identifier ID = registerId("emitter_shape_box", CODEC);
        /// Specifies the offset from the emitter to emit the particles
        /// Evaluated once per particle emitted
        public final FloatMolangExp3 offset;
        public final FloatMolangExp3 halfDimensions;
        /// Specifies the direction of particles.
        public final Direction direction;

        public Box(FloatMolangExp3 offset, FloatMolangExp3 halfDimensions, Direction direction, boolean surfaceOnly) {
            super(surfaceOnly);
            this.offset = offset;
            this.halfDimensions = halfDimensions;
            this.direction = direction;
        }

        @Override
        public Codec<Box> codec() {
            return CODEC;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(
                    offset.exp1(), offset.exp2(), offset.exp3(),
                    halfDimensions.exp1(), halfDimensions.exp2(), halfDimensions.exp3(),
                    direction.direct.exp1(), direction.direct.exp2(), direction.direct.exp3()
            );
        }

        @Override
        protected void initializeParticle(MolangInstance instance, Vector3f position, Vector3f speed) {
            float[] shapeOffset = offset.calculate(instance);
            float[] dimensions = halfDimensions.calculate(instance);
            sampleBox(instance.getLevel().getRandom(), position, dimensions[0], dimensions[1], dimensions[2], surfaceOnly);
            direction.apply(instance, this, position, speed);
            position.add(shapeOffset[0], shapeOffset[1], shapeOffset[2]);
        }

        @Override
        public String toString() {
            return "Box{" +
                    "surfaceOnly=" + surfaceOnly +
                    ", direction=" + direction +
                    ", halfDimensions=" + halfDimensions +
                    ", offset=" + offset +
                    '}';
        }

        @Override
        protected Identifier id() {
            return ID;
        }
    }

    /// All particles come out of the axis-aligned bounding box AABB for the entity the emitter is attached to, or the emitter point if no entity.
    public static final class EntityAABB extends EmitterShape {
        public static final Codec<EntityAABB> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Direction.CODEC.lenientOptionalFieldOf("direction", Direction.OUTWARDS).forGetter(entityAABB -> entityAABB.direction),
                Codec.BOOL.lenientOptionalFieldOf("surface_only", false).forGetter(EmitterShape::isSurfaceOnly)
        ).apply(instance, EntityAABB::new));
        public static final Identifier ID = registerId("emitter_shape_entity_aabb", CODEC);
        public final Direction direction;

        public EntityAABB(Direction direction, boolean surfaceOnly) {
            super(surfaceOnly);
            this.direction = direction;
        }

        @Override
        public Codec<EntityAABB> codec() {
            return CODEC;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(direction.direct.exp1(), direction.direct.exp2(), direction.direct.exp3());
        }

        @Override
        protected void initializeParticle(MolangInstance instance, Vector3f position, Vector3f speed) {
            Entity entity = instance.getAttachedEntity();
            float halfX = 0.0F;
            float halfY = 0.0F;
            float halfZ = 0.0F;
            if (entity != null) {
                AABB bounds = entity.getBoundingBox();
                halfX = (float) (bounds.getXsize() * 0.5);
                halfY = (float) (bounds.getYsize() * 0.5);
                halfZ = (float) (bounds.getZsize() * 0.5);
                sampleBox(instance.getLevel().getRandom(), position, halfX, halfY, halfZ, surfaceOnly);
            } else {
                position.zero();
            }
            direction.apply(instance, this, position, speed);
            position.y += halfY;
        }

        @Override
        public String toString() {
            return "EntityAABB{" +
                    "direction=" + direction +
                    ", surfaceOnly=" + surfaceOnly +
                    '}';
        }

        @Override
        protected Identifier id() {
            return ID;
        }
    }

    /// All particles are emitted at an expression-defined offset and direction.
    public static final class Custom extends EmitterShape {
        public static final Codec<Custom> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FloatMolangExp3.ZERO_COMPONENT_CODEC.lenientOptionalFieldOf("offset", FloatMolangExp3.ZERO).forGetter(shape -> shape.offset),
                FloatMolangExp3.ZERO_COMPONENT_CODEC.lenientOptionalFieldOf("direction", FloatMolangExp3.ZERO).forGetter(shape -> shape.direction)
        ).apply(instance, Custom::new));
        public static final Identifier ID = registerId("emitter_shape_custom", CODEC);
        public final FloatMolangExp3 offset;
        public final FloatMolangExp3 direction;

        public Custom(FloatMolangExp3 offset, FloatMolangExp3 direction) {
            super(false);
            this.offset = offset;
            this.direction = direction;
        }

        @Override public Codec<Custom> codec() { return CODEC; }
        @Override public List<MolangExp> getAllMolangExp() {
            return List.of(offset.exp1(), offset.exp2(), offset.exp3(), direction.exp1(), direction.exp2(), direction.exp3());
        }
        @Override
        protected void initializeParticle(MolangInstance instance, Vector3f position, Vector3f speed) {
            position.x = offset.exp1().calculate(instance);
            speed.x = direction.exp1().calculate(instance);
            position.y = offset.exp2().calculate(instance);
            speed.y = direction.exp2().calculate(instance);
            position.z = offset.exp3().calculate(instance);
            speed.z = direction.exp3().calculate(instance);
            normalizeDirection(speed);
        }
        @Override protected Identifier id() { return ID; }
    }

    /// All particles come out of a point offset from the emitter.
    public static final class Point extends EmitterShape {
        public static final Codec<Point> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FloatMolangExp3.ZERO_COMPONENT_CODEC.lenientOptionalFieldOf("offset", FloatMolangExp3.ZERO).forGetter(point -> point.offset),
                Direction.CODEC.lenientOptionalFieldOf("direction", Direction.OUTWARDS).forGetter(point -> point.direction)
        ).apply(instance, Point::new));
        public static final Identifier ID = registerId("emitter_shape_point", CODEC);
        /// Specifies the offset from the emitter to emit the particles
        ///
        /// Evaluated once per particle emitted
        public final FloatMolangExp3 offset;
        /// Specifies the direction of particles.
        public final Direction direction;

        public Point(FloatMolangExp3 offset, Direction direction) {
            super(false);
            this.offset = offset;
            this.direction = direction;
        }

        @Override
        public Codec<Point> codec() {
            return CODEC;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(
                    offset.exp1(), offset.exp2(), offset.exp3(),
                    direction.direct.exp1(), direction.direct.exp2(), direction.direct.exp3()
            );
        }

        @Override
        protected void initializeParticle(MolangInstance instance, Vector3f position, Vector3f speed) {
            RandomSource random = instance.getLevel().getRandom();
            boolean randomDirection = direction == Direction.INWARDS || direction == Direction.OUTWARDS;
            position.x = offset.exp1().calculate(instance);
            speed.x = randomDirection ? random.nextFloat() * 2.0F - 1.0F : direction.direct.exp1().calculate(instance);
            position.y = offset.exp2().calculate(instance);
            speed.y = randomDirection ? random.nextFloat() * 2.0F - 1.0F : direction.direct.exp2().calculate(instance);
            position.z = offset.exp3().calculate(instance);
            speed.z = randomDirection ? random.nextFloat() * 2.0F - 1.0F : direction.direct.exp3().calculate(instance);
            normalizeDirection(speed);
        }

        @Override
        public String toString() {
            return "Point{" +
                    "surfaceOnly=" + surfaceOnly +
                    ", direction=" + direction +
                    ", offset=" + offset +
                    '}';
        }

        @Override
        protected boolean isPoint() {
            return true;
        }

        @Override
        protected Identifier id() {
            return ID;
        }
    }

    public static final class Sphere extends EmitterShape {
        public static final Codec<Sphere> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FloatMolangExp3.ZERO_COMPONENT_CODEC.lenientOptionalFieldOf("offset", FloatMolangExp3.ZERO).forGetter(sphere -> sphere.offset),
                FloatMolangExp.CODEC.lenientOptionalFieldOf("radius", FloatMolangExp.ONE).forGetter(sphere -> sphere.radius),
                Direction.CODEC.lenientOptionalFieldOf("direction", Direction.OUTWARDS).forGetter(sphere -> sphere.direction),
                Codec.BOOL.lenientOptionalFieldOf("surface_only", false).forGetter(EmitterShape::isSurfaceOnly)
        ).apply(instance, Sphere::new));
        public static final Identifier ID = registerId("emitter_shape_sphere", CODEC);
        /// Specifies the offset from the emitter to emit the particles
        ///
        /// Evaluated once per particle emitted
        public final FloatMolangExp3 offset;
        /// Sphere radius
        ///
        /// Evaluated once per particle emitted
        public final FloatMolangExp radius;
        /// Specifies the direction of particles.
        public final Direction direction;

        public Sphere(FloatMolangExp3 offset, FloatMolangExp radius, Direction direction, boolean surfaceOnly) {
            super(surfaceOnly);
            this.offset = offset;
            this.radius = radius;
            this.direction = direction;
        }

        @Override
        public Codec<Sphere> codec() {
            return CODEC;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(
                    offset.exp1(), offset.exp2(), offset.exp3(), radius,
                    direction.direct.exp1(), direction.direct.exp2(), direction.direct.exp3()
            );
        }

        @Override
        protected void initializeParticle(MolangInstance instance, Vector3f position, Vector3f speed) {
            float[] shapeOffset = offset.calculate(instance);
            RandomSource random = instance.getLevel().getRandom();
            sampleRadialDirection(random, speed);
            normalizeRadialDirection(speed);
            float distance = radius.calculate(instance);
            if (!surfaceOnly) distance *= (random.nextFloat() * 0.5F + 0.5F) - random.nextFloat() * 0.5F;
            position.set(speed).mul(distance);
            if (direction == Direction.INWARDS) speed.negate();
            else if (direction != Direction.OUTWARDS) speed.set(direction.direct.calculate(instance));
            position.add(shapeOffset[0], shapeOffset[1], shapeOffset[2]);
        }

        @Override
        public String toString() {
            return "Sphere{" +
                    "offset=" + offset +
                    ", radius=" + radius +
                    ", direction=" + direction +
                    ", surfaceOnly=" + surfaceOnly +
                    '}';
        }

        @Override
        protected Identifier id() {
            return ID;
        }
    }

    /// Evaluated once per particle emitted
    public record Direction(String name, FloatMolangExp3 direct) implements StringRepresentable {
        /// Particle direction towards center of shape
        public static final Direction INWARDS = new Direction("inwards", FloatMolangExp3.ZERO);
        /// Particle direction away from center of shape
        public static final Direction OUTWARDS = new Direction("outwards", FloatMolangExp3.ZERO);
        public static final Codec<Direction> DIRECTION_CODEC = StringRepresentable.fromValues(() -> new Direction[]{INWARDS, OUTWARDS});
        public static final Codec<Direction> CODEC = Codec.either(DIRECTION_CODEC, FloatMolangExp3.ZERO_COMPONENT_CODEC).xmap(
                either -> either.map(dir -> dir, list -> new Direction("custom", list)),
                dir -> dir.direct == FloatMolangExp3.ZERO ? Either.left(dir) : Either.right(dir.direct)
        );

        @Override
        public String getSerializedName() {
            return name;
        }

        public void apply(MolangInstance instance, EmitterShape shape, Vector3f position, Vector3f speed) {
            apply(instance, position, speed, shape.isPoint());
        }

        private void apply(MolangInstance instance, Vector3f position, Vector3f speed, boolean point) {
            if (this == INWARDS || this == OUTWARDS) {
                speed.set(position);
                if (this == INWARDS) speed.negate();
            } else {
                speed.set(direct.calculate(instance));
            }
            normalizeDirection(speed);
        }

        @Override
        public String toString() {
            return "Direction{" +
                    "name='" + name + '\'' +
                    ", direct=" + direct +
                    '}';
        }
    }
}