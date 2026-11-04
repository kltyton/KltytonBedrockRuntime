package com.kltyton.bedrockruntime.mixin.block;

import com.kltyton.bedrockruntime.api.block.shape.VanillaBlockShapes;
import com.kltyton.bedrockruntime.config.common.BoneHitboxConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateShapeMixin {
    @Inject(method = {
            "getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;"},
            at = @At("RETURN"), cancellable = true)
    private void kltyton_bedrock_runtime$modelShape(BlockGetter level, BlockPos pos, CollisionContext context,
            CallbackInfoReturnable<VoxelShape> callback) {
        replace(level, pos, callback);
    }

    @Inject(method = {
            "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            "getInteractionShape",
            "getBlockSupportShape"},
            at = @At("RETURN"), cancellable = true)
    private void kltyton_bedrock_runtime$cachedModelShape(BlockGetter level, BlockPos pos, CallbackInfoReturnable<VoxelShape> callback) {
        replace(level, pos, callback);
    }

    private void replace(BlockGetter level, BlockPos pos, CallbackInfoReturnable<VoxelShape> callback) {
        VoxelShape original = callback.getReturnValue();
        VoxelShape shape = VanillaBlockShapes.replacement((BlockState) (Object) this, level, pos, original);
        if (shape != original) { callback.setReturnValue(shape); }
    }

    @Inject(method = "hasLargeCollisionShape", at = @At("RETURN"), cancellable = true)
    private void kltyton_bedrock_runtime$expandedModel(CallbackInfoReturnable<Boolean> callback) {
        if (VanillaBlockShapes.contains((BlockState) (Object) this)) { callback.setReturnValue(true); }
    }

    @Inject(method = "isCollisionShapeFullBlock", at = @At("RETURN"), cancellable = true)
    private void kltyton_bedrock_runtime$exactFullBlock(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        BlockState state = (BlockState) (Object) this;
        if (VanillaBlockShapes.contains(state) || BoneHitboxConfig.forceAllBlockObb()
                || !BoneHitboxConfig.allowsBlockModel(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()))) {
            callback.setReturnValue(net.minecraft.world.level.block.Block.isShapeFullBlock(
                    state.getCollisionShape(level, pos)));
        }
    }
}
