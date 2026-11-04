package com.kltyton.bedrockruntime.mixin.block;

import com.kltyton.bedrockruntime.api.block.shape.geometry.BlockShapeCollision;
import java.util.List;
import it.unimi.dsi.fastutil.floats.FloatArraySet;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import com.kltyton.bedrockruntime.api.block.shape.geometry.CompoundShape;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds continuous OBB resolution to the shared client/server movement path while retaining native move and step handling. */
@Mixin(Entity.class)
public abstract class EntityCollisionMixin {
    @Shadow
    private static float[] collectCandidateStepUpHeights(AABB box, List<VoxelShape> colliders, float maximum, float skipped) {
        throw new AssertionError();
    }

    @Redirect(method = "collide", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;collectCandidateStepUpHeights(Lnet/minecraft/world/phys/AABB;Ljava/util/List;FF)[F"))
    private float[] kltyton_bedrock_runtime$useActualStepSurface(AABB box, List<VoxelShape> colliders, float maximum, float skipped,
            Vec3 movement) {
        if (!BlockShapeCollision.containsCompound(colliders)) {
            return collectCandidateStepUpHeights(box, colliders, maximum, skipped);
        }
        List<VoxelShape> nativeShapes = colliders.stream().filter(shape -> !(shape instanceof CompoundShape)).toList();
        FloatArraySet heights = new FloatArraySet(collectCandidateStepUpHeights(box, nativeShapes, maximum, skipped));
        AABB destination = box.move(movement.x, 0, movement.z);
        for (VoxelShape shape : colliders) {
            if (shape instanceof CompoundShape compound) {
                float height = (float) (compound.supportHeight(destination) - box.minY);
                if (height >= 0 && height <= maximum && height != skipped) { heights.add(height); }
            }
        }
        float[] result = heights.toFloatArray();
        FloatArrays.unstableSort(result);
        return result;
    }

    @Inject(method = "collideWithShapes", at = @At("HEAD"), cancellable = true)
    private static void kltyton_bedrock_runtime$collideWithModelGeometry(Vec3 movement, AABB box, List<VoxelShape> shapes,
            CallbackInfoReturnable<Vec3> callback) {
        if (BlockShapeCollision.containsCompound(shapes)) {
            callback.setReturnValue(BlockShapeCollision.collide(movement, box, shapes));
        }
    }

    @Inject(method = "collectCollidersIgnoringWorldBorder(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/Level;Ljava/util/List;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true)
    private static void kltyton_bedrock_runtime$coverDeflectedEntityMovement(@Nullable Entity source, Level level,
            List<VoxelShape> entityColliders, AABB box, CallbackInfoReturnable<List<VoxelShape>> callback) {
        CollisionContext context = source == null ? CollisionContext.empty() : CollisionContext.of(source);
        callback.setReturnValue(BlockShapeCollision.expandedCandidates(source, context, level, entityColliders,
                box, callback.getReturnValue(), true));
    }

    @Inject(method = "collectCollidersIgnoringWorldBorder(Lnet/minecraft/world/phys/shapes/CollisionContext;Lnet/minecraft/world/level/Level;Ljava/util/List;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true)
    private static void kltyton_bedrock_runtime$coverDeflectedContextMovement(CollisionContext context, Level level,
            List<VoxelShape> entityColliders, AABB box, CallbackInfoReturnable<List<VoxelShape>> callback) {
        Entity entity = context instanceof EntityCollisionContext entityContext ? entityContext.getEntity() : null;
        callback.setReturnValue(BlockShapeCollision.expandedCandidates(entity, context, level, entityColliders,
                box, callback.getReturnValue(), false));
    }
}
