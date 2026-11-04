package com.kltyton.bedrockruntime.mixin.client.particle;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the GUI render-state queue for the editor's picture-in-picture preview. */
@Mixin(GuiGraphicsExtractor.class)
public interface GuiGraphicsExtractorAccessor {
    @Accessor("guiRenderState")
    GuiRenderState kltyton_bedrock_runtime$getGuiRenderState();
}
