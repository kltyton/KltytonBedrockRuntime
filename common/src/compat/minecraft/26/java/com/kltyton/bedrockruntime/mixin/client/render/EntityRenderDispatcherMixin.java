package com.kltyton.bedrockruntime.mixin.client.render;

import com.kltyton.bedrockruntime.client.selection.model.VisualPartEntityRenderState;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * CN: 在渲染状态创建完成后写入源实体 id，避免直接修改 EntityRenderer 并和 GeckoLib 争抢同一目标类。
 * EN: Stores the source entity id after render-state creation, avoiding a direct EntityRenderer mixin that races GeckoLib.
 */
@Mixin(targets = "net.minecraft.client.renderer.entity.EntityRenderDispatcher")
public abstract class EntityRenderDispatcherMixin {
    @Inject(method = "extractEntity", at = @At("RETURN"))
    private <E extends Entity> void kltyton_bedrock_runtime$storeEntityId(E entity, float partialTicks, CallbackInfoReturnable<EntityRenderState> callbackInfo) {
        EntityRenderState state = callbackInfo.getReturnValue();
        if (state instanceof VisualPartEntityRenderState visualPartState) {
            visualPartState.kltyton_bedrock_runtime$setEntityId(entity.getId());
        }
    }
}
