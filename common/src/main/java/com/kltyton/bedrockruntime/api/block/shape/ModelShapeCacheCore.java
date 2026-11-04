package com.kltyton.bedrockruntime.api.block.shape;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kltyton.bedrockruntime.api.block.shape.ModelShapeCache.Cell;
import com.kltyton.bedrockruntime.api.block.shape.ModelShapeCache.ShapeSet;
import com.kltyton.bedrockruntime.api.block.shape.geometry.CompoundShape;
import com.kltyton.bedrockruntime.api.block.shape.geometry.ConvexPart;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;

/** Version-neutral cache and model parsing algorithm; the per-version facade supplies native ids and records. */
final class ModelShapeCacheCore {
    static final int MAX_PART_OFFSET = 4;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_PARENT_DEPTH = 16;
    private static final int MAX_ELEMENTS = 4096;
    private static final double MIN_ELEMENT_THICKNESS = 0.1;
    private static final double EPSILON = 1.0E-9;
    private static final AABB MODEL_LIMITS = new AABB(-4, -4, -4, 5, 5, 5);
    private static final Map<ShapeKey, ShapeSet> CACHE = new ConcurrentHashMap<>();

    private ModelShapeCacheCore() { }

    static ShapeSet get(String modelId, Direction facing) {
        Direction horizontalFacing = facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
        return CACHE.computeIfAbsent(new ShapeKey(modelId, horizontalFacing), ModelShapeCacheCore::load);
    }

    static ShapeSet buildForElements(JsonArray elements, Direction facing) {
        if (elements.isEmpty() || elements.size() > MAX_ELEMENTS) {
            throw new IllegalArgumentException("Automatic block model must contain 1.." + MAX_ELEMENTS + " elements");
        }
        List<ConvexPart> wholeParts = new ArrayList<>();
        Map<Cell, List<ConvexPart>> partsByCell = new HashMap<>();
        for (JsonElement element : elements) {
            ConvexPart part = parseElement(element.getAsJsonObject(), facing).intersect(MODEL_LIMITS);
            if (part == null) { continue; }
            wholeParts.add(part);
            AABB bounds = part.bounds();
            for (int x = cellIndex(bounds.minX, false); x <= cellIndex(bounds.maxX, true); x++) {
                for (int y = cellIndex(bounds.minY, false); y <= cellIndex(bounds.maxY, true); y++) {
                    for (int z = cellIndex(bounds.minZ, false); z <= cellIndex(bounds.maxZ, true); z++) {
                        ConvexPart local = part.intersect(new AABB(x, y, z, x + 1, y + 1, z + 1));
                        if (local != null) {
                            partsByCell.computeIfAbsent(new Cell(x, y, z), ignored -> new ArrayList<>())
                                    .add(local.move(new Vec3(-x, -y, -z)));
                        }
                    }
                }
            }
        }
        if (partsByCell.isEmpty()) { throw new IllegalArgumentException("Model has no geometry inside the supported occupied cells"); }
        Map<Cell, VoxelShape> localShapes = new HashMap<>();
        partsByCell.forEach((cell, parts) -> localShapes.put(cell, CompoundShape.of(parts)));
        List<Cell> occupiedCells = new ArrayList<>(localShapes.keySet());
        if (!localShapes.containsKey(Cell.ORIGIN)) { occupiedCells.add(Cell.ORIGIN); }
        occupiedCells.sort(Comparator.comparingInt(Cell::y).thenComparingInt(Cell::x).thenComparingInt(Cell::z));
        return new ShapeSet(Map.copyOf(localShapes), List.copyOf(occupiedCells), CompoundShape.of(wholeParts));
    }

    private static ShapeSet load(ShapeKey key) {
        try {
            JsonArray elements = resolveElements(key.modelId(), new JsonObject());
            if (elements == null) {
                LOGGER.warn("Automatic block shape could not read elements for model {}; using one full origin cell", key.modelId());
                return fallback();
            }
            return buildForElements(elements, key.facing());
        } catch (RuntimeException exception) {
            LOGGER.error("Automatic block shape failed for model {} facing {}; using one full origin cell",
                    key.modelId(), key.facing(), exception);
            return fallback();
        }
    }

    static ConvexPart parseElement(JsonObject element, Direction facing) {
        Vec3 from = vector(element.getAsJsonArray("from"));
        Vec3 to = vector(element.getAsJsonArray("to"));
        double[] x = ensureThickness(Math.min(from.x, to.x), Math.max(from.x, to.x));
        double[] y = ensureThickness(Math.min(from.y, to.y), Math.max(from.y, to.y));
        double[] z = ensureThickness(Math.min(from.z, to.z), Math.max(from.z, to.z));
        AABB box = new AABB(x[0], y[0], z[0], x[1], y[1], z[1]);
        Rotation rotation = parseRotation(element.getAsJsonObject("rotation"));
        int turns = switch (facing) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
        return ConvexPart.transformedBox(box, point -> {
            Vec3 rotated = rotation.apply(point);
            for (int i = 0; i < turns; i++) { rotated = new Vec3(16 - rotated.z, rotated.y, rotated.x); }
            return rotated.scale(1.0 / 16.0);
        });
    }

    private static Rotation parseRotation(JsonObject json) {
        if (json == null) { return new Rotation(Vec3.ZERO, 0, 0, 0, new Vec3(1, 1, 1)); }
        Vec3 origin = vector(json.getAsJsonArray("origin"));
        double x = angle(json, "x"), y = angle(json, "y"), z = angle(json, "z");
        if (json.has("axis") && json.has("angle")) {
            double angle = Math.toRadians(json.get("angle").getAsDouble());
            switch (json.get("axis").getAsString()) {
                case "x" -> x = angle;
                case "y" -> y = angle;
                case "z" -> z = angle;
                default -> throw new IllegalArgumentException("Unknown model rotation axis");
            }
        }
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Model rotation must be finite");
        }
        Vec3 scale = new Vec3(1, 1, 1);
        if (json.has("rescale") && json.get("rescale").getAsBoolean()) {
            // Matches 26.2 CuboidRotation: scale each local axis by its rotated largest component.
            scale = new Vec3(rescale(rotate(new Vec3(1, 0, 0), x, y, z)),
                    rescale(rotate(new Vec3(0, 1, 0), x, y, z)), rescale(rotate(new Vec3(0, 0, 1), x, y, z)));
        }
        return new Rotation(origin, x, y, z, scale);
    }

    private static double rescale(Vec3 axis) {
        return 1.0 / Math.max(Math.abs(axis.x), Math.max(Math.abs(axis.y), Math.abs(axis.z)));
    }

    private static Vec3 rotate(Vec3 point, double x, double y, double z) {
        double sx = Math.sin(x), cx = Math.cos(x), sy = Math.sin(y), cy = Math.cos(y), sz = Math.sin(z), cz = Math.cos(z);
        double px = point.x, py = point.y * cx - point.z * sx, pz = point.y * sx + point.z * cx;
        double rx = px * cy + pz * sy, rz = -px * sy + pz * cy;
        return new Vec3(rx * cz - py * sz, rx * sz + py * cz, rz);
    }

    private static double angle(JsonObject json, String key) {
        return json.has(key) ? Math.toRadians(json.get(key).getAsDouble()) : 0;
    }

    private static Vec3 vector(JsonArray values) {
        if (values == null || values.size() != 3) { throw new IllegalArgumentException("Model vector must contain three coordinates"); }
        Vec3 result = new Vec3(values.get(0).getAsDouble(), values.get(1).getAsDouble(), values.get(2).getAsDouble());
        if (!Double.isFinite(result.x) || !Double.isFinite(result.y) || !Double.isFinite(result.z)) {
            throw new IllegalArgumentException("Model coordinates must be finite");
        }
        return result;
    }

    private static double[] ensureThickness(double minimum, double maximum) {
        if (maximum - minimum > EPSILON) { return new double[]{minimum, maximum}; }
        return new double[]{minimum - MIN_ELEMENT_THICKNESS * 0.5, maximum + MIN_ELEMENT_THICKNESS * 0.5};
    }

    static JsonArray resolveElements(String modelId, JsonObject packagedModels) {
        String current = modelId;
        for (int depth = 0; depth < MAX_PARENT_DEPTH; depth++) {
            JsonElement packaged = packagedModels.get(current);
            if (packaged != null) { return packaged.getAsJsonArray(); }
            JsonObject model = readJson(current);
            if (model == null) { return null; }
            JsonArray elements = model.getAsJsonArray("elements");
            if (elements != null) { return elements; }
            JsonElement parent = model.get("parent");
            if (parent == null || !parent.isJsonPrimitive()) { return null; }
            current = BlockShapePlatform.canonicalId(parent.getAsString());
        }
        return null;
    }

    private static JsonObject readJson(String modelId) {
        String namespace = namespace(modelId);
        String path = path(modelId);
        String resource = "assets/%s/models/%s.json".formatted(namespace, path);
        try (InputStream stream = ModelShapeCacheCore.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) { return null; }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception exception) {
            LOGGER.warn("Failed to read automatic block shape model {}", modelId, exception);
            return null;
        }
    }

    private static String namespace(String modelId) {
        int separator = modelId.indexOf(':');
        return separator < 0 ? "minecraft" : modelId.substring(0, separator);
    }

    private static String path(String modelId) {
        int separator = modelId.indexOf(':');
        return separator < 0 ? modelId : modelId.substring(separator + 1);
    }

    private static ShapeSet fallback() {
        return new ShapeSet(Map.of(Cell.ORIGIN, Shapes.block()), List.of(Cell.ORIGIN), Shapes.block());
    }

    private static int cellIndex(double coordinate, boolean maximum) {
        return (int) Math.floor(coordinate + (maximum ? -EPSILON : EPSILON));
    }

    private record ShapeKey(String modelId, Direction facing) { }

    private record Rotation(Vec3 origin, double x, double y, double z, Vec3 scale) {
        Vec3 apply(Vec3 point) { return rotate(point.subtract(origin).multiply(scale), x, y, z).add(origin); }
    }
}
