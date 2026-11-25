package com.kltyton.bedrockruntime.mixin.client.particle;

import com.kltyton.bedrockruntime.client.particle.definition.molang.RenderFrameContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.render.pip.GuiEntityRenderer;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

final class BedrockMolangUiEntityMixin {
    private BedrockMolangUiEntityMixin() {}

    /** Marks the entity state extraction used by the vanilla inventory preview. */
    @Mixin(InventoryScreen.class)
    abstract static class InventoryPreview {
        @WrapMethod(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;")
        private static EntityRenderState kbr$extractUiEntity(LivingEntity entity, Operation<EntityRenderState> original) {
            boolean enclosing = RenderFrameContext.enterUiEntity();
            try {
                return original.call(entity);
            } finally {
                RenderFrameContext.exitUiEntity(enclosing);
            }
        }
    }

    /** Marks submission when a preview's model and effects are evaluated in the GUI render pass. */
    @Mixin(GuiEntityRenderer.class)
    abstract static class GuiPreview {
        @WrapMethod(method = "renderToTexture(Lnet/minecraft/client/renderer/state/gui/pip/GuiEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V")
        private void kbr$submitUiEntity(GuiEntityRenderState state, PoseStack pose, SubmitNodeCollector nodes, Operation<Void> original) {
            boolean enclosing = RenderFrameContext.enterUiEntity();
            try {
                original.call(state, pose, nodes);
            } finally {
                RenderFrameContext.exitUiEntity(enclosing);
            }
        }
    }
}
