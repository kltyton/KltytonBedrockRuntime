package com.kltyton.bedrockruntime.api.block.shape;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Shared linkage, footprint, cache-selection and drop algorithm for automatic part blocks. */
final class AutoPartShapeBlockSupport {
    static final int MAX_PART_OFFSET = ModelShapeCacheCore.MAX_PART_OFFSET;
    private static final ThreadLocal<Boolean> REMOVING_STRUCTURE =
            ThreadLocal.withInitial(() -> false);

    private final Block owner;
    private final IntegerProperty originX;
    private final IntegerProperty originY;
    private final IntegerProperty originZ;
    private final BlockShapeMode shapeMode;
    private final Map<Direction, ModelShapeCache.ShapeSet> obbShapes = new EnumMap<>(Direction.class);
    private final Map<Direction, ModelShapeCache.ShapeSet> aabbShapes = new EnumMap<>(Direction.class);

    AutoPartShapeBlockSupport(
            Block owner,
            IntegerProperty originX,
            IntegerProperty originY,
            IntegerProperty originZ,
            BlockShapeMode shapeMode,
            Function<Direction, ModelShapeCache.ShapeSet> shapeSource) {
        this.owner = owner;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        this.shapeMode = Objects.requireNonNull(shapeMode);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            ModelShapeCache.ShapeSet original = shapeSource.apply(facing);
            obbShapes.put(facing, original);
            Map<ModelShapeCache.Cell, VoxelShape> local = new HashMap<>();
            original.localShapes().forEach((cell, shape) -> local.put(cell, BlockShapeMode.AABB.apply(shape)));
            VoxelShape whole = Shapes.empty();
            for (var entry : local.entrySet()) {
                var cell = entry.getKey();
                whole = Shapes.or(whole, entry.getValue().move(cell.x(), cell.y(), cell.z()));
            }
            aabbShapes.put(facing, new ModelShapeCache.ShapeSet(Map.copyOf(local),
                    original.occupiedCells(), whole.optimize()));
        }
    }

    BlockShapeMode shapeMode() {
        return shapeMode;
    }

    BlockState withOrigin(BlockState state) {
        return state
                .setValue(originX, encodeOffset(0))
                .setValue(originY, encodeOffset(0))
                .setValue(originZ, encodeOffset(0));
    }

    VoxelShape localShape(BlockGetter level, BlockState state, BlockPos pos) {
        Origin origin = resolveOrigin(level, state, pos);
        if (origin == null) {
            return Shapes.empty();
        }
        ModelShapeCache.Cell cell = new ModelShapeCache.Cell(
                pos.getX() - origin.pos().getX(),
                pos.getY() - origin.pos().getY(),
                pos.getZ() - origin.pos().getZ());
        return shapes(origin.state()).localShape(cell);
    }

    VoxelShape wholeShape(BlockGetter level, BlockState state, BlockPos pos) {
        Origin origin = resolveOrigin(level, state, pos);
        return origin == null
                ? Shapes.empty()
                : shapes(origin.state()).wholeShape();
    }

    BlockState updatePart(BlockState state, BlockGetter level, BlockPos pos) {
        return resolveOrigin(level, state, pos) == null ? Blocks.AIR.defaultBlockState() : state;
    }

    boolean canPlaceAllParts(BlockPlaceContext context, BlockState originState) {
        Level level = context.getLevel();
        BlockPos originPos = context.getClickedPos();
        ModelShapeCache.ShapeSet shapeSet = shapes(originState);
        for (ModelShapeCache.Cell cell : shapeSet.occupiedCells()) {
            BlockPos partPos = originPos.offset(cell.x(), cell.y(), cell.z());
            if (!level.hasChunkAt(partPos) || !level.isInWorldBounds(partPos)
                    || !level.getWorldBorder().isWithinBounds(partPos)
                    || !level.getBlockState(partPos).canBeReplaced(context)) {
                return false;
            }
            VoxelShape placedShape = shapeSet.localShape(cell).move(partPos.getX(), partPos.getY(), partPos.getZ());
            if (!level.isUnobstructed(null, placedShape)) {
                return false;
            }
        }
        return true;
    }

    void placeLinkedParts(Level level, BlockPos originPos, BlockState originState) {
        for (ModelShapeCache.Cell cell : shapes(originState).occupiedCells()) {
            if (cell.equals(ModelShapeCache.Cell.ORIGIN)) {
                continue;
            }
            BlockPos partPos = originPos.offset(cell.x(), cell.y(), cell.z());
            level.setBlock(partPos, partState(originState, cell), Block.UPDATE_ALL);
        }
    }

    void removeLinkedStructure(Level level, BlockState state, BlockPos pos) {
        if (REMOVING_STRUCTURE.get()) {
            return;
        }
        Origin origin = resolveOriginOrStored(level, state, pos);
        if (origin != null) {
            removeStructure(level, origin, !origin.pos().equals(pos));
        }
    }

    void finishPlayerDestroy(
            Level level,
            Origin origin,
            BlockState adjustedState,
            Player player,
            BlockEntity blockEntity,
            ItemStack tool,
            boolean dropItems,
            BlockDestroyAction destroy,
            PlayerDestroyAction playerDestroy) {
        removeStructure(level, origin, true);
        destroy.destroy(level, origin.pos(), adjustedState);
        if (dropItems) {
            playerDestroy.playerDestroy(level, player, origin.pos(), adjustedState, blockEntity, tool);
        }
    }

    List<ItemStack> getPartDrops(
            BlockState state,
            LootParams.Builder params,
            BiFunction<BlockState, LootParams.Builder, List<ItemStack>> originDrops) {
        Vec3 lootOrigin = params.getOptionalParameter(LootContextParams.ORIGIN);
        if (lootOrigin == null) {
            return List.of();
        }
        Origin origin = resolveOrigin(
                params.getLevel(),
                state,
                BlockPos.containing(lootOrigin));
        if (origin == null) {
            return List.of();
        }
        params.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(origin.pos()))
                .withOptionalParameter(
                        LootContextParams.BLOCK_ENTITY,
                        params.getLevel().getBlockEntity(origin.pos()));
        return originDrops.apply(origin.state(), params);
    }

    BlockPos getOriginPosition(BlockState state, BlockPos partPos) {
        return partPos.offset(
                decodeOffset(state.getValue(originX)),
                decodeOffset(state.getValue(originY)),
                decodeOffset(state.getValue(originZ)));
    }

    boolean isOriginPart(BlockState state) {
        return decodeOffset(state.getValue(originX)) == 0
                && decodeOffset(state.getValue(originY)) == 0
                && decodeOffset(state.getValue(originZ)) == 0;
    }

    boolean isValidPart(BlockGetter level, BlockState state, BlockPos pos) {
        return resolveOrigin(level, state, pos) != null;
    }

    Origin resolveOrigin(BlockGetter level, BlockState state, BlockPos pos) {
        if (!state.is(owner)) {
            return null;
        }
        BlockPos originPos = getOriginPosition(state, pos);
        BlockState originState = level.getBlockState(originPos);
        return originState.is(owner) && isOriginPart(originState)
                ? new Origin(originPos, originState)
                : null;
    }

    private Origin resolveOriginOrStored(BlockGetter level, BlockState state, BlockPos pos) {
        Origin origin = resolveOrigin(level, state, pos);
        if (origin != null || isOriginPart(state)) {
            return origin != null ? origin : new Origin(pos, state);
        }
        BlockPos originPos = getOriginPosition(state, pos);
        BlockState originState = level.getBlockState(originPos);
        return originState.is(owner) && isOriginPart(originState)
                ? new Origin(originPos, originState)
                : null;
    }

    private void removeStructure(Level level, Origin origin, boolean includeOrigin) {
        REMOVING_STRUCTURE.set(true);
        try {
            for (ModelShapeCache.Cell cell : shapes(origin.state()).occupiedCells()) {
                if (!includeOrigin && cell.equals(ModelShapeCache.Cell.ORIGIN)) {
                    continue;
                }
                BlockPos partPos =
                        origin.pos().offset(cell.x(), cell.y(), cell.z());
                BlockState partState = level.getBlockState(partPos);
                if (partState.is(owner)
                        && getOriginPosition(partState, partPos).equals(origin.pos())) {
                    level.setBlock(
                            partPos,
                            Blocks.AIR.defaultBlockState(),
                            Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
        } finally {
            REMOVING_STRUCTURE.remove();
        }
    }

    private ModelShapeCache.ShapeSet shapes(BlockState state) {
        boolean obb = BlockShapePlatform.useObb(owner, shapeMode);
        return (obb ? obbShapes : aabbShapes).get(state.getValue(HorizontalDirectionalBlock.FACING));
    }

    private BlockState partState(BlockState originState, ModelShapeCache.Cell cell) {
        return originState
                .setValue(originX, encodeOffset(-cell.x()))
                .setValue(originY, encodeOffset(-cell.y()))
                .setValue(originZ, encodeOffset(-cell.z()));
    }

    static BlockHitResult moveHit(BlockHitResult hitResult, BlockPos blockPos) {
        return new BlockHitResult(
                hitResult.getLocation(),
                hitResult.getDirection(),
                blockPos,
                hitResult.isInside());
    }

    static int encodeOffset(int offset) {
        if (offset < -MAX_PART_OFFSET || offset > MAX_PART_OFFSET) {
            throw new IllegalArgumentException(
                    "automatic block part offset %d is outside [%d, %d]"
                            .formatted(offset, -MAX_PART_OFFSET, MAX_PART_OFFSET));
        }
        return offset + MAX_PART_OFFSET;
    }

    static int decodeOffset(int stored) {
        return stored - MAX_PART_OFFSET;
    }

    record Origin(BlockPos pos, BlockState state) { }

    @FunctionalInterface
    interface BlockDestroyAction {
        void destroy(Level level, BlockPos pos, BlockState state);
    }

    @FunctionalInterface
    interface PlayerDestroyAction {
        void playerDestroy(
                Level level,
                Player player,
                BlockPos pos,
                BlockState state,
                BlockEntity blockEntity,
                ItemStack tool);
    }
}
