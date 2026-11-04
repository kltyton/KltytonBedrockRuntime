package com.kltyton.bedrockruntime.mixin.block;

import com.kltyton.bedrockruntime.api.block.shape.geometry.ShapeOperations;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps native overlap/support/placement queries from treating an OBB's broad-phase proxy as solid. */
@Mixin(Shapes.class)
public abstract class ShapesMixin {
    @Inject(method = "joinIsNotEmpty(Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/phys/shapes/BooleanOp;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void kltyton_bedrock_runtime$exactIntersection(VoxelShape first, VoxelShape second, BooleanOp op,
            CallbackInfoReturnable<Boolean> callback) {
        if (ShapeOperations.hasCompound(first, second)) {
            callback.setReturnValue(ShapeOperations.joinIsNotEmpty(first, second, op));
        }
    }

    @Inject(method = "joinUnoptimized", at = @At("HEAD"), cancellable = true)
    private static void kltyton_bedrock_runtime$preserveCompoundGeometry(VoxelShape first, VoxelShape second, BooleanOp op,
            CallbackInfoReturnable<VoxelShape> callback) {
        if (ShapeOperations.hasCompound(first, second) && ShapeOperations.isFinite(first) && ShapeOperations.isFinite(second)) {
            callback.setReturnValue(ShapeOperations.join(first, second, op));
        }
    }
}
