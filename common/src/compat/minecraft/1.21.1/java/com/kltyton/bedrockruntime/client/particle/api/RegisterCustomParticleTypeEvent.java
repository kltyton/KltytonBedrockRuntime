package com.kltyton.bedrockruntime.client.particle.api;

import com.kltyton.bedrockruntime.client.particle.api.event.ParticleEvents;

import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleEngine;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleDescription;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockJson;
import com.kltyton.bedrockruntime.client.particle.bedrock.JavaParticleResourceManager;
import com.kltyton.bedrockruntime.client.particle.definition.DefinedParticleEffect;
import com.kltyton.bedrockruntime.client.particle.runtime.ExtendMutableSpriteSet;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticlePreset;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.IOException;
import java.io.Reader;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public class RegisterCustomParticleTypeEvent {
    private static final Map<ParticleType<?>, Provider<?>> PROVIDERS = new HashMap<>();
    private static final ExtendMutableSpriteSet SPRITES = new ExtendMutableSpriteSet();
    private static final Set<ParticleType<?>> SPRITE_TYPES = new LinkedHashSet<>();
    private static @Nullable List<ResourceLocation> boundSpriteIds;

    public void register(ParticleType<?> type, Provider<?> provider) {
        PROVIDERS.put(type, provider);
    }

    public void registerWithSprites(ParticleType<?> type, ProviderWithSprites<?> provider) {
        SPRITE_TYPES.add(type);
        register(type, provider);
    }

    public static Map<ParticleType<?>, Provider<?>> map() {
        return PROVIDERS;
    }

    public static ExtendMutableSpriteSet sprites() {
        return SPRITES;
    }

    public static void registerDefaults() {
        PROVIDERS.clear();
        SPRITES.clear();
        SPRITE_TYPES.clear();
        boundSpriteIds = null;
        Provider<com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleInstance> molang =
                (emitter, particlePreset, level, x, y, z) ->
                        new com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleInstance(
                                particlePreset, level, x, y, z, level.getRandom());
        PROVIDERS.put(BedrockParticles.MOLANG, molang);
        ParticleEvents.post(new RegisterCustomParticleTypeEvent());
    }

    public static List<ResourceLocation> loadSpriteIdentifiers(ResourceManager resources) {
        List<ResourceLocation> sprites = new ArrayList<>();
        for (ParticleType<?> type : SPRITE_TYPES) {
            ResourceLocation id = java.util.Objects.requireNonNull(BuiltInRegistries.PARTICLE_TYPE.getKey(type),
                    "Unregistered particle type in sprite provider");
            ResourceLocation file = id.withPath("particles/" + id.getPath() + ".json");
            List<Resource> stack = resources.getResourceStack(file);
            for (int i = stack.size() - 1; i >= 0; i--) {
                Resource candidate = stack.get(i);
                try {
                    if (JavaParticleResourceManager.isBedrockDefinition(candidate)) continue;
                    try (Reader reader = candidate.openAsReader()) {
                        sprites.addAll(ParticleDescription.fromJson(BedrockJson.parse(reader)).getTextures());
                    }
                    break;
                } catch (IOException | RuntimeException exception) {
                    throw new IllegalStateException("Cannot load particle sprite description " + file, exception);
                }
            }
        }
        return sprites;
    }

    public static void bindSprites(Map<ResourceLocation, DefinedParticleEffect> effects, List<ResourceLocation> spriteIds) {
        SPRITES.clear();
        TextureAtlas atlas = (TextureAtlas) Minecraft.getInstance().getTextureManager()
                .getTexture(TextureAtlas.LOCATION_PARTICLES);
        TextureAtlasSprite missing = atlas.getSprite(MissingTextureAtlasSprite.getLocation());
        SPRITES.bindMissing(missing);
        for (ResourceLocation id : spriteIds) {
            TextureAtlasSprite sprite = atlas.getSprite(id);
            if (sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
                BedrockParticles.LOGGER.warn("Missing particle sprite {}", id);
            }
            SPRITES.addSprite(sprite);
        }
        int index = spriteIds.size();
        for (DefinedParticleEffect effect : effects.values()) {
            effect.description.parameters().bindTexture(index++);
            SPRITES.addSprite(effect.description.parameters().getTexture(effect.usesTerrainAtlas()));
        }
        boundSpriteIds = List.copyOf(spriteIds);
    }

    public static void rebindSprites() {
        if (boundSpriteIds != null) bindSprites(MolangParticleEngine.INSTANCE.id2Effect(), boundSpriteIds);
    }

    @SuppressWarnings("unchecked")
    public static <V extends Particle & IMolangParticleInstance> V createParticle(ParticleEmitter emitter) {
        Provider<?> provider = PROVIDERS.get(emitter.getPreset().type);
        if (provider == null) {
            throw new NullPointerException("Provider from '" + BuiltInRegistries.PARTICLE_TYPE.getKey(emitter.getPreset().type) + "' is not registered");
        }

        return (V) provider.create(emitter, emitter.getParticlePreset(), (ClientLevel) emitter.level, emitter.getX(), emitter.getY(), emitter.getZ(), SPRITES);
    }

    @FunctionalInterface
    public interface Provider<V extends Particle & IMolangParticleInstance> {
        V create(ParticleEmitter emitter, ParticlePreset particlePreset, ClientLevel level, double x, double y, double z);

        default V create(ParticleEmitter emitter, ParticlePreset particlePreset, ClientLevel level, double x, double y, double z, ExtendMutableSpriteSet sprites) {
            return create(emitter, particlePreset, level, x, y, z);
        }
    }

    @FunctionalInterface
    public interface ProviderWithSprites<V extends Particle & IMolangParticleInstance> extends Provider<V> {
        @Override
        default V create(ParticleEmitter emitter, ParticlePreset particlePreset, ClientLevel level, double x, double y, double z) {
            return create(emitter, particlePreset, level, x, y, z, RegisterCustomParticleTypeEvent.sprites());
        }

        @Override
        V create(ParticleEmitter emitter, ParticlePreset particlePreset, ClientLevel level, double x, double y, double z, ExtendMutableSpriteSet sprites);
    }
}
