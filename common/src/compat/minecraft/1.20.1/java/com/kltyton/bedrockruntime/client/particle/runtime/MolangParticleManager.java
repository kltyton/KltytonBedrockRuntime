package com.kltyton.bedrockruntime.client.particle.runtime;
import com.kltyton.bedrockruntime.client.particle.runtime.timing.ParticleSimulationClock;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
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
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.api.IntAllocator;
import com.kltyton.bedrockruntime.client.particle.api.RegisterCustomEmitterTypeEvent;
import com.kltyton.bedrockruntime.client.particle.definition.component.ParticleMotionParametric;
import com.kltyton.bedrockruntime.mixin.client.particle.ParticleEnginePreviewAccessor;
import com.kltyton.bedrockruntime.client.particle.runtime.screen.NativePreviewParticleEngine;
import com.kltyton.bedrockruntime.client.particle.runtime.screen.PreviewParticleScope;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

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
        if (simulationClock.sample(simulationPaused || !isIndependentParticles() && minecraft.isPaused()) == 0L) return;
        tickPhase(minecraft, player, true);
    }

    boolean isAdvancingFrame() {
        return framePhaseActive;
    }
    protected final Queue<ParticleEmitter> emittersToAdd = new ArrayDeque<>();
    protected final Int2ObjectOpenHashMap<ParticleEmitter> emitters = new Int2ObjectOpenHashMap<>();
    protected final Map<ParticleEmitter, Queue<IMolangParticleInstance>> particlesForEmitter = new IdentityHashMap<>();
    private final Map<ParticleEmitter, ResourceLocation> emitterEffectIds = new IdentityHashMap<>();
    private final Object2IntOpenHashMap<ResourceLocation> emittersByEffect = new Object2IntOpenHashMap<>();
    private final Object2IntOpenHashMap<ResourceLocation> particlesByEffect = new Object2IntOpenHashMap<>();
    protected final Queue<IMolangParticleInstance> particlesToAdd = new ArrayDeque<>();
    protected final Reference2ObjectOpenHashMap<ParticleRenderType, Queue<IMolangParticleInstance>> groupedParticles = new Reference2ObjectOpenHashMap<>();
    private final PerFrameParticleExpressions perFrameExpressions = new PerFrameParticleExpressions(this);
    private final Reference2ObjectOpenHashMap<IMolangParticleInstance, ParticleFrameMotion> frameMotions = new Reference2ObjectOpenHashMap<>();
    private final Set<IMolangParticleInstance> admittedParticles = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<IMolangParticleInstance> nativePreviewParticles = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<IMolangParticleInstance, ParticleGroup> manualPreviewGroups = new IdentityHashMap<>();
    private final Map<ParticleGroup, Integer> manualPreviewCounts = new IdentityHashMap<>();
    private final Queue<Runnable> pendingPreviewActions = new ArrayDeque<>();
    private boolean constructingPreviewNativeEngine;
    private boolean drainingPreviewActions;
    private long sceneRevision;
    private @Nullable NativePreviewParticleEngine nativePreviewParticleEngine;
    private final Set<Integer> ownedEmitterIds = new HashSet<>();
    private @Nullable Camera previewCamera;
    private boolean clearingScene;
    public final IntAllocator allocator;

    public MolangParticleManager(IntAllocator allocator) { this.allocator = allocator; }

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
            if (constructingPreviewNativeEngine) {
                deferPreviewAction(() -> tickPhase(minecraft, player, framePhase));
                return;
            }
            boolean scoped = isIndependentParticles()
                    && !PreviewParticleScope.isActive(this, player.clientLevel, previewCamera);
            if (scoped) PreviewParticleScope.enter(this, player.clientLevel, previewCamera);
            try {
                tickInternal(minecraft, player, framePhase);
            } finally {
                if (scoped) PreviewParticleScope.exit();
            }
        } finally {
            framePhaseActive = previousPhase;
        }
    }

    private void tickInternal(Minecraft minecraft, LocalPlayer player, boolean framePhase) {
        if (framePhase) {
            ParticleEmitter pending;
            while ((pending = emittersToAdd.poll()) != null) emitters.put(pending.id, pending);
            int renderDistSqr = Mth.square(minecraft.options.renderDistance().get() * 16);
            for (ParticleEmitter emitter : new ArrayList<>(emitters.values())) {
                if (emitters.get(emitter.id) != emitter) continue;
                try {
                    if (emitter.isReadyForRemoval() || emitter.level.dimension() != player.clientLevel.dimension()) {
                        emitters.remove(emitter.id);
                        removeEmitterState(emitter);
                    } else if (isIndependentParticles() || emitter.isManualEventEmitter() || Mth.lengthSquared(emitter.getPosition().x - player.getX(), emitter.getPosition().z - player.getZ()) < renderDistSqr) {
                        emitter.tick();
                        tickSimulationParticles(emitter);
                    }
                } catch (RuntimeException exception) {
                    BedrockParticles.LOGGER.error("Error ticking emitter {}", emitter.id, exception);
                    if (emitters.get(emitter.id) == emitter) {
                        emitters.remove(emitter.id);
                        removeEmitterState(emitter);
                    }
                }
            }
        }
        if (!framePhase && isIndependentParticles() && nativePreviewParticleEngine != null) {
            NativePreviewParticleEngine previewEngine = nativePreviewParticleEngine;
            withPreviewScope(previewEngine.level(), previewCamera, previewEngine::tick);
        }
        IMolangParticleInstance instance;
        while ((instance = particlesToAdd.poll()) != null) {
            groupedParticles.computeIfAbsent(instance.self().getRenderType(), ignored -> new ArrayDeque<>()).add(instance);
        }
        if (!framePhase) {
            for (var entry : new ArrayList<>(groupedParticles.entrySet())) {
                Queue<IMolangParticleInstance> queue = entry.getValue();
                int count = queue.size();
                for (int index = 0; index < count; index++) {
                    IMolangParticleInstance current = queue.poll();
                    if (current == null) break;
                    if (!admittedParticles.contains(current)) continue;
                    try {
                        current.self().tick();
                    } catch (RuntimeException exception) {
                        BedrockParticles.LOGGER.error("Error ticking particle from emitter {}", current.getEmitter().id, exception);
                        current.discard();
                    }
                    if (!current.isDiscarded() && admittedParticles.contains(current)) queue.add(current);
                    else removeParticleFromEmitter(current);
                }
                if (queue.isEmpty()) groupedParticles.remove(entry.getKey(), queue);
            }
        }
        for (Queue<IMolangParticleInstance> queue : new ArrayList<>(particlesForEmitter.values())) {
            for (IMolangParticleInstance particle : List.copyOf(queue)) {
                if (particle.isDiscarded()) removeParticleFromEmitter(particle);
            }
        }
        particlesForEmitter.values().removeIf(Queue::isEmpty);
        if (framePhase) perFrameExpressions.advanceRetainedParticles(simulationClock.deltaNanos());
    }

    void discardLocalParticles(ParticleEmitter emitter) {
        Queue<IMolangParticleInstance> owned = particlesForEmitter.get(emitter);
        if (owned == null) return;
        for (IMolangParticleInstance instance : new ArrayList<>(owned)) {
            if (instance instanceof MolangParticleInstance particle) particle.discardForInvalidFrame();
            else instance.discard();
        }
    }

    private void tickSimulationParticles(ParticleEmitter emitter) {
        if (!emitter.updatedThisFrame()) return;
        Queue<IMolangParticleInstance> owned = particlesForEmitter.get(emitter);
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
        if (constructingPreviewNativeEngine) {
            deferPreviewAction(() -> _addParticle(instance));
            return;
        }
        if (!isIndependentParticles()) {
            Minecraft.getInstance().particleEngine.add(instance.self());
            return;
        }
        if (isIndependentParticles() && usesNativePreview(instance)) {
            long revision = sceneRevision;
            NativePreviewParticleEngine preview = nativePreviewParticleEngine(Minecraft.getInstance(),
                    (net.minecraft.client.multiplayer.ClientLevel) instance.getLevel());
            if (revision == sceneRevision) withPreviewScope(preview.level(), previewCamera, () -> preview.add(instance.self()));
            return;
        }
        if (admittedParticles.contains(instance)) return;
        Optional<ParticleGroup> group = instance.self().getParticleGroup();
        ParticleEmitter emitter = instance.getEmitter();
        Queue<IMolangParticleInstance> existing = particlesForEmitter.get(emitter);
        if (group.isPresent()) {
            if (existing != null && existing.size() >= group.get().getLimit()) return;
            if (isIndependentParticles()) {
                int count = nativePreviewParticleEngine == null
                        ? manualPreviewCounts.getOrDefault(group.get(), 0)
                        : ((ParticleEnginePreviewAccessor) nativePreviewParticleEngine.engine())
                                .kltyton_bedrock_runtime$getTrackedParticleCounts().getInt(group.get());
                if (count >= group.get().getLimit()) return;
                manualPreviewGroups.put(instance, group.get());
                manualPreviewCounts.merge(group.get(), 1, Integer::sum);
                if (nativePreviewParticleEngine != null) {
                    ((ParticleEnginePreviewAccessor) nativePreviewParticleEngine.engine())
                            .kltyton_bedrock_runtime$updateGroupCount(group.get(), 1);
                }
            }
        }
        particlesToAdd.add(instance);
        admitParticle(instance);
    }

    private static boolean usesNativePreview(IMolangParticleInstance instance) {
        ParticleRenderType type = instance.self().getRenderType();
        boolean standard = type == ParticleRenderType.TERRAIN_SHEET
                || type == ParticleRenderType.PARTICLE_SHEET_OPAQUE
                || type == ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT
                || type == ParticleRenderType.PARTICLE_SHEET_LIT
                || type == ParticleRenderType.NO_RENDER
                || type == com.kltyton.bedrockruntime.client.particle.runtime.render.ParticleRenderLayers.particleAdd() || type == com.kltyton.bedrockruntime.client.particle.runtime.render.ParticleRenderLayers.particleBlend()
                || com.kltyton.bedrockruntime.client.particle.runtime.render.BedrockParticleRenderTypes.owns(type);
        return instance.self().getClass() != MolangParticleInstance.class || !standard;
    }

    private void admitParticle(IMolangParticleInstance instance) {
        if (!admittedParticles.add(instance)) return;
        ParticleEmitter emitter = instance.getEmitter();
        particlesForEmitter.computeIfAbsent(emitter, ignored -> new ArrayDeque<>()).add(instance);
        particlesByEffect.addTo(instance.getPreset().effect.description.identifier(), 1);
        perFrameExpressions.add(instance);
        emitter.onAdded();
    }

    public static void onNativeParticleAdmitted(ParticleEngine source, IMolangParticleInstance instance) {
        if (instance.getEmitter() == null || instance.isDiscarded()) return;
        PreviewParticleScope.Context scope = PreviewParticleScope.current();
        MolangParticleManager manager = scope != null && scope.manager().isIndependentParticles()
                ? scope.manager() : MolangParticleEngine.managerFor(instance);
        if (manager != null) {
            if (manager.isIndependentParticles() && source != Minecraft.getInstance().particleEngine) {
                manager.nativePreviewParticles.add(instance);
            }
            manager.admitParticle(instance);
        }
    }

    /** Routes particles created by the world engine during this preview's callback. */
    public static boolean routePreviewParticle(ParticleEngine source, Particle particle) {
        PreviewParticleScope.Context context = PreviewParticleScope.current();
        if (context == null || !context.manager().isIndependentParticles()) return false;
        return context.manager().routePreviewParticle(source, particle, context.level());
    }

    private boolean routePreviewParticle(ParticleEngine source, Particle particle, net.minecraft.client.multiplayer.ClientLevel level) {
        if (constructingPreviewNativeEngine) {
            deferPreviewAction(() -> nativePreviewParticleEngine(Minecraft.getInstance(), level).add(particle));
            return true;
        }
        long revision = sceneRevision;
        NativePreviewParticleEngine preview = nativePreviewParticleEngine(Minecraft.getInstance(), level);
        if (preview.engine() == source) return false;
        if (revision == sceneRevision) preview.add(particle);
        return true;
    }

    private void deferPreviewAction(Runnable action) {
        long revision = sceneRevision;
        pendingPreviewActions.add(() -> {
            if (revision == sceneRevision) action.run();
        });
    }

    public boolean isConstructingPreviewNativeEngine() {
        return constructingPreviewNativeEngine;
    }

    public void deferPreviewParticleAction(Runnable action) {
        deferPreviewAction(action);
    }

    public void ensurePreviewFactories(net.minecraft.client.multiplayer.ClientLevel level) {
        if (isIndependentParticles() && nativePreviewParticleEngine == null && !constructingPreviewNativeEngine) {
            nativePreviewParticleEngine(Minecraft.getInstance(), level);
        }
    }

    private NativePreviewParticleEngine nativePreviewParticleEngine(Minecraft minecraft, net.minecraft.client.multiplayer.ClientLevel level) {
        if (nativePreviewParticleEngine == null) {
            if (constructingPreviewNativeEngine) throw new IllegalStateException("Reentrant preview engine construction");
            long revision = sceneRevision;
            constructingPreviewNativeEngine = true;
            try {
                nativePreviewParticleEngine = new NativePreviewParticleEngine(level, minecraft.particleEngine, minecraft.getTextureManager());
                restoreManualPreviewCounts();
            } finally {
                constructingPreviewNativeEngine = false;
            }
            if (revision != sceneRevision) pendingPreviewActions.clear();
        } else if (nativePreviewParticleEngine.level() != level) {
            nativePreviewParticleEngine.setLevel(level);
            restoreManualPreviewCounts();
        }
        PreviewParticleScope.refreshRegistrationState(this);
        if (!drainingPreviewActions) {
            drainingPreviewActions = true;
            try {
                Runnable action;
                while ((action = pendingPreviewActions.poll()) != null) action.run();
            } finally {
                drainingPreviewActions = false;
            }
        }
        return nativePreviewParticleEngine;
    }

    private void restoreManualPreviewCounts() {
        var engine = (ParticleEnginePreviewAccessor) nativePreviewParticleEngine.engine();
        manualPreviewCounts.forEach(engine::kltyton_bedrock_runtime$updateGroupCount);
    }

    public @Nullable PreviewParticleScope.RegistrationState previewRegistrationState() {
        return nativePreviewParticleEngine == null ? null : nativePreviewParticleEngine.registrationState();
    }

    private void withPreviewScope(ClientLevel level, @Nullable Camera camera, Runnable action) {
        NativePreviewParticleEngine preview = nativePreviewParticleEngine;
        try (PreviewParticleScope.RegistrationLease ignored = PreviewParticleScope.useRegistrationState(
                preview == null ? null : preview.registrationState())) {
            if (PreviewParticleScope.isActive(this, level, camera)) {
                action.run();
                return;
            }
            PreviewParticleScope.enter(this, level, camera);
            try {
                action.run();
            } finally {
                PreviewParticleScope.exit();
            }
        }
    }

    @ApiStatus.Internal
    public void _addEmitter(ParticleEmitter emitter) {
        if (clearingScene) {
            emitter.abortInitialization();
            return;
        }
        emitter.id = allocator.allocate();
        emitter.recordRegistration();
        ownedEmitterIds.add(emitter.id);
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
            emitter.abortInitialization();
            removeEmitterState(emitter);
        } finally {
            particlesForEmitter.remove(emitter);
        }
    }

    public void loadEmitter(Level level, int id, CompoundTag tag) {
        ParticleEmitter previous = _removeEmitter(id);
        ParticleEmitter emitter = RegisterCustomEmitterTypeEvent.create(level, tag); emitter.id = id;
        if (allocator.forceAllocate(id) && previous == null) BedrockParticles.LOGGER.warn("Emitter id {} was already allocated; replacing its registration", id);
        ownedEmitterIds.add(id); rememberEmitterEffect(emitter); emitters.put(id, emitter);
    }

    @ApiStatus.Internal
    public @Nullable ParticleEmitter _removeEmitter(int id) {
        ParticleEmitter removed = emitters.remove(id);
        if (removed == null) for (Iterator<ParticleEmitter> iterator = emittersToAdd.iterator(); iterator.hasNext();) {
            ParticleEmitter pending = iterator.next(); if (pending.id == id) { iterator.remove(); removed = pending; break; }
        }
        if (removed != null) removeEmitterState(removed);
        return removed;
    }

    private void removeEmitterState(ParticleEmitter emitter) {
        ResourceLocation effect = emitterEffectIds.remove(emitter);
        if (effect == null) throw new IllegalStateException("Emitter effect was not registered: " + emitter.id);
        decrementEffectCount(emittersByEffect, effect);
        emitter.remove();
        try {
            emitter.onRemove();
        } finally {
            if (ownedEmitterIds.remove(emitter.id)) allocator.release(emitter.id);
        }
    }

    private void removeParticleFromEmitter(IMolangParticleInstance particle) {
        if (!admittedParticles.remove(particle)) return;
        decrementEffectCount(particlesByEffect, particle.getPreset().effect.description.identifier());
        nativePreviewParticles.remove(particle);
        ParticleEmitter emitter = particle.getEmitter();
        Queue<IMolangParticleInstance> queue = particlesForEmitter.get(emitter);
        if (queue != null) queue.remove(particle);
        ParticleGroup group = manualPreviewGroups.remove(particle);
        if (group != null) {
            manualPreviewCounts.computeIfPresent(group, (key, count) -> count <= 1 ? null : count - 1);
            if (nativePreviewParticleEngine != null) {
                ((ParticleEnginePreviewAccessor) nativePreviewParticleEngine.engine())
                        .kltyton_bedrock_runtime$updateGroupCount(group, -1);
            }
        }
        perFrameExpressions.remove(particle);
        frameMotions.remove(particle);
        emitter.onRemoved();
    }

    @ApiStatus.Internal
    public void unregisterParticle(IMolangParticleInstance particle) {
        removeParticleFromEmitter(particle);
    }

    @ApiStatus.Internal
    public void clearScene() {
        if (clearingScene) return;
        clearingScene = true;
        sceneRevision++;
        pendingPreviewActions.clear();
        net.minecraft.client.multiplayer.ClientLevel level = nativePreviewParticleEngine == null
                ? Minecraft.getInstance().level : nativePreviewParticleEngine.level();
        boolean scoped = isIndependentParticles() && level != null && !PreviewParticleScope.isActive(this, level, previewCamera);
        boolean entered = false;
        RuntimeException failure = null;
        try {
            if (scoped) {
                PreviewParticleScope.enter(this, level, previewCamera);
                entered = true;
            }
            do {
                ParticleEmitter pending;
                while ((pending = emittersToAdd.poll()) != null) {
                    try {
                        removeEmitterState(pending);
                    } catch (RuntimeException exception) {
                        failure = accumulateFailure(failure, exception);
                    }
                }
                while (!emitters.isEmpty()) {
                    try {
                        _removeEmitter(emitters.keySet().iterator().nextInt());
                    } catch (RuntimeException exception) {
                        failure = accumulateFailure(failure, exception);
                    }
                }
                try {
                    discardQueuedParticles();
                } catch (RuntimeException exception) {
                    failure = accumulateFailure(failure, exception);
                }
                try {
                    clearNativeParticles();
                } catch (RuntimeException exception) {
                    failure = accumulateFailure(failure, exception);
                }
                groupedParticles.values().removeIf(Queue::isEmpty);
                particlesForEmitter.values().removeIf(Queue::isEmpty);
            } while (!emittersToAdd.isEmpty() || !emitters.isEmpty() || !particlesToAdd.isEmpty()
                    || !groupedParticles.isEmpty() || !particlesForEmitter.isEmpty());
            if (failure != null) throw failure;
        } finally {
            pendingPreviewActions.clear();
            try {
                if (entered) PreviewParticleScope.exit();
            } finally {
                clearingScene = false;
            }
        }
    }

    private static RuntimeException accumulateFailure(@Nullable RuntimeException first, RuntimeException next) {
        if (first == null) return next;
        first.addSuppressed(next);
        return first;
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
            for (IMolangParticleInstance particle : List.copyOf(queue)) {
                queue.remove(particle);
                removeParticleFromEmitter(particle);
                particle.discard();
            }
        }
        for (Queue<IMolangParticleInstance> queue : new ArrayList<>(particlesForEmitter.values())) {
            for (IMolangParticleInstance particle : List.copyOf(queue)) {
                removeParticleFromEmitter(particle);
                particle.discard();
            }
        }
    }

    private void clearNativeParticles() {
        if (nativePreviewParticleEngine != null) nativePreviewParticleEngine.clear();
    }
    public boolean contains(int id) { return allocator.isAllocated(id); }

    @ApiStatus.Internal
    public @Nullable ParticleEmitter _getEmitter(int id) {
        ParticleEmitter emitter = emitters.get(id); if (emitter != null) return emitter;
        for (ParticleEmitter pending : emittersToAdd) if (pending.id == id) return pending; return null;
    }

    @ApiStatus.Internal public @Nullable Queue<IMolangParticleInstance> _getParticlesForEmitter(ParticleEmitter emitter) { return particlesForEmitter.get(emitter); }
    public ObjectIterator<Int2ObjectMap.Entry<ParticleEmitter>> getEmitters() { return emitters.int2ObjectEntrySet().fastIterator(); }
    public boolean isIndependentParticles() { return true; }
    public @Nullable Camera previewCamera() { return previewCamera; }
    public void setPreviewCamera(@Nullable Camera previewCamera) { this.previewCamera = previewCamera; }
    @ApiStatus.Internal public @Nullable ParticleEmitter _peekPendingEmitter() { return emittersToAdd.peek(); }
    public int totalEmitterCount() { return emitters.size() + emittersToAdd.size(); }
    public int effectEmitterCount(ResourceLocation effect) { return emittersByEffect.getInt(effect); }
    public int effectParticleCount(ResourceLocation effect) { return particlesByEffect.getInt(effect); }

    private void rememberEmitterEffect(ParticleEmitter emitter) {
        ParticlePreset preset = emitter.getParticlePreset();
        ResourceLocation effect = preset == null ? emitter.getIdentity() : preset.effect.description.identifier();
        emitterEffectIds.put(emitter, effect);
        emittersByEffect.addTo(effect, 1);
    }

    private static void decrementEffectCount(Object2IntOpenHashMap<ResourceLocation> counts, ResourceLocation effect) {
        int count = counts.getInt(effect);
        if (count <= 0) throw new IllegalStateException("Effect count underflow: " + effect);
        if (count == 1) counts.removeInt(effect);
        else counts.put(effect, count - 1);
    }
    public int totalParticleCount() {
        int count = nativePreviewParticleEngine == null ? 0 : nativePreviewParticleEngine.particleCount();
        for (Queue<IMolangParticleInstance> queue : particlesForEmitter.values()) count += queue.size();
        return count - nativePreviewParticles.size();
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
        NativePreviewParticleEngine preview = nativePreviewParticleEngine;
        if (preview == null) {
            perFrameExpressions.evaluate(partialTick);
        } else {
            withPreviewScope(preview.level(), previewCamera, () -> perFrameExpressions.evaluate(partialTick));
        }
    }

    public static void evaluateWorldFrameExpressions(ParticleEngine engine, float partialTick) {
        if (engine == Minecraft.getInstance().particleEngine) {
            MolangParticleEngine.INSTANCE.evaluateWorldFrameExpressions(partialTick);
        }
    }

    public void renderParticles(LightTexture lightTexture, TextureManager textureManager, Camera camera, float partialTick, Frustum frustum, Predicate<ParticleRenderType> renderTypePredicate) {
        renderParticles(lightTexture, textureManager, null, camera, partialTick, frustum, renderTypePredicate);
    }

    public void renderParticles(LightTexture lightTexture, TextureManager textureManager, @Nullable PoseStack poseStack, Camera camera, float partialTick, Frustum frustum, Predicate<ParticleRenderType> renderTypePredicate) {
        previewCamera = camera;
        NativePreviewParticleEngine preview = nativePreviewParticleEngine;
        boolean scoped = preview != null && !PreviewParticleScope.isActive(this, preview.level(), camera);
        try (PreviewParticleScope.RegistrationLease ignored = PreviewParticleScope.useRegistrationState(
                preview == null ? null : preview.registrationState())) {
        if (scoped) PreviewParticleScope.enter(this, preview.level(), camera);
        try {
        perFrameExpressions.evaluate(partialTick);
        if (preview != null) preview.render(lightTexture, camera, partialTick, poseStack);
        if (groupedParticles.isEmpty()) return;
        lightTexture.turnOnLightLayer(); boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE); if (cull) RenderSystem.disableCull(); RenderSystem.enableDepthTest();
        if (poseStack != null) { PoseStack modelView = RenderSystem.getModelViewStack(); modelView.pushPose(); modelView.mulPoseMatrix(poseStack.last().pose()); RenderSystem.applyModelViewMatrix(); }
        Tesselator tesselator = Tesselator.getInstance(); List<ParticleRenderType> types =
                com.kltyton.bedrockruntime.client.particle.runtime.render.ParticleRenderLayers.renderOrder(
                        new ArrayList<>(groupedParticles.keySet()));
        for (ParticleRenderType type : types) {
            if (type == ParticleRenderType.NO_RENDER || !renderTypePredicate.test(type)) continue;
            Queue<IMolangParticleInstance> molang = groupedParticles.get(type);
            if (molang == null || molang.isEmpty()) continue;
            RenderSystem.setShader(!com.kltyton.bedrockruntime.client.particle.runtime.render.ParticleRenderLayers.irisLoaded() && (type == ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT || type == com.kltyton.bedrockruntime.client.particle.runtime.render.ParticleRenderLayers.particleBlend())
                    ? com.kltyton.bedrockruntime.client.particle.runtime.render.ParticleRenderLayers::particleNoDiscardShader : GameRenderer::getParticleShader);
            BufferBuilder builder = tesselator.getBuilder(); type.begin(builder, textureManager);
            for (Iterator<IMolangParticleInstance> ordered = orderForRender(type, molang, camera, partialTick); ordered.hasNext();) {
                IMolangParticleInstance instance = ordered.next(); if (instance.isVisible(camera, frustum, partialTick)) renderInstance(instance, builder, camera, partialTick);
            }
            try (var materialDraw = com.kltyton.bedrockruntime.client.particle.runtime.render.ParticleRenderLayers.enterDraw(type)) {
                type.end(tesselator);
            }
        }
        com.kltyton.bedrockruntime.client.particle.model.runtime.ModelParticleBatch.finish();
        if (poseStack != null) { RenderSystem.getModelViewStack().popPose(); RenderSystem.applyModelViewMatrix(); }
        RenderSystem.depthMask(true); RenderSystem.disableBlend(); lightTexture.turnOffLightLayer(); if (cull) RenderSystem.enableCull();
        } finally {
            if (scoped) PreviewParticleScope.exit();
        }
        }
    }

    protected Iterator<IMolangParticleInstance> orderForRender(ParticleRenderType type, Queue<IMolangParticleInstance> queue, Camera camera, float partialTick) { return queue.iterator(); }
    private void renderInstance(IMolangParticleInstance instance, BufferBuilder builder, Camera camera, float partialTick) {
        try { instance.self().render(builder, camera, partialTick); }
        catch (RuntimeException e) { CrashReport report = CrashReport.forThrowable(e, "Rendering Molang Particle"); CrashReportCategory category = report.addCategory("Molang Particle being rendered"); category.setDetail("Molang Particle Id", () -> instance.getEmitter().particleId.toString()); throw new ReportedException(report); }
    }
}
