package com.kltyton.bedrockruntime.mixin.client.render;

import com.kltyton.bedrockruntime.client.selection.model.VisualPartEntityRenderState;
import com.kltyton.bedrockruntime.client.render.capture.EntityCapturePose;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * CN: 在渲染状态提取完成后保留源实体 id 和插值根姿势，不改变原提取回调。
 * EN: Retains source identity and interpolated root pose after extraction without changing the original callbacks.
 */
@Mixin(targets = "net.minecraft.client.renderer.entity.EntityRenderDispatcher")
public abstract class EntityRenderDispatcherMixin {
    @Inject(method = "extractEntity", at = @At("RETURN"))
    private <E extends Entity> void kltyton_bedrock_runtime$storeEntityId(E entity, float partialTicks, CallbackInfoReturnable<EntityRenderState> callbackInfo) {
        EntityRenderState state = callbackInfo.getReturnValue();
        if (state instanceof VisualPartEntityRenderState visualPartState) {
            visualPartState.kltyton_bedrock_runtime$setEntityId(entity.getId());
            visualPartState.kltyton_bedrock_runtime$setCapturePose(
                    new EntityCapturePose(new Vec3(state.x, state.y, state.z),
                            Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot())));
        }
    }
}
