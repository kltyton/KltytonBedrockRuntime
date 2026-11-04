package com.kltyton.bedrockruntime.particle.registry;

import com.kltyton.bedrockruntime.client.particle.definition.molang.ItemUseDurations;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Server-owned entity state used by particle Molang queries. Transport is injected through the
 * loader-specific {@link ParticleEntityStateSink}; no client class is loaded here.
 */
public final class MolangEntityStateTracker {
    private static final Map<LivingEntity, Tracked> TRACKED = new IdentityHashMap<>();
    private static volatile ParticleEntityStateSink sink;

    private MolangEntityStateTracker() {
    }

    public static void install(ParticleEntityStateSink stateSink) {
        if (sink != null && sink != stateSink) {
            throw new IllegalStateException("Particle entity state sink already installed");
        }
        sink = stateSink;
    }

    public static void onEntityLoad(Entity entity, ServerLevel level) {
        if (!(entity instanceof LivingEntity living) || !(living instanceof Mob || living instanceof Player)
                || living.isRemoved()) {
            return;
        }
        long current = state(living);
        TRACKED.computeIfAbsent(living,
                ignored -> new Tracked(current, recoveryTarget(living, current), living.tickCount));
    }

    public static void onEntityUnload(Entity entity, ServerLevel level) {
        if (entity instanceof LivingEntity living) {
            TRACKED.remove(living);
        }
    }

    public static void onStartTracking(ServerPlayer player, Entity target) {
        ParticleEntityStateSink currentSink = sink;
        if (currentSink == null || !(target instanceof LivingEntity living)
                || !(living instanceof Mob || living instanceof Player)
                || living.isRemoved()) {
            return;
        }
        long current = state(living);
        Optional<GlobalPos> recoveryTarget = recoveryTarget(living, current);
        if (living.isRemoved() || !currentSink.canSend(player)
                || !currentSink.isTracking((ServerLevel) living.level(), living, player)) {
            return;
        }
        Tracked tracked = TRACKED.computeIfAbsent(living,
                ignored -> new Tracked(current, recoveryTarget, living.tickCount));
        tracked.players.add(player);
        currentSink.sendToPlayer(player, snapshot(living, true));
    }

    public static void onStopTracking(ServerPlayer player, Entity target) {
        if (!(target instanceof LivingEntity living)) {
            return;
        }
        Tracked tracked = TRACKED.get(living);
        if (tracked != null && tracked.players.remove(player) && tracked.players.isEmpty()) {
            TRACKED.remove(living);
        }
    }

    public static void onPlayerLoggedOut(ServerPlayer player) {
        TRACKED.values().removeIf(tracked -> {
            tracked.players.remove(player);
            return tracked.players.isEmpty();
        });
    }

    public static void onEntityLeft(Entity entity) {
        if (!entity.level().isClientSide() && entity instanceof LivingEntity living) {
            TRACKED.remove(living);
        }
    }

    public static void onServerStopped(MinecraftServer server) {
        TRACKED.keySet().removeIf(entity -> entity.level() instanceof ServerLevel level
                && level.getServer() == server);
    }

    public static void tick(MinecraftServer server) {
        ParticleEntityStateSink currentSink = sink;
        if (currentSink == null) {
            return;
        }
        for (LivingEntity entity : List.copyOf(TRACKED.keySet())) {
            Tracked tracked = TRACKED.get(entity);
            if (tracked == null) {
                continue;
            }
            if (entity.isRemoved()) {
                TRACKED.remove(entity, tracked);
                continue;
            }
            if (!(entity.level() instanceof ServerLevel level) || level.getServer() != server) {
                continue;
            }
            long current = state(entity);
            Optional<GlobalPos> recoveryTarget = recoveryTarget(entity, current);
            if (entity.isRemoved() || TRACKED.get(entity) != tracked) {
                continue;
            }
            int elapsed = entity.tickCount - tracked.tick;
            int previousRemaining = remaining(tracked.state);
            int predicted = previousRemaining > 0 ? Math.max(0, previousRemaining - elapsed) : previousRemaining;
            boolean changed = flags(current) != flags(tracked.state)
                    || (flags(current) & MolangEntityStateFlags.ITEM_USE_ACTIVE) != 0
                    && remaining(current) != predicted
                    || !Objects.equals(recoveryTarget, tracked.recoveryTarget);
            tracked.state = current;
            tracked.recoveryTarget = recoveryTarget;
            tracked.tick = entity.tickCount;
            if (!changed) {
                continue;
            }
            currentSink.sendToTracking(level, entity, snapshot(entity, false), tracked.players);
        }
    }

    public static MolangEntityStateSnapshot snapshot(LivingEntity entity) {
        return snapshot(entity, true);
    }

    public static boolean isTracking(LivingEntity entity, ServerPlayer player) {
        ParticleEntityStateSink currentSink = sink;
        return currentSink != null
                && entity.level() instanceof ServerLevel level
                && player.level() == level
                && level.getEntity(entity.getId()) == entity
                && currentSink.isTracking(level, entity, player);
    }

    private static MolangEntityStateSnapshot snapshot(LivingEntity entity, boolean initial) {
        long state = state(entity);
        int packetFlags = initial ? flags(state) | MolangEntityStateFlags.INITIAL : flags(state);
        return new MolangEntityStateSnapshot(entity.getId(), entity.getUUID(), packetFlags,
                remaining(state), recoveryTarget(entity, state));
    }

    private static long state(LivingEntity entity) {
        int flags = 0;
        if (entity instanceof Mob mob) {
            flags |= MolangEntityStateFlags.TARGET_KNOWN;
            if (mob.getTarget() != null) {
                flags |= MolangEntityStateFlags.HAS_TARGET;
            }
        }
        if (!entity.isRemoved() && entity instanceof Animal animal) {
            flags |= MolangEntityStateFlags.LOVE_KNOWN;
            if (animal.isInLove()) {
                flags |= MolangEntityStateFlags.IN_LOVE;
            }
        }
        if (!entity.isRemoved() && entity instanceof Player player
                && (player.getMainHandItem().is(Items.RECOVERY_COMPASS)
                || player.getOffhandItem().is(Items.RECOVERY_COMPASS))) {
            flags |= MolangEntityStateFlags.RECOVERY_KNOWN;
        }
        int itemUseRemaining = 0;
        if (!entity.isRemoved() && entity.isUsingItem()
                && (entity instanceof Player || ItemUseDurations.usesMappedTimer(entity.getUseItem()))) {
            flags |= MolangEntityStateFlags.ITEM_USE_ACTIVE;
            itemUseRemaining = ItemUseDurations.remaining(entity);
        }
        return ((long) itemUseRemaining << 32) | (flags & 0xffffffffL);
    }

    private static int flags(long state) {
        return (int) state;
    }

    private static int remaining(long state) {
        return (int) (state >> 32);
    }

    private static Optional<GlobalPos> recoveryTarget(LivingEntity entity, long state) {
        return (flags(state) & MolangEntityStateFlags.RECOVERY_KNOWN) != 0 && entity instanceof Player player
                ? player.getLastDeathLocation() : Optional.empty();
    }

    private static final class Tracked {
        private final Set<ServerPlayer> players = Collections.newSetFromMap(new IdentityHashMap<>());
        private long state;
        private Optional<GlobalPos> recoveryTarget;
        private int tick;

        private Tracked(long state, Optional<GlobalPos> recoveryTarget, int tick) {
            this.state = state;
            this.recoveryTarget = recoveryTarget;
            this.tick = tick;
        }
    }
}
