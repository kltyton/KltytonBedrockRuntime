package com.kltyton.bedrockruntime.api.block.shape;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kltyton.bedrockruntime.api.block.shape.geometry.ConvexPart;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Caches immutable server-safe model OBBs, local occupied-cell pieces, and the original whole outline. */
final class ModelShapeCache {
    static final int MAX_PART_OFFSET = ModelShapeCacheCore.MAX_PART_OFFSET;

    private ModelShapeCache() { }

    static ShapeSet get(ResourceLocation modelId, Direction facing) {
        return ModelShapeCacheCore.get(modelId.toString(), facing);
    }

    static ShapeSet buildForElements(JsonArray elements, Direction facing) {
        return ModelShapeCacheCore.buildForElements(elements, facing);
    }

    static ConvexPart parseElement(JsonObject element, Direction facing) {
        return ModelShapeCacheCore.parseElement(element, facing);
    }

    static JsonArray resolveElements(ResourceLocation modelId) {
        return resolveElements(modelId, new JsonObject());
    }

    static JsonArray resolveElements(ResourceLocation modelId, JsonObject packagedModels) {
        return ModelShapeCacheCore.resolveElements(modelId.toString(), packagedModels);
    }

    record Cell(int x, int y, int z) {
        static final Cell ORIGIN = new Cell(0, 0, 0);
    }

    record ShapeSet(Map<Cell, VoxelShape> localShapes, List<Cell> occupiedCells, VoxelShape wholeShape) {
        VoxelShape localShape(Cell cell) { return localShapes.getOrDefault(cell, Shapes.empty()); }
    }
}
