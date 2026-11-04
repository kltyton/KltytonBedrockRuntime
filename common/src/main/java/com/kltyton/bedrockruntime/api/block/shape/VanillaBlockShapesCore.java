package com.kltyton.bedrockruntime.api.block.shape;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kltyton.bedrockruntime.Constants;
import com.kltyton.bedrockruntime.api.block.shape.geometry.CompoundShape;
import com.kltyton.bedrockruntime.api.block.shape.geometry.ConvexPart;
import com.kltyton.bedrockruntime.api.block.shape.geometry.ShapeOperations;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Version-neutral packaged-vanilla/mod block-shape algorithm; block ids are supplied by the adapter. */
final class VanillaBlockShapesCore {
    private static volatile Map<BlockState, Prepared> shapes = Map.of();
    private static JsonObject packagedModels = new JsonObject();
    private static boolean modBlocksPrepared;
    private static final LoadingCache<VoxelShape, VoxelShape> FORCED_SHAPES = CacheBuilder.newBuilder()
            .weakKeys().maximumSize(4096).build(CacheLoader.from((VoxelShape shape) ->
                    CompoundShape.ofObb(shape.toAabbs().stream().map(ConvexPart::box).toList())));

    private static final LoadingCache<VoxelShape, VoxelShape> AABB_SHAPES = CacheBuilder.newBuilder()
            .weakKeys().maximumSize(4096).build(CacheLoader.from(BlockShapeMode.AABB::apply));

    private VanillaBlockShapesCore() { }

    static void initialize() {
        String resource = "data/kltyton_bedrock_runtime/block_shapes/vanilla.json";
        Map<BlockState, Prepared> prepared = new HashMap<>();
        Map<VariantKey, List<ConvexPart>> models = new HashMap<>();
        Map<Plan, Prepared> plans = new HashMap<>();
        try (var stream = VanillaBlockShapesCore.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) { throw new IllegalStateException("Missing generated vanilla block shape data"); }
            JsonObject data = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            packagedModels = data.getAsJsonObject("models");
            for (var entry : data.getAsJsonObject("states").entrySet()) {
                prepareBlock(BlockShapePlatform.canonicalId(entry.getKey()), entry.getValue().getAsJsonObject(),
                        packagedModels, models, plans, prepared);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load packaged vanilla block shape data", exception);
        }
        shapes = Map.copyOf(prepared);
        Constants.LOG.info("Prepared vanilla block model geometry for {} states ({} distinct plans)", shapes.size(), plans.size());
    }

    static synchronized void initializeModBlocks() {
        if (modBlocksPrepared) { return; }
        Map<BlockState, Prepared> prepared = new HashMap<>(shapes);
        Map<VariantKey, List<ConvexPart>> models = new HashMap<>();
        Map<Plan, Prepared> plans = new HashMap<>();
        JsonObject modelData = packagedModels.deepCopy();
        for (Block block : BlockShapePlatform.blocks()) {
            String id = BlockShapePlatform.blockId(block);
            if (namespace(id).equals("minecraft") || BlockShapePlatform.isAutoPartBlock(block)) { continue; }
            String resource = "assets/%s/blockstates/%s.json".formatted(namespace(id), path(id));
            try (var stream = VanillaBlockShapesCore.class.getClassLoader().getResourceAsStream(resource)) {
                if (stream == null) { continue; }
                JsonObject definition = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                collectModels(definition, modelData);
                Map<BlockState, Prepared> blockShapes = new HashMap<>();
                prepareBlock(id, definition, modelData, models, plans, blockShapes);
                prepared.putAll(blockShapes);
            } catch (IOException | RuntimeException exception) {
                Constants.LOG.warn("Cannot prepare model geometry for {}; retaining the block's own shapes", id, exception);
            }
        }
        int additionalStates = prepared.size() - shapes.size();
        shapes = Map.copyOf(prepared);
        modBlocksPrepared = true;
        Constants.LOG.info("Prepared packaged mod block geometry for {} additional states", additionalStates);
    }

    private static void collectModels(JsonElement value, JsonObject modelData) {
        if (value.isJsonArray()) {
            for (JsonElement child : value.getAsJsonArray()) { collectModels(child, modelData); }
        } else if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            if (object.has("model") && object.get("model").isJsonPrimitive()) {
                String id = BlockShapePlatform.canonicalId(object.get("model").getAsString());
                if (!modelData.has(id)) {
                    JsonArray elements = ModelShapeCacheCore.resolveElements(id, modelData);
                    modelData.add(id, elements == null ? new JsonArray() : elements);
                }
            }
            for (var entry : object.entrySet()) { collectModels(entry.getValue(), modelData); }
        }
    }

    private static void prepareBlock(String id, JsonObject definition, JsonObject modelData,
            Map<VariantKey, List<ConvexPart>> models, Map<Plan, Prepared> plans, Map<BlockState, Prepared> prepared) {
        Block block = BlockShapePlatform.blockById(id);
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            List<List<Option>> groups = new ArrayList<>();
            boolean multipart = definition.has("multipart");
            if (multipart) {
                for (JsonElement raw : definition.getAsJsonArray("multipart")) {
                    JsonObject part = raw.getAsJsonObject();
                    if (!part.has("when") || matches(state, part.getAsJsonObject("when"))) {
                        groups.add(options(part.get("apply"), modelData, models));
                    }
                }
            } else if (definition.has("variants")) {
                for (var variant : definition.getAsJsonObject("variants").entrySet()) {
                    if (matchesVariant(state, variant.getKey())) {
                        groups.add(options(variant.getValue(), modelData, models));
                        break;
                    }
                }
            }
            if (groups.isEmpty() || groups.stream().anyMatch(List::isEmpty)
                    || groups.stream().flatMap(List::stream).allMatch(option -> option.parts.isEmpty())) { continue; }
            Plan plan = new Plan(List.copyOf(groups), multipart);
            prepared.put(state, plans.computeIfAbsent(plan, VanillaBlockShapesCore::compile));
        }
    }

    static boolean contains(BlockState state) {
        Prepared prepared = shapes.get(state);
        return enabled(state) && prepared != null && (BlockShapePlatform.forceAllBlockObb()
                || prepared.slanted);
    }

    private static boolean enabled(BlockState state) {
        String id = BlockShapePlatform.blockId(state.getBlock());
        return (BlockShapePlatform.forceAllBlockObb()
                || BlockShapePlatform.vanillaSlantedBlockObb() && namespace(id).equals("minecraft"))
                && BlockShapePlatform.allowsBlockModel(state.getBlock());
    }

    static VoxelShape replacement(BlockState state, BlockGetter level, BlockPos pos, VoxelShape original) {
        if (original.isEmpty()) { return original; }
        if (!BlockShapePlatform.allowsBlockModel(state.getBlock())) {
            return original instanceof CompoundShape ? AABB_SHAPES.getUnchecked(original) : original;
        }
        if (!enabled(state)) { return original; }
        VoxelShape model = shape(state, pos);
        if (model != null && !model.isEmpty()) {
            Vec3 offset = BlockShapePlatform.blockOffset(state, level, pos);
            return model.move(offset.x, offset.y, offset.z);
        }
        if (!BlockShapePlatform.forceAllBlockObb() || original instanceof CompoundShape
                || !ShapeOperations.isFinite(original)) { return original; }
        return FORCED_SHAPES.getUnchecked(original);
    }

    static VoxelShape shape(BlockState state, BlockPos pos) {
        if (!enabled(state)) { return null; }
        Prepared prepared = shapes.get(state);
        if (prepared == null) { return null; }
        if (prepared.choices.size() == 1) {
            Choice choice = prepared.choices.get(0);
            return choice.slanted || BlockShapePlatform.forceAllBlockObb() ? choice.shape : null;
        }
        RandomSource random = RandomSource.create(state.getSeed(pos));
        long multipartSeed = prepared.plan.multipart ? random.nextLong() : 0;
        int index = 0;
        for (List<Option> group : prepared.plan.groups) {
            if (prepared.plan.multipart) { random.setSeed(multipartSeed); }
            int selected = 0;
            if (group.size() > 1) {
                int weight = group.stream().mapToInt(Option::weight).sum();
                int value = random.nextInt(weight);
                while (value >= group.get(selected).weight) { value -= group.get(selected++).weight; }
            }
            index = index * group.size() + selected;
        }
        Choice choice = prepared.choices.get(index);
        return choice.slanted || BlockShapePlatform.forceAllBlockObb() ? choice.shape : null;
    }

    private static Prepared compile(Plan plan) {
        List<Choice> choices = new ArrayList<>();
        expand(plan.groups, 0, List.of(), false, choices);
        return new Prepared(plan, List.copyOf(choices), choices.stream().anyMatch(Choice::slanted));
    }

    private static void expand(List<List<Option>> groups, int depth, List<ConvexPart> parts,
            boolean slanted, List<Choice> output) {
        if (output.size() >= 4096) { throw new IllegalArgumentException("Too many vanilla shape combinations"); }
        if (depth == groups.size()) {
            output.add(new Choice(CompoundShape.ofObb(parts), slanted));
            return;
        }
        for (Option option : groups.get(depth)) {
            List<ConvexPart> combined = new ArrayList<>(parts);
            combined.addAll(option.parts);
            expand(groups, depth + 1, combined, slanted || option.slanted, output);
        }
    }

    private static List<Option> options(JsonElement raw, JsonObject modelData, Map<VariantKey, List<ConvexPart>> cache) {
        JsonArray variants = raw.isJsonArray() ? raw.getAsJsonArray() : new JsonArray();
        if (!raw.isJsonArray()) { variants.add(raw); }
        List<Option> result = new ArrayList<>();
        int totalWeight = 0;
        for (JsonElement value : variants) {
            JsonObject variant = value.getAsJsonObject();
            VariantKey key = new VariantKey(BlockShapePlatform.canonicalId(variant.get("model").getAsString()),
                    variant.has("x") ? variant.get("x").getAsInt() : 0,
                    variant.has("y") ? variant.get("y").getAsInt() : 0);
            List<ConvexPart> parts = cache.computeIfAbsent(key, ignored -> {
                List<ConvexPart> parsed = new ArrayList<>();
                JsonArray elements = modelData.getAsJsonArray(key.model);
                if (elements == null) { return List.of(); }
                for (JsonElement element : elements) {
                    parsed.add(ModelShapeCacheCore.parseElement(element.getAsJsonObject(), Direction.NORTH)
                            .transform(point -> rotateVariant(point, key.x, key.y)));
                }
                return List.copyOf(parsed);
            });
            int weight = variant.has("weight") ? variant.get("weight").getAsInt() : 1;
            if (weight < 1) { throw new IllegalArgumentException("Invalid model weight"); }
            totalWeight = Math.addExact(totalWeight, weight);
            result.add(new Option(parts, weight, parts.stream().anyMatch(part -> !part.isAxisAligned())));
        }
        return List.copyOf(result);
    }

    private static Vec3 rotateVariant(Vec3 point, int x, int y) {
        Vec3 p = point.subtract(0.5, 0.5, 0.5);
        for (int turn = 0; turn < Math.floorMod(x, 360) / 90; turn++) { p = new Vec3(p.x, p.z, -p.y); }
        for (int turn = 0; turn < Math.floorMod(y, 360) / 90; turn++) { p = new Vec3(-p.z, p.y, p.x); }
        return p.add(0.5, 0.5, 0.5);
    }

    private static boolean matchesVariant(BlockState state, String properties) {
        if (properties.isEmpty()) { return true; }
        for (String entry : properties.split(",")) {
            String[] pair = entry.split("=", 2);
            if (pair.length != 2 || !matchesValue(state, pair[0], pair[1])) { return false; }
        }
        return true;
    }

    private static boolean matches(BlockState state, JsonObject condition) {
        for (var entry : condition.entrySet()) {
            if (entry.getKey().equals("OR") || entry.getKey().equals("AND")) {
                boolean or = entry.getKey().equals("OR");
                boolean matched = !or;
                for (JsonElement child : entry.getValue().getAsJsonArray()) {
                    if (or) { matched |= matches(state, child.getAsJsonObject()); }
                    else { matched &= matches(state, child.getAsJsonObject()); }
                }
                if (!matched) { return false; }
            } else if (!matchesValue(state, entry.getKey(), entry.getValue().getAsString())) { return false; }
        }
        return true;
    }

    private static boolean matchesValue(BlockState state, String name, String expected) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
        if (property == null) { return false; }
        boolean negated = expected.startsWith("!");
        String accepted = negated ? expected.substring(1) : expected;
        String current = valueName(state, property);
        boolean matches = List.of(accepted.split("[|]")).contains(current);
        return negated != matches;
    }

    private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    private static String namespace(String id) {
        int separator = id.indexOf(':');
        return separator < 0 ? "minecraft" : id.substring(0, separator);
    }

    private static String path(String id) {
        int separator = id.indexOf(':');
        return separator < 0 ? id : id.substring(separator + 1);
    }

    private record VariantKey(String model, int x, int y) { }
    private record Option(List<ConvexPart> parts, int weight, boolean slanted) { }
    private record Plan(List<List<Option>> groups, boolean multipart) { }
    private record Choice(VoxelShape shape, boolean slanted) { }
    private record Prepared(Plan plan, List<Choice> choices, boolean slanted) { }
}
