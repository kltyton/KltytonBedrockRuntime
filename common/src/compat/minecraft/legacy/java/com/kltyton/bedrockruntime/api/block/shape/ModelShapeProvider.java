package com.kltyton.bedrockruntime.api.block.shape;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import com.kltyton.bedrockruntime.api.block.shape.geometry.ShapeOperations;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Exposes model-derived shapes for blocks whose collision follows authored model geometry.
 */
public final class ModelShapeProvider {
    private static final Map<List<ResourceLocation>, VoxelShape> CACHE = new ConcurrentHashMap<>();

    private ModelShapeProvider() {
    }

    public static VoxelShape union(List<ResourceLocation> modelIds, BlockShapeMode mode) {
        return mode.apply(union(modelIds));
    }

    public static VoxelShape union(List<ResourceLocation> modelIds) {
        List<ResourceLocation> key = List.copyOf(modelIds);
        return CACHE.computeIfAbsent(key, ModelShapeProvider::loadUnion);
    }

    public static VoxelShape shape(ResourceLocation modelId, Direction facing, BlockShapeMode mode) {
        return mode.apply(ModelShapeCache.get(modelId, facing).wholeShape());
    }

    public static VoxelShape shape(ResourceLocation modelId, BlockShapeMode mode) {
        return shape(modelId, Direction.NORTH, mode);
    }

    /** Call during registration to prepare packaged model geometry outside render and collision loops. */
    public static void prepare(ResourceLocation modelId) {
        for (Direction facing : Direction.Plane.HORIZONTAL) { ModelShapeCache.get(modelId, facing); }
    }

    public static VoxelShape shape(ResourceLocation modelId) {
        return union(List.of(modelId));
    }

    public static VoxelShape selectEqualWeight(List<VoxelShape> shapes, long seed) {
        if (shapes.isEmpty()) {
            throw new IllegalArgumentException("Shape list must not be empty");
        }
        return shapes.get(RandomSource.create(seed).nextInt(shapes.size()));
    }

    private static VoxelShape loadUnion(List<ResourceLocation> modelIds) {
        VoxelShape shape = Shapes.empty();
        for (ResourceLocation modelId : modelIds) {
            shape = ShapeOperations.union(shape, ModelShapeCache.get(modelId, Direction.NORTH).wholeShape());
        }
        return shape.isEmpty() ? Shapes.block() : shape.optimize();
    }
}
