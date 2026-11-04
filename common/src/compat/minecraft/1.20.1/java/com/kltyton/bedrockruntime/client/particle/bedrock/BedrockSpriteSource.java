package com.kltyton.bedrockruntime.client.particle.bedrock;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.particle.BedrockParticles;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class BedrockSpriteSource implements SpriteSource {
    public static Codec<BedrockSpriteSource> codec() { return Codec.unit(BedrockSpriteSource::new); }
    public static volatile SpriteSourceType TYPE;
    public static void bindType(SpriteSourceType type) { TYPE = type; }

    private record MissingTexture(ResourceLocation effect, ResourceLocation texture) {}
    private record Bindings(Map<ResourceLocation, ResourceLocation> aliases,
                            Map<MissingTexture, ResourceLocation> missing) {}
    private static volatile Bindings pending = new Bindings(Map.of(), Map.of());
    private static volatile Bindings active = pending;

    @Override public void run(ResourceManager resources, Output output) {
        Map<ResourceLocation, ResourceLocation> aliases = new HashMap<>();
        Map<MissingTexture, ResourceLocation> missing = new HashMap<>();
        Set<MissingTexture> ambiguous = new HashSet<>();
        collect(resources, output, "particle_definitions", aliases, missing, ambiguous);
        collect(resources, output, "particles", aliases, missing, ambiguous);
        collect(resources, output, BedrockPackResources.DEFINITIONS, aliases, missing, ambiguous);
        pending = new Bindings(Map.copyOf(aliases), Map.copyOf(missing));
    }

    private static void collect(ResourceManager resources, Output output, String directory,
                                Map<ResourceLocation, ResourceLocation> aliases,
                                Map<MissingTexture, ResourceLocation> missing, Set<MissingTexture> ambiguous) {
        Map<ResourceLocation, Resource> definitions = resources.listResources(directory, id -> id.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, Resource> entry : definitions.entrySet()) {
            try {
                Resource source = directory.equals("particles")
                        ? BedrockParticleResources.findBedrockDefinition(resources, entry.getKey()).orElse(null)
                        : entry.getValue();
                if (source == null) continue;
                try (Reader reader = source.openAsReader()) {
                    JsonObject document;
                    try {
                        document = BedrockJson.parse(reader);
                    } catch (JsonParseException exception) {
                        if (!directory.equals("particles")) throw exception;
                        BedrockParticles.LOGGER.error("Cannot classify particle resource {} while loading sprites", entry.getKey(), exception);
                        continue;
                    }
                    if (!document.has("particle_effect") || !document.get("particle_effect").isJsonObject()) {
                        if (directory.equals("particles")) continue;
                        throw new JsonParseException("Missing particle_effect object");
                    }
                    JsonObject effect = document.getAsJsonObject("particle_effect");
                    BedrockParticleResources.Texture texture = BedrockParticleResources.resolveTexture(resources, entry.getKey(), effect);
                    if (texture != null) {
                        output.add(texture.sprite(), texture.resource());
                        for (ResourceLocation alias : BedrockParticleResources.spriteAliases(texture)) {
                            aliases.put(alias, texture.sprite());
                        }
                        JsonObject description = effect.has("description") && effect.get("description").isJsonObject()
                                ? effect.getAsJsonObject("description") : null;
                        if (description != null && description.has("identifier")
                                && description.get("identifier").isJsonPrimitive()
                                && description.getAsJsonPrimitive("identifier").isString()) {
                            ResourceLocation effectId = ResourceLocation.tryParse(description.get("identifier").getAsString());
                            ResourceLocation rawTexture = ResourceLocation.tryParse(texture.sourcePath());
                            if (effectId != null && rawTexture != null) {
                                MissingTexture key = new MissingTexture(effectId, rawTexture);
                                if (!ambiguous.contains(key)) {
                                    ResourceLocation previous = missing.putIfAbsent(key, texture.sprite());
                                    if (previous != null && !previous.equals(texture.sprite())) {
                                        missing.remove(key);
                                        ambiguous.add(key);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (IOException | JsonParseException | IllegalStateException exception) {
                throw new IllegalStateException("Cannot read particle definition " + entry.getKey() + " while loading sprites", exception);
            }
        }
    }

    public static void atlasStitched(TextureAtlas atlas) {
        if (atlas.location().equals(TextureAtlas.LOCATION_PARTICLES)) active = pending;
    }

    public static ResourceLocation currentSprite(ResourceLocation oldSprite, @Nullable String sourcePath) {
        return BedrockParticleResources.resolvesExtensions(sourcePath)
                ? active.aliases().getOrDefault(oldSprite, oldSprite) : oldSprite;
    }

    public static @Nullable ResourceLocation missingSprite(ResourceLocation effect, ResourceLocation texture) {
        return active.missing().get(new MissingTexture(effect, texture));
    }

    @Override public SpriteSourceType type() {
        SpriteSourceType type = TYPE;
        if (type == null) throw new IllegalStateException("Bedrock sprite source type has not been registered");
        return type;
    }
}
