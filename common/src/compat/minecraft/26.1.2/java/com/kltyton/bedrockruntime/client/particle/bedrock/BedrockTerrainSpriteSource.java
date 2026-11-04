package com.kltyton.bedrockruntime.client.particle.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CreakingHeartBlock;
import net.minecraft.world.level.block.FrostedIceBlock;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SnifferEggBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.WallSide;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.definition.description.DescriptionMaterial;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** Adds explicitly defined Bedrock surface textures to the block atlas. */
public final class BedrockTerrainSpriteSource implements SpriteSource {
    public static final MapCodec<BedrockTerrainSpriteSource> CODEC = MapCodec.unit(new BedrockTerrainSpriteSource());
    private static final Map<String, String> STAIR_TILES = Map.of(
            "minecraft:normal_stone_stairs", "stone",
            "minecraft:stone_brick_stairs", "stonebrick",
            "minecraft:sandstone_stairs", "sandstone_bottom",
            "minecraft:red_sandstone_stairs", "redsandstone_bottom",
            "minecraft:prismarine_stairs", "prismarine",
            "minecraft:smooth_quartz_stairs", "quartz_block_top"
    );
    private static final Map<String, String> WALL_TILES = Map.ofEntries(
            Map.entry("minecraft:cobblestone_wall", "flattened_cobblestone_wall"),
            Map.entry("minecraft:mossy_cobblestone_wall", "mossy_cobblestone_wall"),
            Map.entry("minecraft:granite_wall", "granite_wall"),
            Map.entry("minecraft:diorite_wall", "diorite_wall"),
            Map.entry("minecraft:andesite_wall", "andesite_wall"),
            Map.entry("minecraft:sandstone_wall", "sandstone_wall"),
            Map.entry("minecraft:brick_wall", "brick_wall"),
            Map.entry("minecraft:stone_brick_wall", "stone_brick_wall"),
            Map.entry("minecraft:mossy_stone_brick_wall", "mossy_stone_brick_wall"),
            Map.entry("minecraft:nether_brick_wall", "nether_brick_wall"),
            Map.entry("minecraft:end_stone_brick_wall", "end_stone_brick_wall"),
            Map.entry("minecraft:prismarine_wall", "prismarine_wall"),
            Map.entry("minecraft:red_sandstone_wall", "red_sandstone_wall"),
            Map.entry("minecraft:red_nether_brick_wall", "red_nether_brick_wall"),
            Map.entry("minecraft:blackstone_wall", "blackstone"),
            Map.entry("minecraft:polished_blackstone_wall", "polished_blackstone"),
            Map.entry("minecraft:polished_blackstone_brick_wall", "polished_blackstone_bricks"),
            Map.entry("minecraft:cobbled_deepslate_wall", "cobbled_deepslate"),
            Map.entry("minecraft:polished_deepslate_wall", "polished_deepslate"),
            Map.entry("minecraft:deepslate_brick_wall", "deepslate_bricks"),
            Map.entry("minecraft:deepslate_tile_wall", "deepslate_tiles"),
            Map.entry("minecraft:mud_brick_wall", "mud_bricks"),
            Map.entry("minecraft:tuff_wall", "tuff"),
            Map.entry("minecraft:polished_tuff_wall", "polished_tuff"),
            Map.entry("minecraft:tuff_brick_wall", "tuff_bricks"),
            Map.entry("minecraft:resin_brick_wall", "resin_bricks"),
            Map.entry("minecraft:sulfur_wall", "sulfur"),
            Map.entry("minecraft:polished_sulfur_wall", "polished_sulfur"),
            Map.entry("minecraft:sulfur_brick_wall", "sulfur_bricks"),
            Map.entry("minecraft:cinnabar_wall", "cinnabar"),
            Map.entry("minecraft:polished_cinnabar_wall", "polished_cinnabar"),
            Map.entry("minecraft:cinnabar_brick_wall", "cinnabar_bricks")
    );
    private static final Map<String, String> MUSHROOM_TILES = Map.of(
            "minecraft:brown_mushroom_block", "mushroom_brown_bottom",
            "minecraft:red_mushroom_block", "mushroom_red_bottom",
            "minecraft:mushroom_stem", "mushroom_stem_bottom"
    );
    private static final String MUSHROOM_INSIDE = "textures/blocks/mushroom_block_inside";
    private static final String MUSHROOM_STEM = "textures/blocks/mushroom_block_skin_stem";
    private static final Map<String, List<String>> STATEFUL_IMAGES = Map.of(
            "structure_block", List.of("textures/blocks/structure_block", "textures/blocks/structure_block_data",
                    "textures/blocks/structure_block_save", "textures/blocks/structure_block_load",
                    "textures/blocks/structure_block_corner", "textures/blocks/structure_block_export"),
            "creaking_heart_top", List.of("textures/blocks/creaking_heart_top_inactive",
                    "textures/blocks/creaking_heart_top_dormant", "textures/blocks/creaking_heart_top_active"),
            "vault_bottom", List.of("textures/blocks/vault_bottom", "textures/blocks/vault_bottom",
                    "textures/blocks/vault_bottom", "textures/blocks/vault_bottom",
                    "textures/blocks/vault_bottom_ominous", "textures/blocks/vault_bottom_ominous",
                    "textures/blocks/vault_bottom_ominous", "textures/blocks/vault_bottom_ominous")
    );
    private static final Map<String, String> LEAF_TILES = Map.ofEntries(
            Map.entry("minecraft:oak_leaves", "oak_leaves"),
            Map.entry("minecraft:spruce_leaves", "spruce_leaves"),
            Map.entry("minecraft:birch_leaves", "birch_leaves"),
            Map.entry("minecraft:jungle_leaves", "jungle_leaves"),
            Map.entry("minecraft:acacia_leaves", "acacia_leaves"),
            Map.entry("minecraft:dark_oak_leaves", "big_oak_leaves"),
            Map.entry("minecraft:azalea_leaves", "azalea_leaves"),
            Map.entry("minecraft:azalea_leaves_flowered", "azalea_leaves_flowered"),
            Map.entry("minecraft:pale_oak_leaves", "pale_oak_leaves"),
            Map.entry("minecraft:mangrove_leaves", "mangrove_leaves"),
            Map.entry("minecraft:cherry_leaves", "cherry_leaves")
    );
    private static final Map<String, String> LEAF_IMAGES = Map.ofEntries(
            Map.entry("oak_leaves", "textures/blocks/leaves_oak"),
            Map.entry("spruce_leaves", "textures/blocks/leaves_spruce"),
            Map.entry("birch_leaves", "textures/blocks/leaves_birch"),
            Map.entry("jungle_leaves", "textures/blocks/leaves_jungle"),
            Map.entry("acacia_leaves", "textures/blocks/leaves_acacia"),
            Map.entry("big_oak_leaves", "textures/blocks/leaves_big_oak"),
            Map.entry("azalea_leaves", "textures/blocks/azalea_leaves"),
            Map.entry("azalea_leaves_flowered", "textures/blocks/azalea_leaves_flowers"),
            Map.entry("pale_oak_leaves", "textures/blocks/pale_oak_leaves"),
            Map.entry("mangrove_leaves", "textures/blocks/mangrove_leaves"),
            Map.entry("cherry_leaves", "textures/blocks/cherry_leaves")
    );
    // Java-to-Bedrock state mappings from GeyserMC's mappings (MIT).
    private static final String MUSHROOM_CAP_VARIANTS = "e313131379794646121212127878454ee3131313797946461212121278784540";
    private static final String MUSHROOM_STEM_VARIANTS = "ffffffffffffffffffffffffffffffffffaaffaaffaaffaaffaaffaaffaaffa0";
    private static volatile Bindings pending = new Bindings(Map.of(), Map.of(), Map.of(), Set.of());
    private static volatile Bindings active = pending;

    private record Face(String block, Direction direction) {}
    private record MultiTint(@Nullable String mapPath, BedrockTerrainTint.Channels base,
                             BedrockTerrainTint.Channels target) {}
    private record TexturePath(String path, float weight, @Nullable Integer tint, @Nullable Integer overlay,
                               @Nullable MultiTint multiTint) {}
    private record WeightedSprite(Identifier sprite, float weight, int priority, int variationPriority,
                                  @Nullable Identifier reloadKey) {}
    public record SurfaceBinding(TextureAtlasSprite sprite, @Nullable Identifier reloadKey) {}
    private record ImageResource(String path, Resource resource) {}
    private record AnimationKey(String tile, int atlasIndex, int variation) {}
    private record Bindings(Map<Face, List<List<WeightedSprite>>> configured,
                            Map<Identifier, Identifier> direct,
                            Map<Identifier, Integer> javaPriorities,
                            Set<Face> implicitDefaults) {}

    @Override
    public void run(ResourceManager resources, Output output) {
        Map<BedrockTerrainFlipbook.Key, List<BedrockTerrainFlipbook.Rule>> flipbooks = BedrockTerrainFlipbook.load(resources);
        Map<String, Integer> priorities = new HashMap<>();
        int packPriority = 0;
        for (var pack : resources.listPacks().toList()) priorities.put(pack.packId(), packPriority++);
        Map<String, JsonElement> textures = new HashMap<>();
        for (Resource resource : resources.getResourceStack(BedrockPackResources.TERRAIN_TEXTURES)) {
            JsonObject document = read(resource);
            JsonElement data = document.get("texture_data");
            if (data == null) continue;
            if (!data.isJsonObject()) throw new JsonParseException("terrain texture_data must be an object");
            data.getAsJsonObject().entrySet().forEach(entry -> textures.put(entry.getKey(), entry.getValue()));
        }
        Set<String> implicitDefaultTiles = new HashSet<>();
        for (var entry : LEAF_IMAGES.entrySet()) {
            if (textures.containsKey(entry.getKey())) continue;
            String path = entry.getValue();
            if (findImage(resources, path, priorities) == null
                    && findImage(resources, path + "_opaque", priorities) == null) continue;
            JsonArray variants = new JsonArray();
            variants.add(path);
            variants.add(path + "_opaque");
            JsonObject definition = new JsonObject();
            definition.add("textures", variants);
            textures.put(entry.getKey(), definition);
            implicitDefaultTiles.add(entry.getKey());
        }
        for (String tile : MUSHROOM_TILES.values()) {
            if (textures.containsKey(tile)) continue;
            boolean stem = tile.equals("mushroom_stem_bottom");
            String skin = stem ? MUSHROOM_STEM : "textures/blocks/mushroom_block_skin_"
                    + (tile.equals("mushroom_red_bottom") ? "red" : "brown");
            if (findImage(resources, MUSHROOM_INSIDE, priorities) == null
                    && findImage(resources, MUSHROOM_STEM, priorities) == null
                    && (stem || findImage(resources, skin, priorities) == null)) continue;
            JsonArray variants = new JsonArray();
            for (int variant = 0; variant < 16; variant++) {
                variants.add(stem ? variant == 10 ? MUSHROOM_INSIDE : MUSHROOM_STEM
                        : variant < 14 ? MUSHROOM_INSIDE : variant == 14 ? skin : MUSHROOM_STEM);
            }
            JsonObject definition = new JsonObject();
            definition.add("textures", variants);
            textures.put(tile, definition);
            implicitDefaultTiles.add(tile);
        }
        for (var entry : STATEFUL_IMAGES.entrySet()) {
            if (textures.containsKey(entry.getKey()) || entry.getValue().stream().distinct()
                    .noneMatch(path -> findImage(resources, path, priorities) != null)) continue;
            JsonArray variants = new JsonArray();
            entry.getValue().forEach(variants::add);
            JsonObject definition = new JsonObject();
            definition.add("textures", variants);
            textures.put(entry.getKey(), definition);
            implicitDefaultTiles.add(entry.getKey());
        }

        Map<Face, String> faces = new HashMap<>();
        for (Resource resource : resources.getResourceStack(BedrockPackResources.BLOCK_TEXTURES)) {
            JsonObject document = read(resource);
            for (var entry : document.entrySet()) {
                if (!entry.getValue().isJsonObject()) continue;
                String block = entry.getKey().contains(":") ? entry.getKey() : "minecraft:" + entry.getKey();
                JsonElement texture = entry.getValue().getAsJsonObject().get("textures");
                if (texture == null) continue;
                Direction face = block.equals("minecraft:sniffer_egg") ? Direction.NORTH : Direction.DOWN;
                String name = faceTexture(texture, face);
                if (name != null) faces.put(new Face(block, face), name);
            }
        }
        if (textures.containsKey("grass_bottom")) {
            faces.putIfAbsent(new Face("minecraft:grass_block", Direction.DOWN), "grass_bottom");
        }
        if (textures.containsKey("frosted_ice")) {
            faces.putIfAbsent(new Face("minecraft:frosted_ice", Direction.DOWN), "frosted_ice");
        }
        if (textures.containsKey("sniffer_egg_north")) {
            faces.putIfAbsent(new Face("minecraft:sniffer_egg", Direction.NORTH), "sniffer_egg_north");
        }
        for (String block : List.of("suspicious_sand", "suspicious_gravel")) {
            if (textures.containsKey(block)) faces.putIfAbsent(new Face("minecraft:" + block, Direction.DOWN), block);
        }
        for (var wall : WALL_TILES.entrySet()) {
            if (textures.containsKey(wall.getValue())) {
                faces.putIfAbsent(new Face(wall.getKey(), Direction.DOWN), wall.getValue());
            }
        }
        for (var mushroom : MUSHROOM_TILES.entrySet()) {
            if (textures.containsKey(mushroom.getValue())) {
                faces.putIfAbsent(new Face(mushroom.getKey(), Direction.DOWN), mushroom.getValue());
            }
        }
        for (String block : List.of("structure_block", "creaking_heart", "vault")) {
            String tile = switch (block) {
                case "creaking_heart" -> "creaking_heart_top";
                case "vault" -> "vault_bottom";
                default -> block;
            };
            if (textures.containsKey(tile)) faces.putIfAbsent(new Face("minecraft:" + block, Direction.DOWN), tile);
        }
        for (var leaves : LEAF_TILES.entrySet()) {
            if (textures.containsKey(leaves.getValue())) {
                faces.putIfAbsent(new Face(leaves.getKey(), Direction.DOWN), leaves.getValue());
            }
        }
        for (var entry : STAIR_TILES.entrySet()) {
            if (textures.containsKey(entry.getValue())) {
                faces.putIfAbsent(new Face(entry.getKey(), Direction.DOWN), entry.getValue());
            }
        }
        if (textures.containsKey("dirt")) {
            faces.putIfAbsent(new Face("minecraft:grass_path", Direction.DOWN), "dirt");
        }

        Map<Face, List<List<WeightedSprite>>> bindings = new HashMap<>();
        Set<Face> implicitDefaults = new HashSet<>();
        Set<Identifier> added = new HashSet<>();
        Set<String> unresolved = new HashSet<>();
        Map<Identifier, Identifier> direct = new HashMap<>();
        Map<Identifier, Integer> javaPriorities = new HashMap<>();
        for (var javaEntry : resources.listResources("textures",
                id -> id.getPath().startsWith("textures/")
                        && id.getPath().endsWith(".png")).entrySet()) {
            Identifier javaTexture = javaEntry.getKey();
            Identifier javaSprite = Identifier.fromNamespaceAndPath(javaTexture.getNamespace(),
                    javaTexture.getPath().substring("textures/".length(), javaTexture.getPath().length() - ".png".length()));
            int javaPriority = priorities.getOrDefault(javaEntry.getValue().sourcePackId(), -1);
            javaPriorities.put(javaSprite, javaPriority);
            if (!javaTexture.getNamespace().equals("minecraft")
                    || !javaTexture.getPath().startsWith("textures/block/")) continue;
            String base = javaTexture.getPath().substring("textures/block/".length(),
                    javaTexture.getPath().length() - ".png".length());
            String defaultTile = BedrockTerrainNames.defaultTile(base);
            if (defaultTile != null && !textures.containsKey(defaultTile)) {
                defaultTile = BedrockTerrainNames.legacyTile(base);
            }
            if (defaultTile != null && textures.containsKey(defaultTile)) {
                faces.putIfAbsent(new Face("minecraft:" + base, Direction.DOWN), defaultTile);
                for (String relatedBlock : BedrockTerrainNames.relatedBlocks(base)) {
                    faces.putIfAbsent(new Face("minecraft:" + relatedBlock, Direction.DOWN), defaultTile);
                }
            }
            String bedrockName = BedrockTerrainNames.texture(base);
            ImageResource image = bedrockName == null ? null : findImage(resources, "textures/blocks/" + bedrockName, priorities);
            if (image == null) image = findImage(resources, "textures/blocks/" + base, priorities);
            if (image == null) continue;
            if (javaPriority > priorities.getOrDefault(image.resource().sourcePackId(), -1)) continue;
            Identifier sprite = BedrockPackResources.textureSprite(image.path());
            if (added.add(sprite)) output.add(sprite, image.resource());
            direct.put(javaSprite, sprite);
        }
        for (var entry : faces.entrySet()) {
            boolean implicitDefault = implicitDefaultTiles.contains(entry.getValue());
            JsonElement value = textures.get(entry.getValue());
            if (value == null || !value.isJsonObject()) {
                unresolved.add(entry.getValue());
                continue;
            }
            JsonObject definition = value.getAsJsonObject();
            JsonElement variants = definition.get("textures");
            if (variants == null) {
                unresolved.add(entry.getValue());
                continue;
            }
            int atlasEntries = variants.isJsonArray() ? variants.getAsJsonArray().size() : 1;
            for (BedrockTerrainFlipbook.Key key : flipbooks.keySet()) {
                if (key.tile().equals(entry.getValue()) && key.atlasIndex() >= atlasEntries)
                    unresolved.add(entry.getValue());
            }
            if (variants.isJsonArray() && variants.getAsJsonArray().size() > 1
                    && !entry.getKey().block().equals("minecraft:stone")
                    && !STAIR_TILES.containsKey(entry.getKey().block())
                    && !entry.getKey().block().equals("minecraft:sand")
                    && !entry.getKey().block().equals("minecraft:dirt")
                    && !entry.getKey().block().equals("minecraft:frosted_ice")
                    && !entry.getKey().block().equals("minecraft:sniffer_egg")
                    && !entry.getKey().block().equals("minecraft:suspicious_sand")
                    && !entry.getKey().block().equals("minecraft:suspicious_gravel")
                    && !WALL_TILES.containsKey(entry.getKey().block())
                    && !MUSHROOM_TILES.containsKey(entry.getKey().block())
                    && !LEAF_TILES.containsKey(entry.getKey().block())
                    && !entry.getKey().block().equals("minecraft:structure_block")
                    && !entry.getKey().block().equals("minecraft:creaking_heart")
                    && !entry.getKey().block().equals("minecraft:vault")
                    && !BedrockBlockNames.isSplitSlabTextureKey(entry.getKey().block())
                    && texturePaths(variants) == null) {
                unresolved.add(entry.getValue());
                continue;
            }
            Iterable<JsonElement> elements = variants.isJsonArray()
                    ? variants.getAsJsonArray() : List.of(variants);
            List<List<WeightedSprite>> resolvedVariants = new ArrayList<>();
            for (JsonElement element : elements) {
                int variantIndex = resolvedVariants.size();
                List<TexturePath> paths = texturePaths(element);
                if (paths == null) {
                    unresolved.add(entry.getValue());
                    resolvedVariants.add(List.of());
                    continue;
                }
                List<WeightedSprite> sprites = new ArrayList<>(paths.size());
                List<Resource> sources = new ArrayList<>(paths.size());
                List<@Nullable Resource> baseSources = new ArrayList<>(paths.size());
                List<Boolean> sameSources = new ArrayList<>(paths.size());
                List<@Nullable Resource> tintSources = new ArrayList<>(paths.size());
                List<TexturePath> acceptedPaths = new ArrayList<>(paths.size());
                List<BedrockTerrainFlipbook> animations = new ArrayList<>(paths.size());
                for (int optionIndex = 0; optionIndex < paths.size(); optionIndex++) {
                    TexturePath path = paths.get(optionIndex);
                    AnimationKey key = new AnimationKey(entry.getValue(), variantIndex, optionIndex);
                    BedrockTerrainFlipbook animation = BedrockTerrainFlipbook.select(
                            flipbooks, entry.getValue(), variantIndex, optionIndex, paths.size());
                    if (animation != null && (animation.replicate() <= 0 || animation.ticks() <= 0)) {
                        unresolved.add(entry.getValue());
                        continue;
                    }
                    ImageResource baseImage = findImage(resources, path.path(), priorities);
                    ImageResource image = animation == null ? baseImage : findImage(resources, animation.texture(), priorities);
                    if (animation != null && image == null) {
                        unresolved.add(entry.getValue());
                        continue;
                    }
                    if (image == null) {
                        if (!implicitDefault) unresolved.add(entry.getValue());
                        continue;
                    }
                    Identifier sprite = spriteName(image.path(), path.tint(), path.overlay(),
                            animation == null && path.multiTint() == null ? null : key);
                    sources.add(image.resource());
                    baseSources.add(baseImage == null ? null : baseImage.resource());
                    sameSources.add(baseImage != null && baseImage.path().equals(image.path()));
                    ImageResource tintMap = path.multiTint() == null || path.multiTint().mapPath() == null
                            ? null : findImage(resources, path.multiTint().mapPath(), priorities);
                    if (path.multiTint() != null && path.multiTint().mapPath() != null && tintMap == null) {
                        unresolved.add(entry.getValue());
                    }
                    tintSources.add(tintMap == null ? null : tintMap.resource());
                    acceptedPaths.add(path);
                    animations.add(animation);
                    Identifier reloadKey = spriteName(path.path(), null, null, key);
                    sprites.add(new WeightedSprite(sprite, path.weight(),
                            priorities.getOrDefault(image.resource().sourcePackId(), -1),
                            baseImage == null ? -1 : priorities.getOrDefault(baseImage.resource().sourcePackId(), -1),
                            reloadKey));
                }
                if (sprites.isEmpty()) {
                    if (!implicitDefault) unresolved.add(entry.getValue());
                    resolvedVariants.add(List.of());
                    continue;
                }
                int highestPriority = sprites.stream().mapToInt(WeightedSprite::variationPriority).max().orElseThrow();
                List<WeightedSprite> selected = new ArrayList<>(sprites.size());
                for (int i = 0; i < sprites.size(); i++) {
                    WeightedSprite candidate = sprites.get(i);
                    if (candidate.variationPriority() != highestPriority) continue;
                    Identifier sprite = candidate.sprite();
                    if (added.add(sprite)) addSprite(output, sprite, sources.get(i), baseSources.get(i),
                            sameSources.get(i), tintSources.get(i), acceptedPaths.get(i), animations.get(i));
                    selected.add(candidate);
                }
                resolvedVariants.add(List.copyOf(selected));
            }
            if (!resolvedVariants.isEmpty()) {
                bindings.put(entry.getKey(), List.copyOf(resolvedVariants));
                if (implicitDefault) implicitDefaults.add(entry.getKey());
            }
        }
        if (!unresolved.isEmpty()) {
            BedrockParticles.LOGGER.warn("Cannot map {} Bedrock terrain texture entries; examples: {}",
                    unresolved.size(), unresolved.stream().sorted().limit(8).toList());
        }
        pending = new Bindings(Map.copyOf(bindings), Map.copyOf(direct), Map.copyOf(javaPriorities), Set.copyOf(implicitDefaults));
    }

    private static JsonObject read(Resource resource) {
        try (Reader reader = resource.openAsReader()) {
            return BedrockJson.parse(reader);
        } catch (IOException | JsonParseException exception) {
            throw new IllegalStateException("Cannot read Bedrock terrain configuration from " + resource.sourcePackId(), exception);
        }
    }

    private static @Nullable String faceTexture(JsonElement texture, Direction face) {
        if (texture.isJsonPrimitive() && texture.getAsJsonPrimitive().isString()) return texture.getAsString();
        if (!texture.isJsonObject()) return null;
        JsonObject entries = texture.getAsJsonObject();
        JsonElement selected = entries.get(face == Direction.DOWN ? "down" : "north");
        if (selected == null && face == Direction.NORTH) selected = entries.get("side");
        if (selected == null) selected = entries.get("*");
        return selected != null && selected.isJsonPrimitive() && selected.getAsJsonPrimitive().isString()
                ? selected.getAsString() : null;
    }

    private static @Nullable List<TexturePath> texturePaths(@Nullable JsonElement value) {
        if (value == null) return null;
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString())
            return List.of(new TexturePath(value.getAsString(), 1.0F, null, null, null));
        if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            if (object.has("variations")) {
                JsonElement variations = object.get("variations");
                if (!variations.isJsonArray() || variations.getAsJsonArray().size() == 0) return null;
                if (variations.getAsJsonArray().size() > 256)
                    throw new JsonParseException("Terrain texture variations exceed 256 entries");
                List<TexturePath> result = new ArrayList<>();
                for (JsonElement variant : variations.getAsJsonArray()) {
                    if (variant.isJsonPrimitive() && variant.getAsJsonPrimitive().isString()) {
                        result.add(new TexturePath(variant.getAsString(), 1.0F, null, null, null));
                        continue;
                    }
                    if (!variant.isJsonObject()) return null;
                    JsonObject option = variant.getAsJsonObject();
                    JsonElement path = option.get("path");
                    if (path == null || !path.isJsonPrimitive() || !path.getAsJsonPrimitive().isString()) return null;
                    JsonElement tintValue = option.get("tint_color");
                    Integer optionTint = tintValue != null && tintValue.isJsonObject() ? null : color(tintValue);
                    Integer optionOverlay = color(option.get("overlay_color"));
                    Integer resolvedTint = optionTint;
                    Integer resolvedOverlay = optionOverlay;
                    JsonElement weight = option.get("weight");
                    float amount = 1.0F;
                    if (weight != null && weight.isJsonPrimitive()) {
                        if (weight.getAsJsonPrimitive().isNumber()) amount = weight.getAsFloat();
                        else if (weight.getAsJsonPrimitive().isBoolean()) amount = weight.getAsBoolean() ? 1.0F : 0.0F;
                    }
                    if (!Float.isFinite(amount)) return null;
                    amount = Math.max(0.05F, Math.min(1_000_000.0F, amount));
                    result.add(new TexturePath(path.getAsString(), amount, resolvedTint, resolvedOverlay,
                            multiTint(option)));
                }
                return List.copyOf(result);
            }
            JsonElement path = object.get("path");
            JsonElement tintValue = object.get("tint_color");
            Integer tint = tintValue != null && tintValue.isJsonObject() ? null : color(tintValue);
            Integer overlay = color(object.get("overlay_color"));
            return path != null && path.isJsonPrimitive() && path.getAsJsonPrimitive().isString()
                    ? List.of(new TexturePath(path.getAsString(), 1.0F, tint, overlay, multiTint(object))) : null;
        }
        if (!value.isJsonArray()) return null;
        JsonArray array = value.getAsJsonArray();
        List<TexturePath> result = null;
        for (JsonElement element : array) {
            List<TexturePath> paths = texturePaths(element);
            if (paths == null || result != null && !result.equals(paths)) return null;
            result = paths;
        }
        return result;
    }

    private static @Nullable MultiTint multiTint(JsonObject object) {
        JsonElement base = object.get("tint_base_color");
        JsonElement target = object.get("tint_color");
        boolean hasBase = base != null && base.isJsonObject();
        boolean hasTarget = target != null && target.isJsonObject();
        if (!hasBase && !hasTarget) return null;
        JsonElement map = object.get("tint_map_path");
        String mapPath = map != null && map.isJsonPrimitive() && map.getAsJsonPrimitive().isString()
                ? map.getAsString() : null;
        if (mapPath != null && mapPath.isEmpty()) mapPath = null;
        return new MultiTint(mapPath, tintChannels(hasBase ? base.getAsJsonObject() : null),
                tintChannels(hasTarget ? target.getAsJsonObject() : null));
    }

    private static BedrockTerrainTint.Channels tintChannels(@Nullable JsonObject object) {
        if (object == null) return new BedrockTerrainTint.Channels(0, 0, 0, 0);
        return new BedrockTerrainTint.Channels(tintChannel(object, "r_color"), tintChannel(object, "g_color"),
                tintChannel(object, "b_color"), tintChannel(object, "a_color"));
    }

    private static int tintChannel(JsonObject object, String name) {
        JsonElement element = object.get(name);
        Integer value = color(element);
        if (value != null && element != null && element.isJsonPrimitive()
                && element.getAsJsonPrimitive().isString()) {
            String text = element.getAsString();
            if (text.startsWith("#")) text = text.substring(1);
            if (text.length() == 6) value &= 0x00FFFFFF;
        }
        return value == null ? 0 : value;
    }

    private static @Nullable Integer color(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull()) return null;
        if (element.isJsonArray()) {
            JsonArray components = element.getAsJsonArray();
            if (components.size() < 3) throw new JsonParseException("Terrain texture color requires at least three components");
            int red = colorComponent(components.get(0));
            int green = colorComponent(components.get(1));
            int blue = colorComponent(components.get(2));
            int alpha = components.size() > 3 ? colorComponent(components.get(3)) : 255;
            return alpha << 24 | red << 16 | green << 8 | blue;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
            throw new JsonParseException("Terrain texture color must be a hex string or component array");
        String value = element.getAsString();
        if (value.startsWith("#")) value = value.substring(1);
        if (!value.matches("[0-9a-fA-F]{6}([0-9a-fA-F]{2})?"))
            throw new JsonParseException("Invalid terrain texture color");
        long parsed = Long.parseLong(value, 16);
        return (int) (value.length() == 6 ? parsed | 0xFF000000L : parsed);
    }

    private static int colorComponent(JsonElement element) {
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()
                || !element.getAsJsonPrimitive().isNumber()) return 255;
        return Math.max(0, Math.min(255, element.getAsInt()));
    }

    private static Identifier spriteName(String path, @Nullable Integer tint, @Nullable Integer overlay,
                                         @Nullable AnimationKey animation) {
        if (animation != null) {
            return Identifier.fromNamespaceAndPath(BedrockPackResources.NAMESPACE,
                    "bedrock_terrain/animated/" + HexFormat.of().formatHex(animation.tile().getBytes(StandardCharsets.UTF_8))
                            + "/" + animation.atlasIndex() + "/" + animation.variation()
                            + "/" + (tint == null ? "none" : Integer.toUnsignedString(tint, 16))
                            + "/" + (overlay == null ? "none" : Integer.toUnsignedString(overlay, 16)));
        }
        if (tint == null && overlay == null) return BedrockPackResources.textureSprite(path);
        return Identifier.fromNamespaceAndPath(BedrockPackResources.NAMESPACE,
                "bedrock_terrain/" + HexFormat.of().formatHex(path.getBytes(StandardCharsets.UTF_8))
                        + "/" + (tint == null ? "none" : Integer.toUnsignedString(tint, 16))
                        + "/" + (overlay == null ? "none" : Integer.toUnsignedString(overlay, 16)));
    }

    private static void addSprite(Output output, Identifier id, Resource resource, @Nullable Resource baseResource,
                                  boolean sameSource, @Nullable Resource tintResource, TexturePath path,
                                  @Nullable BedrockTerrainFlipbook flipbook) {
        if (path.tint() == null && path.overlay() == null && path.multiTint() == null && flipbook == null) {
            output.add(id, resource);
            return;
        }
        output.add(id, loader -> loadCustomSprite(loader, id, resource, baseResource, sameSource, tintResource, path, flipbook));
    }

    private static @Nullable SpriteContents loadCustomSprite(SpriteResourceLoader loader, Identifier id, Resource resource,
                                                             @Nullable Resource baseResource, boolean sameSource,
                                                             @Nullable Resource tintResource, TexturePath path,
                                                             @Nullable BedrockTerrainFlipbook flipbook) {
        Optional<AnimationMetadataSection> animation;
        ResourceMetadata resourceMetadata;
        try {
            resourceMetadata = resource.metadata();
            animation = resourceMetadata.getSection(AnimationMetadataSection.TYPE);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read sprite metadata for " + id, exception);
        }
        NativeImage image;
        try (InputStream stream = resource.open()) {
            image = NativeImage.read(stream);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read sprite image for " + id + " from " + resource.sourcePackId(), exception);
        }
        FrameSize frame;
        if (animation.isPresent()) {
            frame = animation.get().calculateFrameSize(image.getWidth(), image.getHeight());
            if (!Mth.isMultipleOf(image.getWidth(), frame.width()) || !Mth.isMultipleOf(image.getHeight(), frame.height())) {
                image.close();
                throw new JsonParseException("Image " + id + " size is not a multiple of frame size");
            }
        } else {
            frame = new FrameSize(image.getWidth(), image.getHeight());
        }
        NativeImage selected = image;
        try {
            if (flipbook != null && (image.getWidth() <= 0 || image.getHeight() % image.getWidth() != 0))
                throw new JsonParseException("Terrain flipbook image must contain square vertical frames: " + id);
            int frameTotal = flipbook == null ? 0 : image.getHeight() / image.getWidth();
            FrameSize frameSize = frame;
            if (flipbook != null) {
                int[] slot = animationSlot(image, baseResource, sameSource, tintResource, id);
                frameSize = new FrameSize(slot[0], slot[1]);
                if (slot[0] != image.getWidth() || slot[1] != image.getWidth() || flipbook.replicate() > 1) {
                    selected = resampleFrames(image, slot[0], slot[1], flipbook.replicate());
                    image.close();
                }
            } else if (path.multiTint() != null) {
                NativeImage tintMap = tintResource == null ? null : readTerrainImage(tintResource, id, "tint map");
                try {
                    selected = tintImage(image, tintMap, path.multiTint(), path.overlay());
                } finally {
                    if (tintMap != null) tintMap.close();
                }
                image.close();
            } else if (path.tint() != null || path.overlay() != null) {
                selected = image.mappedCopy(pixel -> tintPixel(pixel, path.tint(), path.overlay()));
                image.close();
            }
            Optional<AnimationMetadataSection> animationData = animation;
            if (flipbook != null) {
                AnimationMetadataSection sequence = flipbook.animation(frameTotal);
                animationData = Optional.of(new AnimationMetadataSection(sequence.frames(),
                        Optional.of(frameSize.width()), Optional.of(frameSize.height()),
                        sequence.defaultFrameTime(), sequence.interpolatedFrames()));
            }
            Resource transformed = BedrockSpriteResource.fromImage(resource, resourceMetadata, selected, animationData);
            return loader.loadSprite(id, transformed);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot prepare transformed terrain sprite " + id, exception);
        } finally {
            selected.close();
        }
    }

    private static NativeImage readTerrainImage(Resource resource, Identifier sprite, String role) {
        try (InputStream stream = resource.open()) {
            return NativeImage.read(stream);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read " + role + " for " + sprite + " from " + resource.sourcePackId(), exception);
        }
    }

    private static NativeImage tintImage(NativeImage source, @Nullable NativeImage tintMap, MultiTint tint,
                                         @Nullable Integer overlay) {
        int width = source.getWidth();
        int height = source.getHeight();
        int frameHeight = Math.min(width, height);
        NativeImage result = new NativeImage(width, height, false);
        BedrockTerrainTint transform = new BedrockTerrainTint(tint.base(), tint.target());
        try {
            for (int y = 0; y < height; y++) {
                int withinFrame = Math.min(y, frameHeight - 1);
                for (int x = 0; x < width; x++) {
                    int mask = 0;
                    if (tintMap != null) {
                        int mapX = Math.min(width - 1, (int) ((long) x * tintMap.getWidth() / width));
                        int mapHeight = Math.min(tintMap.getWidth(), tintMap.getHeight());
                        int mapY = Math.min(frameHeight - 1, (int) ((long) withinFrame * mapHeight / frameHeight));
                        mask = tintMap.getPixel(mapX, mapY);
                    }
                    result.setPixel(x, y, transform.apply(source.getPixel(x, y), mask, 0, overlay));
                }
            }
            return result;
        } catch (RuntimeException exception) {
            result.close();
            throw exception;
        }
    }

    private static int[] animationSlot(NativeImage animation, @Nullable Resource baseResource,
                                       boolean sameSource, @Nullable Resource tintResource, Identifier name) {
        int width = 16;
        int height = 16;
        if (sameSource) {
            width = Math.max(width, animation.getWidth());
            height = Math.max(height, Math.min(animation.getWidth(), animation.getHeight()));
        } else if (baseResource != null) {
            try (NativeImage base = readTerrainImage(baseResource, name, "base texture")) {
                width = Math.max(width, base.getWidth());
                height = Math.max(height, Math.min(base.getWidth(), base.getHeight()));
            }
        }
        if (tintResource != null) {
            try (NativeImage tint = readTerrainImage(tintResource, name, "tint map")) {
                width = Math.max(width, tint.getWidth());
                height = Math.max(height, Math.min(tint.getWidth(), tint.getHeight()));
            }
        }
        return new int[]{width, height};
    }

    private static NativeImage resampleFrames(NativeImage source, int width, int height, int replicate) {
        int sourceWidth = source.getWidth();
        int frames = source.getHeight() / sourceWidth;
        NativeImage result = new NativeImage(width, Math.multiplyExact(height, frames), false);
        try {
            for (int frame = 0; frame < frames; frame++) {
                for (int y = 0; y < height; y++) {
                    int sourceY = frame * sourceWidth
                            + (int) ((2L * y + 1) * sourceWidth / (2L * height * replicate));
                    for (int x = 0; x < width; x++) {
                        int sourceX = (int) ((2L * x + 1) * sourceWidth / (2L * width * replicate));
                        result.setPixel(x, frame * height + y, source.getPixel(sourceX, sourceY));
                    }
                }
            }
            return result;
        } catch (RuntimeException exception) {
            result.close();
            throw exception;
        }
    }

    private static int tintPixel(int pixel, @Nullable Integer tint, @Nullable Integer overlay) {
        float scale = 1.0F / 255.0F;
        float alpha = (pixel >>> 24) * scale;
        float red = (pixel >> 16 & 255) * scale;
        float green = (pixel >> 8 & 255) * scale;
        float blue = (pixel & 255) * scale;
        if (tint != null) {
            red *= (tint >> 16 & 255) / 255.0F;
            green *= (tint >> 8 & 255) / 255.0F;
            blue *= (tint & 255) / 255.0F;
        }
        if (overlay != null) {
            float inverse = 1.0F - alpha;
            red = inverse * red + ((overlay >> 16 & 255) / 255.0F) * red * alpha;
            green = inverse * green + ((overlay >> 8 & 255) / 255.0F) * green * alpha;
            blue = inverse * blue + ((overlay & 255) / 255.0F) * blue * alpha;
            alpha = 1.0F;
        }
        return (int) (alpha * 255.0F) << 24 | (int) (red * 255.0F) << 16
                | (int) (green * 255.0F) << 8 | (int) (blue * 255.0F);
    }

    private static List<String> candidates(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png") || lower.endsWith(".tga") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return List.of(path);
        }
        return List.of(path + ".png", path + ".tga", path + ".jpg", path + ".jpeg");
    }

    private static @Nullable ImageResource findImage(ResourceManager resources, String path, Map<String, Integer> priorities) {
        ImageResource best = null;
        int bestPriority = Integer.MIN_VALUE;
        for (String candidate : candidates(path)) {
            var resource = resources.getResource(BedrockPackResources.textureResource(candidate));
            if (resource.isEmpty()) continue;
            int priority = priorities.getOrDefault(resource.get().sourcePackId(), -1);
            if (best == null || priority > bestPriority) {
                best = new ImageResource(candidate, resource.get());
                bestPriority = priority;
            }
        }
        return best;
    }

    public static void atlasStitched(TextureAtlas atlas) {
        if (atlas.location().equals(TextureAtlas.LOCATION_BLOCKS)) active = pending;
    }

    public static @Nullable Identifier currentSprite(Identifier reloadKey, Identifier javaSprite,
                                                     BlockState state, Direction face) {
        Bindings snapshot = active;
        Identifier block = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (block == null) return null;
        List<List<WeightedSprite>> variants = snapshot.configured().get(new Face(BedrockBlockNames.textureKey(block, state), face));
        if (variants == null) variants = snapshot.configured().get(new Face(block.toString(), face));
        if (variants == null || variants.isEmpty()) return null;
        int variant = textureVariant(state, variants);
        if (variant < 0) return null;
        if (variant >= variants.size()) variant = 0;
        WeightedSprite choice = null;
        for (WeightedSprite candidate : variants.get(variant)) {
            if (reloadKey.equals(candidate.reloadKey())) {
                choice = candidate;
                break;
            }
        }
        if (choice == null) return null;
        Integer javaPriority = snapshot.javaPriorities().get(javaSprite);
        return javaPriority != null && javaPriority > choice.priority() ? javaSprite : choice.sprite();
    }

    public static @Nullable TextureAtlasSprite surfaceTexture(BlockState state, Direction face, TextureAtlasSprite javaSprite) {
        SurfaceBinding binding = surfaceTextureBinding(state, face, javaSprite);
        return binding == null ? null : binding.sprite();
    }

    public static @Nullable SurfaceBinding surfaceTextureBinding(BlockState state, Direction face, TextureAtlasSprite javaSprite) {
        return surfaceTextureBinding(state, face, javaSprite, ThreadLocalRandom.current().nextFloat());
    }

    public static @Nullable SurfaceBinding surfaceTextureBinding(BlockState state, Direction face, TextureAtlasSprite javaSprite,
                                                                   float selection) {
        Identifier block = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (block == null) return null;
        Bindings snapshot = active;
        Face key = new Face(BedrockBlockNames.textureKey(block, state), face);
        List<List<WeightedSprite>> variants = snapshot.configured().get(key);
        if (variants == null) {
            key = new Face(block.toString(), face);
            variants = snapshot.configured().get(key);
        }
        if (variants == null || variants.isEmpty()) return null;
        int variant = textureVariant(state, variants);
        if (variant < 0) return null;
        if (variant >= variants.size()) variant = 0;
        List<WeightedSprite> choices = variants.get(variant);
        if (choices.isEmpty()) return snapshot.implicitDefaults().contains(key) ? new SurfaceBinding(javaSprite, null) : null;
        float total = 0.0F;
        for (WeightedSprite choice : choices) total += choice.weight();
        float selected = selection * total;
        WeightedSprite choice = choices.get(choices.size() - 1);
        for (WeightedSprite candidate : choices) {
            if (selected <= candidate.weight()) {
                choice = candidate;
                break;
            }
            selected -= candidate.weight();
        }
        Integer javaPriority = snapshot.javaPriorities().get(javaSprite.contents().name());
        if (javaPriority != null && javaPriority > choice.priority()) return new SurfaceBinding(javaSprite, null);
        TextureAtlasSprite result = DescriptionMaterial.TERRAIN_SHEET.getAtlas().getSprite(choice.sprite());
        return result.contents().name().equals(MissingTextureAtlasSprite.getLocation())
                ? null : new SurfaceBinding(result, choice.reloadKey());
    }

    public static @Nullable TextureAtlasSprite matchingTexture(TextureAtlasSprite javaSprite) {
        Identifier name = javaSprite.contents().name();
        Identifier id = active.direct().get(name);
        if (id == null) return null;
        TextureAtlas atlas = DescriptionMaterial.TERRAIN_SHEET.getAtlas();
        TextureAtlasSprite sprite = atlas.getSprite(id);
        return sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation()) ? null : sprite;
    }

    private static int textureVariant(BlockState state, List<List<WeightedSprite>> variants) {
        boolean same = true;
        for (int i = 1; i < variants.size(); i++) {
            if (!sameChoices(variants.get(0), variants.get(i))) {
                same = false;
                break;
            }
        }
        if (same) return 0;
        if (state.is(Blocks.STONE) || state.is(Blocks.STONE_STAIRS)
                || state.is(Blocks.STONE_BRICK_STAIRS) || state.is(Blocks.SANDSTONE_STAIRS)
                || state.is(Blocks.RED_SANDSTONE_STAIRS) || state.is(Blocks.PRISMARINE_STAIRS)
                || state.is(Blocks.SMOOTH_QUARTZ_STAIRS) || state.is(Blocks.SAND) || state.is(Blocks.DIRT)) return 0;
        if (state.is(Blocks.FROSTED_ICE)) return state.getValue(FrostedIceBlock.AGE);
        if (state.is(Blocks.SNIFFER_EGG)) return state.getValue(SnifferEggBlock.HATCH);
        if (state.is(Blocks.SUSPICIOUS_SAND) || state.is(Blocks.SUSPICIOUS_GRAVEL)) {
            return state.getValue(BlockStateProperties.DUSTED);
        }
        if (state.is(Blocks.STRUCTURE_BLOCK)) {
            return switch (state.getValue(StructureBlock.MODE)) {
                case DATA -> 0;
                case SAVE -> 1;
                case LOAD -> 2;
                case CORNER -> 3;
            };
        }
        if (state.is(Blocks.CREAKING_HEART)) {
            return switch (state.getValue(CreakingHeartBlock.STATE)) {
                case UPROOTED -> 0;
                case DORMANT -> 1;
                case AWAKE -> 2;
            };
        }
        if (state.is(Blocks.VAULT)) {
            int vaultState = switch (state.getValue(VaultBlock.STATE)) {
                case INACTIVE -> 0;
                case ACTIVE -> 1;
                case UNLOCKING -> 2;
                case EJECTING -> 3;
            };
            return vaultState + (state.getValue(VaultBlock.OMINOUS) ? 4 : 0);
        }
        if (state.getBlock() instanceof WallBlock) {
            Identifier block = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (block != null && WALL_TILES.containsKey(block.toString())) {
                return (state.getValue(WallBlock.UP) ? 1 : 0)
                        | wallSide(state.getValue(WallBlock.NORTH)) << 1
                        | wallSide(state.getValue(WallBlock.EAST)) << 3
                        | wallSide(state.getValue(WallBlock.SOUTH)) << 5
                        | wallSide(state.getValue(WallBlock.WEST)) << 7;
            }
        }
        if (state.getBlock() instanceof HugeMushroomBlock) {
            Identifier block = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (block != null && MUSHROOM_TILES.containsKey(block.toString())) {
                int index = (state.getValue(HugeMushroomBlock.DOWN) ? 0 : 32)
                        | (state.getValue(HugeMushroomBlock.EAST) ? 0 : 16)
                        | (state.getValue(HugeMushroomBlock.NORTH) ? 0 : 8)
                        | (state.getValue(HugeMushroomBlock.SOUTH) ? 0 : 4)
                        | (state.getValue(HugeMushroomBlock.UP) ? 0 : 2)
                        | (state.getValue(HugeMushroomBlock.WEST) ? 0 : 1);
                String mapping = state.is(Blocks.MUSHROOM_STEM) ? MUSHROOM_STEM_VARIANTS : MUSHROOM_CAP_VARIANTS;
                return Character.digit(mapping.charAt(index), 16);
            }
        }
        if (state.getBlock() instanceof LeavesBlock) {
            Identifier block = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (block != null && LEAF_TILES.containsKey(BedrockBlockNames.textureKey(block, state))) {
                return Minecraft.getInstance().options.cutoutLeaves().get() ? 0 : 1;
            }
        }
        if (state.getBlock() instanceof SlabBlock) {
            Identifier block = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (block != null && BedrockBlockNames.isSplitSlabTextureKey(BedrockBlockNames.textureKey(block, state))) {
                return state.getValue(SlabBlock.TYPE) == SlabType.TOP ? 1 : 0;
            }
        }
        return -1;
    }

    private static int wallSide(WallSide side) {
        return switch (side) {
            case NONE -> 0;
            case LOW -> 1;
            case TALL -> 2;
        };
    }

    private static boolean sameChoices(List<WeightedSprite> first, List<WeightedSprite> second) {
        if (first.size() != second.size()) return false;
        for (int i = 0; i < first.size(); i++) {
            WeightedSprite a = first.get(i);
            WeightedSprite b = second.get(i);
            if (!a.sprite().equals(b.sprite()) || a.weight() != b.weight() || a.priority() != b.priority()
                    || a.variationPriority() != b.variationPriority()) return false;
        }
        return true;
    }

    @Override
    public MapCodec<? extends SpriteSource> codec() {
        return CODEC;
    }
}

