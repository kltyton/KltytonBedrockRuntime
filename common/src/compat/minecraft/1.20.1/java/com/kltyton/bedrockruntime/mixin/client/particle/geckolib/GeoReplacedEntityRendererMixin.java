package com.kltyton.bedrockruntime.mixin.client.particle.geckolib;

import net.minecraft.world.entity.Entity;
import com.kltyton.bedrockruntime.client.particle.api.geckolib.GeckoLibHelper;
import com.kltyton.bedrockruntime.client.particle.runtime.geckolib.ActorAnimationContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.core.animatable.GeoAnimatable;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.renderer.GeoReplacedEntityRenderer", remap = false)
public abstract class GeoReplacedEntityRendererMixin<E extends Entity, T extends GeoAnimatable> {
    @Shadow
    @Final
    protected T animatable;
    @Shadow
    protected E currentEntity;

    @Unique
    private ActorAnimationContext.Scope kltyton_bedrock_runtime$actorScope;
    @Unique
    private @org.jetbrains.annotations.Nullable Entity kltyton_bedrock_runtime$previousEntity;

    @Inject(method = "preRender", at = @At("HEAD"))
    private void setCurrentEntity(CallbackInfo ci) {
        kltyton_bedrock_runtime$previousEntity = animatable instanceof com.kltyton.bedrockruntime.client.particle.api.geckolib.WithCurrentEntity replaced
                ? replaced.getCurrentEntity() : null;
        if (currentEntity != null) {
            kltyton_bedrock_runtime$actorScope = ActorAnimationContext.enter(animatable, currentEntity);
        }
        GeckoLibHelper.setCurrentEntity(animatable, currentEntity);
    }

    @Inject(method = "doPostRenderCleanup", at = @At("TAIL"))
    private void cleanup(CallbackInfo ci) {
        GeckoLibHelper.setCurrentEntity(animatable, kltyton_bedrock_runtime$previousEntity);
        kltyton_bedrock_runtime$previousEntity = null;
        if (kltyton_bedrock_runtime$actorScope != null) {
            kltyton_bedrock_runtime$actorScope.close();
            kltyton_bedrock_runtime$actorScope = null;
        }
    }
}
