package com.kltyton.bedrockruntime.mixin.client.render;

import com.kltyton.bedrockruntime.api.block.shape.AutoWholeShapeBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.phys.shapes.VoxelShape;

@Mixin(LevelRenderer.class)
public abstract class BlockOutlineMixin {
    @Shadow
    private static void renderShape(PoseStack pose, VertexConsumer output, VoxelShape shape,
            double x, double y, double z, float red, float green, float blue, float alpha) { throw new AssertionError(); }

    @Inject(method = "renderHitOutline", at = @At("HEAD"), cancellable = true)
    private void kltyton_bedrock_runtime$wholeOutline(PoseStack pose, VertexConsumer output, Entity entity,
            double cameraX, double cameraY, double cameraZ, BlockPos pos, BlockState state, CallbackInfo callback) {
        var level = Minecraft.getInstance().level;
        if (level != null && state.getBlock() instanceof AutoWholeShapeBlock block && block.isValidPart(level, state, pos)) {
            BlockPos origin = block.getOriginPosition(state, pos);
            renderShape(pose, output, block.getWholeOutlineShape(level, state, pos),
                    origin.getX() - cameraX, origin.getY() - cameraY, origin.getZ() - cameraZ, 0, 0, 0, 0.4F);
            callback.cancel();
        }
    }
}
