package com.kltyton.bedrockruntime.mixin.client.render;

import com.kltyton.bedrockruntime.api.block.shape.AutoWholeShapeBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replace the extracted outline only; ray selection still uses each occupied cell's local shape. */
@Mixin(LevelRenderer.class)
public abstract class BlockOutlineMixin {
    @Inject(method = "submitBlockOutline", at = @At("HEAD"))
    private void kltyton_bedrock_runtime$wholeOutline(PoseStack pose, SubmitNodeCollector output, LevelRenderState renderState,
            CallbackInfo callback) {
        BlockOutlineRenderState outline = renderState.blockOutlineRenderState;
        var level = Minecraft.getInstance().level;
        if (outline == null || level == null) { return; }
        var state = level.getBlockState(outline.pos());
        if (state.getBlock() instanceof AutoWholeShapeBlock block && block.isValidPart(level, state, outline.pos())) {
            var origin = block.getOriginPosition(state, outline.pos()).immutable();
            renderState.blockOutlineRenderState = new BlockOutlineRenderState(outline.pos(), outline.isTranslucent(),
                    outline.highContrast(), block.getWholeOutlineShape(level, state, outline.pos()).move(
                            origin.getX() - outline.pos().getX(), origin.getY() - outline.pos().getY(),
                            origin.getZ() - outline.pos().getZ()),
                    outline.collisionShape(), outline.occlusionShape(), outline.interactionShape());
        }
    }
}
