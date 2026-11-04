package com.kltyton.bedrockruntime.client.particle.definition.molang;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Vanilla item identifiers that differ between Bedrock and Java particle contexts. */
public final class BedrockItemIdentifiers {
    private static final Map<String, String> JAVA_TO_BEDROCK = Map.ofEntries(
            Map.entry("map", "empty_map"),
            Map.entry("melon", "melon_block"),
            Map.entry("stone_slab", "normal_stone_slab"),
            Map.entry("frogspawn", "frog_spawn"),
            Map.entry("glow_item_frame", "glow_frame"),
            Map.entry("item_frame", "frame"),
            Map.entry("oak_door", "wooden_door"),
            Map.entry("small_dripleaf", "small_dripleaf_block"),
            Map.entry("rooted_dirt", "dirt_with_roots"),
            Map.entry("nether_quartz_ore", "quartz_ore"),
            Map.entry("flowering_azalea_leaves", "azalea_leaves_flowered"),
            Map.entry("cobweb", "web"),
            Map.entry("dead_bush", "deadbush"),
            Map.entry("bricks", "brick_block"),
            Map.entry("spawner", "mob_spawner"),
            Map.entry("jack_o_lantern", "lit_pumpkin"),
            Map.entry("lily_pad", "waterlily"),
            Map.entry("end_stone_bricks", "end_bricks"),
            Map.entry("light", "light_block"),
            Map.entry("terracotta", "hardened_clay"),
            Map.entry("dirt_path", "grass_path"),
            Map.entry("prismarine_brick_stairs", "prismarine_bricks_stairs"),
            Map.entry("magma_block", "magma"),
            Map.entry("red_nether_bricks", "red_nether_brick"),
            Map.entry("end_stone_brick_stairs", "end_brick_stairs"),
            Map.entry("slime_block", "slime"),
            Map.entry("note_block", "noteblock"),
            Map.entry("oak_button", "wooden_button"),
            Map.entry("oak_pressure_plate", "wooden_pressure_plate"),
            Map.entry("oak_trapdoor", "trapdoor"),
            Map.entry("oak_fence_gate", "fence_gate"),
            Map.entry("powered_rail", "golden_rail"),
            Map.entry("zombified_piglin_spawn_egg", "zombie_pigman_spawn_egg"),
            Map.entry("light_gray_glazed_terracotta", "silver_glazed_terracotta"),
            Map.entry("waxed_copper_block", "waxed_copper")
    );
    private static final Map<String, String> BEDROCK_TO_JAVA;
    private static final Map<String, String> EQUIPPED_ITEM_PATHS;
    private static final Map<String, String> BEDROCK_EQUIPPED_ITEM_PATHS;
    private static final Map<String, String> BEDROCK_QUERY_PATHS;
    private static final Map<String, String> CONFLICTING_JAVA_NAMES;
    private static final Map<String, String> COLORED_TO_BEDROCK;
    private static final List<ResourceLocation> COLORED_BEDS;
    private static final List<ResourceLocation> COLORED_BANNERS;
    private static final Map<String, String> CONFLICTING_BEDROCK_NAMES = Map.of(
            "netherbrick", "nether_brick",
            "nether_brick", "nether_bricks",
            "snow_layer", "snow",
            "snow", "snow_block",
            "normal_stone_stairs", "stone_stairs",
            "stone_stairs", "cobblestone_stairs",
            "stonecutter_block", "stonecutter",
            "undyed_shulker_box", "shulker_box"
    );
    private static final Set<String> AMBIGUOUS_BEDROCK_NAMES = Set.of(
            "nether_brick", "snow", "stone_stairs", "stonecutter", "shulker_box");
    private static final List<String> DYE_COLORS = List.of("white", "orange", "magenta", "light_blue", "yellow", "lime",
            "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black");

    static {
        Map<String, String> reverse = new HashMap<>();
        Map<String, String> equipped = new HashMap<>();
        Map<String, String> bedrockEquipped = new HashMap<>();
        Map<String, String> queryAliases = new IdentityHashMap<>();
        Map<String, String> conflictingJava = new HashMap<>();
        Map<String, String> colored = new HashMap<>();
        JAVA_TO_BEDROCK.forEach((javaName, bedrockName) -> {
            reverse.put(bedrockName, javaName);
            equipped.put(javaName, javaName);
            String path = new String(javaName);
            bedrockEquipped.put(javaName, path);
            queryAliases.put(path, bedrockName);
        });
        CONFLICTING_BEDROCK_NAMES.forEach((bedrockName, javaName) -> {
            conflictingJava.put(javaName, bedrockName);
            if (!AMBIGUOUS_BEDROCK_NAMES.contains(javaName)) {
                String path = new String(javaName);
                bedrockEquipped.put(javaName, path);
                queryAliases.put(path, bedrockName);
            }
        });
        for (String color : DYE_COLORS) {
            String bedName = color + "_bed";
            String bedPath = new String(bedName);
            colored.put(bedName, "bed");
            bedrockEquipped.put(bedName, bedPath);
            queryAliases.put(bedPath, "bed");
            String bannerName = color + "_banner";
            String bannerPath = new String(bannerName);
            colored.put(bannerName, "banner");
            bedrockEquipped.put(bannerName, bannerPath);
            queryAliases.put(bannerPath, "banner");
        }
        BEDROCK_TO_JAVA = Map.copyOf(reverse);
        EQUIPPED_ITEM_PATHS = Map.copyOf(equipped);
        BEDROCK_EQUIPPED_ITEM_PATHS = Map.copyOf(bedrockEquipped);
        BEDROCK_QUERY_PATHS = Collections.unmodifiableMap(queryAliases);
        CONFLICTING_JAVA_NAMES = Map.copyOf(conflictingJava);
        COLORED_TO_BEDROCK = Map.copyOf(colored);
        COLORED_BEDS = DYE_COLORS.stream().map(color -> new ResourceLocation("minecraft", color + "_bed")).toList();
        COLORED_BANNERS = DYE_COLORS.stream().map(color -> new ResourceLocation("minecraft", color + "_banner")).toList();
    }

    private BedrockItemIdentifiers() {}

    /** Keeps Java-facing names while retaining their Bedrock comparison values. */
    @ApiStatus.Internal
    public static String equippedItemPath(ResourceLocation item) {
        if (!item.getNamespace().equals("minecraft")) return item.getPath();
        return EQUIPPED_ITEM_PATHS.getOrDefault(item.getPath(), item.getPath());
    }

    @ApiStatus.Internal
    public static String bedrockEquippedItemPath(ResourceLocation item) {
        if (!item.getNamespace().equals("minecraft")) return item.getPath();
        if (AMBIGUOUS_BEDROCK_NAMES.contains(item.getPath())) return bedrockPackName(item).getPath();
        return BEDROCK_EQUIPPED_ITEM_PATHS.getOrDefault(item.getPath(), item.getPath());
    }

    @ApiStatus.Internal
    public static @Nullable String bedrockQueryPath(String value) {
        return BEDROCK_QUERY_PATHS.get(value);
    }

    public static ResourceLocation bedrockName(ResourceLocation item) {
        if (!item.getNamespace().equals("minecraft")) return item;
        String name = JAVA_TO_BEDROCK.get(item.getPath());
        if (name == null) name = COLORED_TO_BEDROCK.get(item.getPath());
        return name == null ? item : new ResourceLocation("minecraft", name);
    }

    @ApiStatus.Internal
    public static ResourceLocation bedrockPackName(ResourceLocation item) {
        if (!item.getNamespace().equals("minecraft")) return item;
        String name = CONFLICTING_JAVA_NAMES.get(item.getPath());
        return name == null ? bedrockName(item) : new ResourceLocation("minecraft", name);
    }

    public static @Nullable ResourceLocation javaAlias(ResourceLocation item) {
        if (!item.getNamespace().equals("minecraft")) return null;
        String name = BEDROCK_TO_JAVA.get(item.getPath());
        if (name == null && !AMBIGUOUS_BEDROCK_NAMES.contains(item.getPath())) {
            name = CONFLICTING_BEDROCK_NAMES.get(item.getPath());
        }
        return name == null ? null : new ResourceLocation("minecraft", name);
    }

    @ApiStatus.Internal
    public static @Nullable ResourceLocation bedrockPackJavaAlias(ResourceLocation item) {
        if (!item.getNamespace().equals("minecraft")) return null;
        String name = CONFLICTING_BEDROCK_NAMES.get(item.getPath());
        if (name == null) name = BEDROCK_TO_JAVA.get(item.getPath());
        return name == null ? null : new ResourceLocation("minecraft", name);
    }

    @ApiStatus.Internal
    public static List<ResourceLocation> javaAliases(ResourceLocation item, boolean bedrockItemNames) {
        if (item.getNamespace().equals("minecraft")) {
            if (item.getPath().equals("bed")) return COLORED_BEDS;
            if (item.getPath().equals("banner")) return COLORED_BANNERS;
        }
        ResourceLocation alias = bedrockItemNames ? bedrockPackJavaAlias(item) : javaAlias(item);
        return alias == null ? List.of() : List.of(alias);
    }

    @ApiStatus.Internal
    public static boolean hasAmbiguousBedrockMeaning(ResourceLocation item) {
        return item.getNamespace().equals("minecraft") && AMBIGUOUS_BEDROCK_NAMES.contains(item.getPath());
    }
}
