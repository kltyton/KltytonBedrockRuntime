package com.kltyton.bedrockruntime.client.particle.runtime;
import com.kltyton.bedrockruntime.client.particle.runtime.timing.ParticleSimulationClock;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.particles.ParticleLimit;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.api.IntAllocator;
import com.kltyton.bedrockruntime.client.particle.api.RegisterCustomEmitterTypeEvent;
import com.kltyton.bedrockruntime.client.particle.definition.component.ParticleMotionParametric;
import com.kltyton.bedrockruntime.client.particle.access.ParticleAdmission;
import com.kltyton.bedrockruntime.client.particle.access.ParticleEnginePreviewOwner;
import com.kltyton.bedrockruntime.mixin.client.particle.ParticleEngineAccessor;
import com.kltyton.bedrockruntime.mixin.client.particle.SingleQuadParticleAccessor;
import com.kltyton.bedrockruntime.client.particle.runtime.screen.UnboundedFrustum;
import com.kltyton.bedrockruntime.client.particle.runtime.screen.PreviewParticleScope;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Queue;
import java.util.function.Predicate;

/// 发射器/粒子的生命周期管理器。
///
/// 世界模式（[MolangParticleEngine]）保持既有行为：粒子仍然交给原版 `ParticleEngine` 负责 tick 与渲染，
/// 本类只维护发射器、粒子归属队列和同步所需的状态。
///
/// 预览模式（`new MolangParticleManager(allocator)`）使用独立粒子状态：四边形由本类处理，
/// 其他原生粒子组按需交给独立的预览 `ParticleEngine`，不会加入世界粒子引擎。
public class MolangParticleManager {
    private final ParticleSimulationClock simulationClock = new ParticleSimulationClock();
    private @Nullable Minecraft frameMinecraft;
    private @Nullable LocalPlayer framePlayer;
    private boolean simulationPaused = true;
    private boolean framePhaseActive;
    private long lastSimulationFrame = Long.MIN_VALUE;

    public long updateDeltaNanos() { return simulationClock.deltaNanos(); }

    public void pauseSimulation() {
        simulationPaused = true;
        simulationClock.pause();
    }

    public void advanceFrame() {
        long frame = com.kltyton.bedrockruntime.client.particle.definition.molang.RenderFrameContext.frameIndex();
        if (lastSimulationFrame == frame) return;
        lastSimulationFrame = frame;
        Minecraft minecraft = frameMinecraft;
        LocalPlayer player = framePlayer;
        if (minecraft == null || player == null) return;
        boolean paused = simulationPaused || !isIndependentParticles() && minecraft.isPaused();
        simulationClock.sample(paused);
        if (paused) return;
        tickPhase(minecraft, player, true);
    }

    boolean isAdvancingFrame() {
        return framePhaseActive;
    }
    /// 所有新建的发射器都先进这个队列，tick 开始时统一并入 `emitters`：
    /// tick 期间（事件节点里发射粒子）不会破坏正在遍历的 fastutil 迭代器。
    protected final Queue<ParticleEmitter> emittersToAdd = new ArrayDeque<>();
    protected final Int2ObjectOpenHashMap<ParticleEmitter> emitters = new Int2ObjectOpenHashMap<>();
    protected final Int2ObjectOpenHashMap<Queue<IMolangParticleInstance>> particlesForEmitter = new Int2ObjectOpenHashMap<>();
    protected final Queue<IMolangParticleInstance> particlesToAdd = new ArrayDeque<>();
    /// 粒子按 [SingleQuadParticle.Layer] 分组（旧的 ParticleRenderType 已不再参与 quad 渲染）。
    protected final Reference2ObjectOpenHashMap<SingleQuadParticle.Layer, Queue<IMolangParticleInstance>> groupedParticles = new Reference2ObjectOpenHashMap<>();
    /// 没有可用渲染层的预览粒子：仍然要 tick/过期，但不绘制。
    protected final Queue<IMolangParticleInstance> unrenderedParticles = new ArrayDeque<>();
    private final Object2IntOpenHashMap<Identifier> particlesByEffect = new Object2IntOpenHashMap<>();
    private final Object2IntOpenHashMap<Identifier> emittersByEffect = new Object2IntOpenHashMap<>();
    private final Reference2ObjectOpenHashMap<ParticleEmitter, Identifier> emitterEffectIds = new Reference2ObjectOpenHashMap<>();
    private final PerFrameParticleExpressions perFrameExpressions = new PerFrameParticleExpressions(this);
    private final Reference2ObjectOpenHashMap<IMolangParticleInstance, ParticleFrameMotion> frameMotions = new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectOpenHashMap<IMolangParticleInstance, ParticleLimit> previewParticleLimits = new Reference2ObjectOpenHashMap<>();
    private final Object2IntOpenHashMap<ParticleLimit> previewLimitCounts = new Object2IntOpenHashMap<>();
    private @Nullable ParticleEngine previewNativeEngine;
    private @Nullable ClientLevel previewNativeLevel;
    private @Nullable ClientLevel previewScopeLevel;
    private @Nullable Camera previewCamera;
    private int plainPreviewParticleCount;
    private boolean constructingPreviewNativeEngine;
    private boolean drainingPreviewParticleActions;
    private final Queue<Runnable> pendingPreviewParticleActions = new ArrayDeque<>();
    private long sceneRevision;
    private boolean clearingScene;
    private int admittedParticleCount;
    public final IntAllocator allocator;

    /// 是否由本管理器独立 tick/渲染粒子。屏幕预览为 true，世界模式为 false。
    protected final boolean independentParticles;

    /// 预览模式：独立 tick + 独立渲染。
    public MolangParticleManager(IntAllocator allocator) {
        this(allocator, true);
    }

    /// @param independentParticles false 时粒子仍由原版 `ParticleEngine` tick/render（世界模式）
    protected MolangParticleManager(IntAllocator allocator, boolean independentParticles) {
        this.allocator = allocator;
        this.independentParticles = independentParticles;
    }

    public boolean isIndependentParticles() {
        return independentParticles;
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public void tick(Minecraft minecraft, LocalPlayer player) {
        frameMinecraft = minecraft;
        framePlayer = player;
        simulationPaused = false;
        tickPhase(minecraft, player, false);
    }

    private void tickPhase(Minecraft minecraft, LocalPlayer player, boolean framePhase) {
        boolean previousPhase = framePhaseActive;
        framePhaseActive |= framePhase;
        try {
            if (!independentParticles) {
                tickInternal(minecraft, player, framePhase);
                return;
            }
            previewScopeLevel = (ClientLevel) player.level();
            PreviewParticleScope.enter(this, previewScopeLevel, previewCamera);
            try {
                tickInternal(minecraft, player, framePhase);
            } finally {
                PreviewParticleScope.exit();
            }
        } finally {
            framePhaseActive = previousPhase;
        }
    }

    private void tickInternal(Minecraft minecraft, LocalPlayer player, boolean framePhase) {
        if (framePhase) {
            if (!emittersToAdd.isEmpty()) {
                ParticleEmitter emitter;
                while ((emitter = emittersToAdd.poll()) != null) {
                    emitters.put(emitter.id, emitter);
                }
            }
            if (!emitters.isEmpty()) {
                int renderDistSqr = Mth.square(minecraft.options.renderDistance().get() * 16);
                // Expiration and custom tick callbacks may remove or replace any emitter.
                for (ParticleEmitter emitter : new ArrayList<>(emitters.values())) {
                    if (emitters.get(emitter.id) != emitter) continue;
                    try {
                        if (emitter.isReadyForRemoval() || emitter.level.dimension() != player.level().dimension()) {
                            emitters.remove(emitter.id);
                            forgetEmitterEffect(emitter);
                            removeEmitterNoUpdate(emitter);
                        } else if (emitter.isBindingDiscarded() || independentParticles || emitter.isManualEventEmitter() || Mth.lengthSquared(
                                emitter.getPosition().x - player.getX(),
                                emitter.getPosition().z - player.getZ()
                        ) < renderDistSqr || itemOwnerInRange(emitter, player, renderDistSqr)) {
                            emitter.tick();
                            if (emitters.get(emitter.id) == emitter && !emitter.isRemoved()) {
                                tickSimulationParticles(emitter);
                            }
                        }
                    } catch (RuntimeException e) {
                        BedrockParticles.LOGGER.error("Error ticking emitter {}", emitter.id, e);
                        if (emitters.remove(emitter.id, emitter)) {
                            forgetEmitterEffect(emitter);
                            removeEmitterNoUpdate(emitter);
                        }
                    }
                }
            }
        }
        if (!framePhase && independentParticles && previewNativeEngine != null) previewNativeEngine.tick();
        if (independentParticles && !particlesToAdd.isEmpty()) {
            for (IMolangParticleInstance instance : particlesToAdd) {
                ParticleRenderType group = instance.self().getGroup();
                SingleQuadParticle.Layer layer = group == ParticleRenderType.SINGLE_QUADS
                        && instance.self() instanceof SingleQuadParticle particle
                        ? ((SingleQuadParticleAccessor) particle).kltyton_bedrock_runtime$getLayer() : null;
                if (layer != null) {
                    groupedParticles.computeIfAbsent(layer, o -> new ArrayDeque<>()).add(instance);
                } else {
                    unrenderedParticles.add(instance);
                }
            }
            particlesToAdd.clear();
        }
        if (!framePhase && independentParticles && !groupedParticles.isEmpty()) {
            var iterator = groupedParticles.reference2ObjectEntrySet().fastIterator();
            while (iterator.hasNext()) {
                iterator.next().getValue().removeIf(MolangParticleManager::tickPreviewParticle);
            }
        }
        if (!framePhase && independentParticles && !unrenderedParticles.isEmpty()) {
            unrenderedParticles.removeIf(MolangParticleManager::tickPreviewParticle);
        }
        if (!particlesForEmitter.isEmpty()) {
            var iterator = particlesForEmitter.int2ObjectEntrySet().fastIterator();
            while (iterator.hasNext()) {
                iterator.next().getValue().removeIf(instance -> instance.isDiscarded()
                        || instance.self() instanceof ParticleAdmission tracked && tracked.kltyton_bedrock_runtime$isReleased());
            }
        }
        if (framePhase) perFrameExpressions.advanceRetainedParticles(simulationClock.deltaNanos());
    }

    private static boolean itemOwnerInRange(ParticleEmitter emitter, LocalPlayer player, int renderDistSqr) {
        var owner = emitter.getItemOwner();
        if (owner == null) return false;
        Vec3 position = owner.position();
        return Mth.lengthSquared(position.x - player.getX(), position.z - player.getZ()) < renderDistSqr;
    }

    void discardLocalParticles(ParticleEmitter emitter) {
        Queue<IMolangParticleInstance> owned = particlesForEmitter.get(emitter.id);
        if (owned == null) return;
        for (IMolangParticleInstance instance : new ArrayList<>(owned)) {
            if (instance instanceof MolangParticleInstance particle) particle.discardForInvalidFrame();
            else instance.discard();
        }
    }

    private void tickSimulationParticles(ParticleEmitter emitter) {
        if (!emitter.updatedThisFrame()) return;
        Queue<IMolangParticleInstance> owned = particlesForEmitter.get(emitter.id);
        if (owned == null) return;
        for (IMolangParticleInstance instance : new ArrayList<>(owned)) {
            if (!(instance instanceof MolangParticleInstance particle)) continue;
            try {
                particle.advanceSimulation();
            } catch (RuntimeException exception) {
                BedrockParticles.LOGGER.error("Error updating particle from emitter {}", emitter.id, exception);
                particle.discard();
            }
        }
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private static boolean tickPreviewParticle(IMolangParticleInstance instance) {
        try {
            instance.self().tick();
            return instance.isDiscarded();
        } catch (RuntimeException e) {
            BedrockParticles.LOGGER.error("Error ticking preview particle", e);
            instance.discard();
            return true;
        }
    }

    @ApiStatus.Internal
    public void _addParticle(IMolangParticleInstance instance) {
        if (independentParticles) {
            ParticleRenderType group = instance.self().getGroup();
            if (group != ParticleRenderType.SINGLE_QUADS && group != ParticleRenderType.NO_RENDER) {
                addNativePreviewParticle(instance.self(), (ClientLevel) instance.getLevel());
                return;
            }
            if (instance.self() instanceof ParticleAdmission tracked) {
                tracked.kltyton_bedrock_runtime$admitted(this);
                if (tracked.kltyton_bedrock_runtime$isAdmitted()) {
                    particlesToAdd.add(instance);
                    instance.self().getParticleLimit().ifPresent(limit -> {
                        previewParticleLimits.put(instance, limit);
                        previewLimitCounts.addTo(limit, 1);
                        if (previewNativeEngine != null) ((ParticleEngineAccessor) previewNativeEngine).kltyton_bedrock_runtime$getTrackedParticleCounts().addTo(limit, 1);
                    });
                }
            }
            return;
        }
        Minecraft.getInstance().particleEngine.add(instance.self());
    }

    @ApiStatus.Internal
    public ParticleEngine previewNativeEngine(ClientLevel level) {
        if (previewNativeEngine == null) {
            constructingPreviewNativeEngine = true;
            try {
                var resources = ((ParticleEngineAccessor) Minecraft.getInstance().particleEngine).kltyton_bedrock_runtime$getParticleResources();
                previewNativeEngine = new ParticleEngine(level, resources);
                ((ParticleEnginePreviewOwner) previewNativeEngine).kltyton_bedrock_runtime$setPreviewOwner(this);
                ((ParticleEngineAccessor) previewNativeEngine).kltyton_bedrock_runtime$getTrackedParticleCounts().putAll(previewLimitCounts);
                previewNativeLevel = level;
            } finally {
                constructingPreviewNativeEngine = false;
            }
        } else if (previewNativeLevel != level) {
            previewNativeEngine.setLevel(level);
            ((ParticleEngineAccessor) previewNativeEngine).kltyton_bedrock_runtime$getTrackedParticleCounts().putAll(previewLimitCounts);
            previewNativeLevel = level;
        }
        if (!drainingPreviewParticleActions && !pendingPreviewParticleActions.isEmpty()) {
            drainingPreviewParticleActions = true;
            try {
                Runnable pending;
                while ((pending = pendingPreviewParticleActions.poll()) != null) pending.run();
            } finally {
                drainingPreviewParticleActions = false;
            }
        }
        return previewNativeEngine;
    }

    @ApiStatus.Internal
    public boolean isConstructingPreviewNativeEngine() { return constructingPreviewNativeEngine; }

    @ApiStatus.Internal
    public void deferPreviewParticleAction(Runnable action) {
        long revision = sceneRevision;
        pendingPreviewParticleActions.add(() -> {
            if (revision == sceneRevision) action.run();
        });
    }

    @ApiStatus.Internal
    public void addNativePreviewParticle(Particle particle, ClientLevel level) {
        if (constructingPreviewNativeEngine) {
            deferPreviewParticleAction(() -> addNativePreviewParticle(particle, level));
            return;
        }
        long revision = sceneRevision;
        ParticleEngine engine = previewNativeEngine(level);
        if (revision != sceneRevision) return;
        engine.add(particle);
        if (particle instanceof IMolangParticleInstance instance && particle instanceof ParticleAdmission tracked
                && tracked.kltyton_bedrock_runtime$isAdmitted()) {
            particlesForEmitter.computeIfAbsent(instance.getEmitter().id, ignored -> new ArrayDeque<>()).add(instance);
        }
    }

    @ApiStatus.Internal
    public void _addEmitter(ParticleEmitter emitter) {
        if (clearingScene) {
            emitter.abortInitialization();
            return;
        }
        if (independentParticles) previewScopeLevel = (ClientLevel) emitter.level;
        emitter.id = allocator.allocate();
        emitter.recordRegistration();
        rememberEmitterEffect(emitter);
        // 所有新建发射器都先进队列，tick 开始时统一并入 `emitters`
        emittersToAdd.add(emitter);
    }

    @ApiStatus.Internal
    public void _abortEmitterInitialization(ParticleEmitter emitter, int registeredId, long registrationGeneration) {
        if (emitter.registrationGeneration() != registrationGeneration) return;
        boolean found = emitters.get(registeredId) == emitter;
        if (found) {
            emitters.remove(registeredId);
        } else {
            Iterator<ParticleEmitter> iterator = emittersToAdd.iterator();
            while (iterator.hasNext()) {
                if (iterator.next() == emitter) {
                    iterator.remove();
                    found = true;
                    break;
                }
            }
        }
        if (!found) return;
        try {
            forgetEmitterEffect(emitter);
        } finally {
            try {
                emitter.abortInitialization();
            } finally {
                allocator.release(registeredId);
                particlesForEmitter.remove(registeredId);
            }
        }
    }

    /// 丢弃还没并入的发射器（清场/重置时用，避免 id 泄漏）。
    private void discardPendingEmitters() {
        ParticleEmitter emitter;
        while ((emitter = emittersToAdd.poll()) != null) {
            forgetEmitterEffect(emitter);
            emitter.remove();
            try {
                runRemovalCallbacks(emitter);
            } finally {
                allocator.release(emitter.id);
                particlesForEmitter.remove(emitter.id);
            }
        }
    }

    public void loadEmitter(Level level, int id, CompoundTag tag) {
        ParticleEmitter emitter = RegisterCustomEmitterTypeEvent.create(level, tag);
        emitter.id = id;
        boolean replaced = false;
        while (_getEmitter(id) != null) {
            _removeEmitter(id);
            replaced = true;
        }
        emitters.put(id, emitter);
        rememberEmitterEffect(emitter);
        if (allocator.forceAllocate(id) || replaced) {
            BedrockParticles.LOGGER.warn("There was an emitter exist before, now replaced");
        }
    }

    @ApiStatus.Internal
    public @Nullable ParticleEmitter _removeEmitter(int id) {
        ParticleEmitter removed = emitters.remove(id);
        if (removed != null) {
            forgetEmitterEffect(removed);
            removed.remove();
            try {
                runRemovalCallbacks(removed);
            } finally {
                allocator.release(id);
                particlesForEmitter.remove(id);
            }
            return removed;
        }
        // 可能还在待加入队列里（新建发射器先入队、tick 开始才并入）
        Iterator<ParticleEmitter> iterator = this.emittersToAdd.iterator();
        while (iterator.hasNext()) {
            ParticleEmitter pending = iterator.next();
            if (pending.id != id) {
                continue;
            }
            iterator.remove();
            forgetEmitterEffect(pending);
            pending.remove();
            try {
                runRemovalCallbacks(pending);
            } finally {
                allocator.release(id);
                particlesForEmitter.remove(id);
            }
            return pending;
        }
        return null;
    }

    /// 清空场景：移除全部发射器并回收 id，同时丢弃已生成与待生成的粒子队列（不动 allocator 本身）。
    @ApiStatus.Internal
    public void clearScene() {
        clearContents();
    }

    private void clearContents() {
        if (clearingScene) return;
        clearingScene = true;
        try {
            sceneRevision++;
            pendingPreviewParticleActions.clear();
            releasePreviewParticles();
            do {
                discardPendingEmitters();
                while (!emitters.isEmpty()) {
                    _removeEmitter(emitters.keySet().iterator().nextInt());
                }
            } while (!emittersToAdd.isEmpty());
            releasePreviewParticles();
            pendingPreviewParticleActions.clear();
            particlesForEmitter.clear();
            particlesToAdd.clear();
            groupedParticles.clear();
            unrenderedParticles.clear();
        } finally {
            clearingScene = false;
        }
    }

    protected void removeEmitterNoUpdate(ParticleEmitter emitter) {
        emitter.remove();
        try {
            runRemovalCallbacks(emitter);
        } finally {
            allocator.release(emitter.id);
            particlesForEmitter.remove(emitter.id);
        }
    }

    public void removeAll() {
        try {
            clearContents();
        } finally {
            simulationClock.reset();
            frameMinecraft = null;
            framePlayer = null;
            lastSimulationFrame = Long.MIN_VALUE;
            simulationPaused = true;
        }
    }

    private void releasePreviewParticles() {
        if (!independentParticles) return;
        if (previewNativeEngine != null) {
            previewNativeEngine.clearParticles();
            ((ParticleEngineAccessor) previewNativeEngine).kltyton_bedrock_runtime$getTrackedParticleCounts().putAll(previewLimitCounts);
        }
        for (IMolangParticleInstance instance : particlesToAdd) releaseParticle(instance);
        for (Queue<IMolangParticleInstance> queue : groupedParticles.values()) {
            for (IMolangParticleInstance instance : queue) releaseParticle(instance);
        }
        for (IMolangParticleInstance instance : unrenderedParticles) releaseParticle(instance);
    }

    private void runRemovalCallbacks(ParticleEmitter emitter) {
        if (!independentParticles) {
            emitter.onRemove();
            return;
        }
        PreviewParticleScope.enter(this, (ClientLevel) emitter.level, previewCamera);
        try {
            emitter.onRemove();
        } finally {
            PreviewParticleScope.exit();
        }
    }

    private static void releaseParticle(IMolangParticleInstance instance) {
        if (instance.self() instanceof ParticleAdmission tracked) tracked.kltyton_bedrock_runtime$released();
    }

    public boolean contains(int id) {
        return allocator.isAllocated(id);
    }

    @ApiStatus.Internal
    public @Nullable ParticleEmitter _getEmitter(int id) {
        ParticleEmitter emitter = emitters.get(id);
        if (emitter != null) {
            return emitter;
        }
        for (ParticleEmitter pending : emittersToAdd) {
            if (pending.id == id) {
                return pending;
            }
        }
        return null;
    }

    @ApiStatus.Internal
    public @Nullable Queue<IMolangParticleInstance> _getParticlesForEmitter(ParticleEmitter emitter) {
        return particlesForEmitter.get(emitter.id);
    }

    public ObjectIterator<Int2ObjectMap.Entry<ParticleEmitter>> getEmitters() {
        return emitters.int2ObjectEntrySet().fastIterator();
    }

    /// 还没被 tick 收编进 `emitters` 的第一个发射器（暂停时不会 tick，发射器会一直停在待加入队列里）。
    @ApiStatus.Internal
    public @Nullable ParticleEmitter _peekPendingEmitter() {
        return emittersToAdd.peek();
    }

    public int totalEmitterCount() {
        return emitters.size() + emittersToAdd.size();
    }

    public int effectEmitterCount(Identifier effect) {
        return emittersByEffect.getInt(effect);
    }

    private void rememberEmitterEffect(ParticleEmitter emitter) {
        ParticlePreset preset = emitter.getParticlePreset();
        Identifier effect = preset == null ? emitter.getIdentity() : preset.effect.description.identifier();
        emitterEffectIds.put(emitter, effect);
        emittersByEffect.addTo(effect, 1);
    }

    private void forgetEmitterEffect(ParticleEmitter emitter) {
        Identifier effect = emitterEffectIds.remove(emitter);
        if (effect == null) throw new IllegalStateException("Emitter effect was not registered: " + emitter.id);
        int count = emittersByEffect.getInt(effect);
        if (count == 1) emittersByEffect.removeInt(effect);
        else if (count > 1) emittersByEffect.put(effect, count - 1);
        else throw new IllegalStateException("Emitter effect count underflow: " + effect);
    }

    public int totalParticleCount() {
        return admittedParticleCount + plainPreviewParticleCount;
    }

    public void kltyton_bedrock_runtime$plainParticleAdded() { plainPreviewParticleCount++; }

    public void kltyton_bedrock_runtime$plainParticleRemoved() {
        if (plainPreviewParticleCount <= 0) throw new IllegalStateException("Preview particle count underflow");
        plainPreviewParticleCount--;
    }

    @ApiStatus.Internal
    public void setPreviewCamera(Camera camera) { previewCamera = camera; }

    @ApiStatus.Internal
    public @Nullable Camera previewCamera() { return previewCamera; }

    public void kltyton_bedrock_runtime$particleAdded(Identifier effect, IMolangParticleInstance particle) {
        particlesForEmitter.computeIfAbsent(particle.getEmitter().id, ignored -> new ArrayDeque<>()).add(particle);
        perFrameExpressions.add(particle);
        particlesByEffect.addTo(effect, 1);
        admittedParticleCount++;
    }

    public void kltyton_bedrock_runtime$particleRemoved(Identifier effect, IMolangParticleInstance particle) {
        int count = particlesByEffect.getInt(effect);
        if (count <= 0) throw new IllegalStateException("Particle effect count underflow: " + effect);
        perFrameExpressions.remove(particle);
        frameMotions.remove(particle);
        ParticleLimit limit = previewParticleLimits.remove(particle);
        if (limit != null) {
            int limitCount = previewLimitCounts.getInt(limit);
            if (limitCount <= 0) throw new IllegalStateException("Preview particle limit count underflow: " + limit);
            if (limitCount == 1) previewLimitCounts.removeInt(limit);
            else previewLimitCounts.put(limit, limitCount - 1);
            if (previewNativeEngine != null) ((ParticleEngineAccessor) previewNativeEngine).kltyton_bedrock_runtime$getTrackedParticleCounts().addTo(limit, -1);
        }
        if (count == 1) particlesByEffect.removeInt(effect);
        else particlesByEffect.put(effect, count - 1);
        admittedParticleCount--;
    }

    public int effectParticleCount(Identifier effect) {
        return particlesByEffect.getInt(effect);
    }

    @ApiStatus.Internal
    public void prepareFrameMotion(IMolangParticleInstance particle, float partialTick) {
        if (!(particle.getPreset().effect.components.get(ParticleMotionParametric.ID) instanceof ParticleMotionParametric parametric)) {
            frameMotions.remove(particle);
            return;
        }
        ParticleFrameMotion state = frameMotions.computeIfAbsent(particle, ignored -> new ParticleFrameMotion());
        parametric.sampleRender(particle, state, partialTick);
    }

    public @Nullable ParticleFrameMotion frameMotion(IMolangParticleInstance particle) {
        return frameMotions.get(particle);
    }

    @ApiStatus.Internal
    public void evaluatePerRenderExpressions(float partialTick) {
        perFrameExpressions.evaluate(partialTick);
    }

    /// 决定某个渲染层粒子的绘制顺序。默认返回队列自身的迭代器（世界/普通预览保持队列顺序）。
    ///
    /// 编辑器预览可以覆写为“按相机距离从远到近”，因为 alpha 混合有顺序要求，
    /// 否则后生成的“远处粒子”会盖在“近处粒子”上面，看起来像穿透。
    protected Iterator<IMolangParticleInstance> orderForRender(SingleQuadParticle.Layer layer, Queue<IMolangParticleInstance> queue, Camera camera, float partialTick) {
        return queue.iterator();
    }

    /// 独立屏幕预览的渲染入口（先提取到 [QuadParticleRenderState]，再作为 GUI 渲染状态提交）。
    ///
    /// @param projection 预览相机投影矩阵
    /// @param view       预览相机视图矩阵（相机位于原点，只含旋转；粒子顶点是相机相对坐标）
    /// @param x,y,width,height 场景在 GUI 中的视口
    public void renderParticles(GuiGraphicsExtractor graphics, Camera camera, float partialTick, @Nullable Frustum frustum,
                                Matrix4f projection, Matrix4f view, int x, int y, int width, int height) {
        renderParticles(graphics, camera, partialTick, frustum, projection, view, x, y, width, height, layer -> true);
    }

    /// 使用整个 GUI 作为预览视口。
    public void renderParticles(GuiGraphicsExtractor graphics, Camera camera, float partialTick, @Nullable Frustum frustum,
                                Matrix4f projection, Matrix4f view) {
        renderParticles(graphics, camera, partialTick, frustum, projection, view, 0, 0, graphics.guiWidth(), graphics.guiHeight());
    }

    public void renderParticles(GuiGraphicsExtractor graphics, Camera camera, float partialTick, @Nullable Frustum frustum,
                                Matrix4f projection, Matrix4f view, int x, int y, int width, int height,
                                Predicate<SingleQuadParticle.Layer> layerPredicate) {
        renderParticles(graphics, camera, partialTick, frustum, projection, view, x, y, width, height,
                layerPredicate, java.util.List.of(), new ScreenRectangle(x, y, width, height));
    }

    public void renderParticles(GuiGraphicsExtractor graphics, Camera camera, float partialTick, @Nullable Frustum frustum,
                                Matrix4f projection, Matrix4f view, int x, int y, int width, int height,
                                Predicate<SingleQuadParticle.Layer> layerPredicate,
                                java.util.List<com.kltyton.bedrockruntime.client.particle.runtime.screen.PreviewBlockQuad> blocks,
                                ScreenRectangle scissor) {
        previewCamera = camera;
        boolean scoped = independentParticles && previewScopeLevel != null;
        if (scoped) PreviewParticleScope.enter(this, previewScopeLevel, camera);
        try {
            evaluatePerRenderExpressions(partialTick);
            if (width <= 0 || height <= 0) return;
            QuadParticleRenderState state = new QuadParticleRenderState();
            extractParticlesInternal(state, camera, partialTick, frustum, layerPredicate);
            ParticlesRenderState nativeState = new ParticlesRenderState();
            boolean submitted = false;
            boolean failureCleanup = false;
            try {
                Frustum nativeFrustum = frustum == null ? new UnboundedFrustum(camera) : new Frustum(frustum);
                if (previewNativeEngine != null) previewNativeEngine.extract(nativeState, nativeFrustum, camera, partialTick);
                if (state.isEmpty() && blocks.isEmpty() && nativeState.particles.isEmpty()) return;
                CameraRenderState cameraState = new CameraRenderState();
                if (!nativeState.particles.isEmpty()) {
                    camera.extractRenderState(cameraState, partialTick);
                    cameraState.cullFrustum = nativeFrustum;
                    cameraState.projectionMatrix.set(projection);
                    cameraState.viewRotationMatrix.set(view);
                }
                var renderState = new com.kltyton.bedrockruntime.client.particle.runtime.screen.ParticlePreviewRenderState(
                        x, y, x + width, y + height, 1.0F, new org.joml.Matrix3x2f(graphics.pose()), scissor,
                        state, nativeState, cameraState, this, previewScopeLevel, camera,
                        java.util.List.copyOf(blocks), new Matrix4f(projection), new Matrix4f(view), projection.m33() == 0.0F);
                if (renderState.bounds() == null) return;
                ((com.kltyton.bedrockruntime.mixin.client.particle.GuiGraphicsExtractorAccessor) graphics)
                        .kltyton_bedrock_runtime$getGuiRenderState().addPicturesInPictureState(renderState);
                submitted = true;
            } catch (RuntimeException | Error exception) {
                failureCleanup = true;
                try {
                    nativeState.reset();
                } catch (RuntimeException | Error cleanup) {
                    exception.addSuppressed(cleanup);
                }
                throw exception;
            } finally {
                if (!submitted && !failureCleanup) nativeState.reset();
            }
        } finally {
            if (scoped) PreviewParticleScope.exit();
        }
    }

    /// 供自定义界面/渲染器直接复用粒子提取流程（不接触原版 `ParticleEngine`）。
    public void extractParticles(QuadParticleRenderState state, Camera camera, float partialTick, @Nullable Frustum frustum,
                                 Predicate<SingleQuadParticle.Layer> layerPredicate) {
        previewCamera = camera;
        boolean scoped = independentParticles && previewScopeLevel != null;
        if (scoped) PreviewParticleScope.enter(this, previewScopeLevel, camera);
        try {
            evaluatePerRenderExpressions(partialTick);
            extractParticlesInternal(state, camera, partialTick, frustum, layerPredicate);
        } finally {
            if (scoped) PreviewParticleScope.exit();
        }
    }

    public void extractParticles(QuadParticleRenderState state, ParticlesRenderState nativeState, Camera camera,
                                 float partialTick, @Nullable Frustum frustum,
                                 Predicate<SingleQuadParticle.Layer> layerPredicate) {
        previewCamera = camera;
        boolean scoped = independentParticles && previewScopeLevel != null;
        if (scoped) PreviewParticleScope.enter(this, previewScopeLevel, camera);
        try {
            evaluatePerRenderExpressions(partialTick);
            extractParticlesInternal(state, camera, partialTick, frustum, layerPredicate);
            if (previewNativeEngine != null) {
                previewNativeEngine.extract(nativeState, frustum == null ? new UnboundedFrustum(camera) : frustum, camera, partialTick);
            }
        } finally {
            if (scoped) PreviewParticleScope.exit();
        }
    }

    private void extractParticlesInternal(QuadParticleRenderState state, Camera camera, float partialTick, @Nullable Frustum frustum,
                                          Predicate<SingleQuadParticle.Layer> layerPredicate) {
        var iterator = groupedParticles.reference2ObjectEntrySet().fastIterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            SingleQuadParticle.Layer layer = entry.getKey();
            if (layer == null || !layerPredicate.test(layer)) {
                continue;
            }
            for (Iterator<IMolangParticleInstance> ordered = orderForRender(layer, entry.getValue(), camera, partialTick); ordered.hasNext(); ) {
                IMolangParticleInstance instance = ordered.next();
                if (isVisible(instance, frustum, camera, partialTick) && instance.self() instanceof SingleQuadParticle particle) {
                    particle.extract(state, camera, partialTick);
                }
            }
        }
    }

    private static boolean isVisible(IMolangParticleInstance instance, @Nullable Frustum frustum, Camera camera, float partialTick) {
        if (frustum == null) {
            return true;
        }
        if (instance instanceof MolangParticleInstance molang) {
            return molang.kltyton_bedrock_runtime$isVisible(frustum, camera, partialTick);
        }
        Vector3f position = instance.getRenderWorldPosition(new Vector3f(), partialTick);
        return frustum.isVisible(instance.self().getBoundingBox().move(
                position.x - instance.getX(), position.y - instance.getY(), position.z - instance.getZ()));
    }

}
