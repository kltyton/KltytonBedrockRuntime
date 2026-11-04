package com.kltyton.bedrockruntime.client.particle.runtime;
import com.kltyton.bedrockruntime.client.particle.runtime.timing.ParticleSimulationClock;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.api.IntAllocator;
import com.kltyton.bedrockruntime.client.particle.api.RegisterCustomEmitterTypeEvent;
import com.kltyton.bedrockruntime.client.particle.definition.component.ParticleMotionParametric;
import com.kltyton.bedrockruntime.client.particle.definition.molang.RenderFrameContext;
import com.kltyton.bedrockruntime.client.particle.runtime.screen.PreviewParticleScope;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
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
        long frame = RenderFrameContext.frameIndex();
        if (lastSimulationFrame == frame) return;
        lastSimulationFrame = frame;
        Minecraft minecraft = frameMinecraft;
        LocalPlayer player = framePlayer;
        if (minecraft == null || player == null) return;
        boolean paused = simulationPaused || !isIndependentParticles() && minecraft.isPaused();
        if (simulationClock.sample(paused) == 0L) return;
        tickPhase(minecraft, player, true);
    }

    boolean isAdvancingFrame() {
        return framePhaseActive;
    }

    protected final Queue<ParticleEmitter> emittersToAdd = new ArrayDeque<>();
    protected final Int2ObjectOpenHashMap<ParticleEmitter> emitters = new Int2ObjectOpenHashMap<>();
    protected final Int2ObjectOpenHashMap<Queue<IMolangParticleInstance>> particlesForEmitter = new Int2ObjectOpenHashMap<>();
    protected final Queue<IMolangParticleInstance> particlesToAdd = new ArrayDeque<>();
    protected final Reference2ObjectOpenHashMap<ParticleRenderType, Queue<IMolangParticleInstance>> groupedParticles = new Reference2ObjectOpenHashMap<>();
    private final Set<IMolangParticleInstance> admittedParticles = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<IMolangParticleInstance, ParticleGroup> previewParticleGroups = new IdentityHashMap<>();
    private final Map<ParticleGroup, Integer> previewGroupCounts = new IdentityHashMap<>();
    private final Object2IntOpenHashMap<ResourceLocation> particlesByEffect = new Object2IntOpenHashMap<>();
    private final Object2IntOpenHashMap<ResourceLocation> emittersByEffect = new Object2IntOpenHashMap<>();
    private final Reference2ObjectOpenHashMap<ParticleEmitter, ResourceLocation> emitterEffectIds = new Reference2ObjectOpenHashMap<>();
    private final PerFrameParticleExpressions perFrameExpressions = new PerFrameParticleExpressions(this);
    private final Reference2ObjectOpenHashMap<IMolangParticleInstance, ParticleFrameMotion> frameMotions = new Reference2ObjectOpenHashMap<>();
    private final Queue<Runnable> pendingPreviewActions = new ArrayDeque<>();
    private @Nullable Camera previewCamera;
    private @Nullable ClientLevel previewScopeLevel;
    private int admittedParticleCount;
    private long sceneRevision;
    private boolean clearingScene;
    public final IntAllocator allocator;
    protected final boolean independentParticles;

    public MolangParticleManager(IntAllocator allocator) {
        this(allocator, true);
    }

    protected MolangParticleManager(IntAllocator allocator, boolean independentParticles) {
        this.allocator = allocator;
        this.independentParticles = independentParticles;
    }

    public boolean isIndependentParticles() {
        return independentParticles;
    }

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

    @SuppressWarnings("CallToPrintStackTrace")
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
        if (independentParticles && !particlesToAdd.isEmpty()) {
            IMolangParticleInstance instance;
            while ((instance = particlesToAdd.poll()) != null) {
                groupedParticles.computeIfAbsent(instance.self().getRenderType(), ignored -> new ArrayDeque<>()).add(instance);
            }
        }
        if (!framePhase && independentParticles) {
            for (var entry : new ArrayList<>(groupedParticles.entrySet())) {
                Queue<IMolangParticleInstance> queue = entry.getValue();
                int count = queue.size();
                for (int index = 0; index < count; index++) {
                    IMolangParticleInstance current = queue.poll();
                    if (current == null) break;
                    try {
                        current.self().tick();
                    } catch (RuntimeException exception) {
                        BedrockParticles.LOGGER.error("Error ticking preview particle", exception);
                        current.discard();
                    }
                    if (!current.isDiscarded() && admittedParticles.contains(current)) queue.add(current);
                    else removeParticleFromEmitter(current);
                }
                if (queue.isEmpty()) groupedParticles.remove(entry.getKey(), queue);
            }
        }
        if (!particlesForEmitter.isEmpty()) {
            for (Queue<IMolangParticleInstance> queue : new ArrayList<>(particlesForEmitter.values())) {
                for (IMolangParticleInstance instance : ListCopy.of(queue)) {
                    if (instance.isDiscarded()) removeParticleFromEmitter(instance);
                }
            }
            particlesForEmitter.values().removeIf(Queue::isEmpty);
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

    @ApiStatus.Internal
    public void _addParticle(IMolangParticleInstance instance) {
        if (admittedParticles.contains(instance)) return;
        OptionalGroup group = groupOf(instance);
        ParticleEmitter emitter = instance.getEmitter();
        Queue<IMolangParticleInstance> existing = particlesForEmitter.get(emitter.id);
        if (group.present()) {
            if (existing != null && existing.size() >= group.limit()) return;
            if (independentParticles) {
                int count = previewGroupCounts.getOrDefault(group.group(), 0);
                if (count >= group.limit()) return;
                previewParticleGroups.put(instance, group.group());
                previewGroupCounts.merge(group.group(), 1, Integer::sum);
            }
        }
        if (independentParticles) particlesToAdd.add(instance);
        else Minecraft.getInstance().particleEngine.add(instance.self());
        admitParticle(instance);
    }

    private record OptionalGroup(@Nullable ParticleGroup group) {
        static OptionalGroup of(IMolangParticleInstance instance) {
            return new OptionalGroup(instance.self().getParticleGroup().orElse(null));
        }
        boolean present() { return group != null; }
        int limit() { return group == null ? Integer.MAX_VALUE : group.getLimit(); }
    }

    private static OptionalGroup groupOf(IMolangParticleInstance instance) {
        return OptionalGroup.of(instance);
    }

    private void admitParticle(IMolangParticleInstance instance) {
        if (!admittedParticles.add(instance)) return;
        particlesByEffect.addTo(instance.getPreset().effect.description.identifier(), 1);
        particlesForEmitter.computeIfAbsent(instance.getEmitter().id, ignored -> new ArrayDeque<>()).add(instance);
        perFrameExpressions.add(instance);
        admittedParticleCount++;
        instance.getEmitter().onAdded();
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
            removeEmitterNoUpdate(removed);
            return removed;
        }
        Iterator<ParticleEmitter> iterator = this.emittersToAdd.iterator();
        while (iterator.hasNext()) {
            ParticleEmitter pending = iterator.next();
            if (pending.id != id) continue;
            iterator.remove();
            forgetEmitterEffect(pending);
            removeEmitterNoUpdate(pending);
            return pending;
        }
        return null;
    }

    @ApiStatus.Internal
    public void clearScene() {
        if (clearingScene) return;
        clearingScene = true;
        sceneRevision++;
        pendingPreviewActions.clear();
        try {
            do {
                discardPendingEmitters();
                while (!emitters.isEmpty()) {
                    _removeEmitter(emitters.keySet().iterator().nextInt());
                }
                discardQueuedParticles();
                groupedParticles.values().removeIf(Queue::isEmpty);
                particlesForEmitter.values().removeIf(Queue::isEmpty);
            } while (!emittersToAdd.isEmpty() || !emitters.isEmpty() || !particlesToAdd.isEmpty()
                    || !groupedParticles.isEmpty() || !particlesForEmitter.isEmpty());
        } finally {
            pendingPreviewActions.clear();
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
            clearScene();
        } finally {
            simulationClock.reset();
            frameMinecraft = null;
            framePlayer = null;
            lastSimulationFrame = Long.MIN_VALUE;
            simulationPaused = true;
        }
    }

    private void discardQueuedParticles() {
        IMolangParticleInstance pending;
        while ((pending = particlesToAdd.poll()) != null) {
            removeParticleFromEmitter(pending);
            pending.discard();
        }
        for (Queue<IMolangParticleInstance> queue : new ArrayList<>(groupedParticles.values())) {
            for (IMolangParticleInstance particle : ListCopy.of(queue)) {
                queue.remove(particle);
                removeParticleFromEmitter(particle);
                particle.discard();
            }
        }
        for (Queue<IMolangParticleInstance> queue : new ArrayList<>(particlesForEmitter.values())) {
            for (IMolangParticleInstance particle : ListCopy.of(queue)) {
                removeParticleFromEmitter(particle);
                particle.discard();
            }
        }
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

    private void removeParticleFromEmitter(IMolangParticleInstance particle) {
        if (!admittedParticles.remove(particle)) return;
        forgetParticleEffect(particle);
        ParticleGroup group = previewParticleGroups.remove(particle);
        if (group != null) {
            previewGroupCounts.computeIfPresent(group, (key, count) -> count <= 1 ? null : count - 1);
        }
        ParticleEmitter emitter = particle.getEmitter();
        Queue<IMolangParticleInstance> queue = particlesForEmitter.get(emitter.id);
        if (queue != null) queue.remove(particle);
        perFrameExpressions.remove(particle);
        frameMotions.remove(particle);
        admittedParticleCount--;
        emitter.onRemoved();
    }

    @ApiStatus.Internal
    public void unregisterParticle(IMolangParticleInstance particle) {
        removeParticleFromEmitter(particle);
    }

    public boolean contains(int id) {
        return allocator.isAllocated(id);
    }

    @ApiStatus.Internal
    public @Nullable ParticleEmitter _getEmitter(int id) {
        ParticleEmitter emitter = emitters.get(id);
        if (emitter != null) return emitter;
        for (ParticleEmitter pending : emittersToAdd) {
            if (pending.id == id) return pending;
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

    @ApiStatus.Internal
    public @Nullable ParticleEmitter _peekPendingEmitter() {
        return emittersToAdd.peek();
    }

    public int totalEmitterCount() {
        return emitters.size() + emittersToAdd.size();
    }

    public int effectEmitterCount(ResourceLocation effect) {
        return emittersByEffect.getInt(effect);
    }

    private void rememberEmitterEffect(ParticleEmitter emitter) {
        ParticlePreset preset = emitter.getParticlePreset();
        ResourceLocation effect = preset == null ? emitter.getIdentity() : preset.effect.description.identifier();
        emitterEffectIds.put(emitter, effect);
        emittersByEffect.addTo(effect, 1);
    }

    private void forgetEmitterEffect(ParticleEmitter emitter) {
        ResourceLocation effect = emitterEffectIds.remove(emitter);
        if (effect == null) throw new IllegalStateException("Emitter effect was not registered: " + emitter.id);
        int count = emittersByEffect.getInt(effect);
        if (count == 1) emittersByEffect.removeInt(effect);
        else if (count > 1) emittersByEffect.put(effect, count - 1);
        else throw new IllegalStateException("Emitter effect count underflow: " + effect);
    }

    public int totalParticleCount() {
        return admittedParticleCount;
    }

    public void kltyton_bedrock_runtime$plainParticleAdded() {
        admittedParticleCount++;
    }

    public void kltyton_bedrock_runtime$plainParticleRemoved() {
        if (admittedParticleCount <= 0) throw new IllegalStateException("Preview particle count underflow");
        admittedParticleCount--;
    }

    @ApiStatus.Internal
    public void setPreviewCamera(@Nullable Camera camera) { previewCamera = camera; }

    @ApiStatus.Internal
    public @Nullable Camera previewCamera() { return previewCamera; }

    public void kltyton_bedrock_runtime$particleAdded(ResourceLocation effect, IMolangParticleInstance particle) {
        admitParticle(particle);
    }

    public void kltyton_bedrock_runtime$particleRemoved(ResourceLocation effect, IMolangParticleInstance particle) {
        removeParticleFromEmitter(particle);
    }

    public int effectParticleCount(ResourceLocation effect) {
        return particlesByEffect.getInt(effect);
    }

    private void forgetParticleEffect(IMolangParticleInstance particle) {
        ResourceLocation effect = particle.getPreset().effect.description.identifier();
        int count = particlesByEffect.getInt(effect);
        if (count == 1) particlesByEffect.removeInt(effect);
        else if (count > 1) particlesByEffect.put(effect, count - 1);
        else throw new IllegalStateException("Particle effect count underflow: " + effect);
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

    /// Compatibility hook retained for previews that used to construct a native engine.
    @ApiStatus.Internal
    public boolean isConstructingPreviewNativeEngine() {
        return false;
    }

    @ApiStatus.Internal
    public void deferPreviewParticleAction(Runnable action) {
        long revision = sceneRevision;
        pendingPreviewActions.add(() -> {
            if (revision == sceneRevision) action.run();
        });
    }

    private void drainPreviewActions() {
        if (pendingPreviewActions.isEmpty()) return;
        Runnable action;
        while ((action = pendingPreviewActions.poll()) != null) action.run();
    }

    protected Iterator<IMolangParticleInstance> orderForRender(ParticleRenderType layer, Queue<IMolangParticleInstance> queue, Camera camera, float partialTick) {
        return queue.iterator();
    }

    public void renderParticles(Camera camera, float partialTick, @Nullable Frustum frustum) {
        renderParticles(null, Minecraft.getInstance().getTextureManager(), camera, partialTick, frustum,
                ignored -> true);
    }

    public void renderParticles(LightTexture lightTexture, TextureManager textureManager, Camera camera, float partialTick,
                                @Nullable Frustum frustum, Predicate<ParticleRenderType> renderTypePredicate) {
        if (!independentParticles) return;
        previewCamera = camera;
        boolean scoped = previewScopeLevel != null;
        if (scoped) PreviewParticleScope.enter(this, previewScopeLevel, camera);
        try {
            drainPreviewActions();
            IMolangParticleInstance pending;
            while ((pending = particlesToAdd.poll()) != null) {
                groupedParticles.computeIfAbsent(pending.self().getRenderType(), ignored -> new ArrayDeque<>()).add(pending);
            }
            evaluatePerRenderExpressions(partialTick);
            if (groupedParticles.isEmpty()) return;
            LightTexture light = lightTexture == null ? Minecraft.getInstance().gameRenderer.lightTexture() : lightTexture;
            light.turnOnLightLayer();
            RenderSystem.enableDepthTest();
            Tesselator tesselator = Tesselator.getInstance();
            try {
                for (ParticleRenderType type : new ArrayList<>(groupedParticles.keySet())) {
                    if (type == ParticleRenderType.NO_RENDER || !renderTypePredicate.test(type)) continue;
                    Queue<IMolangParticleInstance> queue = groupedParticles.get(type);
                    if (queue == null || queue.isEmpty()) continue;
                    RenderSystem.setShader(GameRenderer::getParticleShader);
                    BufferBuilder builder = type.begin(tesselator, textureManager);
                    if (builder == null) continue;
                    for (Iterator<IMolangParticleInstance> ordered = orderForRender(type, queue, camera, partialTick); ordered.hasNext(); ) {
                        IMolangParticleInstance instance = ordered.next();
                        if (isVisible(instance, frustum, camera, partialTick)) instance.self().render(builder, camera, partialTick);
                    }
                    MeshData mesh = builder.build();
                    if (mesh != null) BufferUploader.drawWithShader(mesh);
                }
                com.kltyton.bedrockruntime.client.particle.model.runtime.ModelParticleBatch.finish();
            } finally {
                RenderSystem.depthMask(true);
                RenderSystem.disableBlend();
                light.turnOffLightLayer();
            }
        } finally {
            if (scoped) PreviewParticleScope.exit();
        }
    }

    private static boolean isVisible(IMolangParticleInstance instance, @Nullable Frustum frustum, Camera camera, float partialTick) {
        if (frustum == null) return true;
        if (instance instanceof MolangParticleInstance molang) {
            return molang.kltyton_bedrock_runtime$isVisible(frustum, camera, partialTick);
        }
        return frustum.isVisible(instance.self().getBoundingBox());
    }

    private static final class ListCopy {
        private static <T> java.util.List<T> of(Queue<T> queue) {
            return java.util.List.copyOf(queue);
        }
    }
}
