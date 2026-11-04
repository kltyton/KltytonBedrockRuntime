package com.kltyton.bedrockruntime.server.skill.keyframe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.kltyton.bedrockruntime.Constants;
import com.kltyton.bedrockruntime.api.geckolib.skill.context.GeoKeyframeSkillContext;
import com.kltyton.bedrockruntime.api.geckolib.skill.context.GeoKeyframeSkillPhase;
import com.kltyton.bedrockruntime.api.geckolib.skill.entity.GeoKeyframeSkillEntity;
import com.kltyton.bedrockruntime.api.geckolib.skill.event.GeoKeyframeSkillEvents;
import com.kltyton.bedrockruntime.api.geckolib.skill.registration.GeoKeyframeSkillRegistrar;
import com.kltyton.bedrockruntime.api.geckolib.skill.registration.GeoKeyframeSkillRegistration;
import com.kltyton.bedrockruntime.api.geckolib.state.GeoObbAnimationState;
import com.kltyton.bedrockruntime.api.geometry.query.ObbServerGeometry;
import com.kltyton.bedrockruntime.network.payload.skill.GeoKeyframeSkillPayload;
import com.kltyton.bedrockruntime.server.sync.snapshot.ServerObbStore;
import com.kltyton.bedrockruntime.server.network.ObbReportValidation;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * CN: 可信客户端 marker 的服务端注册分发、跨客户端去重和状态窗口生命周期管理器。
 * EN: Server registry dispatcher, cross-client deduplicator, and state-window lifecycle manager for trusted client markers.
 */
public final class GeoKeyframeSkillDispatcher {
    private static final long CROSS_CLIENT_OBSERVED_TOLERANCE = 4L;
    private static final long SEEN_MARKER_RETENTION = 80L;
    private static final long MAX_WINDOW_LIFETIME = 20L * 60L;

    private static final Map<UUID, Long> LAST_SEQUENCE_BY_REPORTER = new HashMap<>();
    private static final Map<MarkerKey, SeenMarker> SEEN_MARKERS = new LinkedHashMap<>();
    private static final Map<WindowKey, ActiveWindow> ACTIVE_WINDOWS = new LinkedHashMap<>();

    private GeoKeyframeSkillDispatcher() {
    }

    public static synchronized void handle(GeoKeyframeSkillPayload payload, ServerPlayer reporter) {
        ServerLevel level = (ServerLevel) reporter.level();
        Entity rawEntity = level.getEntity(payload.entityId());
        if (!(rawEntity instanceof GeoKeyframeSkillEntity entity)
                || !ObbReportValidation.canReport(reporter, rawEntity)
                || !ServerObbStore.isReporter(rawEntity, reporter)
                || !ObbReportValidation.validAnimation(payload.animationState())
                || !payload.animationState().active()
                || !Double.isFinite(payload.markerTimeSeconds()) || payload.markerTimeSeconds() < 0
                || !Double.isFinite(payload.animationSpeed()) || payload.animationSpeed() <= 0
                || !entity.kltyton_bedrock_runtime$acceptGeoKeyframeSkill(reporter, payload.marker(), payload.animationState(), payload.markerTimeSeconds())) {
            return;
        }

        long previousSequence = LAST_SEQUENCE_BY_REPORTER.getOrDefault(reporter.getUUID(), Long.MIN_VALUE);
        if (payload.clientSequence() <= previousSequence) {
            return;
        }
        LAST_SEQUENCE_BY_REPORTER.put(reporter.getUUID(), payload.clientSequence());

        String marker = GeoKeyframeSkillRegistrar.normalizeMarker(payload.marker());
        if (marker.isBlank()) {
            return;
        }
        GeoKeyframeSkillRegistrar registrar = registrations(entity);
        List<GeoKeyframeSkillRegistration> registrations = registrar.registrations();
        if (registrations.stream().noneMatch(registration -> registration.beginMarker().equals(marker)
                || registration.window() && registration.endMarker().equals(marker))) { return; }
        long serverTick = level.getServer().getTickCount();
        MarkerKey markerKey = new MarkerKey(
                level.dimension().toString(),
                rawEntity.getUUID(),
                payload.animationState().controllerName(),
                payload.animationState().animationName(),
                marker);
        SeenMarker seen = SEEN_MARKERS.get(markerKey);
        if (seen != null
                && Math.abs(payload.observedGameTime() - seen.observedGameTime) <= CROSS_CLIENT_OBSERVED_TOLERANCE
                && serverTick - seen.serverTick <= SEEN_MARKER_RETENTION) {
            return;
        }
        SEEN_MARKERS.put(markerKey, new SeenMarker(payload.observedGameTime(), serverTick));
        // Animation heartbeats are applied before marker dispatch; markers do not replace other controllers.

        // CN: 同一 marker 同时作为旧窗口结束和新窗口开始时，先退出再进入。
        // EN: If one marker closes an old window and opens a new one, exit before entering.
        for (GeoKeyframeSkillRegistration registration : registrations) {
            if (registration.window() && registration.endMarker().equals(marker)) {
                endWindow(level, entity, registration, reporter, payload, marker);
            }
        }
        for (GeoKeyframeSkillRegistration registration : registrations) {
            if (!registration.beginMarker().equals(marker)) {
                continue;
            }
            if (registration.window()) {
                beginWindow(level, entity, registration, reporter, payload, marker);
            } else {
                dispatch(entity, registration, context(
                        reporter, rawEntity, registration.skillId(), marker,
                        GeoKeyframeSkillPhase.POINT, payload, payload.animationState()));
            }
        }
    }

    public static synchronized void tick(MinecraftServer server) {
        long serverTick = server.getTickCount();
        SEEN_MARKERS.entrySet().removeIf(entry -> serverTick - entry.getValue().serverTick > SEEN_MARKER_RETENTION);
        Set<UUID> onlinePlayers = server.getPlayerList().getPlayers().stream()
                .map(ServerPlayer::getUUID)
                .collect(java.util.stream.Collectors.toSet());
        LAST_SEQUENCE_BY_REPORTER.keySet().retainAll(onlinePlayers);

        List<ActiveWindow> cancelled = new ArrayList<>();
        for (Map.Entry<WindowKey, ActiveWindow> entry : List.copyOf(ACTIVE_WINDOWS.entrySet())) {
            ActiveWindow active = entry.getValue();
            if (ACTIVE_WINDOWS.get(entry.getKey()) != active) { continue; }
            Entity rawEntity = active.entity;
            if (rawEntity.isRemoved() || active.reporter.isRemoved()
                    || rawEntity.level() != active.reporter.level()
                    || !rawEntity.level().dimension().toString().equals(active.levelId)
                    || !(rawEntity instanceof GeoKeyframeSkillEntity entity)
                    || serverTick - active.openedServerTick > MAX_WINDOW_LIFETIME) {
                ACTIVE_WINDOWS.remove(entry.getKey(), active);
                cancelled.add(active);
                continue;
            }

            GeoObbAnimationState currentAnimation = ServerObbStore.animationState(rawEntity, active.payload.animationState().controllerName());
            if (animationChanged(active.payload.animationState(), currentAnimation)
                    || currentAnimation.animationTimeSeconds() + 0.05 < active.lastAnimationTime) {
                ACTIVE_WINDOWS.remove(entry.getKey(), active);
                cancelled.add(active);
                continue;
            }

            active.lastAnimationTime = currentAnimation.animationTimeSeconds();
            GeoKeyframeSkillContext tickContext = context(
                    active.reporter,
                    rawEntity,
                    active.registration.skillId(),
                    active.marker,
                    GeoKeyframeSkillPhase.TICK,
                    active.payload,
                    currentAnimation.active() ? currentAnimation : active.payload.animationState());
            dispatch(entity, active.registration, tickContext);
        }

        for (ActiveWindow active : cancelled) {
            if (active.entity instanceof GeoKeyframeSkillEntity entity) {
                dispatch(entity, active.registration, context(
                        active.reporter,
                        active.entity,
                        active.registration.skillId(),
                        active.marker,
                        GeoKeyframeSkillPhase.CANCEL,
                        active.payload,
                        ServerObbStore.animationState(active.entity)));
            }
        }
    }

    public static synchronized void clear() {
        List<ActiveWindow> windows = List.copyOf(ACTIVE_WINDOWS.values());
        ACTIVE_WINDOWS.clear();
        SEEN_MARKERS.clear();
        LAST_SEQUENCE_BY_REPORTER.clear();
        for (ActiveWindow active : windows) {
            if (active.entity instanceof GeoKeyframeSkillEntity entity) {
                dispatch(entity, active.registration, context(active.reporter, active.entity,
                        active.registration.skillId(), active.marker, GeoKeyframeSkillPhase.CANCEL,
                        active.payload, GeoObbAnimationState.NONE));
            }
        }
    }

    private static void beginWindow(ServerLevel level, GeoKeyframeSkillEntity entity,
            GeoKeyframeSkillRegistration registration, ServerPlayer reporter,
            GeoKeyframeSkillPayload payload, String marker) {
        Entity rawEntity = (Entity) entity;
        WindowKey key = new WindowKey(level.dimension().toString(), rawEntity.getUUID(), registration.skillId());
        ActiveWindow replaced = ACTIVE_WINDOWS.put(key, new ActiveWindow(
                rawEntity, registration, reporter, payload, marker, level.getServer().getTickCount(), level.dimension().toString()));
        if (replaced != null) {
            dispatch((GeoKeyframeSkillEntity) replaced.entity, replaced.registration, context(
                    replaced.reporter,
                    replaced.entity,
                    replaced.registration.skillId(),
                    replaced.marker,
                    GeoKeyframeSkillPhase.CANCEL,
                    replaced.payload,
                    ServerObbStore.animationState(replaced.entity)));
        }
        dispatch(entity, registration, context(
                reporter, rawEntity, registration.skillId(), marker,
                GeoKeyframeSkillPhase.BEGIN, payload, payload.animationState()));
    }

    private static void endWindow(ServerLevel level, GeoKeyframeSkillEntity entity,
            GeoKeyframeSkillRegistration registration, ServerPlayer reporter,
            GeoKeyframeSkillPayload payload, String marker) {
        Entity rawEntity = (Entity) entity;
        WindowKey key = new WindowKey(level.dimension().toString(), rawEntity.getUUID(), registration.skillId());
        ActiveWindow active = ACTIVE_WINDOWS.get(key);
        if (active == null
                || !active.payload.animationState().controllerName().equals(payload.animationState().controllerName())
                || !active.payload.animationState().animationName().equals(payload.animationState().animationName())) {
            return;
        }
        ACTIVE_WINDOWS.remove(key, active);
        dispatch(entity, registration, context(
                reporter, rawEntity, registration.skillId(), marker,
                GeoKeyframeSkillPhase.END, payload, payload.animationState()));
    }

    private static GeoKeyframeSkillRegistrar registrations(GeoKeyframeSkillEntity entity) {
        GeoKeyframeSkillRegistrar registrar = new GeoKeyframeSkillRegistrar();
        try {
            entity.kltyton_bedrock_runtime$registerGeoKeyframeSkills(registrar);
        } catch (RuntimeException exception) {
            Constants.LOG.error("Geo keyframe skill registration failed for {}",
                    ((Entity) entity).getScoreboardName(), exception);
        }
        return registrar;
    }

    private static GeoKeyframeSkillContext context(ServerPlayer reporter, Entity entity, String skillId,
            String marker, GeoKeyframeSkillPhase phase, GeoKeyframeSkillPayload payload,
            GeoObbAnimationState animationState) {
        return new GeoKeyframeSkillContext(
                reporter,
                entity,
                skillId,
                marker,
                phase,
                animationState,
                payload.markerTimeSeconds(),
                payload.animationSpeed(),
                payload.clientSequence(),
                payload.observedGameTime(),
                entity.level().getGameTime(),
                entity.position(),
                entity.getDeltaMovement(),
                entity.getYRot(),
                entity.getXRot(),
                ObbServerGeometry.snapshot(entity));
    }

    private static void dispatch(GeoKeyframeSkillEntity entity,
            GeoKeyframeSkillRegistration registration, GeoKeyframeSkillContext context) {
        try {
            if (!entity.kltyton_bedrock_runtime$beforeGeoKeyframeSkill(context)) {
                return;
            }
            try {
                registration.handler().handle(context);
                GeoKeyframeSkillEvents.fire(context);
            } finally {
                entity.kltyton_bedrock_runtime$afterGeoKeyframeSkill(context);
            }
        } catch (RuntimeException exception) {
            Constants.LOG.error("Geo keyframe skill callback failed: entity={}, skill={}, phase={}",
                    context.entity().getScoreboardName(), registration.skillId(), context.phase(), exception);
        }
    }

    private static boolean animationChanged(GeoObbAnimationState opening, GeoObbAnimationState current) {
        return opening.active() && (!current.active() || current.finished()
                || !opening.controllerName().equals(current.controllerName())
                || !opening.animationName().equals(current.animationName())
                || current.animationTimeSeconds() + 0.05 < opening.animationTimeSeconds());
    }

    private record MarkerKey(String levelId, UUID entityId, String controller, String animation, String marker) {
    }

    private record SeenMarker(long observedGameTime, long serverTick) {
    }

    private record WindowKey(String levelId, UUID entityId, String skillId) {
    }

    private static final class ActiveWindow {
        private final Entity entity;
        private final GeoKeyframeSkillRegistration registration;
        private final ServerPlayer reporter;
        private final GeoKeyframeSkillPayload payload;
        private final String marker;
        private final long openedServerTick;
        private final String levelId;
        private double lastAnimationTime;

        private ActiveWindow(Entity entity, GeoKeyframeSkillRegistration registration,
                ServerPlayer reporter, GeoKeyframeSkillPayload payload, String marker, long openedServerTick, String levelId) {
            this.entity = entity;
            this.registration = registration;
            this.reporter = reporter;
            this.payload = payload;
            this.marker = marker;
            this.openedServerTick = openedServerTick;
            this.levelId = levelId;
            this.lastAnimationTime = payload.animationState().animationTimeSeconds();
        }
    }
}
