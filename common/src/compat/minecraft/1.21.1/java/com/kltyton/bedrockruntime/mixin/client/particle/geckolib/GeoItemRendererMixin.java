package com.kltyton.bedrockruntime.mixin.client.particle.geckolib;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.Item;
import com.kltyton.bedrockruntime.client.particle.api.geckolib.GeckoLibHelper;
import com.kltyton.bedrockruntime.client.particle.runtime.geckolib.ActorAnimationContext;
import com.kltyton.bedrockruntime.client.particle.access.IPSGeoBone;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.util.RenderUtil;

/** Captures item locators after the native display and bone transforms. */
@Pseudo
@Mixin(targets = "software.bernie.geckolib.renderer.GeoItemRenderer", remap = false)
public abstract class GeoItemRendererMixin<T extends Item & GeoAnimatable> {
    @WrapMethod(method = "actuallyRender(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/Item;Lsoftware/bernie/geckolib/cache/object/BakedGeoModel;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZFIII)V")
    private void kltyton_bedrock_runtime$withItemPose(PoseStack pose, T animatable, BakedGeoModel model, RenderType type,
            MultiBufferSource buffers, VertexConsumer vertices, boolean reRender, float partialTick,
            int light, int overlay, int color, Operation<Void> original) {
        try (var ignored = ActorAnimationContext.enterItem(animatable)) {
            original.call(pose, animatable, model, type, buffers, vertices, reRender, partialTick, light, overlay, color);
        }
    }

    @Inject(method = "renderRecursively(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/Item;Lsoftware/bernie/geckolib/cache/object/GeoBone;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZFIII)V",
            at = @At(value = "INVOKE", target = "Lsoftware/bernie/geckolib/renderer/GeoRenderer;renderRecursively(Lcom/mojang/blaze3d/vertex/PoseStack;Lsoftware/bernie/geckolib/animatable/GeoAnimatable;Lsoftware/bernie/geckolib/cache/object/GeoBone;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZFIII)V"))
    private void kltyton_bedrock_runtime$captureLocator(PoseStack pose, T animatable, GeoBone bone, RenderType type,
            MultiBufferSource buffers, VertexConsumer vertices, boolean reRender, float partialTick,
            int light, int overlay, int color, CallbackInfo ci) {
        if (reRender || !GeckoLibHelper.usesNativeItemLocator(animatable)) return;
        var locators = IPSGeoBone.of(bone).kltyton_bedrock_runtime$getLocators();
        if (locators == null || locators.isEmpty()) return;
        PoseStack bonePose = new PoseStack();
        bonePose.last().pose().set(pose.last().pose());
        RenderUtil.prepMatrixForBone(bonePose, bone);
        GeckoLibHelper.captureNativeItemLocator(bone, animatable, bonePose.last().pose(), partialTick);
    }
}
