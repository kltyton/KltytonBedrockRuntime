package com.kltyton.bedrockruntime.client.selection.service;

import com.kltyton.bedrockruntime.client.selection.model.VisualPartId;
import com.kltyton.bedrockruntime.client.render.capture.ModelBoundsCapture;
import com.kltyton.bedrockruntime.client.skill.keyframe.GeoKeyframeSkillClientQueue;
import com.kltyton.bedrockruntime.client.skill.keyframe.GeoKeyframeSkillClientTicker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.kltyton.bedrockruntime.api.bone.attribute.ObbBoneAttribute;
import com.kltyton.bedrockruntime.api.bone.key.ObbBoneKey;
import com.kltyton.bedrockruntime.api.bone.builtin.ObbBuiltinBones;
import com.kltyton.bedrockruntime.api.bone.collision.ObbCollisionMode;
import com.kltyton.bedrockruntime.api.context.collision.ObbCollisionPhase;
import com.kltyton.bedrockruntime.api.context.collision.ObbContactKind;
import com.kltyton.bedrockruntime.api.entity.BoneHitboxEntity;
import com.kltyton.bedrockruntime.api.geckolib.state.GeoObbAnimationState;
import com.kltyton.bedrockruntime.api.state.runtime.ObbBoneState;
import com.kltyton.bedrockruntime.client.config.BoneHitboxClientOptions;
import com.kltyton.bedrockruntime.client.geometry.bounds.PartBounds;
import com.kltyton.bedrockruntime.network.protocol.BoneHitboxNetworking;
import com.kltyton.bedrockruntime.network.payload.contact.ObbContactReportPayload;
import com.kltyton.bedrockruntime.network.payload.entity.ObbEntityPartsPayload;
import com.kltyton.bedrockruntime.network.payload.selection.ObbPartSelectionPayload;
import com.kltyton.bedrockruntime.geometry.obb.ObbGeometry;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Vector3fc;

/**
 * CN: 客户端模型部位准星选择、调试显示和服务端 OBB 状态同步。
 * EN: Client-side model-part crosshair selection, debug rendering, and server OBB state sync.
 */
public abstract class BonePartSelectionLogic {
    private static final Object LOCK = new Object();
    private static final double PICK_RANGE = 64.0;
    private static final int STATE_SYNC_INTERVAL_TICKS = 1;
    private static final int DAMAGE_FLASH_TICKS = 10;
    private static final int RED_OVERLAY_SUPPRESS_TICKS = 30;
    private static final int RENDER_REFRESH_FLASH_TICKS = 2;
    private static final int CONTACT_HEARTBEAT_TICKS = 5;
    private static final double HARD_SUPPORT_NORMAL_Y = 0.70;
    private static final double SUPPORT_RAY_MARGIN = 0.05;
    private static final double SUPPORT_SKIN = 0.03;
    private static final double SUPPORT_SNAP_DOWN = 0.12;
    private static final double SUPPORT_STEP_UP = 0.60;
    private static final double SUPPORT_JUMP_VELOCITY = 0.10;
    private static final double SUPPORT_SAMPLE_EPSILON = 1.0E-8;
    private static final double HARD_CONTACT_OFFSET = 1.0E-4;
    private static final int HARD_SWEEP_ITERATIONS = 4;
    private static final double MAX_LOCAL_CORRECTION = 4.0;
    private static final double SOFT_PENETRATION_SLOP = 0.01;
    private static final double SOFT_STIFFNESS = 0.10;
    private static final double SOFT_DAMPING = 0.28;
    private static final double MAX_SOFT_IMPULSE = 0.12;
    private static final String CLIENT_ATTACK_SCOPE_PREFIX = "client_attack_contact/";
    private static final Map<VisualPartId, VisualPart> RECORDED_PARTS = new LinkedHashMap<>();
    private static final Map<Integer, List<GeoObbAnimationState>> RECORDED_ANIMATIONS = new HashMap<>();
    private static final Set<Integer> LAST_REPORTED_ENTITIES = new LinkedHashSet<>();
    private static final Map<VisualPartId, Integer> DAMAGE_FLASHES = new HashMap<>();
    private static final Map<String, Integer> RED_OVERLAY_SUPPRESSIONS = new HashMap<>();
    private static final Map<String, VisualPartId> LAST_ATTACK_PARTS = new HashMap<>();
    private static final Map<ContactKey, ContactCandidate> ACTIVE_CONTACTS = new LinkedHashMap<>();
    private static final Map<Integer, CarrierPose> PREVIOUS_CARRIER_POSES = new HashMap<>();
    private static final List<SupportSurface> HARD_SUPPORT_SURFACES = new ArrayList<>();
    private static final List<ObbGeometry> LOCAL_PLAYER_COLLIDERS = new ArrayList<>();
    private static final Map<ClientNormalKey, StableClientNormal> STABLE_PAIR_NORMALS = new HashMap<>();
    private static final ThreadLocal<SupportCandidate> PENDING_MOVEMENT_SUPPORT = new ThreadLocal<>();

    private static SupportAnchor activeSupport;
    private static Vec3 localPlayerColliderSnapshotPosition = Vec3.ZERO;

    private static VisualPartId selectedPart;
    private static VisualPartId lastSyncedPart;
    private static int stateSyncTicks;

    protected BonePartSelectionLogic() {
    }

    public static void clientTick(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null || minecraft.gameRenderer == null) {
            clear();
            return;
        }

        GeoKeyframeSkillClientTicker.tick(minecraft);
        com.kltyton.bedrockruntime.client.render.item.HeldItemCapture.captureLocalPlayer(minecraft);
        Vec3 cameraPosition = ClientSceneAccess.position(ClientSceneAccess.camera(minecraft));
        synchronized (LOCK) {
            RECORDED_PARTS.replaceAll((id, part) -> part.relativeTo(cameraPosition));
        }
        long currentGameTime = minecraft.level.getGameTime();
        STABLE_PAIR_NORMALS.entrySet().removeIf(entry -> currentGameTime - entry.getValue().gameTime() > 3L);

        List<ObbEntityPartsPayload> entityPartsPayloads;
        ObbContactReportPayload contactReport;
        VisualPartId nextSelection = pick(minecraft);
        synchronized (LOCK) {
            if (!Objects.equals(selectedPart, nextSelection)) {
                selectedPart = nextSelection;
                syncSelection(nextSelection);
            }
            contactReport = buildContactReport(minecraft);
            entityPartsPayloads = buildEntityPartsPayloads(minecraft);
            RECORDED_PARTS.clear();
            RECORDED_ANIMATIONS.clear();
            DAMAGE_FLASHES.replaceAll((id, ticks) -> ticks - 1);
            DAMAGE_FLASHES.values().removeIf(ticks -> ticks <= 0);
            RED_OVERLAY_SUPPRESSIONS.replaceAll((ownerKey, ticks) -> ticks - 1);
            RED_OVERLAY_SUPPRESSIONS.values().removeIf(ticks -> ticks <= 0);
            LAST_ATTACK_PARTS.keySet().removeIf(ownerKey -> !RED_OVERLAY_SUPPRESSIONS.containsKey(ownerKey));
        }
        entityPartsPayloads.forEach(BoneHitboxNetworking::sendClientEntityParts);
        GeoKeyframeSkillClientQueue.flush();
        if (contactReport != null) {
            BoneHitboxNetworking.sendClientContactReport(contactReport);
        }
    }

    public static void recordAnimations(int entityId, List<GeoObbAnimationState> animations) {
        synchronized (LOCK) { RECORDED_ANIMATIONS.put(entityId, List.copyOf(animations)); }
    }

    public static boolean record(VisualPartId id, PartBounds bounds) {
        return record(id, bounds, GeoObbAnimationState.NONE);
    }

    public static boolean record(VisualPartId id, PartBounds bounds, GeoObbAnimationState animationState) {
        ModelBoundsCapture.record(id, bounds.renderBounds());
        ObbBoneState bone = registeredBone(id);
        if (bone == null) {
            return false;
        }

        if (bounds.renderBounds().getXsize() <= 1.0E-6 || bounds.renderBounds().getYsize() <= 1.0E-6 || bounds.renderBounds().getZsize() <= 1.0E-6) {
            return false;
        }

        synchronized (LOCK) {
            RECORDED_PARTS.put(id, new VisualPart(id, bounds, bone,
                    animationState == null ? GeoObbAnimationState.NONE : animationState,
                    ClientSceneAccess.position(ClientSceneAccess.camera(Minecraft.getInstance()))));
        }
        return true;
    }

    /**
     * CN: 在原版 {@code Entity.move} 的 {@code collide(delta)} 结果上追加连续 OBB 求解；返回值仍由原版移动流程统一应用。
     * EN: Adds continuous OBB resolution to vanilla {@code Entity.move}'s {@code collide(delta)} result; vanilla still applies the final movement.
     */
    public static Vec3 resolveLocalPlayerMovement(Entity entity, Vec3 requestedMovement, Vec3 vanillaMovement) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(entity instanceof Player player) || minecraft.player != player || minecraft.level == null) {
            return vanillaMovement;
        }

        synchronized (LOCK) {
            PENDING_MOVEMENT_SUPPORT.remove();
            if (requestedMovement.y() > SUPPORT_JUMP_VELOCITY) {
                activeSupport = null;
            }
            if (HARD_SUPPORT_SURFACES.isEmpty() || LOCAL_PLAYER_COLLIDERS.isEmpty()) {
                return vanillaMovement;
            }

            Vec3 resolved = vanillaMovement;
            SupportCandidate support = null;
            if (requestedMovement.y() <= SUPPORT_JUMP_VELOCITY) {
                boolean followingSupport = activeSupport != null;
                double currentY = player.getY();
                support = findCompoundSupport(
                        player,
                        player.getX() + resolved.x(),
                        player.getZ() + resolved.z(),
                        Math.min(currentY, currentY + resolved.y()) - SUPPORT_SKIN,
                        currentY + (followingSupport ? SUPPORT_STEP_UP : SUPPORT_SKIN),
                        followingSupport ? activeSupport.carrierEntityId() : null);
                if (support != null) {
                    double surfaceMovement = support.height() - currentY;
                    boolean crossesSurface = resolved.y() <= 0.0
                            && currentY + resolved.y() <= support.height() + SUPPORT_SKIN
                            && currentY >= support.height() - SUPPORT_SKIN;
                    boolean followsCurrentCarrier = followingSupport
                            && support.carrierEntityId() == activeSupport.carrierEntityId()
                            && surfaceMovement >= -SUPPORT_SNAP_DOWN
                            && surfaceMovement <= SUPPORT_STEP_UP;
                    if (crossesSurface || followsCurrentCarrier) {
                        resolved = new Vec3(resolved.x(), surfaceMovement, resolved.z());
                        PENDING_MOVEMENT_SUPPORT.set(support);
                    } else {
                        support = null;
                    }
                }
            }
            Vec3 clipped = clipHardCompoundMovement(player, resolved, support);
            return clipped;
        }
    }

    /**
     * CN: 仅补充原版无法从自定义 OBB 得知的落地标志；位置和速度均由原版 move 流程处理。
     * EN: Only supplies grounded flags vanilla cannot infer from custom OBBs; position and velocity remain owned by vanilla move.
     */
    public static void finishLocalPlayerMovement(Entity entity, Vec3 movement) {
        SupportCandidate support = PENDING_MOVEMENT_SUPPORT.get();
        PENDING_MOVEMENT_SUPPORT.remove();
        Minecraft minecraft = Minecraft.getInstance();
        if (support == null || !(entity instanceof Player player) || minecraft.player != player) {
            return;
        }

        synchronized (LOCK) {
            activeSupport = support.anchor();
            player.verticalCollision = true;
            player.verticalCollisionBelow = true;
            ClientSceneAccess.ground(player, movement);
            player.resetFallDistance();
        }
    }

    public static boolean isSelected(VisualPartId id) {
        synchronized (LOCK) {
            return id.equals(selectedPart);
        }
    }

    public static boolean shouldRenderOutline(VisualPartId id) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean renderAllDebugObbs = BoneHitboxClientOptions.shouldRenderDebugObbs(minecraft);
        synchronized (LOCK) {
            return renderAllDebugObbs || id.equals(selectedPart) && BoneHitboxClientOptions.shouldRenderCrosshairObb(minecraft);
        }
    }

    public static boolean isDamageFlashing(VisualPartId id) {
        synchronized (LOCK) {
            return DAMAGE_FLASHES.containsKey(id);
        }
    }



    public static boolean shouldSuppressFullEntityRed(Entity entity) {
        if (!shouldReplaceFullRed(entity)) {
            return false;
        }
        return shouldSuppressFullEntityRed(VisualPartId.ownerKey(entity));
    }

    public static void recordAttack(Entity target) {
        String targetOwner = VisualPartId.ownerKey(target);
        synchronized (LOCK) {
            syncSelection(selectedPart, true);
            if (selectedPart != null && selectedPart.ownerKey().equals(targetOwner)) {
                DAMAGE_FLASHES.put(selectedPart, DAMAGE_FLASH_TICKS);
                RED_OVERLAY_SUPPRESSIONS.put(targetOwner, RED_OVERLAY_SUPPRESS_TICKS);
                LAST_ATTACK_PARTS.put(targetOwner, selectedPart);
            }
        }
    }

    public static void syncCurrentSelection() {
        synchronized (LOCK) {
            syncSelection(selectedPart, true);
        }
    }

    private static VisualPartId pick(Minecraft minecraft) {
        Camera camera = ClientSceneAccess.camera(minecraft);
        Vector3fc forward = ClientSceneAccess.forward(camera);
        Vec3 from = Vec3.ZERO;
        Vec3 to = new Vec3(forward.x() * PICK_RANGE, forward.y() * PICK_RANGE, forward.z() * PICK_RANGE);
        VisualPartId best = null;
        double bestDistance = Double.MAX_VALUE;

        synchronized (LOCK) {
            for (VisualPart part : RECORDED_PARTS.values()) {
                double distance = part.bounds().clipDistanceSqr(from, to);
                if (!Double.isFinite(distance)) {
                    continue;
                }

                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = part.id();
                }
            }
        }

        return best;
    }

    private static void clear() {
        synchronized (LOCK) {
            selectedPart = null;
            RECORDED_PARTS.clear();
            RECORDED_ANIMATIONS.clear();
            LAST_REPORTED_ENTITIES.clear();
            DAMAGE_FLASHES.clear();
            RED_OVERLAY_SUPPRESSIONS.clear();
            LAST_ATTACK_PARTS.clear();
            ACTIVE_CONTACTS.values().forEach(ContactCandidate::endTemporaryHurt);
            ACTIVE_CONTACTS.clear();
            PREVIOUS_CARRIER_POSES.clear();
            HARD_SUPPORT_SURFACES.clear();
            LOCAL_PLAYER_COLLIDERS.clear();
            STABLE_PAIR_NORMALS.clear();
            localPlayerColliderSnapshotPosition = Vec3.ZERO;
            PENDING_MOVEMENT_SUPPORT.remove();
            activeSupport = null;
            GeoKeyframeSkillClientQueue.clear();
            GeoKeyframeSkillClientTicker.clear();
            ModelBoundsCapture.clear();
            syncSelection(null);
        }
    }

    private static void syncSelection(VisualPartId part) {
        syncSelection(part, false);
    }

    private static void syncSelection(VisualPartId part, boolean force) {
        if (!force && Objects.equals(lastSyncedPart, part)) {
            return;
        }

        lastSyncedPart = part;
        if (part == null || part.ownerEntityId() < 0) {
            BoneHitboxNetworking.sendClientSelection(ObbPartSelectionPayload.clear());
            return;
        }

        BoneHitboxNetworking.sendClientSelection(new ObbPartSelectionPayload(
                part.ownerEntityId(),
                part.source(),
                part.partName(),
                part.cubeIndex()));
    }

    private static List<ObbEntityPartsPayload> buildEntityPartsPayloads(Minecraft minecraft) {
        stateSyncTicks++;
        List<SyntheticPart> syntheticParts = localPlayerFallbackParts(minecraft);
        if (stateSyncTicks % STATE_SYNC_INTERVAL_TICKS != 0) {
            return List.of();
        }

        Camera camera = ClientSceneAccess.camera(minecraft);
        Vec3 cameraPosition = ClientSceneAccess.position(camera);
        Map<Integer, List<ObbEntityPartsPayload.Part>> partsByEntity = new HashMap<>();
        Map<Integer, GeoObbAnimationState> animationStateByEntity = new HashMap<>();
        for (VisualPart part : RECORDED_PARTS.values()) {
            int entityId = part.id().ownerEntityId();
            if (entityId < 0) {
                continue;
            }

            if (part.animationState().active()) {
                animationStateByEntity.put(entityId, part.animationState());
            }
            PartBounds bounds = part.bounds();
            AABB localBounds = bounds.localBounds();
            AABB worldBounds = bounds.worldBounds(cameraPosition);
            Matrix4f localToWorld = bounds.localToWorld(cameraPosition);
            Matrix4f worldToLocal = bounds.worldToLocal(cameraPosition);
            partsByEntity.computeIfAbsent(entityId, ignored -> new ArrayList<>())
                    .add(new ObbEntityPartsPayload.Part(
                            part.id().source(),
                            part.id().partName(),
                            part.id().cubeIndex(),
                            localBounds,
                            worldBounds,
                            localToWorld,
                            worldToLocal,
                            part.bone().attributes(),
                            part.bone().collisionMode()));
        }
        for (SyntheticPart part : syntheticParts) {
            ObbGeometry geometry = part.geometry();
            partsByEntity.computeIfAbsent(part.entityId(), ignored -> new ArrayList<>())
                    .add(new ObbEntityPartsPayload.Part(
                            part.bone().key().source(),
                            part.bone().key().name(),
                            part.bone().key().cubeIndex(),
                            geometry.localBounds(),
                            geometry.worldBounds(),
                            geometry.localToWorld(),
                            geometry.worldToLocal(),
                            part.bone().attributes(),
                            part.bone().collisionMode()));
        }

        RECORDED_ANIMATIONS.forEach((entityId, states) -> {
            partsByEntity.computeIfAbsent(entityId, ignored -> new ArrayList<>());
            animationStateByEntity.put(entityId, states.stream().filter(GeoObbAnimationState::active).findFirst()
                    .orElse(GeoObbAnimationState.NONE));
        });
        List<ObbEntityPartsPayload> payloads = new ArrayList<>(partsByEntity.size());
        partsByEntity.forEach((entityId, parts) -> {
            GeoObbAnimationState primary = animationStateByEntity.getOrDefault(entityId, GeoObbAnimationState.NONE);
            payloads.add(new ObbEntityPartsPayload(entityId, primary, parts,
                    RECORDED_ANIMATIONS.getOrDefault(entityId, primary.active() ? List.of(primary) : List.of())));
        });
        for (int entityId : LAST_REPORTED_ENTITIES) {
            if (!partsByEntity.containsKey(entityId)) {
                payloads.add(new ObbEntityPartsPayload(entityId, GeoObbAnimationState.NONE, List.of()));
            }
        }
        LAST_REPORTED_ENTITIES.clear();
        LAST_REPORTED_ENTITIES.addAll(partsByEntity.keySet());
        return payloads;
    }

    private static ObbContactReportPayload buildContactReport(Minecraft minecraft) {
        List<SyntheticPart> syntheticParts = localPlayerFallbackParts(minecraft);
        if (RECORDED_PARTS.isEmpty() && syntheticParts.isEmpty()) {
            if (ACTIVE_CONTACTS.isEmpty()) {
                return null;
            }
            List<ObbContactReportPayload.Contact> ended = ACTIVE_CONTACTS.values().stream()
                    .limit(ObbContactReportPayload.MAX_CONTACTS)
                    .map(candidate -> candidate.toPayload(ObbCollisionPhase.END))
                    .toList();
            ACTIVE_CONTACTS.values().forEach(ContactCandidate::endTemporaryHurt);
            ACTIVE_CONTACTS.clear();
            return new ObbContactReportPayload(
                    minecraft.level.getGameTime(), minecraft.player.getDeltaMovement(), ended);
        }

        Vec3 cameraPosition = ClientSceneAccess.position(ClientSceneAccess.camera(minecraft));
        Map<Integer, MutableEntityParts> grouped = new LinkedHashMap<>();
        for (VisualPart part : RECORDED_PARTS.values()) {
            int entityId = part.id().ownerEntityId();
            if (entityId < 0) {
                continue;
            }
            ObbGeometry geometry = part.bounds().geometry(cameraPosition);
            grouped.computeIfAbsent(entityId, MutableEntityParts::new)
                    .add(new GeometryPart(part.bone(), geometry));
        }
        for (SyntheticPart part : syntheticParts) {
            grouped.computeIfAbsent(part.entityId(), MutableEntityParts::new)
                    .add(new GeometryPart(part.bone(), part.geometry()));
        }
        addSyntheticProjectiles(minecraft, grouped);
        refreshHardSupportSurfaces(minecraft, grouped);

        List<MutableEntityParts> entities = new ArrayList<>(grouped.values());
        entities.removeIf(entity -> entity.bounds == null);
        entities.sort(Comparator.comparingDouble((MutableEntityParts entity) -> entity.bounds.minX)
                .thenComparingInt(MutableEntityParts::entityId));
        Map<ContactKey, ContactCandidate> current = new LinkedHashMap<>();
        for (int i = 0; i < entities.size() && current.size() < ObbContactReportPayload.MAX_CONTACTS; i++) {
            MutableEntityParts first = entities.get(i);
            for (int j = i + 1; j < entities.size() && current.size() < ObbContactReportPayload.MAX_CONTACTS; j++) {
                MutableEntityParts second = entities.get(j);
                if (second.bounds.minX > first.bounds.maxX) { break; }
                if (first.bounds == null || second.bounds == null || !first.bounds.intersects(second.bounds)) {
                    continue;
                }
                collectContacts(first, second, current);
            }
        }

        Map<Integer, CarrierPose> currentCarrierPoses = collectCarrierPoses(minecraft, grouped.keySet());
        applyLocalPhysicalResponses(minecraft, current, currentCarrierPoses);
        PREVIOUS_CARRIER_POSES.clear();
        PREVIOUS_CARRIER_POSES.putAll(currentCarrierPoses);

        List<ObbContactReportPayload.Contact> reports = new ArrayList<>();
        for (Map.Entry<ContactKey, ContactCandidate> entry : current.entrySet()) {
            if (!ACTIVE_CONTACTS.containsKey(entry.getKey())) {
                entry.getValue().beginTemporaryHurt();
                reports.add(entry.getValue().toPayload(ObbCollisionPhase.BEGIN));
            } else if (entry.getValue().kind() == ObbContactKind.COLLISION
                    || stateSyncTicks % CONTACT_HEARTBEAT_TICKS == 0) {
                reports.add(entry.getValue().toPayload(ObbCollisionPhase.STAY));
            }
        }
        for (Map.Entry<ContactKey, ContactCandidate> entry : ACTIVE_CONTACTS.entrySet()) {
            if (!current.containsKey(entry.getKey())) {
                reports.add(entry.getValue().toPayload(ObbCollisionPhase.END));
                entry.getValue().endTemporaryHurt();
            }
        }
        ACTIVE_CONTACTS.clear();
        ACTIVE_CONTACTS.putAll(current);

        if (reports.isEmpty()) {
            return null;
        }
        if (reports.size() > ObbContactReportPayload.MAX_CONTACTS) {
            reports = new ArrayList<>(reports.subList(0, ObbContactReportPayload.MAX_CONTACTS));
        }
        return new ObbContactReportPayload(
                minecraft.level.getGameTime(), minecraft.player.getDeltaMovement(), reports);
    }

    private static void addSyntheticProjectiles(Minecraft minecraft, Map<Integer, MutableEntityParts> grouped) {
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof Projectile projectile) || !(projectile instanceof BoneHitboxEntity hitboxEntity)) {
                continue;
            }
            ObbBoneKey key = ObbBuiltinBones.projectile(projectile);
            ObbBoneState bone = hitboxEntity.kltyton_bedrock_runtime$obbState().resolve(key).orElse(null);
            if (bone == null) {
                continue;
            }
            AABB worldBounds = projectile.getBoundingBox();
            ObbGeometry geometry = new ObbGeometry(
                    worldBounds,
                    worldBounds,
                    new Matrix4f(),
                    new Matrix4f());
            grouped.computeIfAbsent(projectile.getId(), MutableEntityParts::new)
                    .add(new GeometryPart(bone, geometry));
        }
    }

    /**
     * CN: 第一人称不会提交本地玩家完整模型；仅在该情况下按原版玩家模型比例生成六个旋转 OBB，供物理接触使用。
     * EN: First person does not submit the local player's full model; only then, six rotated OBBs using vanilla player-model proportions are generated for physics.
     */
    private static List<SyntheticPart> localPlayerFallbackParts(Minecraft minecraft) {
        Player player = minecraft.player;
        if (!(player instanceof BoneHitboxEntity hitboxEntity)
                || RECORDED_PARTS.values().stream().anyMatch(part -> part.id().ownerEntityId() == player.getId()
                        && !part.id().source().equals(ObbBuiltinBones.HELD_ITEM_SOURCE))) {
            return List.of();
        }

        float widthScale = player.getBbWidth() / 0.6F;
        float heightScale = player.getBbHeight() / 1.8F;
        Matrix4f transform = new Matrix4f()
                .translation((float) player.getX(), (float) player.getY(), (float) player.getZ())
                .rotateY((float) Math.toRadians(-player.getYRot()))
                .scale(widthScale, heightScale, widthScale);
        List<SyntheticPart> parts = new ArrayList<>(6);
        addPlayerFallbackPart(parts, hitboxEntity, player, transform, "head",
                new AABB(-0.25, 1.50, -0.25, 0.25, 2.00, 0.25));
        addPlayerFallbackPart(parts, hitboxEntity, player, transform, "body",
                new AABB(-0.25, 0.75, -0.125, 0.25, 1.50, 0.125));
        addPlayerFallbackPart(parts, hitboxEntity, player, transform, "left_arm",
                new AABB(0.25, 0.75, -0.125, 0.50, 1.50, 0.125));
        addPlayerFallbackPart(parts, hitboxEntity, player, transform, "right_arm",
                new AABB(-0.50, 0.75, -0.125, -0.25, 1.50, 0.125));
        addPlayerFallbackPart(parts, hitboxEntity, player, transform, "left_leg",
                new AABB(0.0, 0.0, -0.125, 0.25, 0.75, 0.125));
        addPlayerFallbackPart(parts, hitboxEntity, player, transform, "right_leg",
                new AABB(-0.25, 0.0, -0.125, 0.0, 0.75, 0.125));
        return parts;
    }

    private static void addPlayerFallbackPart(List<SyntheticPart> parts, BoneHitboxEntity hitboxEntity, Player player,
            Matrix4f transform, String name, AABB localBounds) {
        ObbBoneState bone = hitboxEntity.kltyton_bedrock_runtime$obbState()
                .resolve(new ObbBoneKey("vanilla", name, 0))
                .orElse(null);
        if (bone != null) {
            parts.add(new SyntheticPart(player.getId(), bone, ObbGeometry.fromLocalBounds(localBounds, transform)));
        }
    }

    private static void collectContacts(MutableEntityParts first, MutableEntityParts second,
            Map<ContactKey, ContactCandidate> contacts) {
        for (GeometryPart firstPart : first.parts) {
            for (GeometryPart secondPart : second.parts) {
                if (!firstPart.geometry.worldBounds().intersects(secondPart.geometry.worldBounds())) { continue; }
                var overlap = firstPart.geometry.overlap(secondPart.geometry);
                if (overlap.isEmpty()) {
                    continue;
                }
                ObbGeometry.Overlap exactOverlap = overlap.get();
                ObbCollisionMode mode = physicalMode(firstPart.bone, secondPart.bone);
                if (firstPart.bone.hasAttribute(ObbBoneAttribute.ATTACK)) {
                    putContact(contacts, new ContactCandidate(
                            ObbContactKind.ATTACK,
                            first.entityId,
                            firstPart.bone,
                            second.entityId,
                            secondPart.bone,
                            mode,
                            exactOverlap.correction()));
                }
                if (secondPart.bone.hasAttribute(ObbBoneAttribute.ATTACK)) {
                    putContact(contacts, new ContactCandidate(
                            ObbContactKind.ATTACK,
                            second.entityId,
                            secondPart.bone,
                            first.entityId,
                            firstPart.bone,
                            mode,
                            exactOverlap.correction().scale(-1.0)));
                }
                putContact(contacts, new ContactCandidate(
                        ObbContactKind.COLLISION,
                        first.entityId,
                        firstPart.bone,
                        second.entityId,
                        secondPart.bone,
                        mode,
                        exactOverlap.correction()));
                if (contacts.size() >= ObbContactReportPayload.MAX_CONTACTS) {
                    return;
                }
            }
        }
    }

    private static void putContact(Map<ContactKey, ContactCandidate> contacts, ContactCandidate candidate) {
        ContactCandidate canonical = candidate.canonical();
        if (contacts.size() >= ObbContactReportPayload.MAX_CONTACTS && !contacts.containsKey(canonical.key())) {
            return;
        }
        contacts.putIfAbsent(canonical.key(), canonical);
    }

    private static ObbCollisionMode physicalMode(ObbBoneState first, ObbBoneState second) {
        boolean firstPhysical = first.hasAttribute(ObbBoneAttribute.COLLISION);
        boolean secondPhysical = second.hasAttribute(ObbBoneAttribute.COLLISION);
        if (firstPhysical && !secondPhysical) {
            return first.collisionMode();
        }
        if (secondPhysical && !firstPhysical) {
            return second.collisionMode();
        }
        return firstPhysical
                ? ObbCollisionMode.strongest(first.collisionMode(), second.collisionMode())
                : ObbCollisionMode.NONE;
    }

    private static Map<Integer, CarrierPose> collectCarrierPoses(Minecraft minecraft, Set<Integer> entityIds) {
        Map<Integer, CarrierPose> poses = new HashMap<>();
        for (int entityId : entityIds) {
            Entity entity = minecraft.level.getEntity(entityId);
            if (entity != null) {
                poses.put(entityId, new CarrierPose(entity.position(), entity.getYRot()));
            }
        }
        return poses;
    }

    private static void refreshHardSupportSurfaces(Minecraft minecraft, Map<Integer, MutableEntityParts> grouped) {
        HARD_SUPPORT_SURFACES.clear();
        LOCAL_PLAYER_COLLIDERS.clear();
        Player player = minecraft.player;
        if (player == null) {
            activeSupport = null;
            return;
        }
        localPlayerColliderSnapshotPosition = player.position();
        grouped.forEach((entityId, entityParts) -> {
            if (entityId == player.getId()) {
                entityParts.parts.forEach(part -> LOCAL_PLAYER_COLLIDERS.add(part.geometry));
                return;
            }
            Entity carrier = minecraft.level.getEntity(entityId);
            if (carrier == null) {
                return;
            }
            for (GeometryPart part : entityParts.parts) {
                if (part.bone.hasAttribute(ObbBoneAttribute.COLLISION)
                        && part.bone.collisionMode() == ObbCollisionMode.HARD) {
                    HARD_SUPPORT_SURFACES.add(new SupportSurface(
                            entityId,
                            part.bone,
                            part.geometry,
                            carrier.position()));
                }
            }
        });
        if (activeSupport != null && HARD_SUPPORT_SURFACES.stream()
                .noneMatch(surface -> surface.carrierEntityId() == activeSupport.carrierEntityId())) {
            activeSupport = null;
        }
    }

    private static Vec3 clipHardCompoundMovement(Player player, Vec3 movement, SupportCandidate support) {
        Vec3 remaining = removeInwardMovementFromInitialOverlaps(player, movement);
        Vec3 resolved = Vec3.ZERO;
        for (int iteration = 0; iteration < HARD_SWEEP_ITERATIONS && remaining.lengthSqr() > 1.0E-12; iteration++) {
            HardSweepContact contact = findEarliestHardSweep(player, resolved, remaining, support);
            if (contact == null) {
                resolved = resolved.add(remaining);
                break;
            }

            double safeTime = contact.hit().time();
            double movementLength = remaining.length();
            if (movementLength > HARD_CONTACT_OFFSET) {
                safeTime = Math.max(0.0, safeTime - HARD_CONTACT_OFFSET / movementLength);
            }
            Vec3 step = remaining.scale(safeTime);
            resolved = resolved.add(step);
            Vec3 leftover = remaining.scale(1.0 - safeTime);
            Vec3 contactNormal = stableContactNormal(player, contact.surface(), contact.hit().normal(), resolved);
            double inward = leftover.dot(contactNormal);
            if (inward < 0.0) {
                leftover = leftover.subtract(contactNormal.scale(inward));
            }
            if (step.lengthSqr() <= 1.0E-14 && leftover.distanceToSqr(remaining) <= 1.0E-14) {
                break;
            }
            remaining = leftover;
        }

        if (support != null) {
            double supportMovement = support.height() - player.getY();
            if (resolved.y() > supportMovement) {
                resolved = new Vec3(resolved.x(), supportMovement, resolved.z());
            }
        }
        resolved = constrainResolvedDirection(resolved, movement, support != null);
        return finiteMovement(resolved, movement);
    }

    private static Vec3 removeInwardMovementFromInitialOverlaps(Player player, Vec3 movement) {
        Vec3 resolved = movement;
        Vec3 playerRootOffset = player.position().subtract(localPlayerColliderSnapshotPosition);
        for (ObbGeometry playerGeometrySnapshot : LOCAL_PLAYER_COLLIDERS) {
            ObbGeometry playerGeometry = playerGeometrySnapshot.translated(playerRootOffset);
            for (SupportSurface surface : HARD_SUPPORT_SURFACES) {
                ObbGeometry carrierGeometry = currentCarrierGeometry(surface);
                ObbGeometry.Overlap overlap = playerGeometry.overlap(carrierGeometry).orElse(null);
                if (overlap == null || overlap.depth() <= HARD_CONTACT_OFFSET
                        || isUpwardSupportCorrection(overlap.correction())) {
                    continue;
                }
                Vec3 normal = stableContactNormal(player, surface, overlap.normal(), Vec3.ZERO);
                double inward = resolved.dot(normal);
                if (inward < 0.0) {
                    resolved = resolved.subtract(normal.scale(inward));
                }
            }
        }
        return resolved;
    }

    private static Vec3 stableContactNormal(Player player, SupportSurface surface, Vec3 rawNormal,
            Vec3 resolvedOffset) {
        if (rawNormal.lengthSqr() <= 1.0E-12) {
            return Vec3.ZERO;
        }
        Vec3 normalized = rawNormal.normalize();
        if (Math.abs(normalized.y()) >= HARD_SUPPORT_NORMAL_Y) {
            return stabilizePairNormal(player, entityById(surface.carrierEntityId()),
                    normalized, ObbCollisionMode.HARD);
        }

        Vec3 horizontal = new Vec3(normalized.x(), 0.0, normalized.z());
        Entity carrier = entityById(surface.carrierEntityId());
        if (carrier == null) {
            Vec3 fallback = horizontal.lengthSqr() <= 1.0E-12 ? Vec3.ZERO : horizontal.normalize();
            return stabilizePairNormal(player, null, fallback, ObbCollisionMode.HARD);
        }
        Vec3 rootOutward = player.position()
                .add(resolvedOffset)
                .subtract(carrier.position());
        rootOutward = new Vec3(rootOutward.x(), 0.0, rootOutward.z());
        if (rootOutward.lengthSqr() <= 1.0E-12) {
            Vec3 fallback = horizontal.lengthSqr() <= 1.0E-12 ? Vec3.ZERO : horizontal.normalize();
            return stabilizePairNormal(player, carrier, fallback, ObbCollisionMode.HARD);
        }
        rootOutward = rootOutward.normalize();
        if (horizontal.lengthSqr() <= 1.0E-12) {
            return stabilizePairNormal(player, carrier, rootOutward, ObbCollisionMode.HARD);
        }
        horizontal = horizontal.normalize();
        Vec3 candidate = horizontal.dot(rootOutward) < 0.25 ? rootOutward : horizontal;
        return stabilizePairNormal(player, carrier, candidate, ObbCollisionMode.HARD);
    }

    private static Vec3 constrainResolvedDirection(Vec3 resolved, Vec3 requested, boolean hasSupport) {
        Vec3 requestedHorizontal = new Vec3(requested.x(), 0.0, requested.z());
        Vec3 resolvedHorizontal = new Vec3(resolved.x(), 0.0, resolved.z());
        double requestedLength = requestedHorizontal.length();
        if (requestedLength <= 1.0E-12) {
            resolvedHorizontal = Vec3.ZERO;
        } else {
            Vec3 requestedDirection = requestedHorizontal.scale(1.0 / requestedLength);
            double backwards = resolvedHorizontal.dot(requestedDirection);
            if (backwards < 0.0) {
                resolvedHorizontal = resolvedHorizontal.subtract(requestedDirection.scale(backwards));
            }
            double resolvedLength = resolvedHorizontal.length();
            if (resolvedLength > requestedLength) {
                resolvedHorizontal = resolvedHorizontal.scale(requestedLength / resolvedLength);
            }
        }

        double y = resolved.y();
        if (!hasSupport && (requested.y() == 0.0 || Math.signum(y) != Math.signum(requested.y()))) {
            y = 0.0;
        }
        return new Vec3(resolvedHorizontal.x(), y, resolvedHorizontal.z());
    }

    private static HardSweepContact findEarliestHardSweep(Player player, Vec3 resolved, Vec3 remaining,
            SupportCandidate support) {
        Vec3 playerRootOffset = player.position()
                .subtract(localPlayerColliderSnapshotPosition)
                .add(resolved);
        HardSweepContact earliest = null;
        for (ObbGeometry playerGeometrySnapshot : LOCAL_PLAYER_COLLIDERS) {
            ObbGeometry playerGeometry = playerGeometrySnapshot.translated(playerRootOffset);
            for (SupportSurface surface : HARD_SUPPORT_SURFACES) {
                Entity carrier = entityById(surface.carrierEntityId());
                if (carrier == null || player.isPassengerOfSameVehicle(carrier)) {
                    continue;
                }
                ObbGeometry.SweepHit hit = playerGeometry
                        .sweepAgainst(currentCarrierGeometry(surface), remaining)
                        .orElse(null);
                if (hit != null && support != null
                        && surface.carrierEntityId() == support.carrierEntityId()
                        && surface.bone().key().equals(support.surface().bone().key())
                        && hit.normal().y() >= HARD_SUPPORT_NORMAL_Y) {
                    // CN: 已由中心支撑平面解析的可行走顶面不再参与实体 OBB sweep；侧面和顶棚仍正常阻挡。
                    // EN: A walkable top face already solved by the center support plane is excluded from body sweep; sides and ceilings still block.
                    continue;
                }
                if (hit != null && (earliest == null || hit.time() < earliest.hit().time())) {
                    earliest = new HardSweepContact(surface, hit);
                }
            }
        }
        return earliest;
    }

    private static ObbGeometry currentCarrierGeometry(SupportSurface surface) {
        Entity carrier = entityById(surface.carrierEntityId());
        return carrier == null
                ? surface.geometry()
                : surface.geometry().translated(carrier.position().subtract(surface.snapshotRootPosition()));
    }

    /**
     * CN: 本地玩家必须在客户端立即解穿透；竖直支撑由复合 OBB 表面统一求解，SAT 仅处理侧面和顶面。
     * EN: The local player must be depenetrated immediately on the client; a compound OBB surface solves vertical support while SAT handles sides and ceilings.
     */
    private static void applyLocalPhysicalResponses(Minecraft minecraft,
            Map<ContactKey, ContactCandidate> contacts,
            Map<Integer, CarrierPose> currentCarrierPoses) {
        Player player = minecraft.player;
        if (player == null) {
            return;
        }

        Map<Integer, List<ContactCandidate>> byOtherEntity = new LinkedHashMap<>();
        for (ContactCandidate candidate : contacts.values()) {
            if (candidate.kind != ObbContactKind.COLLISION) {
                continue;
            }
            int otherEntityId;
            if (candidate.firstEntityId == player.getId()) {
                otherEntityId = candidate.secondEntityId;
            } else if (candidate.secondEntityId == player.getId()) {
                otherEntityId = candidate.firstEntityId;
            } else {
                continue;
            }
            if (candidate.collisionMode != ObbCollisionMode.NONE) {
                byOtherEntity.computeIfAbsent(otherEntityId, ignored -> new ArrayList<>()).add(candidate);
            }
        }

        SupportCandidate support = null;
        if (player.getDeltaMovement().y() <= SUPPORT_JUMP_VELOCITY) {
            double penetrationAllowance = 0.35
                    + Math.min(1.0, Math.abs(Math.min(0.0, player.getDeltaMovement().y())) * 2.0);
            support = findCompoundSupport(
                    player,
                    player.getX(),
                    player.getZ(),
                    player.getY() - SUPPORT_SNAP_DOWN,
                    player.getY() + penetrationAllowance,
                    activeSupport == null ? null : activeSupport.carrierEntityId());
        }
        if (support != null) {
            support = applyLocalSupport(player, minecraft, support, currentCarrierPoses);
        } else {
            activeSupport = null;
        }

        for (Map.Entry<Integer, List<ContactCandidate>> entry : byOtherEntity.entrySet()) {
            Entity other = minecraft.level.getEntity(entry.getKey());
            if (other == null || other == player || player.isPassengerOfSameVehicle(other)) {
                continue;
            }
            resolveLocalPlayerPair(player, other, entry.getValue());
        }
    }

    private static SupportCandidate applyLocalSupport(Player player, Minecraft minecraft, SupportCandidate support,
            Map<Integer, CarrierPose> currentCarrierPoses) {
        Entity carrier = minecraft.level.getEntity(support.carrierEntityId());
        Vec3 carryingMovement = Vec3.ZERO;
        if (carrier != null && support.surface().bone().carriesEntities()) {
            carryingMovement = applyLocalCarrying(player, carrier, currentCarrierPoses);
            SupportCandidate movedSupport = findCompoundSupport(
                    player,
                    player.getX(),
                    player.getZ(),
                    player.getY() - SUPPORT_SNAP_DOWN,
                    player.getY() + SUPPORT_STEP_UP,
                    support.carrierEntityId());
            if (movedSupport != null && movedSupport.carrierEntityId() == support.carrierEntityId()) {
                support = movedSupport;
            }
        }

        player.verticalCollision = true;
        player.verticalCollisionBelow = true;
        ClientSceneAccess.ground(player, carryingMovement);
        player.resetFallDistance();
        activeSupport = support.anchor();
        return support;
    }

    private static void resolveLocalPlayerPair(Player player, Entity other, List<ContactCandidate> contacts) {
        ObbCollisionMode mode = contacts.stream()
                .map(ContactCandidate::collisionMode)
                .max(Comparator.comparingInt(Enum::ordinal))
                .orElse(ObbCollisionMode.NONE);
        if (mode != ObbCollisionMode.SOFT) {
            return;
        }
        ContactCandidate side = contacts.stream()
                .filter(candidate -> candidate.collisionMode == mode)
                .filter(candidate -> !isUpwardSupportCorrection(correctionFor(candidate, player.getId())))
                .filter(candidate -> horizontalLengthSqr(correctionFor(candidate, player.getId())) > 1.0E-10)
                .max(Comparator.comparingDouble(candidate -> horizontalLengthSqr(correctionFor(candidate, player.getId()))))
                .orElse(null);

        if (side != null) {
            Vec3 correction = correctionFor(side, player.getId());
            double depth = Math.sqrt(horizontalLengthSqr(correction));
            Vec3 rawNormal = depth <= 1.0E-10
                    ? Vec3.ZERO
                    : new Vec3(correction.x() / depth, 0.0, correction.z() / depth);
            Vec3 stableNormal = stabilizePairNormal(player, other, rawNormal, ObbCollisionMode.SOFT);
            applyLocalSoftResponse(player, other, stableNormal.scale(depth));
        }
    }

    private static void applyLocalSoftResponse(Player player, Entity other, Vec3 correction) {
        Vec3 horizontal = new Vec3(correction.x(), 0.0, correction.z());
        double depth = horizontal.length();
        if (depth <= 1.0E-6) {
            return;
        }
        Vec3 normal = horizontal.scale(1.0 / depth);
        Vec3 relativeVelocity = player.getDeltaMovement().subtract(other.getDeltaMovement());
        double closingSpeed = Math.max(0.0, -relativeVelocity.dot(normal));
        double penetration = Math.max(0.0, depth - SOFT_PENETRATION_SLOP);
        double separationSpeed = Math.min(MAX_SOFT_IMPULSE,
                penetration * SOFT_STIFFNESS + closingSpeed * SOFT_DAMPING);
        double playerInverseMass = localInverseMass(player);
        double otherInverseMass = localInverseMass(other);
        double inverseMassSum = playerInverseMass + otherInverseMass;
        double playerDeltaSpeed = inverseMassSum <= 1.0E-10
                ? 0.0
                : separationSpeed * playerInverseMass / inverseMassSum;
        if (playerDeltaSpeed <= 1.0E-6) {
            return;
        }
        player.push(normal.x() * playerDeltaSpeed, 0.0, normal.z() * playerDeltaSpeed);
    }

    private static Vec3 stabilizePairNormal(Player player, Entity other, Vec3 candidate, ObbCollisionMode mode) {
        if (candidate.lengthSqr() <= 1.0E-12) {
            return Vec3.ZERO;
        }
        Vec3 normal = candidate.normalize();
        ClientNormalKey key = new ClientNormalKey(other == null ? -1 : other.getId(), mode);
        long gameTime = player.level().getGameTime();
        StableClientNormal previous = STABLE_PAIR_NORMALS.get(key);
        if (previous != null && gameTime - previous.gameTime() <= 2L) {
            Vec3 previousNormal = previous.normal();
            boolean previousSupport = Math.abs(previousNormal.y()) >= HARD_SUPPORT_NORMAL_Y;
            boolean currentSupport = Math.abs(normal.y()) >= HARD_SUPPORT_NORMAL_Y;
            if (previousSupport == currentSupport && previousNormal.dot(normal) >= 0.35) {
                normal = previousNormal.scale(0.72).add(normal.scale(0.28)).normalize();
            }
        }
        STABLE_PAIR_NORMALS.put(key, new StableClientNormal(normal, gameTime));
        return normal;
    }

    private static double localInverseMass(Entity entity) {
        if (!entity.isPushable()) {
            return 0.0;
        }
        double volume = Math.max(0.5,
                entity.getBbWidth() * entity.getBbWidth() * entity.getBbHeight());
        return 1.0 / volume;
    }

    private static boolean isUpwardSupportCorrection(Vec3 correction) {
        double length = correction.length();
        return length > 1.0E-10
                && correction.y() > 0.0
                && correction.y() / length >= HARD_SUPPORT_NORMAL_Y;
    }

    private static SupportCandidate findCompoundSupport(Player player, double x, double z,
            double minimumY, double maximumY, Integer preferredCarrierEntityId) {
        if (HARD_SUPPORT_SURFACES.isEmpty() || minimumY > maximumY) {
            return null;
        }
        double radius = Math.max(0.05, player.getBbWidth() * 0.40);
        double[][] sampleOffsets = {
                { 0.0, 0.0 },
                { radius, radius },
                { radius, -radius },
                { -radius, radius },
                { -radius, -radius }
        };
        SupportCandidate best = null;
        for (SupportSurface surface : HARD_SUPPORT_SURFACES) {
            ObbGeometry geometry = currentCarrierGeometry(surface);
            AABB bounds = geometry.worldBounds();
            if (x + radius < bounds.minX || x - radius > bounds.maxX
                    || z + radius < bounds.minZ || z - radius > bounds.maxZ
                    || maximumY < bounds.minY - SUPPORT_RAY_MARGIN
                    || minimumY > bounds.maxY + SUPPORT_RAY_MARGIN) {
                continue;
            }
            double rayFromY = Math.max(maximumY, bounds.maxY) + SUPPORT_RAY_MARGIN;
            double rayToY = Math.min(minimumY, bounds.minY) - SUPPORT_RAY_MARGIN;
            SupportCandidate surfaceCandidate = supportCandidateAt(
                    surface, geometry, x, z, x, z,
                    rayFromY, rayToY, minimumY, maximumY, 0.0);
            if (surfaceCandidate == null) {
                for (int i = 1; i < sampleOffsets.length; i++) {
                    double[] offset = sampleOffsets[i];
                    double sampleDistanceSqr = offset[0] * offset[0] + offset[1] * offset[1];
                    SupportCandidate candidate = supportCandidateAt(
                            surface,
                            geometry,
                            x,
                            z,
                            x + offset[0],
                            z + offset[1],
                            rayFromY,
                            rayToY,
                            minimumY,
                            maximumY,
                            sampleDistanceSqr);
                    if (candidate != null && (surfaceCandidate == null
                            || candidate.sampleDistanceSqr() < surfaceCandidate.sampleDistanceSqr() - SUPPORT_SAMPLE_EPSILON
                            || Math.abs(candidate.sampleDistanceSqr() - surfaceCandidate.sampleDistanceSqr())
                                    <= SUPPORT_SAMPLE_EPSILON
                                    && Math.abs(candidate.height() - player.getY())
                                            < Math.abs(surfaceCandidate.height() - player.getY()))) {
                        surfaceCandidate = candidate;
                    }
                }
            }
            if (surfaceCandidate != null && (best == null
                    || surfaceCandidate.height() > best.height() + 1.0E-5
                    || Math.abs(surfaceCandidate.height() - best.height()) <= 1.0E-5
                            && preferredCarrierEntityId != null
                            && surfaceCandidate.carrierEntityId() == preferredCarrierEntityId
                            && best.carrierEntityId() != preferredCarrierEntityId)) {
                best = surfaceCandidate;
            }
        }
        return best;
    }

    /**
     * CN: 边缘采样只确认脚部仍接触该面；最终高度始终投影回玩家中心，避免斜面角点把玩家额外抬高。
     * EN: Edge samples only prove foot contact; height is always projected back to the player center so uphill corners cannot lift the player.
     */
    private static SupportCandidate supportCandidateAt(SupportSurface surface, ObbGeometry geometry,
            double centerX, double centerZ, double sampleX, double sampleZ,
            double rayFromY, double rayToY, double minimumY, double maximumY,
            double sampleDistanceSqr) {
        ObbGeometry.SurfaceHit hit = geometry
                .verticalSurfaceAt(sampleX, sampleZ, rayFromY, rayToY)
                .orElse(null);
        if (hit == null || hit.normal().y() < HARD_SUPPORT_NORMAL_Y) {
            return null;
        }

        double height = supportPlaneHeightAt(hit, centerX, centerZ);
        if (!Double.isFinite(height)
                || height < minimumY - SUPPORT_SKIN
                || height > maximumY + SUPPORT_SKIN) {
            return null;
        }
        return new SupportCandidate(surface, height, hit.normal(), sampleDistanceSqr);
    }

    private static double supportPlaneHeightAt(ObbGeometry.SurfaceHit hit, double x, double z) {
        Vec3 normal = hit.normal();
        if (normal.y() <= 1.0E-8) {
            return Double.NaN;
        }
        Vec3 point = hit.position();
        return point.y() - (normal.x() * (x - point.x()) + normal.z() * (z - point.z())) / normal.y();
    }

    /**
     * CN: 承载只传递实体根位姿，不传递待机/行走骨骼动画，避免把玩家当作骨骼动画的一部分弹起。
     * EN: Carrying transfers only the entity root pose, not idle/walk bone animation, preventing animation-driven launches.
     */
    private static Vec3 applyLocalCarrying(Player player, Entity carrier,
            Map<Integer, CarrierPose> currentCarrierPoses) {
        CarrierPose previous = PREVIOUS_CARRIER_POSES.get(carrier.getId());
        CarrierPose current = currentCarrierPoses.get(carrier.getId());
        if (previous == null || current == null) {
            return Vec3.ZERO;
        }
        float yawDelta = Mth.wrapDegrees(current.yaw - previous.yaw);
        Vec3 relative = player.position().subtract(previous.position);
        Vec3 rotated = relative.yRot((float) Math.toRadians(-yawDelta));
        Vec3 target = current.position.add(rotated);
        Vec3 motion = finiteLocalCorrection(target.subtract(player.position()));
        if (motion.lengthSqr() > 1.0E-10) {
            player.move(MoverType.SHULKER_BOX, motion);
        }
        return motion;
    }

    private static Vec3 correctionFor(ContactCandidate candidate, int entityId) {
        return candidate.firstEntityId == entityId ? candidate.correction : candidate.correction.scale(-1.0);
    }

    private static double horizontalLengthSqr(Vec3 value) {
        return value.x() * value.x() + value.z() * value.z();
    }

    private static Vec3 finiteLocalCorrection(Vec3 value) {
        if (!Double.isFinite(value.x()) || !Double.isFinite(value.y()) || !Double.isFinite(value.z())) {
            return Vec3.ZERO;
        }
        double length = value.length();
        return length > MAX_LOCAL_CORRECTION ? value.scale(MAX_LOCAL_CORRECTION / length) : value;
    }

    private static Vec3 finiteMovement(Vec3 value, Vec3 fallback) {
        return Double.isFinite(value.x()) && Double.isFinite(value.y()) && Double.isFinite(value.z())
                ? value
                : fallback;
    }

    private static ObbBoneState registeredBone(VisualPartId id) {
        Entity entity = entityById(id.ownerEntityId());
        if (!(entity instanceof BoneHitboxEntity hitboxEntity)) {
            return null;
        }
        return hitboxEntity.kltyton_bedrock_runtime$obbState()
                .resolve(new ObbBoneKey(id.source(), id.partName(), id.cubeIndex()))
                .orElse(null);
    }



    protected static boolean shouldReplaceFullRed(Entity entity) {
        return entity instanceof BoneHitboxEntity hitboxEntity
                && hitboxEntity.kltyton_bedrock_runtime$onlyHitPartTurnsRed();
    }

    protected static Entity entityById(int entityId) {
        Minecraft minecraft = Minecraft.getInstance();
        return entityId >= 0 && minecraft.level != null ? minecraft.level.getEntity(entityId) : null;
    }

    protected static boolean shouldSuppressFullEntityRed(String ownerKey) {
        synchronized (LOCK) {
            if (RED_OVERLAY_SUPPRESSIONS.containsKey(ownerKey)) {
                VisualPartId hitPart = LAST_ATTACK_PARTS.get(ownerKey);
                if (hitPart != null) {
                    DAMAGE_FLASHES.merge(hitPart, RENDER_REFRESH_FLASH_TICKS, Math::max);
                }
                return true;
            }

            for (VisualPartId id : DAMAGE_FLASHES.keySet()) {
                if (id.ownerKey().equals(ownerKey)) {
                    return true;
                }
            }
        }
        return false;
    }

    private record VisualPart(VisualPartId id, PartBounds bounds, ObbBoneState bone,
            GeoObbAnimationState animationState, Vec3 captureCamera) {
        private VisualPart relativeTo(Vec3 camera) {
            return captureCamera.equals(camera) ? this : new VisualPart(id,
                    bounds.translateRenderSpace(captureCamera.subtract(camera)), bone, animationState, camera);
        }
    }

    private record GeometryPart(ObbBoneState bone, ObbGeometry geometry) {
    }

    private record SyntheticPart(int entityId, ObbBoneState bone, ObbGeometry geometry) {
    }

    private static final class MutableEntityParts {
        private final int entityId;
        private final List<GeometryPart> parts = new ArrayList<>();
        private AABB bounds;

        private MutableEntityParts(int entityId) {
            this.entityId = entityId;
        }

        private int entityId() {
            return entityId;
        }

        private void add(GeometryPart part) {
            parts.add(part);
            AABB partBounds = part.geometry.worldBounds();
            bounds = bounds == null ? partBounds : new AABB(
                    Math.min(bounds.minX, partBounds.minX),
                    Math.min(bounds.minY, partBounds.minY),
                    Math.min(bounds.minZ, partBounds.minZ),
                    Math.max(bounds.maxX, partBounds.maxX),
                    Math.max(bounds.maxY, partBounds.maxY),
                    Math.max(bounds.maxZ, partBounds.maxZ));
        }
    }

    private record ContactKey(ObbContactKind kind, int firstEntityId, ObbBoneKey firstBone,
            int secondEntityId, ObbBoneKey secondBone) {
    }

    private record CarrierPose(Vec3 position, float yaw) {
    }

    private record SupportSurface(int carrierEntityId, ObbBoneState bone, ObbGeometry geometry,
            Vec3 snapshotRootPosition) {
    }

    private record HardSweepContact(SupportSurface surface, ObbGeometry.SweepHit hit) {
    }

    private record SupportCandidate(SupportSurface surface, double height, Vec3 normal, double sampleDistanceSqr) {
        private int carrierEntityId() {
            return surface.carrierEntityId();
        }

        private SupportAnchor anchor() {
            return new SupportAnchor(carrierEntityId(), height);
        }
    }

    private record SupportAnchor(int carrierEntityId, double height) {
    }

    private record ClientNormalKey(int otherEntityId, ObbCollisionMode mode) {
    }

    private record StableClientNormal(Vec3 normal, long gameTime) {
    }

    private record ContactCandidate(ObbContactKind kind, int firstEntityId, ObbBoneState firstBone,
            int secondEntityId, ObbBoneState secondBone, ObbCollisionMode collisionMode, Vec3 correction) {

        private ContactCandidate canonical() {
            if (kind != ObbContactKind.COLLISION
                    || firstEntityId < secondEntityId
                    || firstEntityId == secondEntityId && firstBone.key().compareTo(secondBone.key()) <= 0) {
                return this;
            }
            return new ContactCandidate(
                    kind,
                    secondEntityId,
                    secondBone,
                    firstEntityId,
                    firstBone,
                    collisionMode,
                    correction.scale(-1.0));
        }

        private ContactKey key() {
            return new ContactKey(kind, firstEntityId, firstBone.key(), secondEntityId, secondBone.key());
        }

        private ObbContactReportPayload.Contact toPayload(ObbCollisionPhase phase) {
            return new ObbContactReportPayload.Contact(
                    kind,
                    phase,
                    firstEntityId,
                    firstBone.key(),
                    secondEntityId,
                    secondBone.key(),
                    collisionMode,
                    correction);
        }

        private void beginTemporaryHurt() {
            if (kind == ObbContactKind.ATTACK) {
                secondBone.setTemporaryHurtBox(temporaryScope(), true);
            }
        }

        private void endTemporaryHurt() {
            if (kind == ObbContactKind.ATTACK) {
                secondBone.clearTemporaryAttributes(temporaryScope());
            }
        }

        private String temporaryScope() {
            return CLIENT_ATTACK_SCOPE_PREFIX
                    + firstEntityId + "/" + firstBone.key().displayName()
                    + "/" + secondEntityId + "/" + secondBone.key().displayName();
        }
    }
}
