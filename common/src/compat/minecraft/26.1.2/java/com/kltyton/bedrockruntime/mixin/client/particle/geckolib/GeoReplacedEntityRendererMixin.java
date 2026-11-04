package com.kltyton.bedrockruntime.mixin.client.particle.geckolib;

import com.geckolib.animatable.GeoAnimatable;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import com.kltyton.bedrockruntime.client.particle.api.geckolib.GeckoLibHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "com.geckolib.renderer.GeoReplacedEntityRenderer", remap = false)
public abstract class GeoReplacedEntityRendererMixin<T extends GeoAnimatable, E extends Entity, R extends EntityRenderState> {
    @Shadow
    @Final
    protected T animatable;

    @WrapMethod(method = "extractRenderState")
    private void kltyton_bedrock_runtime$withCurrentEntity(E entity, R renderState, float partialTick, Operation<Void> original) {
        Entity previous = GeckoLibHelper.animationEntity(animatable);
        GeckoLibHelper.setCurrentEntity(animatable, entity);
        try {
            original.call(entity, renderState, partialTick);
        } finally {
            GeckoLibHelper.setCurrentEntity(animatable, previous);
        }
    }
}
