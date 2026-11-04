package com.kltyton.bedrockruntime.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.LivingEntity;

public interface HeldItemPoseCapture {
    void kltyton_bedrock_runtime$captureHeldItems(LivingEntity entity, float partialTick, PoseStack pose);
}
