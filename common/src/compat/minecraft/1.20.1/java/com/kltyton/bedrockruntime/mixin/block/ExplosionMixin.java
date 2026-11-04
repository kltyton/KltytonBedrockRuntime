package com.kltyton.bedrockruntime.mixin.block;

import com.kltyton.bedrockruntime.api.block.shape.AutoPartShapeBlock;
import java.util.LinkedHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;

@Mixin(Explosion.class)
public abstract class ExplosionMixin {
    @Shadow @Final private Level level;

    @WrapOperation(method = "finalizeExplosion", at = @At(value = "INVOKE", ordinal = 0, remap = false,
            target = "Lit/unimi/dsi/fastutil/objects/ObjectArrayList;iterator()Lit/unimi/dsi/fastutil/objects/ObjectListIterator;"))
    private ObjectListIterator<BlockPos> kltyton_bedrock_runtime$explodeOrigins(ObjectArrayList<BlockPos> positions,
            Operation<ObjectListIterator<BlockPos>> original) {
        LinkedHashSet<BlockPos> origins = new LinkedHashSet<>();
        boolean changed = false;
        for (BlockPos pos : positions) {
            var state = level.getBlockState(pos);
            if (state.getBlock() instanceof AutoPartShapeBlock block && block.isValidPart(level, state, pos)) {
                origins.add(block.getOriginPosition(state, pos).immutable());
                changed = true;
            } else { origins.add(pos); }
        }
        return changed ? new ObjectArrayList<>(origins).iterator() : original.call(positions);
    }
}
