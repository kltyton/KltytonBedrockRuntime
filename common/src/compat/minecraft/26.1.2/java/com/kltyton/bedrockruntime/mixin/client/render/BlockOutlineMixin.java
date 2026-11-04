package com.kltyton.bedrockruntime.mixin.client.render;

import com.kltyton.bedrockruntime.api.block.shape.AutoWholeShapeBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Keeps cell-local selection while drawing the complete authored block outline. */
@Mixin(LevelRenderer.class)
public abstract class BlockOutlineMixin {
    @ModifyVariable(method = "renderHitOutline", at = @At("HEAD"), argsOnly = true)
    private BlockOutlineRenderState kltyton_bedrock_runtime$wholeOutline(BlockOutlineRenderState outline) {
        var level = Minecraft.getInstance().level;
        if (level == null) { return outline; }
        var state = level.getBlockState(outline.pos());
        if (!(state.getBlock() instanceof AutoWholeShapeBlock block)
                || !block.isValidPart(level, state, outline.pos())) { return outline; }
        var origin = block.getOriginPosition(state, outline.pos());
        return new BlockOutlineRenderState(outline.pos(), outline.isTranslucent(), outline.highContrast(),
                block.getWholeOutlineShape(level, state, outline.pos()).move(
                        origin.getX() - outline.pos().getX(), origin.getY() - outline.pos().getY(),
                        origin.getZ() - outline.pos().getZ()),
                outline.collisionShape(), outline.occlusionShape(), outline.interactionShape());
    }
}
