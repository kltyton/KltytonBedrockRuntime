package com.kltyton.bedrockruntime.client.particle.definition.event;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;
import com.kltyton.bedrockruntime.client.particle.api.IEventNode;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/// Resolves event IDs and reports missing references in the content log.
public final class EventResolver {
    private static final Set<String> REPORTED_MISSING = ConcurrentHashMap.newKeySet();
    private EventResolver() {
    }

    private record Frame(MolangInstance instance, Vec3 position, Vector3f velocity, org.joml.Matrix3f basis) {}
    private static final ThreadLocal<ArrayDeque<Frame>> FRAMES = new ThreadLocal<>();

    public static void executeEvent(Map<String, Map<String, IEventNode>> events, String id, MolangInstance instance) {
        executeEvent(resolve(events, id), instance, capturePosition(instance), captureVelocity(instance));
    }

    public static void executeEvent(Map<String, IEventNode> nodes, MolangInstance instance,
                                    Vec3 position, Vector3f velocity) {
        executeEvent(nodes, instance, position, velocity, new org.joml.Matrix3f());
    }

    public static void executeEvent(Map<String, IEventNode> nodes, MolangInstance instance,
                                    Vec3 position, Vector3f velocity, org.joml.Matrix3f basis) {
        withFrame(instance, position, velocity, basis, () -> execute(nodes, instance));
    }

    public static void executeBatch(MolangInstance instance, Runnable events) {
        withFrame(instance, capturePosition(instance), captureVelocity(instance), new org.joml.Matrix3f(), events);
    }

    public static void executeInBatch(Map<String, Map<String, IEventNode>> events, String id, MolangInstance instance) {
        execute(resolve(events, id), instance);
    }

    private static void withFrame(MolangInstance instance, Vec3 position, Vector3f velocity,
                                  org.joml.Matrix3f basis, Runnable events) {
        ArrayDeque<Frame> frames = FRAMES.get();
        if (frames == null) {
            frames = new ArrayDeque<>();
            FRAMES.set(frames);
        }
        frames.push(new Frame(instance, position, new Vector3f(velocity), new org.joml.Matrix3f(basis)));
        try {
            events.run();
        } finally {
            frames.pop();
            if (frames.isEmpty()) FRAMES.remove();
        }
    }

    private static Frame frame(MolangInstance instance) {
        ArrayDeque<Frame> frames = FRAMES.get();
        if (frames != null) {
            for (Frame frame : frames) if (frame.instance() == instance) return frame;
        }
        return null;
    }

    public static Vec3 position(MolangInstance instance) {
        Frame frame = frame(instance);
        return frame == null ? capturePosition(instance) : frame.position();
    }

    public static Vector3f velocity(MolangInstance instance) {
        Frame frame = frame(instance);
        return frame == null ? captureVelocity(instance) : new Vector3f(frame.velocity());
    }

    public static org.joml.Matrix3f basis(MolangInstance instance) {
        Frame frame = frame(instance);
        return frame == null ? new org.joml.Matrix3f() : new org.joml.Matrix3f(frame.basis());
    }

    public static org.joml.Matrix3f collisionBasis(Vector3f velocity, Vec3 normal) {
        float vx = velocity.x;
        float vy = velocity.y;
        float vz = velocity.z;
        float speedSquared = vz * vz + vx * vx + vy * vy;
        if (!(Math.ulp(1.0F) * Math.ulp(1.0F) < speedSquared)) return new org.joml.Matrix3f();
        float length = (float) Math.sqrt(speedSquared);
        if (length < 1.0E-4F) {
            vx = vy = vz = 0.0F;
        } else {
            vx /= length;
            vy /= length;
            vz /= length;
        }
        float ux = (float) normal.x;
        float uy = (float) normal.y;
        float uz = (float) normal.z;
        if (vz * uz + uy * vy + ux * vx < -0.99F) {
            float helperX = Math.abs(uy) < 0.99F ? 0.0F : 1.0F;
            float helperY = Math.abs(uy) < 0.99F ? 1.0F : 0.0F;
            ux = -vz * helperY;
            uy = vz * helperX;
            uz = helperY * vx - helperX * vy;
        }
        Vector3f side = new Vector3f(vz * uy - uz * vy, vx * uz - ux * vz, vy * ux - uy * vx);
        Vector3f forward = new Vector3f(side.y * uz - side.z * uy,
                side.z * ux - side.x * uz, side.x * uy - side.y * ux);
        return new org.joml.Matrix3f().setColumn(0, side).setColumn(1, new Vector3f(ux, uy, uz)).setColumn(2, forward);
    }

    private static Vec3 capturePosition(MolangInstance instance) {
        if (!(instance instanceof IMolangParticleInstance particle)) return instance.getPosition();
        ParticleEmitter emitter = particle.getEmitter();
        boolean actor = emitter.getAttachedEntity() != null;
        if (actor && (emitter.getPreset().localPosition || emitter.getPreset().localRotation)) {
            return emitter.getPosition();
        }
        Vector3f position = particle.toSimulationSpace(particle.getX(), particle.getY(), particle.getZ(), new Vector3f());
        if (!actor) {
            Vec3 origin = emitter.getPosition();
            position.add((float) origin.x, (float) origin.y, (float) origin.z);
        }
        return new Vec3(position.x, position.y, position.z);
    }

    public static Vector3f captureVelocity(MolangInstance instance) {
        ParticleEmitter emitter = instance.getEmitter();
        if (instance instanceof IMolangParticleInstance particle) {
            boolean actor = emitter.getAttachedEntity() != null;
            if (actor && (emitter.getPreset().localPosition || emitter.getPreset().localRotation)) {
                return new Vector3f(emitter.getMovementVelocity());
            }
            float step = particle.getVelocityTimeScale();
            Vector3f velocity = new Vector3f((float) (particle.getXd() / step),
                    (float) (particle.getYd() / step), (float) (particle.getZd() / step));
            if (!actor) velocity.add(emitter.getMovementVelocity());
            return velocity;
        }
        if (instance instanceof ParticleEmitter) return new Vector3f(emitter.getMovementVelocity());
        if (emitter.getAttachedEntity() != null) {
            return emitter.getAttachedEntity().getDeltaMovement().toVector3f()
                    .div(com.kltyton.bedrockruntime.client.particle.runtime.timing.ParticleSimulationClock.nominalTickSeconds(emitter.level));
        }
        return new Vector3f();
    }

    public static void execute(Map<String, IEventNode> nodes, MolangInstance instance) {
        IEventNode randomize = nodes.get("randomize");
        Map<String, IEventNode> selected = randomize instanceof EventRandomize weighted ? weighted.select(instance) : null;
        executeNode(nodes.get("sequence"), instance);
        if (selected != null) execute(selected, instance);
        else executeNode(randomize, instance);
        executeNode(nodes.get("particle_effect"), instance);
        executeNode(nodes.get("sound_effect"), instance);
        executeNode(nodes.get("expression"), instance);
        executeNode(nodes.get("log"), instance);
        for (Map.Entry<String, IEventNode> entry : nodes.entrySet()) {
            switch (entry.getKey()) {
                case "sequence", "randomize", "particle_effect", "sound_effect", "expression", "log", "weight" -> {}
                default -> entry.getValue().execute(instance);
            }
        }
    }

    private static void executeNode(IEventNode node, MolangInstance instance) {
        if (node != null) node.execute(instance);
    }

    public static Map<String, IEventNode> resolve(Map<String, Map<String, IEventNode>> events, String id) {
        if (id == null || id.isEmpty()) return Map.of();
        Map<String, IEventNode> nodes = events == null ? null : events.get(id);
        if (nodes == null) {
            if (REPORTED_MISSING.add(id)) BedrockParticles.LOGGER.warn("Unknown particle event id: {}", id);
            return Map.of();
        }
        return nodes;
    }
}
