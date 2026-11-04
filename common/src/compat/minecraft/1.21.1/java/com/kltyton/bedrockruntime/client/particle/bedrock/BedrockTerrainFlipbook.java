package com.kltyton.bedrockruntime.client.particle.bedrock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.client.resources.metadata.animation.AnimationFrame;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.particle.BedrockParticles;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Animation data for terrain atlas entries supplied by Bedrock resource packs. */
record BedrockTerrainFlipbook(String texture, int ticks, boolean blend, int replicate,
                              @Nullable List<Integer> frames) {
    record Key(String tile, int atlasIndex) {}
    record Rule(@Nullable Integer variation, BedrockTerrainFlipbook flipbook) {}

    static Map<Key, List<Rule>> load(ResourceManager resources) {
        Map<Key, List<Rule>> result = new HashMap<>();
        for (Resource resource : resources.getResourceStack(BedrockPackResources.TERRAIN_FLIPBOOKS)) {
            try (Reader reader = resource.openAsReader()) {
                for (JsonElement element : BedrockJson.parseArray(reader)) {
                    if (!element.isJsonObject()) throw new JsonParseException("Terrain flipbook entry must be an object");
                    JsonObject object = element.getAsJsonObject();
                    String tile = requiredString(object, "atlas_tile");
                    String texture = requiredString(object, "flipbook_texture");
                    int atlasIndex = integer(object.get("atlas_index"), 0, "atlas_index");
                    JsonElement variationValue = object.get("atlas_tile_variant");
                    Integer variation = variationValue == null || variationValue.isJsonNull()
                            ? null : integer(variationValue, 0, "atlas_tile_variant");
                    int ticks = integer(object.get("ticks_per_frame"), 1, "ticks_per_frame");
                    int replicate = integer(object.get("replicate"), 1, "replicate");
                    if (atlasIndex < 0) {
                        BedrockParticles.LOGGER.warn("Ignoring terrain flipbook {} with negative atlas_index", tile);
                        continue;
                    }
                    if (ticks <= 0 || replicate <= 0) {
                        BedrockParticles.LOGGER.warn("Cannot animate terrain flipbook {} with invalid timing or replicate", tile);
                    }
                    JsonElement blendValue = object.get("blend_frames");
                    if (blendValue != null && !blendValue.isJsonNull()
                            && (!blendValue.isJsonPrimitive() || !blendValue.getAsJsonPrimitive().isBoolean()))
                        throw new JsonParseException("blend_frames must be boolean for " + tile);
                    boolean blend = blendValue == null || blendValue.isJsonNull() || blendValue.getAsBoolean();
                    JsonElement frameValue = object.get("frames");
                    List<Integer> frames = null;
                    if (frameValue != null && frameValue.isJsonArray()) {
                        frames = new ArrayList<>();
                        for (JsonElement frame : frameValue.getAsJsonArray()) {
                            int index = integer(frame, 0, "frames");
                            if (index < 0) throw new JsonParseException("Invalid terrain flipbook frame for " + tile);
                            frames.add(index);
                        }
                        frames = frames.isEmpty() ? null : List.copyOf(frames);
                    }
                    result.computeIfAbsent(new Key(tile, atlasIndex), ignored -> new ArrayList<>())
                            .add(new Rule(variation, new BedrockTerrainFlipbook(texture, ticks, blend, replicate, frames)));
                }
            } catch (IOException | JsonParseException exception) {
                throw new IllegalStateException("Cannot read Bedrock terrain flipbooks from " + resource.sourcePackId(), exception);
            }
        }
        result.replaceAll((key, value) -> List.copyOf(value));
        return Map.copyOf(result);
    }

    static @Nullable BedrockTerrainFlipbook select(Map<Key, List<Rule>> entries, String tile,
                                                   int atlasIndex, int variation, int variationCount) {
        List<Rule> rules = entries.get(new Key(tile, atlasIndex));
        if (rules == null) return null;
        for (int i = rules.size() - 1; i >= 0; i--) {
            Rule rule = rules.get(i);
            Integer requested = rule.variation();
            if (requested == null || (requested < 0 || requested >= variationCount ? 0 : requested) == variation)
                return rule.flipbook();
        }
        return null;
    }

    List<AnimationFrame> animationFrames(int frameTotal) {
        List<AnimationFrame> values = null;
        if (frames != null) {
            values = new ArrayList<>(frames.size());
            for (int frame : frames) {
                values.add(new AnimationFrame(Math.min(frame, frameTotal - 1)));
            }
        }
        if (values != null && values.size() == 1) values.add(values.get(0));
        return values == null ? List.of() : values;
    }

    AnimationMetadataSection animation(int frameTotal) {
        return new AnimationMetadataSection(animationFrames(frameTotal), -1, -1, ticks, blend);
    }

    private static String requiredString(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
            throw new JsonParseException("Missing terrain flipbook " + name);
        return value.getAsString();
    }

    private static int integer(@Nullable JsonElement value, int fallback, String name) {
        if (value == null || value.isJsonNull()) return fallback;
        if (!value.isJsonPrimitive()) return fallback;
        if (value.getAsJsonPrimitive().isBoolean()) return value.getAsBoolean() ? 1 : 0;
        if (!value.getAsJsonPrimitive().isNumber()) return fallback;
        double number = value.getAsDouble();
        if (!Double.isFinite(number) || number >= 0x1.0p31 || number < -0x1.0p31) return Integer.MIN_VALUE;
        return (int) number;
    }
}

