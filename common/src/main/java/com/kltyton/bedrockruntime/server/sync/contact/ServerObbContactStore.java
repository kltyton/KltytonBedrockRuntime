package com.kltyton.bedrockruntime.server.sync.contact;

import com.kltyton.bedrockruntime.server.sync.snapshot.ServerObbStore;
import com.kltyton.bedrockruntime.server.network.ObbReportValidation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.kltyton.bedrockruntime.api.bone.attribute.ObbBoneAttribute;
import com.kltyton.bedrockruntime.api.bone.key.ObbBoneKey;
import com.kltyton.bedrockruntime.api.bone.collision.ObbCollisionMode;
import com.kltyton.bedrockruntime.api.context.collision.ObbCollisionContext;
import com.kltyton.bedrockruntime.api.context.collision.ObbCollisionPhase;
import com.kltyton.bedrockruntime.api.context.collision.ObbContactKind;
import com.kltyton.bedrockruntime.api.entity.BoneHitboxEntity;
import com.kltyton.bedrockruntime.api.state.runtime.ObbBoneState;
import com.kltyton.bedrockruntime.network.payload.contact.ObbContactReportPayload;
import com.kltyton.bedrockruntime.server.collision.contact.BoneHitboxCollisionHooks;
import com.kltyton.bedrockruntime.server.collision.solver.ObbSequentialImpulseSolver;
import com.kltyton.bedrockruntime.server.combat.attack.BoneHitboxContactAttackHooks;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

/**
 * CN: 接收可信客户端 OBB 接触摘要，并按实体/骨骼对规范化、去重和执行独立 OBB 物理。
 * EN: Receives trusted client OBB contacts, canonicalizes/deduplicates bone pairs, and applies independent OBB physics.
 */
public final class ServerObbContactStore {
    private static final long REPORTER_TIMEOUT_TICKS = 20L;
    private static final long ATTACK_TIMEOUT_TICKS = 10L;
    private static final double MAX_CORRECTION = 4.0;
    private static final double MIN_SUPPORT_NORMAL_Y = 0.55;
    private static final double MIN_SUPPORT_ROOT_OFFSET = 0.20;
    private static final double NORMAL_PERSISTENCE_DOT = 0.35;
    private static final double NORMAL_BLEND_PREVIOUS = 0.72;
    private static final String SERVER_ATTACK_SCOPE_PREFIX = "server_attack_contact/";

    private static final Map<CollisionKey, ActiveCollision> ACTIVE_COLLISIONS = new LinkedHashMap<>();
    private static final Map<AttackKey, ActiveAttack> ACTIVE_ATTACKS = new LinkedHashMap<>();
    private static final Map<CarryKey, Long> LAST_CARRY_SNAPSHOT = new HashMap<>();
    private static final Map<UUID, ReportedPlayerMovement> REPORTED_PLAYER_MOVEMENTS = new HashMap<>();
    private static final Set<PhysicalPairKey> DIRTY_PHYSICAL_PAIRS = new LinkedHashSet<>();
    private static final Map<PhysicalPairKey, PersistentManifold> PHYSICAL_MANIFOLDS = new HashMap<>();

    private ServerObbContactStore() {
    }

    public static synchronized void update(ServerPlayer reporter, ObbContactReportPayload payload) {
        ServerLevel level = (ServerLevel) reporter.level();
        long gameTime = level.getServer().getTickCount();
        cleanup(gameTime);
        REPORTED_PLAYER_MOVEMENTS.put(
                reporter.getUUID(),
                new ReportedPlayerMovement(finiteCorrection(payload.reporterMovement()), level.getServer().getTickCount()));
        for (ObbContactReportPayload.Contact contact : payload.contacts()) {
            Entity first = level.getEntity(contact.firstEntityId());
            Entity second = level.getEntity(contact.secondEntityId());
            if (first == null || second == null || first == second
                    || !(first instanceof BoneHitboxEntity firstHitbox)
                    || !(second instanceof BoneHitboxEntity secondHitbox)) {
                continue;
            }

            if (!ObbReportValidation.canReport(reporter, first) || !ObbReportValidation.canReport(reporter, second)
                    || firstHitbox.kltyton_bedrock_runtime$obbState().resolve(contact.firstBone()).isEmpty()
                    || secondHitbox.kltyton_bedrock_runtime$obbState().resolve(contact.secondBone()).isEmpty()) { continue; }
            if (contact.phase() != ObbCollisionPhase.END && !validContact(first, second, contact)) { continue; }

            if (contact.kind() == ObbContactKind.ATTACK) {
                handleAttack(reporter, gameTime, payload.observedGameTime(),
                        first, firstHitbox, second, secondHitbox, contact);
            } else {
                handleCollision(reporter, gameTime, payload.observedGameTime(),
                        first, firstHitbox, second, secondHitbox, contact);
            }
        }
    }

    /** CN: 把一次原版攻击关联到仍在接触的攻击 OBB。EN: Correlates a vanilla attack with an active attacking OBB contact. */
    public static synchronized Optional<AttackContact> findAttack(Entity attacker, Entity target) {
        long now = attacker.level().getServer().getTickCount();
        cleanup(now);
        return ACTIVE_ATTACKS.entrySet().stream()
                .filter(entry -> entry.getKey().levelId.equals(attacker.level().dimension().toString())
                        && entry.getKey().attacker.equals(attacker.getUUID())
                        && entry.getKey().target.equals(target.getUUID()))
                .map(Map.Entry::getValue)
                .filter(active -> now - active.lastSeenTick <= ATTACK_TIMEOUT_TICKS)
                .max((first, second) -> Long.compare(first.lastSeenTick, second.lastSeenTick))
                .map(active -> new AttackContact(active.attackBone, active.hurtBone));
    }

    /**
     * CN: 在服务端 tick 末统一清理接触并解析物理响应，保证同一实体对每 tick 最多求解一次。
     * EN: Cleans contacts and resolves physics at the end of the server tick, so each entity pair is solved at most once per tick.
     */
    public static synchronized void tick(MinecraftServer server) {
        cleanup(server.getTickCount());
        applyPhysicalResponses(server.getTickCount());
        Set<UUID> onlinePlayers = new LinkedHashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            onlinePlayers.add(player.getUUID());
        }
        REPORTED_PLAYER_MOVEMENTS.keySet().retainAll(onlinePlayers);
    }

    private static void handleAttack(ServerPlayer reporter, long gameTime, long observedGameTime,
            Entity attacker, BoneHitboxEntity attackerHitbox, Entity target, BoneHitboxEntity targetHitbox,
            ObbContactReportPayload.Contact contact) {
        ObbBoneState attackBone = resolveTrusted(attackerHitbox, contact.firstBone());
        ObbBoneState hurtBone = resolveTrusted(targetHitbox, contact.secondBone());
        AttackKey key = new AttackKey(
                attacker.level().dimension().toString(),
                attacker.getUUID(),
                attackBone.key(),
                target.getUUID(),
                hurtBone.key());

        if (contact.phase() == ObbCollisionPhase.END) {
            ActiveAttack active = ACTIVE_ATTACKS.get(key);
            if (active == null) {
                return;
            }
            active.reporters.remove(reporter.getUUID());
            if (active.reporters.isEmpty()) {
                ACTIVE_ATTACKS.remove(key);
                finishAttack(active, observedGameTime);
            }
            return;
        }

        ActiveAttack active = ACTIVE_ATTACKS.get(key);
        boolean newContact = active == null;
        if (newContact) {
            active = new ActiveAttack(
                    key,
                    attacker,
                    attackBone,
                    target,
                    hurtBone,
                    contact.collisionMode(),
                    finiteCorrection(contact.correction()),
                    observedGameTime);
            ACTIVE_ATTACKS.put(key, active);
            active.activateTemporaryAttributes();
        } else {
            active.update(contact.collisionMode(), finiteCorrection(contact.correction()), observedGameTime);
        }
        active.reporters.put(reporter.getUUID(), gameTime);
        active.lastSeenTick = gameTime;

        if (newContact) {
            active.lastEventServerTick = gameTime;
            BoneHitboxContactAttackHooks.handleContact(active.context(ObbCollisionPhase.BEGIN));
        } else if (contact.phase() == ObbCollisionPhase.STAY
                && active.lastEventServerTick != gameTime) {
            active.lastEventServerTick = gameTime;
            BoneHitboxContactAttackHooks.handleContact(active.context(ObbCollisionPhase.STAY));
        }
    }

    private static void handleCollision(ServerPlayer reporter, long gameTime, long observedGameTime,
            Entity first, BoneHitboxEntity firstHitbox, Entity second, BoneHitboxEntity secondHitbox,
            ObbContactReportPayload.Contact contact) {
        ObbBoneState firstBone = resolveTrusted(firstHitbox, contact.firstBone());
        ObbBoneState secondBone = resolveTrusted(secondHitbox, contact.secondBone());
        CanonicalCollision canonical = CanonicalCollision.of(
                first,
                firstBone,
                second,
                secondBone,
                contact.collisionMode(),
                finiteCorrection(contact.correction()),
                observedGameTime);
        CollisionKey key = canonical.key();

        if (contact.phase() == ObbCollisionPhase.END) {
            ActiveCollision active = ACTIVE_COLLISIONS.get(key);
            if (active == null) {
                return;
            }
            DIRTY_PHYSICAL_PAIRS.add(active.physicalPairKey());
            active.reporters.remove(reporter.getUUID());
            if (active.reporters.isEmpty()) {
                ACTIVE_COLLISIONS.remove(key);
                BoneHitboxCollisionHooks.handleClientContact(active.context(ObbCollisionPhase.END));
            }
            return;
        }

        ActiveCollision active = ACTIVE_COLLISIONS.get(key);
        boolean newContact = active == null;
        if (newContact) {
            active = new ActiveCollision(canonical);
            ACTIVE_COLLISIONS.put(key, active);
        } else {
            active.update(canonical);
        }
        active.reporters.put(reporter.getUUID(), gameTime);
        active.lastSeenTick = gameTime;
        DIRTY_PHYSICAL_PAIRS.add(active.physicalPairKey());

        if (newContact) {
            active.lastEventServerTick = gameTime;
            BoneHitboxCollisionHooks.handleClientContact(active.context(ObbCollisionPhase.BEGIN));
        } else if (contact.phase() == ObbCollisionPhase.STAY
                && active.lastEventServerTick != gameTime) {
            active.lastEventServerTick = gameTime;
            BoneHitboxCollisionHooks.handleClientContact(active.context(ObbCollisionPhase.STAY));
        }

    }

    /**
     * CN: 每个实体对、每 tick 构建一个持续接触流形。多个骨骼接触被聚合为稳定法线后只求解一次。
     * EN: Builds one persistent manifold per entity pair per tick. Multiple bone contacts are aggregated into one stable normal and solved once.
     */
    private static void applyPhysicalResponses(long serverTick) {
        if (DIRTY_PHYSICAL_PAIRS.isEmpty()) {
            return;
        }

        Set<PhysicalPairKey> dirtyPairs = Set.copyOf(DIRTY_PHYSICAL_PAIRS);
        DIRTY_PHYSICAL_PAIRS.clear();
        Map<PhysicalPairKey, List<ActiveCollision>> contactsByPair = new LinkedHashMap<>();
        for (ActiveCollision active : ACTIVE_COLLISIONS.values()) {
            if (!dirtyPairs.contains(active.physicalPairKey())) {
                continue;
            }
            contactsByPair.computeIfAbsent(active.physicalPairKey(), ignored -> new ArrayList<>()).add(active);
        }

        for (Map.Entry<PhysicalPairKey, List<ActiveCollision>> entry : contactsByPair.entrySet()) {
            PersistentManifold manifold = PHYSICAL_MANIFOLDS.computeIfAbsent(
                    entry.getKey(), ignored -> new PersistentManifold());
            PhysicalContact contact = aggregatePhysicalContact(entry.getValue(), manifold);
            if (contact == null) {
                continue;
            }
            manifold.normal = contact.normal;
            manifold.lastSolvedTick = serverTick;
            applyPhysicalResponse(contact, manifold, serverTick);
        }
    }

    private static PhysicalContact aggregatePhysicalContact(List<ActiveCollision> contacts,
            PersistentManifold manifold) {
        ObbCollisionMode mode = contacts.stream()
                .map(active -> active.collisionMode)
                .max(Comparator.comparingInt(Enum::ordinal))
                .orElse(ObbCollisionMode.NONE);
        if (mode == ObbCollisionMode.NONE) {
            return null;
        }
        List<ActiveCollision> candidates = contacts.stream()
                .filter(active -> active.collisionMode == mode && active.correction.lengthSqr() > 1.0E-10)
                .filter(active -> mode != ObbCollisionMode.SOFT || horizontalLengthSqr(active.correction) > 1.0E-10)
                .toList();
        if (candidates.isEmpty()) {
            return null;
        }

        boolean hasSupport = mode == ObbCollisionMode.HARD
                && candidates.stream().anyMatch(active -> isVerticalSupport(active, active.correction));
        List<ActiveCollision> normalCandidates = hasSupport
                ? candidates.stream().filter(active -> isVerticalSupport(active, active.correction)).toList()
                : candidates;
        Vec3 previous = manifold.normal.lengthSqr() <= 1.0E-10 ? Vec3.ZERO : manifold.normal.normalize();
        Vec3 weightedNormal = Vec3.ZERO;
        for (ActiveCollision candidate : normalCandidates) {
            Vec3 sourceCorrection = mode == ObbCollisionMode.SOFT
                    ? new Vec3(candidate.correction.x(), 0.0, candidate.correction.z())
                    : candidate.correction;
            Vec3 normal = entityOrientedNormal(candidate, sourceCorrection.normalize());
            if (previous != Vec3.ZERO && normal.dot(previous) < -NORMAL_PERSISTENCE_DOT) {
                normal = normal.scale(-1.0);
            }
            if (previous == Vec3.ZERO || normal.dot(previous) >= -0.05) {
                weightedNormal = weightedNormal.add(normal.scale(Math.max(1.0E-4, candidate.correction.length())));
            }
        }
        ActiveCollision fallback = normalCandidates.stream()
                .max(Comparator.comparingInt(ServerObbContactStore::contactStabilityPriority)
                        .thenComparingDouble(active -> active.correction.lengthSqr()))
                .orElse(normalCandidates.get(0));
        Vec3 fallbackCorrection = mode == ObbCollisionMode.SOFT
                ? new Vec3(fallback.correction.x(), 0.0, fallback.correction.z())
                : fallback.correction;
        Vec3 normal = weightedNormal.lengthSqr() <= 1.0E-10
                ? entityOrientedNormal(fallback, fallbackCorrection.normalize())
                : weightedNormal.normalize();
        if (previous != Vec3.ZERO && normal.dot(previous) >= NORMAL_PERSISTENCE_DOT) {
            normal = previous.scale(NORMAL_BLEND_PREVIOUS)
                    .add(normal.scale(1.0 - NORMAL_BLEND_PREVIOUS))
                    .normalize();
        }

        Vec3 finalNormal = normal;
        double depth = normalCandidates.stream()
                .mapToDouble(active -> {
                    Vec3 correction = mode == ObbCollisionMode.SOFT
                            ? new Vec3(active.correction.x(), 0.0, active.correction.z())
                            : active.correction;
                    return Math.max(0.0, correction.dot(finalNormal));
                })
                .max()
                .orElse(0.0);
        if (depth <= 1.0E-6) {
            depth = normalCandidates.stream().mapToDouble(active -> mode == ObbCollisionMode.SOFT
                    ? Math.sqrt(horizontalLengthSqr(active.correction))
                    : active.correction.length()).max().orElse(0.0);
        }
        double finalDepth = depth;
        ActiveCollision representative = normalCandidates.stream()
                .max(Comparator.comparingDouble(active -> {
                    Vec3 candidateCorrection = mode == ObbCollisionMode.SOFT
                            ? new Vec3(active.correction.x(), 0.0, active.correction.z())
                            : active.correction;
                    return Math.max(candidateCorrection.dot(finalNormal), candidateCorrection.length() * 0.1);
                }))
                .orElse(fallback);
        boolean verticalSupport = mode == ObbCollisionMode.HARD
                && isVerticalSupport(representative, normal.scale(finalDepth));
        return new PhysicalContact(representative, mode, normal, depth, verticalSupport);
    }

    private static Vec3 entityOrientedNormal(ActiveCollision active, Vec3 normal) {
        if (Math.abs(normal.y()) >= MIN_SUPPORT_NORMAL_Y) {
            double rootDeltaY = active.first.getY() - active.second.getY();
            return rootDeltaY != 0.0 && Math.signum(normal.y()) != Math.signum(rootDeltaY)
                    ? normal.scale(-1.0)
                    : normal;
        }
        Vec3 rootDelta = active.first.position().subtract(active.second.position());
        rootDelta = new Vec3(rootDelta.x(), 0.0, rootDelta.z());
        Vec3 horizontal = new Vec3(normal.x(), 0.0, normal.z());
        if (rootDelta.lengthSqr() > 1.0E-10 && horizontal.lengthSqr() > 1.0E-10
                && horizontal.dot(rootDelta) < 0.0) {
            return normal.scale(-1.0);
        }
        return normal;
    }

    private static int contactStabilityPriority(ActiveCollision active) {
        if (isVerticalSupport(active, active.correction)) {
            return 2;
        }
        return horizontalLengthSqr(active.correction) > 1.0E-10 ? 1 : 0;
    }

    private static ObbBoneState resolveTrusted(BoneHitboxEntity entity, ObbBoneKey key) {
        return entity.kltyton_bedrock_runtime$obbState().resolve(key).orElseThrow();
    }

    private static boolean validContact(Entity first, Entity second, ObbContactReportPayload.Contact contact) {
        Optional<ServerObbStore.ServerObbPart> a = ServerObbStore.part(first, contact.firstBone());
        Optional<ServerObbStore.ServerObbPart> b = ServerObbStore.part(second, contact.secondBone());
        if (a.isEmpty() || b.isEmpty()
                || !a.get().geometry().worldBounds().inflate(1.0).intersects(b.get().geometry().worldBounds())) { return false; }
        if (contact.kind() == ObbContactKind.ATTACK) {
            return a.get().bone().isAttackBox();
        }
        return contact.collisionMode() == ObbCollisionMode.strongest(a.get().bone().collisionMode(), b.get().bone().collisionMode());
    }

    public static synchronized void clear() {
        List<ActiveAttack> attacks = List.copyOf(ACTIVE_ATTACKS.values());
        List<ActiveCollision> collisions = List.copyOf(ACTIVE_COLLISIONS.values());
        ACTIVE_ATTACKS.clear();
        ACTIVE_COLLISIONS.clear();
        attacks.forEach(active -> finishAttack(active, active.observedGameTime));
        collisions.forEach(active -> BoneHitboxCollisionHooks.handleClientContact(active.context(ObbCollisionPhase.END)));
        LAST_CARRY_SNAPSHOT.clear();
        REPORTED_PLAYER_MOVEMENTS.clear();
        DIRTY_PHYSICAL_PAIRS.clear();
        PHYSICAL_MANIFOLDS.clear();
    }

    private static void applyPhysicalResponse(PhysicalContact contact, PersistentManifold manifold, long serverTick) {
        ActiveCollision active = contact.representative;
        if (contact.collisionMode == ObbCollisionMode.NONE || contact.depth <= 1.0E-6) {
            return;
        }
        if (contact.collisionMode == ObbCollisionMode.SOFT) {
            applySoftResponse(contact, manifold);
            return;
        }

        Vec3 correction = contact.normal.scale(contact.depth);

        if (active.first instanceof ServerPlayer && active.second instanceof ServerPlayer) {
            return;
        }
        if (active.first instanceof ServerPlayer firstPlayer) {
            pushHardColliderFromPlayer(firstPlayer, active.second, correction, serverTick);
            manifold.normalImpulse = 0.0;
            return;
        }
        if (active.second instanceof ServerPlayer secondPlayer) {
            pushHardColliderFromPlayer(secondPlayer, active.first, correction.scale(-1.0), serverTick);
            manifold.normalImpulse = 0.0;
            return;
        }

        double firstInverseMass = inverseMass(active.first);
        double secondInverseMass = inverseMass(active.second);
        if (contact.verticalSupport) {
            if (contact.normal.y() > 0.0 && active.secondBone.hasAttribute(ObbBoneAttribute.COLLISION)) {
                secondInverseMass = 0.0;
            } else if (contact.normal.y() < 0.0 && active.firstBone.hasAttribute(ObbBoneAttribute.COLLISION)) {
                firstInverseMass = 0.0;
            }
        }
        ObbSequentialImpulseSolver.Solution solution = ObbSequentialImpulseSolver.solveHard(
                new ObbSequentialImpulseSolver.Contact(
                        contact.normal,
                        contact.depth,
                        active.first.getDeltaMovement(),
                        active.second.getDeltaMovement(),
                        firstInverseMass,
                        secondInverseMass,
                        contact.verticalSupport),
                manifold.normalImpulse);
        manifold.normalImpulse = solution.normalImpulse();
        applyVelocityDelta(active.first, solution.firstVelocityDelta());
        applyVelocityDelta(active.second, solution.secondVelocityDelta());
        applyPositionCorrection(active.first, solution.firstPositionCorrection());
        applyPositionCorrection(active.second, solution.secondPositionCorrection());
        applyCarrying(active, serverTick);
    }

    private static void applySoftResponse(PhysicalContact contact, PersistentManifold manifold) {
        ActiveCollision active = contact.representative;
        ObbSequentialImpulseSolver.Solution solution = ObbSequentialImpulseSolver.solveSoft(
                new ObbSequentialImpulseSolver.Contact(
                        contact.normal,
                        contact.depth,
                        active.first.getDeltaMovement(),
                        active.second.getDeltaMovement(),
                        inverseMass(active.first),
                        inverseMass(active.second),
                        false));
        manifold.normalImpulse = solution.normalImpulse();
        applyVelocityDelta(active.first, solution.firstVelocityDelta());
        applyVelocityDelta(active.second, solution.secondVelocityDelta());
    }

    private static double inverseMass(Entity entity) {
        if (!entity.isPushable()) {
            return 0.0;
        }
        double volume = Math.max(0.5,
                entity.getBbWidth() * entity.getBbWidth() * entity.getBbHeight());
        return 1.0 / volume;
    }

    private static void applyVelocityDelta(Entity entity, Vec3 delta) {
        if (entity instanceof ServerPlayer || !entity.isPushable() || delta.lengthSqr() <= 1.0E-12) {
            return;
        }
        pushEntity(entity, delta);
    }

    private static void applyPositionCorrection(Entity entity, Vec3 correction) {
        if (entity instanceof ServerPlayer || !entity.isPushable() || correction.lengthSqr() <= 1.0E-12) {
            return;
        }
        moveOutOfHardObb(entity, correction);
    }

    private static void pushEntity(Entity entity, Vec3 push) {
        if (!entity.isPushable()) {
            return;
        }
        entity.push(push.x(), push.y(), push.z());
    }

    /**
     * CN: 本地玩家由客户端立即解穿透；服务端只把水平冲量传给被玩家推动的硬碰撞实体。
     * EN: The client depenetrates its local player immediately; the server transfers horizontal impulse to the hard collider being pushed.
     */
    private static void pushHardColliderFromPlayer(ServerPlayer player, Entity collider, Vec3 playerCorrection,
            long serverTick) {
        double horizontalDepth = Math.sqrt(horizontalLengthSqr(playerCorrection));
        if (playerCorrection.y() > 0.0 && playerCorrection.y() >= horizontalDepth * MIN_SUPPORT_NORMAL_Y
                || isPlayerOnCarryingSurface(player, collider)) {
            return;
        }
        if (!collider.isPushable()) {
            return;
        }

        ReportedPlayerMovement reportedMovement = REPORTED_PLAYER_MOVEMENTS.get(player.getUUID());
        Vec3 playerMovement = reportedMovement != null && reportedMovement.serverTick() == serverTick
                ? reportedMovement.movement()
                : Vec3.ZERO;
        playerMovement = new Vec3(playerMovement.x(), 0.0, playerMovement.z());
        Vec3 direction = new Vec3(
                collider.getX() - player.getX(),
                0.0,
                collider.getZ() - player.getZ());
        if (direction.lengthSqr() <= 1.0E-10) {
            direction = new Vec3(-playerCorrection.x(), 0.0, -playerCorrection.z());
        }
        if (direction.lengthSqr() <= 1.0E-10) {
            return;
        }
        direction = direction.normalize();
        double approachDistance = playerMovement.dot(direction);
        if (approachDistance <= 1.0E-4) {
            return;
        }

        Vec3 transfer = direction.scale(Math.min(0.18, approachDistance * 0.8));
        pushEntity(collider, transfer);
    }

    private static boolean isPlayerOnCarryingSurface(ServerPlayer player, Entity collider) {
        double top = ServerObbStore.parts(collider).stream()
                .filter(part -> part.bone().carriesEntities()
                        && part.bone().hasAttribute(ObbBoneAttribute.COLLISION))
                .mapToDouble(part -> part.geometry().worldBounds().maxY)
                .max()
                .orElse(Double.NEGATIVE_INFINITY);
        return Double.isFinite(top)
                && player.getY() >= top - 0.35
                && player.getY() <= top + 0.75
                && player.getDeltaMovement().y() <= 0.1;
    }

    private static double horizontalLengthSqr(Vec3 value) {
        return value.x() * value.x() + value.z() * value.z();
    }

    private static boolean isVerticalSupport(ActiveCollision active, Vec3 correction) {
        double length = correction.length();
        if (length <= 1.0E-10 || Math.abs(correction.y()) / length < MIN_SUPPORT_NORMAL_Y) {
            return false;
        }
        return correction.y() > 0.0
                ? active.first.getY() >= active.second.getY() + MIN_SUPPORT_ROOT_OFFSET
                : active.second.getY() >= active.first.getY() + MIN_SUPPORT_ROOT_OFFSET;
    }

    private static void moveOutOfHardObb(Entity moving, Vec3 movement) {
        moving.move(MoverType.SHULKER_BOX, movement);
        Vec3 velocity = moving.getDeltaMovement();
        Vec3 normal = movement.lengthSqr() <= 1.0E-12 ? Vec3.ZERO : movement.normalize();
        double intoCollider = velocity.dot(normal);
        if (intoCollider < 0.0) {
            moving.setDeltaMovement(velocity.subtract(normal.scale(intoCollider)));
        }
        if (Math.abs(movement.x()) > 1.0E-5 || Math.abs(movement.z()) > 1.0E-5) {
            moving.horizontalCollision = true;
            moving.minorHorizontalCollision = false;
        }
        if (Math.abs(movement.y()) > 1.0E-5) {
            moving.verticalCollision = true;
            moving.verticalCollisionBelow = movement.y() > 0.0;
        }
        if (movement.y() > 1.0E-5) {
            ServerObbGroundCompat.setOnGround(moving, movement);
            moving.resetFallDistance();
        }
    }

    private static void applyCarrying(ActiveCollision active, long serverTick) {
        if (!isVerticalSupport(active, active.correction)) {
            return;
        }
        if (tryCarry(active.first, active.firstBone, active.second, active.correction.scale(-1.0), serverTick)) {
            return;
        }
        tryCarry(active.second, active.secondBone, active.first, active.correction, serverTick);
    }

    private static boolean tryCarry(Entity carrier, ObbBoneState carrierBone, Entity passenger, Vec3 passengerSeparation,
            long serverTick) {
        if (!carrierBone.carriesEntities()
                || !carrierBone.hasAttribute(ObbBoneAttribute.COLLISION)
                || passenger instanceof ServerPlayer
                || passenger.isPassengerOfSameVehicle(carrier)
                || passengerSeparation.lengthSqr() <= 1.0E-10
                || passengerSeparation.normalize().y() < MIN_SUPPORT_NORMAL_Y) {
            return false;
        }

        Optional<ServerObbStore.PartMotion> motionResult = ServerObbStore.partMotion(
                carrier,
                carrierBone.key(),
                passenger.position());
        if (motionResult.isEmpty()) {
            return false;
        }
        ServerObbStore.PartMotion partMotion = motionResult.get();
        CarryKey key = new CarryKey(
                carrier.level().dimension().toString(),
                carrier.getUUID(),
                carrierBone.key(),
                passenger.getUUID());
        long previousSnapshot = LAST_CARRY_SNAPSHOT.getOrDefault(key, Long.MIN_VALUE);
        if (partMotion.snapshotGameTime() <= previousSnapshot) {
            return true;
        }
        LAST_CARRY_SNAPSHOT.put(key, partMotion.snapshotGameTime());

        Vec3 movement = finiteCorrection(partMotion.movement());
        if (movement.lengthSqr() > 1.0E-10) {
            passenger.move(MoverType.SHULKER_BOX, movement);
            ServerObbGroundCompat.setOnGround(passenger, movement);
            passenger.resetFallDistance();
        }
        return true;
    }

    private static Vec3 finiteCorrection(Vec3 value) {
        if (value == null || !Double.isFinite(value.x()) || !Double.isFinite(value.y()) || !Double.isFinite(value.z())) {
            return Vec3.ZERO;
        }
        double length = value.length();
        return length > MAX_CORRECTION ? value.scale(MAX_CORRECTION / length) : value;
    }

    private static void finishAttack(ActiveAttack active, long observedGameTime) {
        active.observedGameTime = observedGameTime;
        try { BoneHitboxContactAttackHooks.handleContact(active.context(ObbCollisionPhase.END)); }
        finally { active.clearTemporaryAttributes(); }
    }

    private static void cleanup(long gameTime) {
        List<ActiveAttack> expiredAttacks = new ArrayList<>();
        ACTIVE_ATTACKS.entrySet().removeIf(entry -> {
            ActiveAttack active = entry.getValue();
            active.reporters.entrySet().removeIf(reporter -> gameTime - reporter.getValue() > ATTACK_TIMEOUT_TICKS);
            if (active.reporters.isEmpty()) {
                expiredAttacks.add(active);
                return true;
            }
            return false;
        });
        expiredAttacks.forEach(active -> finishAttack(active, active.observedGameTime));

        List<ActiveCollision> expiredCollisions = new ArrayList<>();
        ACTIVE_COLLISIONS.entrySet().removeIf(entry -> {
            ActiveCollision active = entry.getValue();
            active.reporters.entrySet().removeIf(reporter -> gameTime - reporter.getValue() > REPORTER_TIMEOUT_TICKS);
            if (active.reporters.isEmpty()) {
                expiredCollisions.add(active);
                return true;
            }
            return false;
        });
        expiredCollisions.forEach(active ->
                BoneHitboxCollisionHooks.handleClientContact(active.context(ObbCollisionPhase.END)));
        Set<CarrierKey> carriers = new LinkedHashSet<>();
        ACTIVE_COLLISIONS.keySet().forEach(key -> {
            carriers.add(new CarrierKey(key.levelId, key.first));
            carriers.add(new CarrierKey(key.levelId, key.second));
        });
        LAST_CARRY_SNAPSHOT.keySet().removeIf(key -> !carriers.contains(new CarrierKey(key.levelId, key.carrier)));
        Set<PhysicalPairKey> activePairs = ACTIVE_COLLISIONS.values().stream()
                .filter(active -> active.collisionMode != ObbCollisionMode.NONE)
                .map(ActiveCollision::physicalPairKey)
                .collect(java.util.stream.Collectors.toSet());
        PHYSICAL_MANIFOLDS.keySet().retainAll(activePairs);
        DIRTY_PHYSICAL_PAIRS.retainAll(activePairs);
    }

    public record AttackContact(ObbBoneState attackBone, ObbBoneState hurtBone) {
    }

    private record AttackKey(String levelId, UUID attacker, ObbBoneKey attackBone, UUID target, ObbBoneKey hurtBone) {
        private String temporaryScope() {
            return SERVER_ATTACK_SCOPE_PREFIX + attacker + "/" + attackBone.displayName()
                    + "/" + target + "/" + hurtBone.displayName();
        }
    }

    private record CollisionKey(String levelId, UUID first, ObbBoneKey firstBone, UUID second, ObbBoneKey secondBone) {
    }

    private record CarrierKey(String levelId, UUID entity) { }

    private record CarryKey(String levelId, UUID carrier, ObbBoneKey carrierBone, UUID passenger) {
    }

    private record ReportedPlayerMovement(Vec3 movement, long serverTick) {
    }

    private record PhysicalPairKey(String levelId, UUID first, UUID second) {
    }

    private record PhysicalContact(ActiveCollision representative, ObbCollisionMode collisionMode,
            Vec3 normal, double depth, boolean verticalSupport) {
    }

    /** CN: 跨 tick 保留的实体对接触缓存。EN: Entity-pair contact cache retained across ticks. */
    private static final class PersistentManifold {
        private Vec3 normal = Vec3.ZERO;
        private double normalImpulse;
        private long lastSolvedTick = Long.MIN_VALUE;
    }

    private record CanonicalCollision(Entity first, ObbBoneState firstBone, Entity second, ObbBoneState secondBone,
            ObbCollisionMode collisionMode, Vec3 correction, long observedGameTime) {
        private static CanonicalCollision of(Entity first, ObbBoneState firstBone, Entity second, ObbBoneState secondBone,
                ObbCollisionMode collisionMode, Vec3 correction, long observedGameTime) {
            int entityOrder = first.getUUID().compareTo(second.getUUID());
            boolean ordered = entityOrder < 0 || entityOrder == 0 && firstBone.key().compareTo(secondBone.key()) <= 0;
            return ordered
                    ? new CanonicalCollision(first, firstBone, second, secondBone, collisionMode, correction, observedGameTime)
                    : new CanonicalCollision(second, secondBone, first, firstBone, collisionMode,
                            correction.scale(-1.0), observedGameTime);
        }

        private CollisionKey key() {
            return new CollisionKey(
                    first.level().dimension().toString(),
                    first.getUUID(),
                    firstBone.key(),
                    second.getUUID(),
                    secondBone.key());
        }
    }

    private static final class ActiveAttack {
        private final AttackKey key;
        private final Entity attacker;
        private final ObbBoneState attackBone;
        private final Entity target;
        private final ObbBoneState hurtBone;
        private final Map<UUID, Long> reporters = new HashMap<>();
        private ObbCollisionMode collisionMode;
        private Vec3 correction;
        private long observedGameTime;
        private long lastSeenTick;
        private long lastEventServerTick = Long.MIN_VALUE;

        private ActiveAttack(AttackKey key, Entity attacker, ObbBoneState attackBone, Entity target,
                ObbBoneState hurtBone, ObbCollisionMode collisionMode, Vec3 correction, long observedGameTime) {
            this.key = key;
            this.attacker = attacker;
            this.attackBone = attackBone;
            this.target = target;
            this.hurtBone = hurtBone;
            update(collisionMode, correction, observedGameTime);
        }

        private void update(ObbCollisionMode collisionMode, Vec3 correction, long observedGameTime) {
            this.collisionMode = collisionMode == null ? ObbCollisionMode.NONE : collisionMode;
            this.correction = correction;
            this.observedGameTime = observedGameTime;
        }

        private void activateTemporaryAttributes() {
            attackBone.setTemporaryAttackBox(key.temporaryScope(), true);
            hurtBone.setTemporaryHurtBox(key.temporaryScope(), true);
        }

        private void clearTemporaryAttributes() {
            attackBone.clearTemporaryAttributes(key.temporaryScope());
            hurtBone.clearTemporaryAttributes(key.temporaryScope());
        }

        private ObbCollisionContext context(ObbCollisionPhase phase) {
            return new ObbCollisionContext(
                    attacker,
                    attackBone,
                    target,
                    hurtBone,
                    collisionMode,
                    phase,
                    correction,
                    observedGameTime,
                    ServerObbStore.animationState(attacker),
                    ServerObbStore.animationState(target));
        }
    }

    private static final class ActiveCollision {
        private final Entity first;
        private final ObbBoneState firstBone;
        private final Entity second;
        private final ObbBoneState secondBone;
        private final Map<UUID, Long> reporters = new HashMap<>();
        private ObbCollisionMode collisionMode;
        private Vec3 correction;
        private long observedGameTime;
        private long lastSeenTick;
        private long lastEventServerTick = Long.MIN_VALUE;

        private ActiveCollision(CanonicalCollision collision) {
            first = collision.first;
            firstBone = collision.firstBone;
            second = collision.second;
            secondBone = collision.secondBone;
            update(collision);
        }

        private void update(CanonicalCollision collision) {
            collisionMode = collision.collisionMode;
            correction = collision.correction;
            observedGameTime = collision.observedGameTime;
        }

        private ObbCollisionContext context(ObbCollisionPhase phase) {
            return new ObbCollisionContext(
                    first,
                    firstBone,
                    second,
                    secondBone,
                    collisionMode,
                    phase,
                    correction,
                    observedGameTime,
                    ServerObbStore.animationState(first),
                    ServerObbStore.animationState(second));
        }

        private PhysicalPairKey physicalPairKey() {
            return new PhysicalPairKey(first.level().dimension().toString(), first.getUUID(), second.getUUID());
        }
    }
}
