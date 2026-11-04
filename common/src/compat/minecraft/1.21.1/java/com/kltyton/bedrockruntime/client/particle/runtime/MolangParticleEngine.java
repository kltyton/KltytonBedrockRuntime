package com.kltyton.bedrockruntime.client.particle.runtime;

import com.kltyton.bedrockruntime.client.particle.api.event.ParticleEvents;

import com.google.common.collect.Lists;
import com.google.gson.JsonParseException;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.PSClientConfigs;
import com.kltyton.bedrockruntime.client.particle.PSDiagnostics;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockPackResources;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockParticleResources;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockJson;
import com.kltyton.bedrockruntime.client.particle.api.IComponent;
import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.api.IParticleComponent;
import com.kltyton.bedrockruntime.client.particle.api.IntAllocator;
import com.kltyton.bedrockruntime.client.particle.api.MolangParticleLoadEvent;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.api.RegisterCustomParticleTypeEvent;
import com.kltyton.bedrockruntime.client.particle.api.geckolib.GeckoLibHelper;
import com.kltyton.bedrockruntime.client.particle.definition.DefinedParticleEffect;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.access.IEntity;
import com.kltyton.bedrockruntime.client.particle.network.PSClientPacketHandler;

import com.kltyton.bedrockruntime.client.particle.runtime.screen.IParticleManagerScreen;
import com.kltyton.bedrockruntime.client.particle.runtime.screen.PreviewParticleScope;

import java.io.IOException;
import java.io.Reader;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@SuppressWarnings("all")
public final class MolangParticleEngine extends MolangParticleManager implements PreparableReloadListener {
    public static final MolangParticleEngine INSTANCE = new MolangParticleEngine();
    public static final ResourceLocation RELOADER_ID = BedrockParticles.asResource("reloader");
    private static final FileToIdConverter PARTICLE_LISTER = FileToIdConverter.json("particle_definitions");
    private Map<ResourceLocation, DefinedParticleEffect> id2Effect = new Hashtable<>();
    private Map<ResourceLocation, ParticlePreset> id2Particle = new Hashtable<>();
    private Map<ResourceLocation, EmitterPreset> id2Emitter = new Hashtable<>();
    private final Object2ObjectOpenCustomHashMap<Entity, Object2ObjectLinkedOpenHashMap<ResourceLocation, ParticleEmitter>> tracker = new Object2ObjectOpenCustomHashMap<>(new Hash.Strategy<>() {
        @Override
        public int hashCode(Entity o) {
            return o == null ? 0 : o.getUUID().hashCode();
        }

        @Override
        public boolean equals(@Nullable Entity a, @Nullable Entity b) {
            return a == b || (a != null && b != null && a.getUUID().equals(b.getUUID()));
        }
    });

    private boolean initialized = false;
    private @Nullable WeakReference<Level> initializedLevel;

    private MolangParticleEngine() {
        // 世界模式：粒子仍由原版 ParticleEngine tick/渲染。
        super(new IntAllocator(), false);
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

    /// 把编辑器里改过的效果写回内存：同时重建 [ParticlePreset] / [EmitterPreset]。
    public void saveEffect(DefinedParticleEffect effect) {
        ResourceLocation id = effect.description.identifier();
        ParticlePreset particlePreset = new ParticlePreset(effect);
        EmitterPreset emitterPreset = new EmitterPreset(effect.description.type(), effect.orderedEmitterComponents,
                effect.events, effect.molangVersion(), effect.usesBedrockItemNames());
        for (IComponent component : effect.orderedComponents) {
            if (component instanceof IParticleComponent particleComponent) {
                particleComponent.initialize(Minecraft.getInstance().level, effect.usesBedrockBlockNames());
            }
        }
        var sprite = effect.description.parameters().getTexture(effect.usesTerrainAtlas());
        Map<ResourceLocation, DefinedParticleEffect> effects = new Hashtable<>(this.id2Effect);
        Map<ResourceLocation, ParticlePreset> particles = new Hashtable<>(this.id2Particle);
        Map<ResourceLocation, EmitterPreset> emitters = new Hashtable<>(this.id2Emitter);
        DefinedParticleEffect previous = effects.get(id);
        for (Map.Entry<ResourceLocation, DefinedParticleEffect> entry : effects.entrySet()) {
            if (entry.getValue() == previous && previous != null) {
                ResourceLocation alias = entry.getKey();
                entry.setValue(effect);
                particles.put(alias, particlePreset);
                emitters.put(alias, emitterPreset);
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
        this.id2Effect = effects;
        this.id2Particle = particles;
        this.id2Emitter = emitters;
        RegisterCustomParticleTypeEvent.sprites().addSprite(sprite);
    }

    /// 当前正在预览的屏幕管理器。
    public static @Nullable MolangParticleManager getScreenManager() {
        return Minecraft.getInstance().screen instanceof IParticleManagerScreen screen
                ? screen.getManager()
                : null;
    }

    private static @Nullable MolangParticleManager scopedScreenManager() {
        PreviewParticleScope.Context context = PreviewParticleScope.current();
        return context == null ? getScreenManager() : context.manager();
    }

    public static @Nullable MolangParticleManager managerFor(MolangInstance instance) {
        instance = com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.ActorAccess.referenceOwner(instance);
        return isScreenEmitter(instance.getEmitter()) ? scopedScreenManager() : INSTANCE;
    }

    public static float sampleCameraDistance(MolangInstance instance, net.minecraft.world.phys.Vec3 position) {
        MolangParticleManager manager = managerFor(instance);
        var camera = manager != null && manager.isIndependentParticles()
                ? manager.previewCamera() : Minecraft.getInstance().gameRenderer.getMainCamera();
        if (camera == null) return 0.0F;
        var eye = camera.getPosition();
        float x = (float) position.x - (float) eye.x;
        float y = (float) position.y - (float) eye.y;
        float z = (float) position.z - (float) eye.z;
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public static void addEmitter(ParticleEmitter emitter) {
        INSTANCE.addEmitter(emitter, false);
    }

    @ApiStatus.Internal
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

    public static void removeEmitter(int id) {
        PreviewParticleScope.Context context = PreviewParticleScope.current();
        if (context != null) {
            context.manager()._removeEmitter(id);
            return;
        }
        MolangParticleManager manager = getScreenManager();
        if (manager == null || manager._removeEmitter(id) == null) {
            INSTANCE.removeEmitter(id, false);
        }
    }

    public static @Nullable ParticleEmitter getEmitter(int id) {
        PreviewParticleScope.Context context = PreviewParticleScope.current();
        if (context != null) return context.manager()._getEmitter(id);
        MolangParticleManager manager = getScreenManager();
        ParticleEmitter preview = manager == null ? null : manager._getEmitter(id);
        return preview == null ? INSTANCE._getEmitter(id) : preview;
    }

    @Override
    public void tick(Minecraft minecraft, LocalPlayer localPlayer) {
        if (!initialized || initializedLevel == null || initializedLevel.get() != localPlayer.level()) {
            for (ParticlePreset detail : new HashSet<>(id2Particle.values())) {
                for (IParticleComponent component : detail.effect.orderedParticleEarlyComponents) {
                    component.initialize(localPlayer.level(), detail.effect.usesBedrockBlockNames());
                }
                for (IParticleComponent component : detail.effect.orderedParticleComponents) {
                    component.initialize(localPlayer.level(), detail.effect.usesBedrockBlockNames());
                }
            }
            if (!initialized) {
                if (BedrockParticles.GECKOLIB_LOADED) GeckoLibHelper.afterReload();
                removeAll();
            } else {
                removeOtherLevelEmitters(localPlayer.level());
                pauseSimulation();
            }
            this.initialized = true;
            this.initializedLevel = new WeakReference<>(localPlayer.level());
        }
        super.tick(minecraft, localPlayer);
        if (!tracker.isEmpty()) {
            var iterator = tracker.object2ObjectEntrySet().fastIterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (entry.getKey().isRemoved()) {
                    iterator.remove();
                } else if (entry.getValue().values().removeIf(ParticleEmitter::isRemoved) && entry.getValue().isEmpty()) {
                    iterator.remove();
                }
            }
        }
    }

    private void removeOtherLevelEmitters(Level level) {
        List<Integer> oldIds;
        do {
            oldIds = new java.util.ArrayList<>();
            for (ParticleEmitter emitter : emitters.values()) {
                if (emitter.level != level) oldIds.add(emitter.id);
            }
            for (ParticleEmitter emitter : emittersToAdd) {
                if (emitter.level != level) oldIds.add(emitter.id);
            }
            for (int id : oldIds) _removeEmitter(id);
        } while (!oldIds.isEmpty());
        tracker.keySet().removeIf(entity -> entity.level() != level);
    }

    /// 世界模式入口，同时负责把屏幕预览实例路由到独立管理器。
    public static void addParticle(IMolangParticleInstance instance) {
        if (isScreenEmitter(instance.getEmitter())) {
            MolangParticleManager manager = scopedScreenManager();
            if (manager != null) {
                manager._addParticle(instance);
            } else {
                instance.discard();
            }
            return;
        }
        INSTANCE._addParticle(instance);
    }

    public static @Nullable Queue<IMolangParticleInstance> getParticlesForEmitter(ParticleEmitter emitter) {
        if (isScreenEmitter(emitter)) {
            MolangParticleManager manager = scopedScreenManager();
            return manager == null ? null : manager._getParticlesForEmitter(emitter);
        }
        return INSTANCE._getParticlesForEmitter(emitter);
    }

    public void addEmitter(ParticleEmitter emitter, boolean sync) {
        if (isScreenEmitter(emitter)) {
            MolangParticleManager manager = scopedScreenManager();
            if (manager != null) {
                manager._addEmitter(emitter);
            } else {
                emitter.abortInitialization();
            }
            return;
        }
        long previousRegistration = emitter.registrationGeneration();
        super._addEmitter(emitter);
        if (sync && emitter.registrationGeneration() != previousRegistration) PSClientPacketHandler.syncEmitterToServer(emitter);
    }

    public boolean addTrackedEmitter(Entity entity, ResourceLocation particleId) {
        return addTrackedEmitter(entity, particleId, true);
    }

    public boolean addTrackedEmitter(Entity entity, ResourceLocation particleId, boolean hideOutline) {
        var previous = tracker.get(entity);
        if (previous != null) previous.values().removeIf(ParticleEmitter::isRemoved);
        if (previous != null && (previous.isEmpty()
                || previous.values().stream().anyMatch(emitter -> emitter.level != entity.level()))) {
            tracker.remove(entity);
        }
        var queue = tracker.computeIfAbsent(entity, e -> new Object2ObjectLinkedOpenHashMap<>());
        if (!queue.isEmpty() && queue.containsKey(particleId)) return false;
        ParticleEmitter emitter = new ParticleEmitter(entity.level(), entity.position(), particleId, MolangExp.EMPTY, IEntity.of(entity));
        emitter.hideOutline = hideOutline;
        addEmitter(emitter);
        queue.put(particleId, emitter);
        if (queue.size() > PSClientConfigs.maxTrackersPerEntity) {
            queue.removeFirst();
        }
        return true;
    }

    public void removeEmitter(ParticleEmitter emitter, boolean sync) {
        if (isScreenEmitter(emitter)) {
            MolangParticleManager manager = scopedScreenManager();
            if (manager != null) {
                manager._removeEmitter(emitter.id);
            } else {
                emitter.remove();
            }
            return;
        }
        ParticleEmitter removed = super._removeEmitter(emitter.id);
        if (removed != null && sync) PSClientPacketHandler.sendEmitterRemovalToServer(emitter.id);
    }

    public @Nullable ParticleEmitter removeEmitter(int id, boolean sync) {
        ParticleEmitter removed = super._removeEmitter(id);
        if (removed != null && sync) PSClientPacketHandler.sendEmitterRemovalToServer(id);
        return removed;
    }

    @Override
    public void removeAll() {
        super.removeAll();
        tracker.clear();
    }

    private static boolean isScreenEmitter(@Nullable ParticleEmitter emitter) {
        return emitter != null && emitter.hasScreenOwnership();
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager,
                                          ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                                          Executor backgroundExecutor, Executor gameExecutor) {
        return CompletableFuture.supplyAsync(() -> {
            Map<ResourceLocation, Resource> resources = new java.util.LinkedHashMap<>(PARTICLE_LISTER.listMatchingResources(resourceManager));
            for (ResourceLocation id : resourceManager.listResources("particles", id -> id.getPath().endsWith(".json")).keySet()) {
                try {
                    BedrockParticleResources.findBedrockDefinition(resourceManager, id).ifPresent(resource -> resources.put(id, resource));
                } catch (IOException exception) {
                    throw new IllegalStateException("Cannot inspect particle resource " + id, exception);
                }
            }
            resources.putAll(resourceManager.listResources(BedrockPackResources.DEFINITIONS, id -> id.getPath().endsWith(".json")));
            return resources;
        }, backgroundExecutor).thenCompose(map -> {
            ParticleEvents.post(new MolangParticleLoadEvent.Pre(backgroundExecutor));
            CompletableFuture<List<ResourceLocation>> spriteIds = CompletableFuture.supplyAsync(
                    () -> RegisterCustomParticleTypeEvent.loadSpriteIdentifiers(resourceManager), backgroundExecutor);
            CompletableFuture<com.kltyton.bedrockruntime.client.particle.bedrock.BedrockParticleBindings.Snapshot> particleBindings = BedrockParticles.GECKOLIB_LOADED
                    ? CompletableFuture.supplyAsync(
                            () -> com.kltyton.bedrockruntime.client.particle.bedrock.BedrockParticleBindings.loadAll(resourceManager), backgroundExecutor)
                    : CompletableFuture.completedFuture(com.kltyton.bedrockruntime.client.particle.bedrock.BedrockParticleBindings.Snapshot.empty());
            List<CompletableFuture<Optional<LoadedParticle>>> list = Lists.newArrayListWithExpectedSize(map.size());
            Map<String, Integer> priorities = new java.util.HashMap<>();
            int priority = 0;
            for (var pack : resourceManager.listPacks().toList()) priorities.put(pack.packId(), priority++);
            List<Map.Entry<ResourceLocation, Resource>> ordered = new java.util.ArrayList<>(map.entrySet());
            ordered.sort(java.util.Comparator.<Map.Entry<ResourceLocation, Resource>>comparingInt(
                    entry -> entry.getKey().getPath().startsWith("particle_definitions/") ? 0
                            : entry.getKey().getPath().startsWith("particles/") ? 1 : 2)
                    .thenComparing(java.util.Comparator.<Map.Entry<ResourceLocation, Resource>>comparingInt(
                            entry -> priorities.getOrDefault(entry.getValue().sourcePackId(), -1)).reversed())
                    .thenComparing(entry -> entry.getKey().toString()));
            for (Map.Entry<ResourceLocation, Resource> entry : ordered) {
                ResourceLocation resourceId = entry.getKey();
                boolean nativeDefinition = resourceId.getPath().startsWith("particle_definitions/");
                boolean genericParticle = resourceId.getPath().startsWith("particles/");
                ResourceLocation id = nativeDefinition ? PARTICLE_LISTER.fileToId(resourceId)
                        : ResourceLocation.fromNamespaceAndPath(resourceId.getNamespace(),
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
                        JsonObject json = document.getAsJsonObject("particle_effect");
                        String sourceTexture = BedrockParticleResources.sourceTexturePath(json);
                        boolean bedrockSource = !nativeDefinition || "atlas.terrain".equals(sourceTexture)
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
                                .getOrThrow(JsonParseException::new)
                                .withMolangVersion(BedrockParticleResources.readVersion(entry.getValue()))
                                .withBedrockBlockNames(bedrockBlockNames)
                                .withBedrockItemNames(bedrockItemNames);
                        effect.withSourceMetadata(document.get("format_version"), sourceTexture,
                                sourceTexture == null ? null : effect.description.parameters().texture());
                        return Optional.of(new LoadedParticle(id, effect, nativeDefinition || genericParticle));
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
                if (!loaded.javaDefinition() && aliases.stream().anyMatch(nativeAliases::contains)) {
                    BedrockParticles.LOGGER.warn("Ignoring Bedrock particle {} because its id conflicts with a Java definition", id);
                    continue;
                }
                if (loaded.javaDefinition()) nativeAliases.addAll(aliases);
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
            return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), path.substring(0, path.length() - ".particle".length()));
        }
        return id;
    }

    private record LoadedParticle(ResourceLocation fileId, DefinedParticleEffect effect, boolean javaDefinition) {
    }

    private record ReloadBatch(List<Optional<LoadedParticle>> effects, List<ResourceLocation> spriteIds,
                               com.kltyton.bedrockruntime.client.particle.bedrock.BedrockParticleBindings.Snapshot particleBindings) {
    }
}
