package com.kltyton.bedrockruntime.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public interface HeldItemPoseCapture {
    void kltyton_bedrock_runtime$captureHeldItems(LivingEntityRenderState state, PoseStack pose);
}
