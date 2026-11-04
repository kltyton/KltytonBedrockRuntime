package com.kltyton.bedrockruntime.server.network;

import java.util.HashMap;
import com.kltyton.bedrockruntime.mixin.server.ChunkMapAccessor;
import com.kltyton.bedrockruntime.mixin.server.TrackedEntityAccessor;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.kltyton.bedrockruntime.api.entity.BoneHitboxEntity;
import com.kltyton.bedrockruntime.api.geckolib.state.GeoObbAnimationState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/** Server-thread report limits and tracking checks shared by both loaders. */
public final class ObbReportValidation {
    private static final Map<UUID, Budget> BUDGETS = new HashMap<>();

    private ObbReportValidation() { }

    public static boolean consume(ServerPlayer reporter, int cost) {
        long tick = reporter.level().getServer().getTickCount();
        Budget budget = BUDGETS.computeIfAbsent(reporter.getUUID(), ignored -> new Budget());
        if (budget.tick != tick || budget.level != reporter.level()) {
            budget.tick = tick;
            budget.level = (ServerLevel) reporter.level();
            budget.remaining = 8192;
            budget.tracked.clear();
            budget.tracked.add(reporter);
            for (Object value : ((ChunkMapAccessor) budget.level.getChunkSource().chunkMap).kltyton_bedrock_runtime$trackedEntities().values()) {
                TrackedEntityAccessor tracked = (TrackedEntityAccessor) value;
                if (tracked.kltyton_bedrock_runtime$seenBy().contains(reporter.connection)) {
                    budget.tracked.add(tracked.kltyton_bedrock_runtime$entity());
                }
            }
        }
        int required = Math.max(1, cost);
        if (required > budget.remaining) { return false; }
        budget.remaining -= required;
        return true;
    }

    public static boolean canReport(ServerPlayer reporter, Entity target) {
        Budget budget = BUDGETS.get(reporter.getUUID());
        return target != null && !target.isRemoved() && target.level() == reporter.level()
                && budget != null && budget.tracked.contains(target)
                && target instanceof BoneHitboxEntity entity && entity.kltyton_bedrock_runtime$acceptObbReporter(reporter);
    }

    public static boolean recent(ServerPlayer reporter, long observedTime) {
        long now = reporter.level().getGameTime();
        return observedTime >= now - 80 && observedTime <= now + 10;
    }

    public static boolean finite(AABB box) {
        return Double.isFinite(box.minX) && Double.isFinite(box.minY) && Double.isFinite(box.minZ)
                && Double.isFinite(box.maxX) && Double.isFinite(box.maxY) && Double.isFinite(box.maxZ);
    }

    public static boolean validAnimation(GeoObbAnimationState state) {
        return Double.isFinite(state.animationTimeSeconds()) && Double.isFinite(state.timelineTimeSeconds())
                && (!state.active() || state.animationTimeSeconds() >= 0 && state.timelineTimeSeconds() >= 0);
    }

    public static void clear() { BUDGETS.clear(); }

    public static void tick(net.minecraft.server.MinecraftServer server) {
        BUDGETS.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
    }

    private static final class Budget {
        private long tick = Long.MIN_VALUE;
        private ServerLevel level;
        private int remaining;
        private final Set<Entity> tracked = new HashSet<>();
    }
}
