package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler;

import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleEngine;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;

import net.minecraft.client.CameraType;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.illager.SpellcasterIllager;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.skeleton.Bogged;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.dimension.DimensionType;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.api.MolangActorVariableWriter;
import com.kltyton.bedrockruntime.client.particle.api.RegisterMolangQueriesEvent;
import com.kltyton.bedrockruntime.client.particle.api.event.ParticleEvents;
import com.kltyton.bedrockruntime.mixin.client.particle.query.ParticleEngineQueryAccessor;
import com.kltyton.bedrockruntime.mixin.client.particle.query.WolfQueryAccessor;
import com.kltyton.bedrockruntime.mixin.client.particle.query.SnifferQueryAccessor;
import com.kltyton.bedrockruntime.mixin.client.particle.query.DeltaTrackerQueryAccessor;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.Variable;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.ActorAccess;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.math.TrigonometricLookup;
import com.kltyton.bedrockruntime.client.particle.definition.molang.RenderFrameContext;
import com.kltyton.bedrockruntime.client.particle.definition.molang.RenderItemContext;
import com.kltyton.bedrockruntime.client.particle.definition.molang.AnimationFrameContext;
import com.kltyton.bedrockruntime.client.particle.definition.molang.ItemUseDurations;
import com.kltyton.bedrockruntime.client.particle.molang.FrameTimeHistory;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query.ItemChargedFunction;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query.ItemEquippedFunction;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query.ItemDurabilityQueries;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query.EntityQueryDispatch;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.Supplier;

public final class MolangQueries {
    private static final Map<String, Variable> UNFROZEN_QUERIES = new ConcurrentHashMap<>();
    private static final Map<String, Variable> FROZEN_QUERIES = new HashMap<>();
    private static final Map<String, Function<List<MathValue>, MathValue>> QUERY_FUNCTIONS = new HashMap<>();
    private static final Map<String, Function<MolangVersion, ToDoubleFunction<MolangInstance>>> VERSIONED_QUERY_DEFAULTS = new HashMap<>();
    private static final Map<String, Map<EntityType<?>, ToDoubleFunction<MolangInstance>>> ENTITY_QUERY_VARIABLES = new HashMap<>();
    private static final Map<String, Map<EntityType<?>, Function<List<MathValue>, MathValue>>> ENTITY_QUERY_FUNCTIONS = new HashMap<>();
    private static final Map<String, Function<MolangInstance, Object>> CONTEXT_PROVIDERS = new HashMap<>();
    private static final Map<EntityType<?>, List<Function<Entity, Map<String, ?>>>> ACTOR_VARIABLE_PROVIDERS = new HashMap<>();
    private static final Map<EntityType<?>, List<MolangActorVariableWriter>> ACTOR_VARIABLE_WRITERS = new HashMap<>();
    private static final Set<String> REPORTED_UNKNOWN_QUERIES = ConcurrentHashMap.newKeySet();

    static {
        setDefaultQueryValues();
    }

    public static boolean isExistingVariable(String name) {
        return FROZEN_QUERIES.containsKey(name);
    }

    public static boolean isExistingFunction(String name) {
        String canonical = applyQueryAliases(name);
        return QUERY_FUNCTIONS.containsKey(canonical) || ENTITY_QUERY_FUNCTIONS.containsKey(canonical)
                || ENTITY_QUERY_VARIABLES.containsKey(canonical) || isExistingVariable(canonical);
    }

    public static MathValue createFunction(String name, List<MathValue> arguments, Supplier<MathValue> builtIn) {
        String canonical = applyQueryAliases(name);
        List<MathValue> values = List.copyOf(arguments);
        Function<List<MathValue>, MathValue> global = QUERY_FUNCTIONS.get(canonical);
        Map<EntityType<?>, Function<List<MathValue>, MathValue>> entities = ENTITY_QUERY_FUNCTIONS.get(canonical);
        Map<EntityType<?>, Function<List<MathValue>, MathValue>> providers = new HashMap<>();
        if (entities != null) providers.putAll(entities);
        if (values.isEmpty()) {
            Map<EntityType<?>, ToDoubleFunction<MolangInstance>> variables = ENTITY_QUERY_VARIABLES.get(canonical);
            if (variables != null) variables.forEach((type, provider) ->
                    providers.put(type, ignored -> new Variable(canonical, provider)));
        }
        Supplier<MathValue> fallback = global == null ? builtIn : () -> global.apply(values);
        return providers.isEmpty() ? fallback.get() : new EntityQueryDispatch(Map.copyOf(providers), values, fallback);
    }

    public static Object getContext(String name, MolangInstance instance) {
        Function<MolangInstance, Object> provider = CONTEXT_PROVIDERS.get(name);
        if (provider != null) return provider.apply(instance);
        if (name.equals("context.rendered_item")) return RenderItemContext.currentItem();
        if (name.equals("context.is_first_person")) {
            Boolean perspective = com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.ActorAccess
                    .contextOwner(instance).getFirstPersonPerspective();
            return perspective == null ? null : perspective ? 1.0 : 0.0;
        }
        return null;
    }

    public static boolean hasContext(String name) {
        return CONTEXT_PROVIDERS.containsKey(name)
                || name.equals("context.rendered_item") && RenderItemContext.isActive();
    }

    public static boolean hasContext(String name, MolangInstance instance) {
        return hasContext(name) || name.equals("context.is_first_person")
                && com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.ActorAccess
                        .contextOwner(instance).getFirstPersonPerspective() != null;
    }

    public static Map<String, ?> getPublicActorVariables(Entity entity) {
        List<Function<Entity, Map<String, ?>>> providers = ACTOR_VARIABLE_PROVIDERS.get(entity.getType());
        if (providers == null) return Map.of();
        Map<String, Object> values = new HashMap<>();
        for (Function<Entity, Map<String, ?>> provider : providers) {
            provider.apply(entity).forEach((name, value) -> {
                String canonical = applyPrefixAliases(name.toLowerCase(Locale.ROOT), "variable.", "v.");
                if (!canonical.startsWith("variable.")) {
                    throw new IllegalArgumentException("Expected variable namespace: " + name);
                }
                if (values.containsKey(canonical)) {
                    throw new IllegalStateException("Duplicate public Molang variable: " + canonical);
                }
                values.put(canonical, value);
            });
        }
        return values;
    }

    public static boolean writePublicActorVariable(Entity entity, String name, Object value) {
        String canonical = applyPrefixAliases(name.toLowerCase(Locale.ROOT), "variable.", "v.");
        if (!canonical.startsWith("variable.")) return false;
        boolean exposed = getPublicActorVariables(entity).keySet().stream()
                .anyMatch(publicName -> canonical.equals(publicName) || canonical.startsWith(publicName + "."));
        if (!exposed) return false;
        List<MolangActorVariableWriter> writers = ACTOR_VARIABLE_WRITERS.get(entity.getType());
        if (writers == null) return false;
        for (MolangActorVariableWriter writer : writers) {
            if (writer.write(entity, canonical, value)) return true;
        }
        return false;
    }

    public static Variable getQueryFor(String name, MolangVersion version) {
        String canonical = applyQueryAliases(name);
        Function<MolangVersion, ToDoubleFunction<MolangInstance>> factory = VERSIONED_QUERY_DEFAULTS.get(canonical);
        if (factory == null) return getQueryFor(canonical);
        Variable fallback = new Variable(canonical, factory.apply(version));
        Map<EntityType<?>, ToDoubleFunction<MolangInstance>> providers = ENTITY_QUERY_VARIABLES.get(canonical);
        return providers == null ? fallback : bindEntityQueryVariable(canonical, fallback, Map.copyOf(providers));
    }

    public static Variable getQueryFor(String name) {
        String canonicalName = applyPrefixAliases(name, "query.", "q.");
        Variable query = FROZEN_QUERIES.get(canonicalName);
        if (query != null) return query;
        if (REPORTED_UNKNOWN_QUERIES.add(canonicalName)) {
            BedrockParticles.LOGGER.warn("Unsupported Molang query: {}", canonicalName);
        }
        return new Variable(canonicalName, 0);
    }

    public static int totalParticleCount() {
        ParticleEngineQueryAccessor engine = (ParticleEngineQueryAccessor) Minecraft.getInstance().particleEngine;
        return engine.kltyton_bedrock_runtime$getParticlesToAdd().size() + engine.kltyton_bedrock_runtime$getParticles().values().stream()
                .mapToInt(ParticleGroup::size)
                .sum();
    }

    private static void registerQueryVariable(String name, ToDoubleFunction<MolangInstance> value) {
        checkFrozen();
        String canonical = applyQueryAliases(name.toLowerCase(Locale.ROOT));
        if (!canonical.startsWith("query.")) throw new IllegalArgumentException("Expected query namespace: " + name);
        checkUnregistered(canonical);
        UNFROZEN_QUERIES.put(canonical, new Variable(canonical, java.util.Objects.requireNonNull(value)));
    }

    private static void registerQueryFunction(String name, Function<List<MathValue>, MathValue> factory) {
        checkFrozen();
        String canonical = applyQueryAliases(name.toLowerCase(Locale.ROOT));
        if (!canonical.startsWith("query.")) throw new IllegalArgumentException("Expected query namespace: " + name);
        if (QUERY_FUNCTIONS.putIfAbsent(canonical, factory) != null) {
            throw new IllegalArgumentException("Duplicate Molang query function: " + canonical);
        }
    }

    private static void registerEntityQueryVariable(EntityType<?> type, String name, ToDoubleFunction<MolangInstance> provider) {
        checkFrozen();
        String canonical = applyQueryAliases(name.toLowerCase(Locale.ROOT));
        if (!canonical.startsWith("query.")) throw new IllegalArgumentException("Expected query namespace: " + name);
        if (ENTITY_QUERY_VARIABLES.computeIfAbsent(canonical, ignored -> new HashMap<>())
                .putIfAbsent(java.util.Objects.requireNonNull(type), java.util.Objects.requireNonNull(provider)) != null) {
            throw new IllegalArgumentException("Duplicate entity Molang query variable: " + canonical + " for " + type);
        }
    }

    private static void registerEntityQueryFunction(EntityType<?> type, String name,
                                                    Function<List<MathValue>, MathValue> factory) {
        checkFrozen();
        String canonical = applyQueryAliases(name.toLowerCase(Locale.ROOT));
        if (!canonical.startsWith("query.")) throw new IllegalArgumentException("Expected query namespace: " + name);
        if (ENTITY_QUERY_FUNCTIONS.computeIfAbsent(canonical, ignored -> new HashMap<>())
                .putIfAbsent(java.util.Objects.requireNonNull(type), java.util.Objects.requireNonNull(factory)) != null) {
            throw new IllegalArgumentException("Duplicate entity Molang query function: " + canonical + " for " + type);
        }
    }

    private static Variable bindEntityQueryVariable(String name, Variable fallback,
            Map<EntityType<?>, ToDoubleFunction<MolangInstance>> providers) {
        return new Variable(name, instance -> {
            Entity entity = instance.getAttachedEntity();
            ToDoubleFunction<MolangInstance> provider = entity == null ? null : providers.get(entity.getType());
            return provider != null ? provider.applyAsDouble(instance) : fallback == null ? 0.0 : fallback.get(instance);
        });
    }

    private static void bindEntityQueryVariables() {
        ENTITY_QUERY_VARIABLES.forEach((name, providers) -> UNFROZEN_QUERIES.put(name,
                bindEntityQueryVariable(name, UNFROZEN_QUERIES.get(name), Map.copyOf(providers))));
    }

    private static void registerContext(String name, Function<MolangInstance, Object> provider) {
        checkFrozen();
        String canonical = applyPrefixAliases(name.toLowerCase(Locale.ROOT), "context.", "c.");
        if (!canonical.startsWith("context.")) throw new IllegalArgumentException("Expected context namespace: " + name);
        if (CONTEXT_PROVIDERS.putIfAbsent(canonical, provider) != null) {
            throw new IllegalArgumentException("Duplicate Molang context provider: " + canonical);
        }
    }

    private static void registerActorVariables(EntityType<?> type, Function<Entity, Map<String, ?>> provider) {
        checkFrozen();
        ACTOR_VARIABLE_PROVIDERS.computeIfAbsent(type, ignored -> new java.util.ArrayList<>()).add(provider);
    }

    private static void registerActorVariableWriter(EntityType<?> type, MolangActorVariableWriter writer) {
        checkFrozen();
        ACTOR_VARIABLE_WRITERS.computeIfAbsent(type, ignored -> new java.util.ArrayList<>()).add(writer);
    }

    private static void registerVersionedCompatibilityQuery(String name,
            Function<MolangVersion, ToDoubleFunction<MolangInstance>> factory) {
        if (UNFROZEN_QUERIES.putIfAbsent(name, new Variable(name, factory.apply(MolangVersion.LATEST))) == null) {
            VERSIONED_QUERY_DEFAULTS.put(name, factory);
        }
    }

    private static void registerCompatibilityQuery(String name, ToDoubleFunction<MolangInstance> value) {
        UNFROZEN_QUERIES.putIfAbsent(name, new Variable(name, value));
    }

    private static double animationTime(MolangInstance instance) {
        if (instance.getReferencedItem() != null) return 0.0;
        if (ActorAccess.isActorReference(instance)) return ActorAccess.actorAnimationTime(instance);
        if (instance.getEmitter() == null) {
            return instance.getAttachedEntity() instanceof com.kltyton.bedrockruntime.client.particle.access.IEntity entity
                    ? entity.kltyton_bedrock_runtime$getAnimationTime() : 0.0;
        }
        return ActorAccess.isInitializingDeferredParticle(instance) ? 0.0 : AnimationFrameContext.currentTime();
    }

    private static double animationLifetime(MolangInstance instance) {
        if (instance.getReferencedItem() != null) return 0.0;
        if (instance.getEmitter() == null && !ActorAccess.isActorReference(instance)) {
            return instance.getAttachedEntity() instanceof com.kltyton.bedrockruntime.client.particle.access.IEntity entity
                    ? entity.kltyton_bedrock_runtime$getAnimationLifetime() : 0.0;
        }
        if (!ActorAccess.isActorReference(instance)) return animationTime(instance);
        return ActorAccess.actorAnimationLifetime(instance);
    }

    private static void registerEntityQuery(String name, Predicate<Entity> predicate) {
        registerCompatibilityQuery(name, instance -> {
            Entity entity = instance.getAttachedEntity();
            return entity != null && predicate.test(entity) ? 1.0 : 0.0;
        });
    }

    private static void checkUnregistered(String name) {
        if (UNFROZEN_QUERIES.containsKey(name)) {
            throw new IllegalArgumentException(name + " had already registered!");
        }
    }

    private static void checkFrozen() {
        if (!FROZEN_QUERIES.isEmpty()) throw new UnsupportedOperationException("Had already frozen!");
    }

    /**
     * Parse a given string formatted with a prefix, swapping out any potential aliases for the defined proper name
     *
     * @param text       The base text to parse
     * @param properName The "correct" prefix to apply
     * @param aliases    The available prefixes to check and replace
     * @return The unaliased string, or the original string if no aliases match
     */
    public static String applyPrefixAliases(String text, String properName, String... aliases) {
        for (String alias : aliases) {
            if (text.startsWith(alias))
                return properName + text.substring(alias.length());
        }

        return text;
    }

    public static String applyQueryAliases(String text) {
        if (text.startsWith("q.")) {
            return "query" + text.substring(1);
        }
        return text;
    }

    private static int cardinalFacing(Entity entity, boolean horizontal) {
        float pitch = -entity.getXRot() * Mth.DEG_TO_RAD;
        float yaw = horizontal ? -(float) Math.PI - entity.getYRot() * Mth.DEG_TO_RAD
                : -entity.getYRot() * Mth.DEG_TO_RAD - (float) Math.PI;
        float pitchCosine = -TrigonometricLookup.cos(pitch);
        float x = pitchCosine * TrigonometricLookup.sin(yaw);
        float z = TrigonometricLookup.cos(yaw) * pitchCosine;
        if (horizontal) {
            if (!(z * z + x * x > 0.01F)) return 2;
            return x != 0.0F && Math.abs(x) > Math.abs(z) ? (x > 0.0F ? 5 : 4) : (z > 0.0F ? 3 : 2);
        }
        float y = TrigonometricLookup.sin(pitch);
        if (!(z * z + y * y + x * x > 0.01F)) return 1;
        float absY = Math.abs(y), absX = Math.abs(x), absZ = Math.abs(z);
        if (absY > absZ && absY > absX) return y > 0.0F ? 1 : 0;
        if (absX > absY && absX > absZ) return x > 0.0F ? 5 : 4;
        return z > 0.0F ? 3 : 2;
    }

    private static int moonPhase(MolangInstance instance) {
        int time = (int) instance.getLevel().getDefaultClockTime();
        return ((time >> 31) ^ Math.abs(time / 24000)) & 7;
    }

    private static void setDefaultQueryValues() {
        registerQueryVariable("query.cardinal_player_facing", p -> p.getAttachedEntity() instanceof Player player
                ? cardinalFacing(player, false) : 6.0);
        registerQueryVariable("query.day", p -> p.getAttachedEntity() == null ? 0.0
                : ((float) (int) p.getLevel().getDefaultClockTime() + 6000.0F) / 24000.0F);
        registerQueryVariable("query.has_cape", p -> p.getAttachedEntity() instanceof AbstractClientPlayer player && player.getSkin().cape() != null ? 1 : 0);
        registerQueryVariable("query.is_first_person", p -> {
            Boolean perspective = p.getFirstPersonPerspective();
            if (perspective != null) return perspective ? 1.0 : 0.0;
            if (ActorAccess.isInitializingDeferredParticle(p)) return 0.0;
            return p.getAttachedEntity() != null && p.getAttachedEntity() == Minecraft.getInstance().player
                    && Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON ? 1.0 : 0.0;
        });
        registerQueryVariable("query.moon_brightness", p -> {
            if (p.getAttachedEntity() == null) return 0.0;
            return DimensionType.MOON_BRIGHTNESS_PER_PHASE[moonPhase(p)];
        });
        registerQueryVariable("query.moon_phase", p -> p.getAttachedEntity() == null ? 0.0
                : moonPhase(p));
        registerQueryVariable("query.player_level", p -> p.getAttachedEntity() instanceof Player player ? player.experienceLevel : 0.0);
        registerQueryVariable("query.time_of_day", p -> p.getAttachedEntity() == null ? 0.0
                : (((float) (int) p.getLevel().getDefaultClockTime() + 6000.0F) % 24000.0F) / 24000.0F);
        registerQueryVariable("query.time_stamp", p -> p.getAttachedEntity() == null ? 0.0 : p.getLevel().getGameTime());
        registerQueryVariable("query.total_emitter_count", p -> {
            if (p.getReferencedItem() != null || p.getEmitter() == null || ActorAccess.isInitializingDeferredComponents(p)) return 0.0;
            var manager = MolangParticleEngine.managerFor(p);
            return manager == null ? 0.0 : manager.totalEmitterCount();
        });
        registerQueryVariable("query.total_particle_count", p -> {
            if (p.getReferencedItem() != null || p.getEmitter() == null || ActorAccess.isInitializingDeferredComponents(p)) return 0.0;
            var manager = MolangParticleEngine.managerFor(p);
            return manager == null ? 0.0 : manager.isIndependentParticles() ? manager.totalParticleCount() : totalParticleCount();
        });
        registerQueryVariable("query.attached_x", p -> p.getAttachedEntity() == null ? 0.0 : p.getAttachedEntity().getX());
        registerQueryVariable("query.attached_y", p -> p.getAttachedEntity() == null ? 0.0 : p.getAttachedEntity().getY());
        registerQueryVariable("query.attached_z", p -> p.getAttachedEntity() == null ? 0.0 : p.getAttachedEntity().getZ());
        registerQueryVariable("query.attached_xo", p -> p.getAttachedEntity() == null ? 0.0 : p.getAttachedEntity().xo);
        registerQueryVariable("query.attached_yo", p -> p.getAttachedEntity() == null ? 0.0 : p.getAttachedEntity().yo);
        registerQueryVariable("query.attached_zo", p -> p.getAttachedEntity() == null ? 0.0 : p.getAttachedEntity().zo);
        ParticleEvents.post(new RegisterMolangQueriesEvent(MolangQueries::registerQueryVariable,
                MolangQueries::registerQueryFunction, MolangQueries::registerContext, MolangQueries::registerActorVariables,
                MolangQueries::registerActorVariableWriter, MolangQueries::registerEntityQueryVariable,
                MolangQueries::registerEntityQueryFunction));
        registerCompatibilityQuery("query.anim_time", MolangQueries::animationTime);
        registerCompatibilityQuery("query.life_time", MolangQueries::animationLifetime);
        registerCompatibilityQuery("query.key_frame_lerp_time", instance -> {
            if (instance.getReferencedItem() != null) return 0.0;
            if (ActorAccess.isActorReference(instance)) return ActorAccess.actorKeyframeLerpTime(instance);
            return instance.getEmitter() == null
                    && instance.getAttachedEntity() instanceof com.kltyton.bedrockruntime.client.particle.access.IEntity entity
                    ? entity.kltyton_bedrock_runtime$getKeyframeLerpTime() : 0.0;
        });
        registerCompatibilityQuery("query.max_durability", ItemDurabilityQueries::maximum);
        registerCompatibilityQuery("query.remaining_durability", ItemDurabilityQueries::remaining);
        registerCompatibilityQuery("query.main_hand_item_max_duration", instance -> {
            if (!(instance.getAttachedEntity() instanceof LivingEntity entity)) return 0.0;
            var stack = entity.getMainHandItem();
            return stack.isEmpty() ? 0.0 : ItemUseDurations.maximum(stack, entity);
        });
        registerCompatibilityQuery("query.item_max_use_duration", instance -> {
            if (!(instance.getAttachedEntity() instanceof LivingEntity entity)) return 0.0;
            var stack = entity.getMainHandItem();
            return stack.isEmpty() ? 0.0 : (float) ItemUseDurations.maximum(stack, entity) * 20.0F;
        });
        registerCompatibilityQuery("query.main_hand_item_use_duration", instance -> {
            if (!(instance.getAttachedEntity() instanceof Player player) || player.getMainHandItem().isEmpty()) return 0.0;
            return ItemUseDurations.remaining(player);
        });
        registerCompatibilityQuery("query.is_selected_item", instance ->
                instance.getAttachedEntity() instanceof Player player && !player.getMainHandItem().isEmpty() ? 1.0 : 0.0);
        registerCompatibilityQuery("query.cardinal_facing", p -> p.getAttachedEntity() == null
                ? 6.0 : cardinalFacing(p.getAttachedEntity(), false));
        registerCompatibilityQuery("query.cardinal_facing_2d", p -> p.getAttachedEntity() == null
                ? 6.0 : cardinalFacing(p.getAttachedEntity(), true));
        registerCompatibilityQuery("query.client_max_render_distance", p -> p.getAttachedEntity() == null || ActorAccess.hasEmptyBaseRenderContext(p)
                ? 0.0 : Minecraft.getInstance().options.renderDistance().get());
        registerCompatibilityQuery("query.client_memory_tier", p -> p.getAttachedEntity() == null || ActorAccess.hasEmptyBaseRenderContext(p)
                ? 0.0 : com.kltyton.bedrockruntime.client.particle.molang.query.ClientMemoryTier.value());
        ToDoubleFunction<MolangInstance> frameTime = p ->
                ((DeltaTrackerQueryAccessor) Minecraft.getInstance().getDeltaTracker()).kltyton_bedrock_runtime$getRealtimeDeltaTicks() / 20.0;
        registerCompatibilityQuery("query.delta_time", instance -> {
            if (instance.getReferencedItem() != null || ActorAccess.isInitializingDeferredParticle(instance)) return 0.0;
            if (ActorAccess.isActorReference(instance)) return ActorAccess.actorAnimationDeltaTime(instance);
            if (instance.getEmitter() == null) {
                return instance.getAttachedEntity() instanceof com.kltyton.bedrockruntime.client.particle.access.IEntity entity
                        ? entity.kltyton_bedrock_runtime$getAnimationDeltaTime() : 0.0;
            }
            return frameTime.applyAsDouble(instance);
        });
        registerCompatibilityQuery("query.last_frame_time", instance -> ActorAccess.hasEmptyBaseRenderContext(instance)
                ? 0.0 : FrameTimeHistory.query(FrameTimeHistory.Kind.LAST, 0));
        registerCompatibilityQuery("query.distance_from_camera", instance -> {
            if (instance.getReferencedItem() != null || ActorAccess.isInitializingDeferredComponents(instance)) return 0.0;
            if (instance instanceof ParticleEmitter || instance instanceof IMolangParticleInstance) {
                return instance.getEmitter().getCameraDistance();
            }
            return MolangParticleEngine.sampleCameraDistance(instance, instance.getPosition());
        });
        registerCompatibilityQuery("query.effect_emitter_count", instance -> {
            if (instance.getReferencedItem() != null || ActorAccess.isInitializingDeferredComponents(instance)) return 0.0;
            var manager = MolangParticleEngine.managerFor(instance);
            if (manager == null) return 0.0;
            if (instance instanceof IMolangParticleInstance particle) {
                return manager.effectEmitterCount(particle.getPreset().effect.description.identifier());
            }
            var emitter = instance.getEmitter();
            if (emitter == null) return 0.0;
            var preset = emitter.getParticlePreset();
            return manager.effectEmitterCount(preset == null
                    ? instance.getIdentity() : preset.effect.description.identifier());
        });
        registerCompatibilityQuery("query.effect_particle_count", instance -> {
            if (instance.getReferencedItem() != null || ActorAccess.isInitializingDeferredComponents(instance)) return 0.0;
            var manager = MolangParticleEngine.managerFor(instance);
            if (manager == null) return 0.0;
            if (instance instanceof IMolangParticleInstance particle) {
                return manager.effectParticleCount(particle.getPreset().effect.description.identifier());
            }
            var emitter = instance.getEmitter();
            if (emitter == null) return 0.0;
            var preset = emitter.getParticlePreset();
            return manager.effectParticleCount(preset == null
                    ? instance.getIdentity() : preset.effect.description.identifier());
        });
        registerCompatibilityQuery("query.frame_alpha", instance -> {
            if (instance.getReferencedItem() != null || ActorAccess.isInitializingDeferredParticle(instance)) return 0.0;
            if (ActorAccess.isActorReference(instance)) return ActorAccess.actorAnimationFrameAlpha(instance);
            if (instance.getEmitter() == null) {
                return instance.getAttachedEntity() instanceof com.kltyton.bedrockruntime.client.particle.access.IEntity entity
                        ? entity.kltyton_bedrock_runtime$getAnimationFrameAlpha() : 0.0;
            }
            return RenderFrameContext.currentOr(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
        });
        registerCompatibilityQuery("query.body_x_rotation", instance -> {
            Entity entity = instance.getAttachedEntity();
            if (entity == null) return 0.0;
            float delta = (entity.getXRot() - entity.xRotO + 180.0F) % 360.0F;
            if (delta < 0.0F) delta += 360.0F;
            float partial = RenderFrameContext.currentOr(
                    Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
            return (delta - 180.0F) * partial + entity.xRotO;
        });
        registerCompatibilityQuery("query.body_y_rotation", instance -> {
            Entity entity = instance.getAttachedEntity();
            if (entity == null) return 0.0;
            float partial = RenderFrameContext.currentOr(
                    Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
            return entity instanceof LivingEntity living
                    ? Mth.rotLerp(partial, living.yBodyRotO, living.yBodyRot)
                    : Mth.rotLerp(partial, entity.yRotO, entity.getYRot());
        });
        registerCompatibilityQuery("query.lie_amount", instance ->
                instance.getAttachedEntity() instanceof Cat cat
                        ? cat.getLieDownAmount(RenderFrameContext.currentOr(
                                Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false)))
                        : 0.0);
        registerCompatibilityQuery("query.swelling_dir", instance ->
                instance.getAttachedEntity() instanceof Creeper creeper ? creeper.getSwellDir() : 0.0);
        registerCompatibilityQuery("query.ground_speed", p -> {
            Entity entity = p.getAttachedEntity();
            if (entity == null) return 0.0;
            float rate = entity.level().tickRateManager().tickrate();
            float x = (float) (entity.getX() - entity.xo) * rate;
            float y = (float) (entity.getY() - entity.yo) * rate;
            float z = (float) (entity.getZ() - entity.zo) * rate;
            return (float) Math.sqrt(z * z + y * y + x * x);
        });
        registerCompatibilityQuery("query.vertical_speed", p -> {
            Entity entity = p.getAttachedEntity();
            return entity == null ? 0.0 : (float) (entity.getY() - entity.yo) * entity.level().tickRateManager().tickrate();
        });
        registerCompatibilityQuery("query.health", p -> p.getAttachedEntity() instanceof LivingEntity living ? living.getHealth() : 0.0);
        registerCompatibilityQuery("query.max_health", p -> p.getAttachedEntity() instanceof LivingEntity living ? living.getMaxHealth() : 0.0);
        registerCompatibilityQuery("query.death_ticks", p -> p.getAttachedEntity() instanceof LivingEntity living ? living.deathTime : 0.0);
        registerCompatibilityQuery("query.current_squish_value", p -> p.getAttachedEntity() instanceof AbstractCubeMob cube ? cube.squish : 0.0);
        registerCompatibilityQuery("query.previous_squish_value", p -> p.getAttachedEntity() instanceof AbstractCubeMob cube ? cube.oSquish : 0.0);
        registerEntityQuery("query.is_alive", Entity::isAlive);
        registerEntityQuery("query.has_owner", entity -> entity instanceof TamableAnimal animal
                && animal.getOwnerReference() != null);
        registerEntityQuery("query.is_laying_egg", entity -> entity instanceof Turtle turtle && turtle.isLayingEgg());
        registerEntityQuery("query.is_standing", entity -> entity instanceof AbstractHorse horse && horse.isStanding()
                || entity instanceof PolarBear bear && bear.isStanding());
        registerEntityQuery("query.is_resting", entity -> entity instanceof Bat bat && bat.isResting());
        registerEntityQuery("query.is_charging", entity -> entity instanceof net.minecraft.world.entity.monster.Vex vex && vex.isCharging()
                || entity instanceof Blaze blaze && blaze.isOnFire()
                || entity instanceof Ghast ghast && ghast.isCharging());
        registerCompatibilityQuery("query.sneeze_counter", instance -> instance.getAttachedEntity() instanceof Panda panda
                ? panda.getSneezeCounter() : 0.0);
        registerEntityQuery("query.show_bottom", entity -> entity instanceof EndCrystal crystal && crystal.showsBottom());
        registerEntityQuery("query.is_laying_down", entity -> entity instanceof Cat cat && cat.isLying());
        registerEntityQuery("query.is_angry", entity -> entity instanceof Wolf wolf && wolf.isAngry()
                || entity instanceof Bee bee && bee.isAngry());
        registerEntityQuery("query.is_on_ground", Entity::onGround);
        registerEntityQuery("query.is_on_fire", Entity::isOnFire);
        registerEntityQuery("query.is_onfire", Entity::isOnFire);
        registerEntityQuery("query.is_fire_immune", Entity::fireImmune);
        registerEntityQuery("query.is_charged", entity -> entity instanceof Creeper creeper && creeper.isPowered()
                || entity instanceof Blaze blaze && blaze.isOnFire());
        registerEntityQuery("query.is_casting", entity -> entity instanceof SpellcasterIllager caster && caster.isCastingSpell());
        registerVersionedCompatibilityQuery("query.is_carrying_block", version -> instance -> {
            if (!(instance.getAttachedEntity() instanceof EnderMan enderman)) return 0.0;
            var block = enderman.getCarriedBlock();
            return block != null && (version.atLeast(1, 21, 100) || !block.isAir()) ? 1.0 : 0.0;
        });
        registerEntityQuery("query.is_playing_dead", entity -> entity instanceof Axolotl axolotl && axolotl.isPlayingDead());
        registerEntityQuery("query.is_dancing", entity -> entity instanceof Allay allay && allay.isDancing()
                || entity instanceof Piglin piglin && piglin.isDancing()
                || entity instanceof Parrot parrot && parrot.isPartyParrot());
        registerEntityQuery("query.is_eating", entity -> entity instanceof Panda panda && panda.isEating()
                || entity instanceof AbstractHorse horse && horse.isEating());
        registerEntityQuery("query.is_grazing", entity -> entity instanceof AbstractHorse horse && horse.isEating());
        registerEntityQuery("query.is_interested", entity -> entity instanceof Wolf wolf && wolf.isInterested()
                || entity instanceof Fox fox && fox.isInterested());
        registerEntityQuery("query.is_shaking_wetness", entity -> entity instanceof Wolf wolf
                && ((WolfQueryAccessor) wolf).kltyton_bedrock_runtime$isShakingWetness());
        registerEntityQuery("query.is_croaking", entity -> entity.hasPose(Pose.CROAKING));
        registerEntityQuery("query.is_roaring", entity -> entity.hasPose(Pose.ROARING));
        registerEntityQuery("query.is_emerging", entity -> entity.hasPose(Pose.EMERGING));
        registerEntityQuery("query.is_digging", entity -> entity.hasPose(Pose.DIGGING)
                || entity instanceof Sniffer sniffer && ((SnifferQueryAccessor) sniffer).kltyton_bedrock_runtime$getState() == Sniffer.State.DIGGING);
        registerEntityQuery("query.is_sniffing", entity -> entity.hasPose(Pose.SNIFFING)
                || entity instanceof Sniffer sniffer && ((SnifferQueryAccessor) sniffer).kltyton_bedrock_runtime$getState() == Sniffer.State.SNIFFING);
        registerEntityQuery("query.timer_flag_1", entity -> entity instanceof Sniffer sniffer
                && ((SnifferQueryAccessor) sniffer).kltyton_bedrock_runtime$getState() == Sniffer.State.SCENTING);
        registerEntityQuery("query.timer_flag_2", entity -> entity instanceof Sniffer sniffer
                && ((SnifferQueryAccessor) sniffer).kltyton_bedrock_runtime$getState() == Sniffer.State.RISING);
        registerEntityQuery("query.timer_flag_3", entity -> entity instanceof Sniffer sniffer
                && ((SnifferQueryAccessor) sniffer).kltyton_bedrock_runtime$getState() == Sniffer.State.FEELING_HAPPY);
        registerCompatibilityQuery("query.is_scenting", instance -> getQueryFor("query.timer_flag_1").get(instance));
        registerCompatibilityQuery("query.is_rising", instance -> getQueryFor("query.timer_flag_2").get(instance));
        registerCompatibilityQuery("query.is_feeling_happy", instance -> getQueryFor("query.timer_flag_3").get(instance));
        registerEntityQuery("query.is_searching", entity -> entity instanceof Sniffer sniffer && sniffer.isSearching());
        registerEntityQuery("query.is_powered", entity -> entity instanceof Creeper creeper && creeper.isPowered()
                || entity instanceof Bee bee && bee.hasNectar());
        registerEntityQuery("query.is_ignited", entity -> entity instanceof Creeper creeper && creeper.isIgnited());
        registerEntityQuery("query.is_chested", entity -> entity instanceof AbstractChestedHorse horse && horse.hasChest());
        registerEntityQuery("query.is_in_water", Entity::isInWater);
        registerEntityQuery("query.head_is_in_water", entity -> entity.isEyeInFluid(FluidTags.WATER));
        registerEntityQuery("query.is_in_lava", Entity::isInLava);
        registerEntityQuery("query.is_in_water_or_rain", Entity::isInWaterOrRain);
        registerEntityQuery("query.is_sneaking", Entity::isShiftKeyDown);
        registerEntityQuery("query.is_sitting", entity -> entity instanceof TamableAnimal animal && animal.isInSittingPose()
                || entity instanceof Camel camel && camel.isCamelSitting()
                || entity instanceof Fox fox && fox.isSitting()
                || entity instanceof Panda panda && panda.isSitting()
                || entity instanceof EnderDragon dragon && dragon.getPhaseManager().getCurrentPhase().isSitting());
        registerEntityQuery("query.is_rolling", entity -> entity instanceof Panda panda && panda.isRolling());
        registerEntityQuery("query.is_sprinting", Entity::isSprinting);
        registerEntityQuery("query.is_swimming", Entity::isSwimming);
        registerEntityQuery("query.is_crawling", Entity::isVisuallyCrawling);
        registerEntityQuery("query.is_riding", Entity::isPassenger);
        registerEntityQuery("query.is_leashed", entity -> entity instanceof Leashable leashable && leashable.isLeashed());
        registerEntityQuery("query.has_rider", entity -> !entity.getPassengers().isEmpty());
        registerEntityQuery("query.has_player_rider", entity -> entity.getPassengers().stream().anyMatch(Player.class::isInstance));
        registerEntityQuery("query.has_head_gear", entity -> entity instanceof LivingEntity living && !living.getItemBySlot(EquipmentSlot.HEAD).isEmpty());
        registerEntityQuery("query.is_saddled", entity -> entity instanceof Mob mob && mob.isSaddled());
        registerEntityQuery("query.is_using_item", entity -> entity instanceof LivingEntity living && living.isUsingItem());
        registerEntityQuery("query.is_tamed", entity -> entity instanceof TamableAnimal animal && animal.isTame()
                || entity instanceof AbstractHorse horse && horse.isTamed());
        registerEntityQuery("query.is_sheared", entity -> entity instanceof Sheep sheep && sheep.isSheared()
                || entity instanceof Bogged bogged && bogged.isSheared());
        registerEntityQuery("query.blocking", entity -> entity instanceof LivingEntity living && living.isBlocking());
        registerCompatibilityQuery("query.item_is_charged", ItemChargedFunction::mainHand);
        registerCompatibilityQuery("query.is_item_equipped", ItemEquippedFunction::mainHand);
        registerCompatibilityQuery("query.equipment_count", instance -> {
            if (!(instance.getAttachedEntity() instanceof LivingEntity living)) return 0.0;
            int count = 0;
            for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                    EquipmentSlot.FEET, EquipmentSlot.BODY)) {
                if (!living.getItemBySlot(slot).isEmpty()) count++;
            }
            return count;
        });
        registerEntityQuery("query.is_invisible", Entity::isInvisible);
        registerEntityQuery("query.is_levitating", entity -> entity instanceof LivingEntity living
                && living.hasEffect(MobEffects.LEVITATION));
        registerEntityQuery("query.is_silent", Entity::isSilent);
        registerEntityQuery("query.is_spectator", Entity::isSpectator);
        registerEntityQuery("query.has_gravity", entity -> !entity.isNoGravity());
        registerEntityQuery("query.is_local_player", entity -> entity == Minecraft.getInstance().player);
        registerEntityQuery("query.is_baby", entity -> entity instanceof LivingEntity living && living.isBaby());
        registerEntityQuery("query.is_sleeping", entity -> entity instanceof LivingEntity living && living.isSleeping());
        registerEntityQuery("query.is_gliding", entity -> entity instanceof LivingEntity living && living.isFallFlying());
        bindEntityQueryVariables();
        FROZEN_QUERIES.putAll(UNFROZEN_QUERIES);
        UNFROZEN_QUERIES.clear();
    }
}
