package com.kltyton.bedrockruntime.mixin.client.render;

import java.util.List;
import com.kltyton.bedrockruntime.client.render.item.HeldItemPoseCapture;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Reuses the renderer's model and arm transforms for the first-person player's world-space held items. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityHeldItemMixin implements HeldItemPoseCapture {
    @Shadow protected EntityModel<LivingEntityRenderState> model;
    @Shadow @Final protected List<RenderLayer<LivingEntityRenderState, EntityModel<LivingEntityRenderState>>> layers;
    @Shadow protected abstract void setupRotations(LivingEntityRenderState state, PoseStack pose, float bodyRot, float scale);
    @Shadow protected abstract void scale(LivingEntityRenderState state, PoseStack pose);

    @Override
    public void kltyton_bedrock_runtime$captureHeldItems(LivingEntityRenderState state, PoseStack pose) {
        pose.pushPose();
        try {
            if (state.hasPose(Pose.SLEEPING)) {
                Direction direction = state.bedOrientation;
                if (direction != null) {
                    float offset = state.eyeHeight - 0.1F;
                    pose.translate(-direction.getStepX() * offset, 0, -direction.getStepZ() * offset);
                }
            }
            pose.scale(state.scale, state.scale, state.scale);
            setupRotations(state, pose, state.bodyRot, state.scale);
            pose.scale(-1, -1, 1);
            scale(state, pose);
            pose.translate(0, -1.501F, 0);
            model.setupAnim(state);
            SubmitNodeStorage discarded = new SubmitNodeStorage();
            for (RenderLayer<LivingEntityRenderState, EntityModel<LivingEntityRenderState>> layer : layers) {
                if (layer instanceof ItemInHandLayer<?, ?>) {
                    layer.submit(pose, discarded, state.lightCoords, state, state.yRot, state.xRot);
                }
            }
        } finally { pose.popPose(); }
    }
}
