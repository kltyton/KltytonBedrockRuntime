package com.kltyton.bedrockruntime.mixin.block;

import com.kltyton.bedrockruntime.api.block.shape.geometry.BlockShapeCollision;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityCollisionMixin {
    @WrapOperation(method = "collideBoundingBox", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;collideWithShapes(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 kltyton_bedrock_runtime$coverDeflection(Vec3 movement, AABB box, List<VoxelShape> shapes,
            Operation<Vec3> original, @Local(argsOnly = true) Entity source, @Local(argsOnly = true) Level level,
            @Local(argsOnly = true) List<VoxelShape> entityColliders) {
        CollisionContext context = source == null ? CollisionContext.empty() : CollisionContext.of(source);
        return original.call(movement, box, BlockShapeCollision.expandedCandidates(source, context, level,
                entityColliders, box.expandTowards(movement), shapes, true));
    }

    @Inject(method = "collideWithShapes", at = @At("HEAD"), cancellable = true)
    private static void kltyton_bedrock_runtime$collideWithModelGeometry(Vec3 movement, AABB box, List<VoxelShape> shapes,
            CallbackInfoReturnable<Vec3> callback) {
        if (BlockShapeCollision.containsCompound(shapes)) {
            callback.setReturnValue(BlockShapeCollision.collide(movement, box, shapes));
        }
    }
}
