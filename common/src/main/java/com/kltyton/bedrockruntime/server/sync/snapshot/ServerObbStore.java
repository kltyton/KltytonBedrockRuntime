package com.kltyton.bedrockruntime.server.sync.snapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashSet;
import java.util.UUID;
import org.joml.Matrix4f;
import com.kltyton.bedrockruntime.server.network.ObbReportValidation;
import java.util.concurrent.ConcurrentHashMap;

import com.kltyton.bedrockruntime.api.bone.attribute.ObbBoneAttribute;
import com.kltyton.bedrockruntime.api.bone.key.ObbBoneKey;
import com.kltyton.bedrockruntime.api.entity.BoneHitboxEntity;
import com.kltyton.bedrockruntime.api.geckolib.entity.GeoBoneHitboxEntity;
import com.kltyton.bedrockruntime.api.geckolib.state.GeoObbAnimationState;
import com.kltyton.bedrockruntime.api.geometry.snapshot.ObbEntityGeometrySnapshot;
import com.kltyton.bedrockruntime.api.geometry.snapshot.ObbPartGeometrySnapshot;
import com.kltyton.bedrockruntime.api.state.runtime.ObbBoneState;
import com.kltyton.bedrockruntime.geometry.obb.ObbGeometry;
import com.kltyton.bedrockruntime.network.payload.entity.ObbEntityPartsPayload;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

/**
 * CN: 服务端只读 OBB 骨骼快照表。它存储客户端模型结果；公开查询层可在这些快照上执行 SAT 和射线查询。
 * EN: Server-side read-only OBB bone snapshot table. It stores client model results; the public query layer can run SAT and ray queries on them.
 */
public final class ServerObbStore {
    private static final Map<Key, EntityState> STATES = new ConcurrentHashMap<>();

    private ServerObbStore() {
    }

    public static void update(ServerPlayer player, ObbEntityPartsPayload payload) {
        ServerLevel level = (ServerLevel) player.level();
        Entity entity = level.getEntity(payload.entityId());
        Key key = Key.of(level, payload.entityId());
        if (!(entity instanceof BoneHitboxEntity hitboxEntity)
                || !ObbReportValidation.canReport(player, entity)
                || !ObbReportValidation.validAnimation(payload.animationState())
                || payload.controllerStates().stream().anyMatch(state -> !ObbReportValidation.validAnimation(state))) {
            return;
        }
        long gameTime = level.getGameTime();
        EntityState previous = STATES.get(key);
        if (previous != null && previous.entity != entity) {
            STATES.remove(key);
            previous = null;
        }
        if (previous != null && gameTime - previous.gameTime <= 10
                && !previous.reporter.equals(player.getUUID())) {
            return;
        }

        Map<String, GeoObbAnimationState> animations = new java.util.LinkedHashMap<>();
        for (GeoObbAnimationState state : payload.controllerStates()) {
            if (animations.putIfAbsent(state.controllerName(), state) != null) { return; }
        }
        List<ServerObbPart> parts = new ArrayList<>(payload.parts().size());
        HashSet<ObbBoneKey> keys = new HashSet<>();
        double radius = hitboxEntity.kltyton_bedrock_runtime$maxSnapshotRadius();
        if (!Double.isFinite(radius) || radius <= 0) { return; }
        for (ObbEntityPartsPayload.Part payloadPart : payload.parts()) {
            if (payloadPart.cubeIndex() < 0 || payloadPart.cubeIndex() > 4095
                    || payloadPart.partName().isBlank()
                    || !ObbReportValidation.finite(payloadPart.localBounds())) { return; }
            ObbBoneKey boneKey = new ObbBoneKey(payloadPart.source(), payloadPart.partName(), payloadPart.cubeIndex());
            if (!keys.add(boneKey)) { continue; }
            Matrix4f transform = payloadPart.localToWorld();
            if (!transform.isFinite() || transform.m03() != 0 || transform.m13() != 0
                    || transform.m23() != 0 || transform.m33() != 1
                    || !Float.isFinite(transform.determinant()) || Math.abs(transform.determinant()) < 1.0E-9) { return; }
            // Derive both inverse and broad-phase bounds from the accepted transform.
            ObbGeometry geometry = ObbGeometry.fromLocalBounds(payloadPart.localBounds(), transform);
            Optional<ObbGeometry> enhancedGeometry = com.kltyton.bedrockruntime.api.model.EnhancedModels.resolveGeometry(entity, boneKey, geometry);
            if (enhancedGeometry.isEmpty()) { return; }
            geometry = enhancedGeometry.get();
            AABB entityBounds = entity.getBoundingBox().inflate(radius);
            AABB geometryBounds = geometry.worldBounds();
            if (!ObbReportValidation.finite(geometry.worldBounds())
                    || !entityBounds.contains(new Vec3(geometryBounds.minX, geometryBounds.minY, geometryBounds.minZ))
                    || !entityBounds.contains(new Vec3(geometryBounds.maxX, geometryBounds.maxY, geometryBounds.maxZ))) { return; }
            if (!hitboxEntity.kltyton_bedrock_runtime$obbState().isRegistered(boneKey)) { return; }
            Optional<ObbBoneState> bone = hitboxEntity.kltyton_bedrock_runtime$obbState().resolve(boneKey);
            if (bone.isEmpty()) { return; }
            parts.add(new ServerObbPart(bone.get(), geometry));
        }
        hitboxEntity.kltyton_bedrock_runtime$obbState().clearClientSnapshots();
        if (hitboxEntity instanceof GeoBoneHitboxEntity geoEntity) {
            geoEntity.kltyton_bedrock_runtime$geoUpdateAnimationStates(payload.controllerStates());
        }

        if (parts.isEmpty() && animations.isEmpty()) {
            STATES.remove(key);
        } else {
            List<ServerObbPart> previousParts;
            long previousGameTime;
            Vec3 previousPosition;
            float previousYaw;
            if (previous == null) {
                previousParts = List.of();
                previousGameTime = -1L;
                previousPosition = entity.position();
                previousYaw = entity.getYRot();
            } else if (previous.gameTime == gameTime) {
                previousParts = previous.previousParts;
                previousGameTime = previous.previousGameTime;
                previousPosition = previous.previousPosition;
                previousYaw = previous.previousYaw;
            } else {
                previousParts = previous.parts;
                previousGameTime = previous.gameTime;
                previousPosition = previous.position;
                previousYaw = previous.yaw;
            }
            STATES.put(key, new EntityState(
                    entity, player.getUUID(), gameTime,
                    gameTime - payload.captureAgeTicks(),
                    previousGameTime,
                    payload.animationState(), Map.copyOf(animations),
                    List.copyOf(parts),
                    previousParts,
                    entity.position(),
                    previousPosition,
                    entity.getYRot(),
                    previousYaw));
        }
    }

    public static Optional<ServerObbPart> part(Entity entity, ObbBoneKey boneKey) {
        return state(entity).flatMap(state -> state.parts.stream()
                .filter(part -> part.bone.key().equals(boneKey))
                .findFirst());
    }

    public static Optional<ServerObbPart> firstPart(Entity entity, ObbBoneAttribute attribute) {
        return state(entity).flatMap(state -> state.parts.stream()
                .filter(part -> part.bone.hasAttribute(attribute))
                .findFirst());
    }

    public static List<ServerObbPart> parts(Entity entity) {
        return state(entity).map(EntityState::parts).orElseGet(List::of);
    }

    public static GeoObbAnimationState animationState(Entity entity) {
        return state(entity).map(EntityState::animationState).orElse(GeoObbAnimationState.NONE);
    }

    public static GeoObbAnimationState animationState(Entity entity, String controllerName) {
        return state(entity).map(value -> value.animations.getOrDefault(controllerName, GeoObbAnimationState.NONE))
                .orElse(GeoObbAnimationState.NONE);
    }

    public static List<GeoObbAnimationState> animations(Entity entity) {
        return state(entity).map(value -> List.copyOf(value.animations.values())).orElseGet(List::of);
    }

    /** CN: 转换为公开、只读的服务端几何快照。EN: Converts the stored state into the public read-only server geometry snapshot. */
    public static Optional<ObbEntityGeometrySnapshot> snapshot(Entity entity) {
        return state(entity).map(state -> new ObbEntityGeometrySnapshot(
                entity,
                state.gameTime,
                state.previousGameTime,
                state.animationState,
                publicParts(state.parts),
                publicParts(state.previousParts),
                state.position,
                state.previousPosition,
                state.yaw,
                state.previousYaw));
    }

    /**
     * CN: 验证指定 OBB 仍存在后，计算世界坐标点随实体根位姿产生的平移/偏航位移；不传递骨骼动画。
     * EN: After verifying that the OBB still exists, computes root translation/yaw displacement for a world point; skeletal animation is excluded.
     */
    public static Optional<PartMotion> partMotion(Entity entity, ObbBoneKey boneKey, Vec3 worldPoint) {
        return state(entity).flatMap(state -> {
            if (state.previousGameTime < 0L) {
                return Optional.empty();
            }
            Optional<ServerObbPart> current = findPart(state.parts, boneKey);
            Optional<ServerObbPart> previous = findPart(state.previousParts, boneKey);
            if (current.isEmpty() || previous.isEmpty()) {
                return Optional.empty();
            }
            float yawDelta = Mth.wrapDegrees(state.yaw - state.previousYaw);
            Vec3 relative = worldPoint.subtract(state.previousPosition);
            Vec3 movedPoint = state.position.add(relative.yRot((float) Math.toRadians(-yawDelta)));
            Vec3 movement = movedPoint.subtract(worldPoint);
            return Optional.of(new PartMotion(state.gameTime, movement));
        });
    }

    private static Optional<EntityState> state(Entity entity) {
        EntityState state = STATES.get(Key.of(entity.level(), entity.getId()));
        if (state == null) {
            return Optional.empty();
        }
        if (state.entity != entity || entity.isRemoved()
                || entity.level().getGameTime() - state.captureGameTime > ObbEntityPartsPayload.MAX_CAPTURE_AGE_TICKS) {
            STATES.remove(Key.of(entity.level(), entity.getId()), state);
            if (entity instanceof BoneHitboxEntity hitboxEntity) {
                hitboxEntity.kltyton_bedrock_runtime$obbState().clearClientSnapshots();
                if (hitboxEntity instanceof GeoBoneHitboxEntity geo) { geo.kltyton_bedrock_runtime$geoUpdateAnimationStates(List.of()); }
            }
            return Optional.empty();
        }
        return Optional.of(state);
    }

    public static boolean isReporter(Entity entity, ServerPlayer player) {
        return state(entity).map(value -> value.reporter.equals(player.getUUID())).orElse(false);
    }

    public static void tick(net.minecraft.server.MinecraftServer server) {
        for (Map.Entry<Key, EntityState> entry : List.copyOf(STATES.entrySet())) {
            EntityState value = entry.getValue();
            Key currentKey = Key.of(value.entity.level(), value.entity.getId());
            if (!entry.getKey().equals(currentKey)) {
                STATES.remove(entry.getKey(), value);
                if (!STATES.containsKey(currentKey) && value.entity instanceof GeoBoneHitboxEntity geo) {
                    geo.kltyton_bedrock_runtime$geoUpdateAnimationStates(List.of());
                }
            } else { state(value.entity); }
        }
    }

    public static void clear() {
        for (EntityState value : STATES.values()) {
            if (value.entity instanceof BoneHitboxEntity hitbox) { hitbox.kltyton_bedrock_runtime$obbState().clearClientSnapshots(); }
            if (value.entity instanceof GeoBoneHitboxEntity geo) { geo.kltyton_bedrock_runtime$geoUpdateAnimationState(GeoObbAnimationState.NONE); }
        }
        STATES.clear();
    }

    public record ServerObbPart(ObbBoneState bone, ObbGeometry geometry) {
    }

    public record PartMotion(long snapshotGameTime, Vec3 movement) {
    }

    private static Optional<ServerObbPart> findPart(List<ServerObbPart> parts, ObbBoneKey boneKey) {
        return parts.stream().filter(part -> part.bone.key().equals(boneKey)).findFirst();
    }

    private static List<ObbPartGeometrySnapshot> publicParts(List<ServerObbPart> parts) {
        return parts.stream()
                .map(part -> new ObbPartGeometrySnapshot(part.bone, part.geometry))
                .toList();
    }

    private record EntityState(Entity entity, UUID reporter, long gameTime, long captureGameTime, long previousGameTime, GeoObbAnimationState animationState, Map<String, GeoObbAnimationState> animations,
            List<ServerObbPart> parts, List<ServerObbPart> previousParts,
            Vec3 position, Vec3 previousPosition, float yaw, float previousYaw) {
        private EntityState {
            animationState = animationState == null ? GeoObbAnimationState.NONE : animationState;
            parts = List.copyOf(parts);
            previousParts = List.copyOf(previousParts);
        }
    }

    private record Key(String levelId, int entityId) {
        private static Key of(Level level, int entityId) {
            return new Key(level.dimension().toString(), entityId);
        }
    }
}
