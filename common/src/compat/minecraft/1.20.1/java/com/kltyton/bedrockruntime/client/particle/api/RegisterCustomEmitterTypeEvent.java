package com.kltyton.bedrockruntime.client.particle.api;

import com.kltyton.bedrockruntime.client.particle.api.event.ParticleEvents;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import com.kltyton.bedrockruntime.client.particle.definition.event.ParticleEffect;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;
import com.kltyton.bedrockruntime.client.particle.runtime.attachment.PresetVarsParticleEmitter;
import com.kltyton.bedrockruntime.client.particle.runtime.attachment.WithBlockParticleEmitter;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public class RegisterCustomEmitterTypeEvent {

    @FunctionalInterface
    public interface Callback {
        void onRegister(RegisterCustomEmitterTypeEvent event);
    }

    private static final Map<ResourceLocation, BiFunction<Level, CompoundTag, ? extends ParticleEmitter>> fromNbt = new Object2ObjectOpenHashMap<>();
    private static final Map<ResourceLocation, BiFunction<ParticleEmitter, ParticleEffect, ? extends ParticleEmitter>> fromEffect = new Object2ObjectOpenHashMap<>();
    private static final Set<ResourceLocation> managedEffectFactories = new HashSet<>();

    private RegisterCustomEmitterTypeEvent() {}

    public static void postEvent() {
        if (fromNbt.isEmpty()) {
            RegisterCustomEmitterTypeEvent event = new RegisterCustomEmitterTypeEvent();
            event.register(WithBlockParticleEmitter.TYPE, WithBlockParticleEmitter::new, WithBlockParticleEmitter::new);
            managedEffectFactories.add(WithBlockParticleEmitter.TYPE);
            event.register(PresetVarsParticleEmitter.TYPE, PresetVarsParticleEmitter::new, PresetVarsParticleEmitter::new);
            ParticleEvents.post(event);
        }
    }

    public <E extends ParticleEmitter> void register(ResourceLocation type, BiFunction<Level, CompoundTag, E> nbt, BiFunction<ParticleEmitter, ParticleEffect, E> effect) {
        if (fromNbt.put(type, nbt) != null) {
            throw new IllegalStateException("Duplicated emitter type: " + type);
        }
        fromEffect.put(type, effect);
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public static <E extends ParticleEmitter> void registerManagedEffectFactory(ResourceLocation type,
            BiFunction<ParticleEmitter, ParticleEffect, E> effect) {
        registerEffectFactory(type, effect);
        managedEffectFactories.add(type);
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public static boolean hasEffectFactory(ResourceLocation type) {
        return fromEffect.containsKey(type);
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public static boolean hasManagedEffectFactory(ResourceLocation type) {
        return managedEffectFactories.contains(type);
    }

    public static ParticleEmitter create(Level level, CompoundTag tag) {
        return fromNbt.getOrDefault(ResourceLocation.tryParse(tag.getString(ParticleEmitter.TYPE_KEY)), ParticleEmitter::new).apply(level, tag);
    }

    public static <E extends ParticleEmitter> void registerEffectFactory(ResourceLocation type, BiFunction<ParticleEmitter, ParticleEffect, E> effect) {
        if (fromEffect.putIfAbsent(type, effect) != null) {
            throw new IllegalStateException("Duplicated emitter type: " + type);
        }
    }

    public static ParticleEmitter create(ParticleEmitter parent, ParticleEffect effect) {
        return fromEffect.getOrDefault(parent.type, ParticleEmitter::new).apply(parent, effect);
    }
}
