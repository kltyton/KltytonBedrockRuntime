package com.kltyton.bedrockruntime.api.block.shape;

import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Automatic model-derived multi-cell block whose selection outline is local to
 * the exact cell targeted by the player.
 *
 * <p>Extend this class, pass the registered block id to the constructor, and
 * implement the normal Minecraft codec for the concrete block. The packaged
 * {@code assets/<namespace>/models/block/<path>.json} model supplies placement
 * footprint, collision, interaction and selection shapes.</p>
 */
public abstract class AutoPartShapeBlock extends HorizontalDirectionalBlock {
    public static final int MAX_PART_OFFSET = AutoPartShapeBlockSupport.MAX_PART_OFFSET;
    public static final IntegerProperty ORIGIN_X =
            IntegerProperty.create("origin_x", 0, MAX_PART_OFFSET * 2);
    public static final IntegerProperty ORIGIN_Y =
            IntegerProperty.create("origin_y", 0, MAX_PART_OFFSET * 2);
    public static final IntegerProperty ORIGIN_Z =
            IntegerProperty.create("origin_z", 0, MAX_PART_OFFSET * 2);

    private final ResourceLocation modelId;
    private final AutoPartShapeBlockSupport support;

    protected AutoPartShapeBlock(ResourceLocation blockId, Properties properties) {
        this(blockId, properties, BlockShapeMode.OBB);
    }

    protected AutoPartShapeBlock(ResourceLocation blockId, Properties properties, BlockShapeMode mode) {
        super(properties.dynamicShape().noOcclusion().pushReaction(PushReaction.BLOCK));
        ResourceLocation modelId = blockId.withPrefix("block/");
        this.modelId = modelId;
        this.support = new AutoPartShapeBlockSupport(this, ORIGIN_X, ORIGIN_Y, ORIGIN_Z, mode,
                facing -> ModelShapeCache.get(modelId, facing));
        registerDefaultState(support.withOrigin(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)));
    }

    @Override
    public final @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState originState = getOriginStateForPlacement(context);
        if (originState == null) {
            return null;
        }
        BlockState state = support.withOrigin(originState);
        return canPlaceAllParts(context, state) ? state : null;
    }

    /**
     * Origin-only placement-state hook.
     */
    protected @Nullable BlockState getOriginStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public final void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && support.isOriginPart(state)) {
            support.placeLinkedParts(level, pos, state);
            placedOrigin(level, pos, state, placer, stack);
        }
    }

    /**
     * Origin-only placement hook for concrete blocks.
     */
    protected void placedOrigin(
            Level level,
            BlockPos originPos,
            BlockState originState,
            @Nullable LivingEntity placer,
            ItemStack stack) {
    }

    @Override
    protected final VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return support.localShape(level, state, pos);
    }

    @Override
    protected final VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return support.localShape(level, state, pos);
    }

    @Override
    protected final VoxelShape getInteractionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos) {
        return support.localShape(level, state, pos);
    }

    @Override
    protected final RenderShape getRenderShape(BlockState state) {
        return support.isOriginPart(state) ? getOriginRenderShape(state) : RenderShape.INVISIBLE;
    }

    /**
     * Origin-only render-shape hook. Linked parts are always invisible.
     */
    protected RenderShape getOriginRenderShape(BlockState state) {
        return super.getRenderShape(state);
    }

    @Override
    protected final InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult) {
        AutoPartShapeBlockSupport.Origin origin = support.resolveOrigin(level, state, pos);
        if (origin == null) {
            return InteractionResult.PASS;
        }
        return useOriginWithoutItem(
                origin.state(),
                level,
                origin.pos(),
                player,
                AutoPartShapeBlockSupport.moveHit(hitResult, origin.pos()));
    }

    /**
     * Origin-only empty-hand interaction hook.
     */
    protected InteractionResult useOriginWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult) {
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    @Override
    protected final ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult) {
        AutoPartShapeBlockSupport.Origin origin = support.resolveOrigin(level, state, pos);
        if (origin == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return useOriginItemOn(
                stack,
                origin.state(),
                level,
                origin.pos(),
                player,
                hand,
                AutoPartShapeBlockSupport.moveHit(hitResult, origin.pos()));
    }

    /**
     * Origin-only held-item interaction hook.
     */
    protected ItemInteractionResult useOriginItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult) {
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected final @Nullable MenuProvider getMenuProvider(
            BlockState state,
            Level level,
            BlockPos pos) {
        AutoPartShapeBlockSupport.Origin origin = support.resolveOrigin(level, state, pos);
        return origin == null
                ? null
                : getOriginMenuProvider(origin.state(), level, origin.pos());
    }

    /**
     * Origin-only menu-provider hook, including spectator interaction.
     */
    protected @Nullable MenuProvider getOriginMenuProvider(
            BlockState state,
            Level level,
            BlockPos pos) {
        return super.getMenuProvider(state, level, pos);
    }

    @Override
    protected final boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (support.isOriginPart(state)) {
            return canOriginSurvive(state, level, pos);
        }
        return support.resolveOrigin(level, state, pos) != null;
    }

    /**
     * Origin-only survival hook.
     */
    protected boolean canOriginSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return super.canSurvive(state, level, pos);
    }

    @Override
    protected final BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState,
            LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (!support.isOriginPart(state)) {
            return support.updatePart(state, level, pos);
        }
        return updateOriginShape(state, direction, neighbourState, level, pos, neighbourPos);
    }

    protected BlockState updateOriginShape(BlockState state, Direction direction, BlockState neighbourState,
            LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return super.updateShape(state, direction, neighbourState, level, pos, neighbourPos);
    }

    @Override
    protected final void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server) {
            support.removeLinkedStructure(level, state, pos);
            removedPart(state, server, pos, movedByPiston);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    protected void removedPart(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) { }

    @Override
    public final BlockState playerWillDestroy(
            Level level,
            BlockPos pos,
            BlockState state,
            Player player) {
        if (level.isClientSide() || support.isOriginPart(state)) {
            return super.playerWillDestroy(level, pos, state, player);
        }

        AutoPartShapeBlockSupport.Origin origin = support.resolveOrigin(level, state, pos);
        if (origin == null) {
            return super.playerWillDestroy(level, pos, state, player);
        }

        BlockEntity blockEntity = level.getBlockEntity(origin.pos());
        ItemStack tool = player.getMainHandItem().copy();
        BlockState adjustedState =
                super.playerWillDestroy(level, origin.pos(), origin.state(), player);
        support.finishPlayerDestroy(level, origin, adjustedState, player, blockEntity, tool,
                !player.isCreative() && player.hasCorrectToolForDrops(adjustedState),
                (world, blockPos, blockState) -> destroy(world, blockPos, blockState),
                this::playerDestroyOrigin);
        return adjustedState;
    }

    @Override
    public final void playerDestroy(
            Level level,
            Player player,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity blockEntity,
            ItemStack tool) {
        if (support.isOriginPart(state)) {
            playerDestroyOrigin(level, player, pos, state, blockEntity, tool);
        }
    }

    /**
     * Origin-only player-destruction hook.
     */
    protected void playerDestroyOrigin(
            Level level,
            Player player,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity blockEntity,
            ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    @Override
    protected final void onExplosionHit(
            BlockState state,
            Level level,
            BlockPos pos,
            Explosion explosion,
            BiConsumer<ItemStack, BlockPos> onHit) {
        AutoPartShapeBlockSupport.Origin origin = support.resolveOrigin(level, state, pos);
        if (origin != null) {
            explodeOrigin(
                    origin.state(),
                    level,
                    origin.pos(),
                    explosion,
                    onHit);
        }
    }

    /**
     * Origin-only explosion hook.
     */
    protected void explodeOrigin(
            BlockState state,
            Level level,
            BlockPos pos,
            Explosion explosion,
            BiConsumer<ItemStack, BlockPos> onHit) {
        super.onExplosionHit(state, level, pos, explosion, onHit);
    }

    @Override
    protected final List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return support.isOriginPart(state)
                ? getOriginDrops(state, params)
                : support.getPartDrops(state, params, this::getOriginDrops);
    }

    /**
     * Origin-only loot hook.
     */
    protected List<ItemStack> getOriginDrops(BlockState state, LootParams.Builder params) {
        return super.getDrops(state, params);
    }

    @Override
    public final ItemStack getCloneItemStack(
            LevelReader level,
            BlockPos pos,
            BlockState state) {
        AutoPartShapeBlockSupport.Origin origin = support.resolveOrigin(level, state, pos);
        return origin == null
                ? ItemStack.EMPTY
                : getOriginCloneItemStack(
                        level,
                        origin.pos(),
                        origin.state());
    }

    /**
     * Origin-only clone-item hook.
     */
    protected ItemStack getOriginCloneItemStack(
            LevelReader level,
            BlockPos pos,
            BlockState state) {
        return super.getCloneItemStack(level, pos, state);
    }

    /**
     * Returns the canonical origin position encoded in a linked part state.
     */
    public final BlockPos getOriginPosition(BlockState state, BlockPos partPos) {
        return support.getOriginPosition(state, partPos);
    }

    /**
     * Returns whether the supplied state is the canonical origin part.
     */
    public final boolean isOriginPart(BlockState state) {
        return support.isOriginPart(state);
    }

    /**
     * Returns whether this part still points to a valid origin of this block.
     */
    public final boolean isValidPart(BlockGetter level, BlockState state, BlockPos pos) {
        return support.isValidPart(level, state, pos);
    }

    protected final VoxelShape wholeShape(
            BlockGetter level,
            BlockState state,
            BlockPos pos) {
        return support.wholeShape(level, state, pos);
    }

    public final BlockShapeMode shapeMode() { return support.shapeMode(); }

    protected final ResourceLocation modelId() {
        return modelId;
    }

    protected boolean canPlaceAllParts(BlockPlaceContext context, BlockState originState) {
        return support.canPlaceAllParts(context, originState);
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ORIGIN_X, ORIGIN_Y, ORIGIN_Z);
    }
}
