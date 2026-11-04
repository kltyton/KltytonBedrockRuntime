package com.kltyton.bedrockruntime.client.particle.runtime;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.PackResources;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.PSClientConfigs;
import com.kltyton.bedrockruntime.client.particle.PSDiagnostics;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.api.IParticleComponent;
import com.kltyton.bedrockruntime.client.particle.api.IntAllocator;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.api.RegisterCustomEmitterTypeEvent;
import com.kltyton.bedrockruntime.client.particle.api.event.ParticleEvents;
import com.kltyton.bedrockruntime.client.particle.api.MolangParticleLoadEvent;
import com.kltyton.bedrockruntime.client.particle.api.RegisterCustomParticleTypeEvent;
import com.kltyton.bedrockruntime.client.particle.api.geckolib.GeckoLibHelper;
import com.kltyton.bedrockruntime.client.particle.definition.DefinedParticleEffect;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockPackResources;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockJson;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockParticleResources;
import com.kltyton.bedrockruntime.client.particle.bedrock.JavaParticleResourceManager;
import com.kltyton.bedrockruntime.client.particle.network.PSClientPacketHandler;

import com.kltyton.bedrockruntime.client.particle.runtime.screen.IParticleManagerScreen;
import com.kltyton.bedrockruntime.client.particle.runtime.screen.PreviewParticleScope;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

public final class MolangParticleEngine implements PreparableReloadListener {
    public static final MolangParticleEngine INSTANCE = new MolangParticleEngine();
    public static final ResourceLocation RELOADER_ID = BedrockParticles.asResource("reloader");
    private static final FileToIdConverter PARTICLE_LISTER = FileToIdConverter.json("particle_definitions");
    private Map<ResourceLocation, DefinedParticleEffect> id2Effect = new Hashtable<>();
    private Map<ResourceLocation, ParticlePreset> id2Particle = new Hashtable<>();
    private Map<ResourceLocation, EmitterPreset> id2Emitter = new Hashtable<>();
    public final IntAllocator allocator;
    private final MolangParticleManager worldManager;
    private final Object2ObjectOpenCustomHashMap<Entity, Object2ObjectLinkedOpenHashMap<ResourceLocation, ParticleEmitter>> tracker = new Object2ObjectOpenCustomHashMap<>(new Hash.Strategy<>() {
        @Override
        public int hashCode(Entity o) {
            return o == null ? 0 : o.getUUID().hashCode();
        }

        @Override
        public boolean equals(Entity a, Entity b) {
            return a == b || a != null && b != null && a.getUUID().equals(b.getUUID());
        }
    });
    private boolean initialized = false;

    private MolangParticleEngine() {
        this.allocator = new IntAllocator();
        this.worldManager = new MolangParticleManager(this.allocator) {
            @Override
            public boolean isIndependentParticles() {
                return false;
            }
        };
    }

    public Map<ResourceLocation, DefinedParticleEffect> id2Effect() {
        return id2Effect;
    }

    public Map<ResourceLocation, ParticlePreset> id2Particle() {
        return id2Particle;
    }

    public Map<ResourceLocation, EmitterPreset> id2Emitter() {
        return id2Emitter;
    }

    public @Nullable ResourceLocation resolveParticleId(ResourceLocation id) {
        if (id2Emitter.containsKey(id)) {
            return id;
        }
        ResourceLocation normalized = stripParticleSuffix(id);
        return id2Emitter.containsKey(normalized) ? normalized : null;
    }

    public boolean containsParticle(ResourceLocation id) {
        return resolveParticleId(id) != null;
    }

    public Set<ResourceLocation> suggestibleParticleIds() {
        return new HashSet<>(id2Emitter.keySet());
    }

    public void saveEffect(DefinedParticleEffect effect) {
        ResourceLocation id = effect.description.identifier();
        ParticlePreset particlePreset = new ParticlePreset(effect);
        EmitterPreset emitterPreset = new EmitterPreset(effect.description.type(), effect.orderedEmitterComponents,
                effect.events, effect.molangVersion(), effect.usesBedrockItemNames());
        for (IParticleComponent component : effect.orderedParticleComponents) {
            component.initialize(Minecraft.getInstance().level, effect.usesBedrockBlockNames());
        }
        Map<ResourceLocation, DefinedParticleEffect> effects = new LinkedHashMap<>(id2Effect);
        Map<ResourceLocation, ParticlePreset> particles = new LinkedHashMap<>(id2Particle);
        Map<ResourceLocation, EmitterPreset> emitters = new LinkedHashMap<>(id2Emitter);
        DefinedParticleEffect previous = effects.get(id);
        for (Map.Entry<ResourceLocation, DefinedParticleEffect> entry : effects.entrySet()) {
            if (previous != null && entry.getValue() == previous) {
                entry.setValue(effect);
                particles.put(entry.getKey(), particlePreset);
                emitters.put(entry.getKey(), emitterPreset);
            }
        }
        effects.put(id, effect);
        particles.put(id, particlePreset);
        emitters.put(id, emitterPreset);
        ResourceLocation stripped = stripParticleSuffix(id);
        if (!effects.containsKey(stripped) || effects.get(stripped) == effect) {
            effects.put(stripped, effect);
            particles.put(stripped, particlePreset);
            emitters.put(stripped, emitterPreset);
        }
        id2Effect = Map.copyOf(effects);
        id2Particle = Map.copyOf(particles);
        id2Emitter = Map.copyOf(emitters);
        RegisterCustomParticleTypeEvent.rebindSprites();
        com.kltyton.bedrockruntime.client.particle.runtime.screen.NativePreviewParticleEngine.rebindWorldAtlases();
    }

    public void tick(LocalPlayer localPlayer) {
        if (!initialized) {
            RegisterCustomParticleTypeEvent.bindSprites(id2Effect);
            for (ParticlePreset detail : new HashSet<>(id2Particle.values())) {
                for (IParticleComponent component : detail.effect.orderedParticleComponents) {
                    component.initialize(localPlayer.level(), detail.effect.usesBedrockBlockNames());
                }
            }
            if (BedrockParticles.GECKOLIB_LOADED) {
                GeckoLibHelper.afterReload();
            }
            removeAll();
            this.initialized = true;
        }
        this.worldManager.tick(Minecraft.getInstance(), localPlayer);
        if (!tracker.isEmpty()) {
            var iterator1 = tracker.object2ObjectEntrySet().fastIterator();
            while (iterator1.hasNext()) {
                var entry = iterator1.next();
                if (entry.getKey().isRemoved()) {
                    iterator1.remove();
                } else if (entry.getValue().values().removeIf(ParticleEmitter::isRemoved) && entry.getValue().isEmpty()) {
                    iterator1.remove();
                }
            }
        }
    }

    public void renderParticles(LightTexture lightTexture, TextureManager textureManager, Camera camera, float partialTick,
                                Frustum frustum, Predicate<net.minecraft.client.particle.ParticleRenderType> renderTypePredicate) {
        this.worldManager.renderParticles(lightTexture, textureManager, camera, partialTick, frustum, renderTypePredicate);
    }

    public void addEmitter(ParticleEmitter emitter, boolean sync) {
        if (isScreenEmitter(emitter)) {
            MolangParticleManager manager = scopedScreenManager();
            if (manager == null) emitter.abortInitialization();
            else manager._addEmitter(emitter);
            return;
        }
        long previousRegistration = emitter.registrationGeneration();
        this.worldManager._addEmitter(emitter);
        if (sync && emitter.registrationGeneration() != previousRegistration) PSClientPacketHandler.synchronizeEmitterToServer(emitter);
    }

    /// Client-side emitter (block attach or preset vars) that must not be persisted to the server.
    public static void addEmitter(ParticleEmitter emitter) {
        INSTANCE.addEmitter(emitter, false);
    }

    public static float sampleCameraDistance(MolangInstance instance, net.minecraft.world.phys.Vec3 position) {
        MolangParticleManager manager = managerFor(instance);
        net.minecraft.client.Camera camera = manager != null && manager.isIndependentParticles()
                ? manager.previewCamera() : Minecraft.getInstance().gameRenderer.getMainCamera();
        if (camera == null) return 0.0F;
        var eye = camera.getPosition();
        float x = (float) position.x - (float) eye.x;
        float y = (float) position.y - (float) eye.y;
        float z = (float) position.z - (float) eye.z;
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public static void addEmitterWithPreEffect(ParticleEmitter emitter) {
        if (!emitter.hasDeferredPreEffect()) {
            addEmitter(emitter);
            return;
        }
        MolangParticleManager manager = managerFor(emitter);
        long previousRegistration = emitter.registrationGeneration();
        INSTANCE.addEmitter(emitter, false);
        if (emitter.registrationGeneration() == previousRegistration) return;
        int registeredId = emitter.id;
        long registrationGeneration = emitter.registrationGeneration();
        try {
            emitter.applyDeferredPreEffect();
        } catch (RuntimeException | Error failure) {
            if (manager != null) {
                try {
                    manager._abortEmitterInitialization(emitter, registeredId, registrationGeneration);
                } catch (RuntimeException | Error cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            throw failure;
        }
    }

    public Iterable<ParticleEmitter> getEmitters() {
        return () -> new Iterator<>() {
            private final ObjectIterator<Int2ObjectMap.Entry<ParticleEmitter>> iterator = worldManager.getEmitters();

            @Override public boolean hasNext() { return iterator.hasNext(); }
            @Override public ParticleEmitter next() { return iterator.next().getValue(); }
        };
    }

    public int totalEmitterCount() { return this.worldManager.totalEmitterCount(); }
    public int totalParticleCount() { return this.worldManager.totalParticleCount(); }
    public void loadEmitter(Level level, int id, CompoundTag tag) { this.worldManager.loadEmitter(level, id, tag); }
    public boolean contains(int id) { return this.worldManager.contains(id); }

    public Set<Integer> suggestibleEmitterIds() {
        Set<Integer> ids = new HashSet<>();
        for (ParticleEmitter emitter : getEmitters()) ids.add(emitter.id);
        for (ParticleEmitter pending : this.worldManager.emittersToAdd) ids.add(pending.id);
        return ids;
    }

    public boolean addTrackedEmitter(Entity entity, ResourceLocation particleId) {
        Object2ObjectLinkedOpenHashMap<ResourceLocation, ParticleEmitter> queue = tracker.computeIfAbsent(entity, e -> new Object2ObjectLinkedOpenHashMap<>());
        if (!queue.isEmpty() && queue.containsKey(particleId)) return false;
        ParticleEmitter emitter = new ParticleEmitter(entity.level(), entity.position(), particleId,
                com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp.EMPTY,
                com.kltyton.bedrockruntime.client.particle.access.IEntity.of(entity));
        addEmitter(emitter, false);
        queue.put(particleId, emitter);
        if (queue.size() > PSClientConfigs.maxTrackersPerEntity) {
            queue.removeFirst();
        }
        return true;
    }

    public @Nullable Queue<IMolangParticleInstance> _getParticlesForEmitter(ParticleEmitter emitter) {
        if (isScreenEmitter(emitter)) {
            MolangParticleManager manager = scopedScreenManager();
            return manager == null ? null : manager._getParticlesForEmitter(emitter);
        }
        return this.worldManager._getParticlesForEmitter(emitter);
    }

    public @Nullable Queue<IMolangParticleInstance> getParticlesForEmitter(ParticleEmitter emitter) {
        return _getParticlesForEmitter(emitter);
    }

    public void unregisterParticle(IMolangParticleInstance instance) {
        ParticleEmitter emitter = instance.getEmitter();
        if (emitter == null) return;
        MolangParticleManager manager = managerFor(instance);
        if (manager != null) manager.unregisterParticle(instance);
    }

    public void advanceFrame() {
        worldManager.advanceFrame();
    }

    public void evaluateWorldFrameExpressions(float partialTick) {
        worldManager.evaluatePerRenderExpressions(partialTick);
    }

    public void removeEmitter(ParticleEmitter emitter, boolean sync) {
        if (isScreenEmitter(emitter)) {
            MolangParticleManager manager = scopedScreenManager();
            if (manager != null) manager._removeEmitter(emitter.id);
            else emitter.remove();
            return;
        }
        removeEmitter(emitter.id, sync);
    }

    public ParticleEmitter removeEmitter(int id, boolean sync) {
        PreviewParticleScope.Context context = PreviewParticleScope.current();
        if (context != null) {
            ParticleEmitter preview = context.manager()._removeEmitter(id);
            if (preview != null) return preview;
        } else {
            MolangParticleManager manager = screenManager();
            ParticleEmitter preview = manager == null ? null : manager._getEmitter(id);
            if (preview != null) return manager._removeEmitter(id);
        }
        ParticleEmitter removed = this.worldManager._removeEmitter(id);
        if (removed != null && sync) PSClientPacketHandler.sendEmitterRemovalToServer(id);
        return removed;
    }

    public void pauseSimulation() {
        this.worldManager.pauseSimulation();
    }

    public void removeAll() {
        this.worldManager.removeAll();
        tracker.clear();
    }

    public @Nullable ParticleEmitter _getEmitter(int id) {
        PreviewParticleScope.Context context = PreviewParticleScope.current();
        if (context != null) {
            ParticleEmitter preview = context.manager()._getEmitter(id);
            if (preview != null) return preview;
        } else {
            MolangParticleManager manager = screenManager();
            ParticleEmitter preview = manager == null ? null : manager._getEmitter(id);
            if (preview != null) return preview;
        }
        return this.worldManager._getEmitter(id);
    }

    public @Nullable ParticleEmitter getEmitter(int id) {
        return _getEmitter(id);
    }

    public static void addParticle(IMolangParticleInstance instance) {
        if (isScreenEmitter(instance.getEmitter())) {
            MolangParticleManager manager = scopedScreenManager();
            if (manager == null) instance.discard();
            else manager._addParticle(instance);
        } else {
            INSTANCE.worldManager._addParticle(instance);
        }
    }

    public static @Nullable MolangParticleManager managerFor(MolangInstance instance) {
        return isScreenEmitter(instance.getEmitter()) ? scopedScreenManager() : INSTANCE.worldManager;
    }

    private static boolean isScreenEmitter(@Nullable ParticleEmitter emitter) {
        return emitter != null && emitter.hasScreenOwnership();
    }

    private static @Nullable MolangParticleManager scopedScreenManager() {
        PreviewParticleScope.Context context = PreviewParticleScope.current();
        return context == null ? screenManager() : context.manager();
    }

    private static @Nullable MolangParticleManager screenManager() {
        return Minecraft.getInstance().screen instanceof IParticleManagerScreen screen ? screen.getManager() : null;
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, net.minecraft.util.profiling.ProfilerFiller preparationsProfiler, net.minecraft.util.profiling.ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
        return CompletableFuture.supplyAsync(() -> {
            Map<ResourceLocation, Resource> resources = new LinkedHashMap<>(PARTICLE_LISTER.listMatchingResources(resourceManager));
            resources.putAll(resourceManager.listResources("particles", id -> id.getPath().endsWith(".json")));
            resources.putAll(resourceManager.listResources(BedrockPackResources.DEFINITIONS, id -> id.getPath().endsWith(".json")));
            return resources;
        }, backgroundExecutor).thenCompose(map -> {
            ParticleEvents.post(new MolangParticleLoadEvent.Pre(backgroundExecutor));
            CompletableFuture<List<ResourceLocation>> spriteIds = CompletableFuture.supplyAsync(
                    () -> RegisterCustomParticleTypeEvent.loadSpriteIdentifiers(resourceManager), backgroundExecutor);
            CompletableFuture<com.kltyton.bedrockruntime.client.particle.bedrock.BedrockParticleBindings.Snapshot> particleBindings =
                    BedrockParticles.GECKOLIB_LOADED
                            ? CompletableFuture.supplyAsync(() -> com.kltyton.bedrockruntime.client.particle.bedrock.BedrockParticleBindings
                                    .loadAll(resourceManager), backgroundExecutor)
                            : CompletableFuture.completedFuture(
                                    com.kltyton.bedrockruntime.client.particle.bedrock.BedrockParticleBindings.Snapshot.empty());
            List<CompletableFuture<Optional<LoadedParticle>>> list = Lists.newArrayListWithExpectedSize(map.size());
            Map<String, Integer> priorities = new HashMap<>();
            int priority = 0;
            for (PackResources pack : resourceManager.listPacks().toList()) priorities.put(pack.packId(), priority++);
            List<Map.Entry<ResourceLocation, Resource>> ordered = new ArrayList<>(map.entrySet());
            ordered.sort(Comparator.<Map.Entry<ResourceLocation, Resource>>comparingInt(
                    entry -> entry.getKey().getPath().startsWith("particle_definitions/") ? 0 : 1)
                    .thenComparing(Comparator.<Map.Entry<ResourceLocation, Resource>>comparingInt(
                            entry -> priorities.getOrDefault(entry.getValue().sourcePackId(), -1)).reversed())
                    .thenComparing(entry -> entry.getKey().toString()));
            for (Map.Entry<ResourceLocation, Resource> entry : ordered) {
                ResourceLocation resourceId = entry.getKey();
                boolean nativeDefinition = resourceId.getPath().startsWith("particle_definitions/");
                boolean genericParticle = resourceId.getPath().startsWith("particles/");
                ResourceLocation id = nativeDefinition ? PARTICLE_LISTER.fileToId(resourceId)
                        : new ResourceLocation(resourceId.getNamespace(),
                                resourceId.getPath().substring(0, resourceId.getPath().length() - 5));
                list.add(CompletableFuture.supplyAsync(() -> {
                    try (Reader reader = entry.getValue().openAsReader()) {
                        JsonObject document;
                        try {
                            document = BedrockJson.parse(reader);
                        } catch (JsonParseException exception) {
                            if (!genericParticle) throw exception;
                            BedrockParticles.LOGGER.error("Cannot classify particle resource {}", resourceId, exception);
                            return Optional.empty();
                        }
                        if (!document.has("particle_effect") || !document.get("particle_effect").isJsonObject()) {
                            if (genericParticle) return Optional.empty();
                            throw new JsonParseException("Missing particle_effect object");
                        }
                        if (genericParticle) {
                            for (Resource candidate : resourceManager.getResourceStack(resourceId)) {
                                if (!JavaParticleResourceManager.isBedrockDefinition(candidate)) return Optional.empty();
                            }
                        }
                        JsonObject json = document.getAsJsonObject("particle_effect");
                        String sourceTexture = BedrockParticleResources.sourceTexturePath(json);
                        boolean bedrockSource = resourceId.getNamespace().equals(BedrockPackResources.NAMESPACE)
                                || "atlas.terrain".equals(sourceTexture)
                                || sourceTexture != null && sourceTexture.startsWith("textures/");
                        boolean bedrockBlockNames = bedrockSource;
                        if (document.has(com.kltyton.bedrockruntime.particle.BedrockParticles.MODID + ":block_name_format")) {
                            String format = document.get(com.kltyton.bedrockruntime.particle.BedrockParticles.MODID + ":block_name_format").getAsString();
                            bedrockBlockNames = switch (format) {
                                case "bedrock" -> true;
                                case "java" -> false;
                                default -> throw new JsonParseException("Invalid particle block name format: " + format);
                            };
                        }
                        boolean bedrockItemNames = BedrockParticleResources.usesBedrockItemNames(document, bedrockSource);
                        BedrockParticleResources.Texture texture = BedrockParticleResources.resolveTexture(resourceManager, resourceId, json);
                        if (texture != null) BedrockParticleResources.applyTexture(json, texture);
                        DefinedParticleEffect effect = DefinedParticleEffect.CODEC.parse(JsonOps.INSTANCE, json)
                                .getOrThrow(false, message -> { throw new JsonParseException(message); })
                                .withMolangVersion(BedrockParticleResources.readVersion(entry.getValue()))
                                .withBedrockBlockNames(bedrockBlockNames)
                                .withBedrockItemNames(bedrockItemNames);
                        effect.withSourceMetadata(document.get("format_version"), sourceTexture,
                                sourceTexture == null ? null : effect.description.parameters().texture());
                        return Optional.of(new LoadedParticle(id, effect, nativeDefinition));
                    } catch (Exception exception) {
                        PSDiagnostics.error("failed to load particle definition fileId={} resource={}", id, entry.getKey(), exception);
                        throw new IllegalStateException("Failed to load definition for particle " + id, exception);
                    }
                }, backgroundExecutor));
            }
            CompletableFuture<List<Optional<LoadedParticle>>> effects = Util.sequence(list);
            return CompletableFuture.allOf(effects, spriteIds, particleBindings).thenApply(unused ->
                    new ReloadBatch(effects.join(), spriteIds.join(), particleBindings.join()));
        }).thenCompose(preparationBarrier::wait).thenAcceptAsync(batch -> {
            PSDiagnostics.clear();
            Map<ResourceLocation, DefinedParticleEffect> id2Effect = new Hashtable<>();
            Map<ResourceLocation, ParticlePreset> id2Particle = new Hashtable<>();
            Map<ResourceLocation, EmitterPreset> id2Emitter = new Hashtable<>();
            Set<ResourceLocation> nativeAliases = new HashSet<>();
            int loadedCount = 0;
            for (Optional<LoadedParticle> maybeLoaded : batch.effects()) {
                if (maybeLoaded.isEmpty()) continue;
                loadedCount++;
                LoadedParticle loaded = maybeLoaded.get();
                DefinedParticleEffect effect = loaded.effect();
                ResourceLocation id = effect.description.identifier();
                Set<ResourceLocation> aliases = new LinkedHashSet<>();
                aliases.add(id);
                aliases.add(loaded.fileId());
                aliases.add(stripParticleSuffix(loaded.fileId()));
                if (!loaded.nativeDefinition() && aliases.stream().anyMatch(nativeAliases::contains)) {
                    BedrockParticles.LOGGER.warn("Ignoring Bedrock particle {} because its id conflicts with a Java definition", id);
                    continue;
                }
                if (loaded.nativeDefinition()) nativeAliases.addAll(aliases);
                ParticlePreset particlePreset = new ParticlePreset(effect);
                EmitterPreset emitterPreset = new EmitterPreset(
                        effect.description.type(),
                        effect.orderedEmitterComponents,
                        effect.events,
                        effect.molangVersion(),
                        effect.usesBedrockItemNames()
                );
                for (ResourceLocation alias : aliases) {
                    registerAlias(id2Effect, id2Particle, id2Emitter, alias, effect, particlePreset, emitterPreset);
                }
                PSDiagnostics.info("loaded definition fileId={} identifier={} aliases={} material={} texture={} emitterComponents={} particleComponents={} events={}",
                        loaded.fileId(),
                        id,
                        aliases,
                        effect.description.parameters().material(),
                        effect.description.parameters().texture(),
                        effect.orderedEmitterComponents.stream().map(component -> component.getClass().getSimpleName()).toList(),
                        effect.orderedParticleComponents.stream().map(component -> component.getClass().getSimpleName()).toList(),
                        effect.events.keySet()
                );
            }
            this.id2Effect = id2Effect;
            this.id2Particle = id2Particle;
            this.id2Emitter = id2Emitter;
            RegisterCustomParticleTypeEvent.bindSprites(id2Effect, batch.spriteIds());
            com.kltyton.bedrockruntime.client.particle.bedrock.BedrockParticleBindings.publish(batch.particleBindings());
            this.initialized = false;
            ParticleEvents.post(new MolangParticleLoadEvent.Post(gameExecutor));
            BedrockParticles.LOGGER.info("Loaded {} particle definitions with {} usable ids", loadedCount, id2Emitter.size());
        }, gameExecutor);
    }

    private static void registerAlias(
            Map<ResourceLocation, DefinedParticleEffect> id2Effect,
            Map<ResourceLocation, ParticlePreset> id2Particle,
            Map<ResourceLocation, EmitterPreset> id2Emitter,
            ResourceLocation id,
            DefinedParticleEffect effect,
            ParticlePreset particlePreset,
            EmitterPreset emitterPreset
    ) {
        DefinedParticleEffect previous = id2Effect.get(id);
        if (previous != null && previous != effect) {
            BedrockParticles.LOGGER.warn("Duplicate BedrockParticles particle id '{}'; keeping the first loaded definition", id);
            return;
        }
        id2Effect.put(id, effect);
        id2Particle.put(id, particlePreset);
        id2Emitter.put(id, emitterPreset);
    }

    private static ResourceLocation stripParticleSuffix(ResourceLocation id) {
        String path = id.getPath();
        if (path.endsWith(".particle")) {
            return new ResourceLocation(id.getNamespace(), path.substring(0, path.length() - ".particle".length()));
        }
        return id;
    }

    private record LoadedParticle(ResourceLocation fileId, DefinedParticleEffect effect, boolean nativeDefinition) {
    }

    private record ReloadBatch(List<Optional<LoadedParticle>> effects, List<ResourceLocation> spriteIds,
                               com.kltyton.bedrockruntime.client.particle.bedrock.BedrockParticleBindings.Snapshot particleBindings) {
    }
}
