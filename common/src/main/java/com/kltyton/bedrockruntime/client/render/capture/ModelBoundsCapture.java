package com.kltyton.bedrockruntime.client.render.capture;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.kltyton.bedrockruntime.client.selection.model.VisualPartId;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Collects model callbacks during a culled render sample and scopes its cache to one world tick. */
public final class ModelBoundsCapture {
    private static final Map<Entity, AABB> SAMPLED_BOUNDS = new IdentityHashMap<>();
    private static final ThreadLocal<Capture> ACTIVE = new ThreadLocal<>();
    private static ClientLevel lastLevel;
    private static long lastGameTime = Long.MIN_VALUE;

    private ModelBoundsCapture() { }

    public static AABB getOrSample(ClientLevel level, Entity entity, Supplier<AABB> sample) {
        long gameTime = level.getGameTime();
        if (level != lastLevel || gameTime != lastGameTime) {
            SAMPLED_BOUNDS.clear();
            lastLevel = level;
            lastGameTime = gameTime;
        }
        if (!SAMPLED_BOUNDS.containsKey(entity)) {
            SAMPLED_BOUNDS.put(entity, sample.get());
        }
        return SAMPLED_BOUNDS.get(entity);
    }

    public static AABB capture(Entity entity, Vec3 camera, Runnable submit) {
        Capture previous = ACTIVE.get();
        Capture capture = new Capture(VisualPartId.ownerKey(entity));
        ACTIVE.set(capture);
        try {
            submit.run();
            return capture.bounds == null ? null : capture.bounds.move(camera);
        } finally {
            if (previous == null) { ACTIVE.remove(); } else { ACTIVE.set(previous); }
        }
    }

    public static void record(VisualPartId part, AABB renderBounds) {
        Capture capture = ACTIVE.get();
        if (capture != null && capture.owner.equals(part.ownerKey())) {
            capture.bounds = capture.bounds == null ? renderBounds : capture.bounds.minmax(renderBounds);
        }
    }

    public static void clear() {
        SAMPLED_BOUNDS.clear();
        ACTIVE.remove();
        lastLevel = null;
        lastGameTime = Long.MIN_VALUE;
    }

    private static final class Capture {
        private final String owner;
        private AABB bounds;

        private Capture(String owner) { this.owner = owner; }
    }
}
