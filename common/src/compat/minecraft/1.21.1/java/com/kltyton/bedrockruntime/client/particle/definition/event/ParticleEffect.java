package com.kltyton.bedrockruntime.client.particle.definition.event;

import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleEngine;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus;
import com.kltyton.bedrockruntime.client.particle.api.IEventNode;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.api.RegisterCustomEmitterTypeEvent;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.molang.FloatMolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangQueries;
import com.kltyton.bedrockruntime.client.particle.definition.component.EmitterRate;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleManager;
import com.kltyton.bedrockruntime.client.particle.runtime.screen.PreviewParticleScope;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.ArrayDeque;
import java.util.function.IntFunction;

public record ParticleEffect(ResourceLocation effect, Type type, MolangExp preEffectExpression,
                             List<SharedVar> sharedVars) implements IEventNode {
    public record SharedVar(String name, @Nullable FloatMolangExp defaultValue) {
        public SharedVar {
            name = MolangQueries.applyPrefixAliases(name.trim().toLowerCase(Locale.ROOT), "variable.", "v.");
        }

        public SharedVar(String name) {
            this(name, null);
        }
    }

    private static final Codec<List<SharedVar>> SHARED_VARS_CODEC = Codec.either(
            Codec.unboundedMap(Codec.STRING, FloatMolangExp.CODEC), Codec.STRING.listOf()
    ).xmap(either -> either.map(map -> {
        List<SharedVar> result = new ArrayList<>(map.size());
        map.forEach((name, value) -> result.add(new SharedVar(name, value)));
        return result;
    }, names -> names.stream().map(SharedVar::new).toList()), vars -> {
        if (vars.stream().noneMatch(var -> var.defaultValue() != null)) {
            return Either.right(vars.stream().map(SharedVar::name).toList());
        }
        Map<String, FloatMolangExp> result = new LinkedHashMap<>();
        for (SharedVar var : vars) {
            result.put(var.name(), var.defaultValue() == null ? FloatMolangExp.ZERO : var.defaultValue());
        }
        return Either.left(result);
    });

    public static final Codec<ParticleEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("effect").forGetter(ParticleEffect::effect),
            Type.CODEC.fieldOf("type").forGetter(ParticleEffect::type),
            MolangExp.CODEC.lenientOptionalFieldOf("pre_effect_expression", MolangExp.EMPTY).forGetter(ParticleEffect::preEffectExpression),
            SHARED_VARS_CODEC.optionalFieldOf("shared_vars", List.of()).forGetter(ParticleEffect::sharedVars)
    ).apply(instance, ParticleEffect::new));

    public ParticleEffect(ResourceLocation effect, Type type, MolangExp preEffectExpression) {
        this(effect, type, preEffectExpression, List.of());
    }

    private static final ThreadLocal<ArrayDeque<SpawnContext>> SPAWN_CONTEXT = new ThreadLocal<>();

    @ApiStatus.Internal
    public record SpawnContext(ParticleEmitter parent, ParticleEffect effect, Vec3 position, @Nullable Vector3f velocity,
                               org.joml.Matrix3f basis) {
        public SpawnContext(ParticleEmitter parent, ParticleEffect effect, Vec3 position, @Nullable Vector3f velocity) {
            this(parent, effect, position, velocity, new org.joml.Matrix3f());
        }
    }

    @ApiStatus.Internal
    public static @Nullable SpawnContext spawnContext(ParticleEmitter parent, ParticleEffect effect) {
        ArrayDeque<SpawnContext> stack = SPAWN_CONTEXT.get();
        SpawnContext context = stack == null ? null : stack.peek();
        return context != null && context.parent() == parent && context.effect() == effect ? context : null;
    }

    @Override
    public void execute(MolangInstance instance) {
        MolangParticleManager manager = MolangParticleEngine.managerFor(instance);
        if (manager != null && manager.isIndependentParticles() && manager.isConstructingPreviewNativeEngine()) {
            manager.deferPreviewParticleAction(() -> execute(instance));
            return;
        }
        boolean scoped = manager != null && manager.isIndependentParticles();
        if (scoped) PreviewParticleScope.enter(manager, (ClientLevel) instance.getLevel(), manager.previewCamera());
        try {
            ParticleEmitter source = instance.getEmitter();
            Vec3 position = type == Type.EMITTER_BOUND ? source.getPosition() : EventResolver.position(instance);
            Vector3f velocity = type == Type.PARTICLE_WITH_VELOCITY ? EventResolver.velocity(instance) : null;
            boolean particleEvent = type == Type.PARTICLE || type == Type.PARTICLE_WITH_VELOCITY;
            if (particleEvent && manager != null) {
                ParticleEmitter existing = source.findManualEventEmitter(this);
                if (existing != null) {
                    existing.emitManualEvent(this, position, velocity, false);
                    return;
                }
            }
            ArrayDeque<SpawnContext> stack = SPAWN_CONTEXT.get();
            if (stack == null) {
                stack = new ArrayDeque<>();
                SPAWN_CONTEXT.set(stack);
            }
            org.joml.Matrix3f basis = type == Type.EMITTER ? EventResolver.basis(instance) : new org.joml.Matrix3f();
            SpawnContext spawn = new SpawnContext(source, this, position, velocity, basis);
            stack.push(spawn);
            ParticleEmitter emitter;
            try {
                emitter = RegisterCustomEmitterTypeEvent.create(source, this);
            } finally {
                stack.pop();
                if (stack.isEmpty()) SPAWN_CONTEXT.remove();
            }
            if (!emitter.consumeEventSpawnContext(spawn)) {
                emitter.setPos(particleEvent ? Vec3.ZERO : position);
                emitter.posO = emitter.pos;
                if (type == Type.EMITTER) emitter.setEffectFrame(basis);
            }
            boolean reusable = particleEvent && manager != null;
            if (reusable) source.registerManualEventEmitter(this, emitter);
            MolangParticleEngine.addEmitter(emitter);
            if (reusable && !emitter.isRemoved()) {
                emitter.emitManualEvent(this, position, velocity, true);
            }
        } finally {
            if (scoped) PreviewParticleScope.exit();
        }
    }

    @Override
    public String toString() {
        return "ParticleEffect{" +
                "effect=" + effect +
                ", type=" + type +
                ", preEffectExpression=" + preEffectExpression +
                ", sharedVars=" + sharedVars +
                '}';
    }

    public enum Type implements StringRepresentable {
        /// Create an emitter of the specified particle effect at the event's world location
        EMITTER,
        /// Create an emitter of the specified particle effect at the event's location.
        ///
        /// If the firing emitter is bound to an entity or locator, the new emitter will be bound to the same entity or locator.
        EMITTER_BOUND,
        /// Manually emit a particle on an emitter of the specified type at the event location, creating the emitter if it doesn't already exist.
        ///
        /// Make sure to use the Spawn Amount mode "Manual" on the child particle effect.
        PARTICLE,
        /// The same as "Particle" except the new particle will inherit the spawning particle's velocity.
        PARTICLE_WITH_VELOCITY;

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);
        public static final StreamCodec<ByteBuf, Type> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Enum::ordinal,
                Type::getById
        );
        private static final IntFunction<Type> BY_ID = ByIdMap.continuous(Type::getId, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);

        public int getId() {
            return ordinal();
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Type getById(int id) {
            return BY_ID.apply(id);
        }
    }
}
