package com.kltyton.bedrockruntime.mixin.client.render;

import com.kltyton.bedrockruntime.client.render.item.HeldItemCapture;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class HeldItemOutlineMixin {
    @Inject(method = "submitEntities", at = @At("TAIL"))
    private void kltyton_bedrock_runtime$heldItemOutlines(PoseStack pose, LevelRenderState state, SubmitNodeCollector output,
            CallbackInfo callback) {
        HeldItemCapture.submitLocalPlayerOutlines(Minecraft.getInstance(), state.cameraRenderState.pos, output);
    }
}
