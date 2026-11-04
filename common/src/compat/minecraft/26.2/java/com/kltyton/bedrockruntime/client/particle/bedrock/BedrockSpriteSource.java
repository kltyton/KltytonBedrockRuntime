package com.kltyton.bedrockruntime.client.particle.bedrock;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.resources.Identifier;
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
    public static final MapCodec<BedrockSpriteSource> CODEC = MapCodec.unit(new BedrockSpriteSource());
    private record MissingTexture(Identifier effect, Identifier texture) {}
    private record Bindings(Map<Identifier, Identifier> aliases, Map<MissingTexture, Identifier> missing) {}
    private static volatile Bindings pending = new Bindings(Map.of(), Map.of());
    private static volatile Bindings active = pending;

    @Override
    public void run(ResourceManager resources, Output output) {
        Map<Identifier, Identifier> aliases = new HashMap<>();
        Map<MissingTexture, Identifier> missing = new HashMap<>();
        Set<MissingTexture> ambiguous = new HashSet<>();
        collect(resources, output, "particle_definitions", aliases, missing, ambiguous);
        collect(resources, output, "particles", aliases, missing, ambiguous);
        collect(resources, output, BedrockPackResources.DEFINITIONS, aliases, missing, ambiguous);
        pending = new Bindings(Map.copyOf(aliases), Map.copyOf(missing));
    }

    private static void collect(ResourceManager resources, Output output, String directory,
                                Map<Identifier, Identifier> aliases,
                                Map<MissingTexture, Identifier> missing, Set<MissingTexture> ambiguous) {
        Map<Identifier, Resource> definitions = resources.listResources(directory, id -> id.getPath().endsWith(".json"));
        for (Map.Entry<Identifier, Resource> entry : definitions.entrySet()) {
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
                        for (Identifier alias : BedrockParticleResources.spriteAliases(texture)) {
                            aliases.put(alias, texture.sprite());
                        }
                        JsonObject description = effect.has("description") && effect.get("description").isJsonObject()
                                ? effect.getAsJsonObject("description") : null;
                        if (description != null && description.has("identifier")
                                && description.get("identifier").isJsonPrimitive()
                                && description.getAsJsonPrimitive("identifier").isString()) {
                            Identifier effectId = Identifier.tryParse(description.get("identifier").getAsString());
                            Identifier rawTexture = Identifier.tryParse(texture.sourcePath());
                            if (effectId != null && rawTexture != null) {
                                MissingTexture key = new MissingTexture(effectId, rawTexture);
                                if (!ambiguous.contains(key)) {
                                    Identifier previous = missing.putIfAbsent(key, texture.sprite());
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

    public static Identifier currentSprite(Identifier oldSprite, @Nullable String sourcePath) {
        return BedrockParticleResources.resolvesExtensions(sourcePath)
                ? active.aliases().getOrDefault(oldSprite, oldSprite) : oldSprite;
    }

    public static @Nullable Identifier missingSprite(Identifier effect, Identifier texture) {
        return active.missing().get(new MissingTexture(effect, texture));
    }

    @Override
    public MapCodec<? extends SpriteSource> codec() {
        return CODEC;
    }
}
