package com.kltyton.bedrockruntime.mixin.client.particle;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import com.kltyton.bedrockruntime.client.particle.access.IEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Samples the actor's cumulative animation clock before extracting its render state. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void kltyton_bedrock_runtime$sampleAnimationLifetime(Entity entity, EntityRenderState state,
            float partialTick, CallbackInfo ci) {
        IEntity.of(entity).kltyton_bedrock_runtime$sampleAnimationLifetime(partialTick);
    }
}
