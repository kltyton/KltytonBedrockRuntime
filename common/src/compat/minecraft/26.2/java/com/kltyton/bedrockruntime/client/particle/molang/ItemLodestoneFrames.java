package com.kltyton.bedrockruntime.client.particle.molang;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Maintains lodestone compass frames used by actor particle expressions. */
public final class ItemLodestoneFrames {
    private static final long RETAIN_NANOS = 20_000_000_000L;
    private static final ItemCompassFrameState.Direction SPINNING = new ItemCompassFrameState.Direction();
    private static final List<WorldObservers> WORLDS = new ArrayList<>();

    private ItemLodestoneFrames() {}

    public static int frame(Entity entity, ItemStack item) {
        if (entity.isRemoved()) return 0;
        Level level = entity.level();
        LodestoneTracker tracker = item.get(DataComponents.LODESTONE_TRACKER);
        if (tracker == null || tracker.target().isEmpty()) return SPINNING.frame();
        GlobalPos target = tracker.target().get();
        int entityId = entity.getId();
        ActorObservers actor = observersFor(level).actors.computeIfAbsent(entity.getUUID(), ignored -> new ActorObservers(entityId));
        if (actor.entityId != entityId) {
            actor.targets.clear();
            actor.entityId = entityId;
        }
        Observer observer = actor.targets.computeIfAbsent(target, ignored -> new Observer());
        observer.lastUsed = System.nanoTime();
        observer.position = BlockPos.containing((float) entity.getX(), (float) entity.getY(), (float) entity.getZ());
        observer.dimension = level.dimension();
        observer.yaw = (int) entity.getYRot();
        return observer.direction.frame();
    }

    public static void tick() {
        SPINNING.spin();
        if (WORLDS.isEmpty()) return;
        long now = System.nanoTime();
        WORLDS.removeIf(world -> {
            if (world.level.get() == null) return true;
            world.actors.values().removeIf(actor -> {
                Map<GlobalPos, Observer> targets = actor.targets;
                targets.entrySet().removeIf(entry -> {
                    Observer observer = entry.getValue();
                    if (now - observer.lastUsed > RETAIN_NANOS) return true;
                    GlobalPos target = entry.getKey();
                    if (target.dimension() != observer.dimension) {
                        observer.direction.copyFrom(SPINNING);
                    } else {
                        float x = (float) target.pos().getX() - (float) observer.position.getX();
                        float z = (float) target.pos().getZ() - (float) observer.position.getZ();
                        observer.direction.point(x, z, observer.yaw);
                    }
                    return false;
                });
                return targets.isEmpty();
            });
            return world.actors.isEmpty();
        });
    }

    public static void clear() {
        WORLDS.clear();
    }

    private static WorldObservers observersFor(Level level) {
        for (Iterator<WorldObservers> it = WORLDS.iterator(); it.hasNext();) {
            WorldObservers world = it.next();
            Level existing = world.level.get();
            if (existing == null) it.remove();
            else if (existing == level) return world;
        }
        WorldObservers world = new WorldObservers(level);
        WORLDS.add(world);
        return world;
    }

    private static final class Observer {
        private final ItemCompassFrameState.Direction direction = new ItemCompassFrameState.Direction();
        private long lastUsed;
        private BlockPos position;
        private ResourceKey<Level> dimension;
        private int yaw;
    }

    private static final class ActorObservers {
        private final Map<GlobalPos, Observer> targets = new HashMap<>();
        private int entityId;

        private ActorObservers(int entityId) {
            this.entityId = entityId;
        }
    }

    private static final class WorldObservers {
        private final WeakReference<Level> level;
        private final Map<UUID, ActorObservers> actors = new HashMap<>();

        private WorldObservers(Level level) {
            this.level = new WeakReference<>(level);
        }
    }
}
