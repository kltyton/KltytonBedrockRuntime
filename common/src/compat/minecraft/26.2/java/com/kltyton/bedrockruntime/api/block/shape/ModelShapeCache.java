package com.kltyton.bedrockruntime.api.block.shape;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kltyton.bedrockruntime.api.block.shape.geometry.ConvexPart;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Caches immutable server-safe model OBBs, local occupied-cell pieces, and the original whole outline. */
public final class ModelShapeCache {
    static final int MAX_PART_OFFSET = ModelShapeCacheCore.MAX_PART_OFFSET;

    private ModelShapeCache() { }

    static ShapeSet get(Identifier modelId, Direction facing) {
        return ModelShapeCacheCore.get(modelId.toString(), facing);
    }

    /** Builds exact model geometry and its occupied cells without inserting mutable input into the cache. */
    public static ShapeSet buildForElements(JsonArray elements, Direction facing) {
        return ModelShapeCacheCore.buildForElements(elements, facing);
    }

    static ConvexPart parseElement(JsonObject element, Direction facing) {
        return ModelShapeCacheCore.parseElement(element, facing);
    }

    static JsonArray resolveElements(Identifier modelId) {
        return resolveElements(modelId, new JsonObject());
    }

    static JsonArray resolveElements(Identifier modelId, JsonObject packagedModels) {
        return ModelShapeCacheCore.resolveElements(modelId.toString(), packagedModels);
    }

    public record Cell(int x, int y, int z) {
        static final Cell ORIGIN = new Cell(0, 0, 0);
    }

    public record ShapeSet(Map<Cell, VoxelShape> localShapes, List<Cell> occupiedCells, VoxelShape wholeShape) {
        public ShapeSet {
            localShapes = Map.copyOf(localShapes);
            occupiedCells = List.copyOf(occupiedCells);
        }

        public VoxelShape localShape(Cell cell) { return localShapes.getOrDefault(cell, Shapes.empty()); }
    }
}
