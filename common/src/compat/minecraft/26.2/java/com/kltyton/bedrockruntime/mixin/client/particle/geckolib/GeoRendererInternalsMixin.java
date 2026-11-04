package com.kltyton.bedrockruntime.mixin.client.particle.geckolib;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.Entity;
import com.kltyton.bedrockruntime.client.particle.api.geckolib.GeckoLibHelper;
import com.kltyton.bedrockruntime.client.particle.runtime.geckolib.ActorAnimationContext;
import com.kltyton.bedrockruntime.client.particle.access.IEntity;
import com.kltyton.bedrockruntime.client.particle.definition.molang.RenderItemContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** Provides the item being rendered while animation state and keyframes are evaluated. */
@Pseudo
@Mixin(targets = "com.geckolib.renderer.base.GeoRendererInternals", remap = false)
public interface GeoRendererInternalsMixin {
    @WrapMethod(method = "fillRenderState")
    default GeoRenderState kltyton_bedrock_runtime$withRenderedItem(GeoAnimatable animatable, Object relatedObject,
            GeoRenderState renderState, float partialTick, Operation<GeoRenderState> original) {
        Entity entity = relatedObject instanceof Entity value ? value : GeckoLibHelper.animationEntity(animatable);
        renderState.addGeckolibData(ActorAnimationContext.ENTITY, entity);
        ItemStack item = relatedObject instanceof GeoItemRenderer.RenderData data ? data.itemStack() : null;
        ItemOwner owner = relatedObject instanceof GeoItemRenderer.RenderData data
                && data.renderPerspective() != ItemDisplayContext.GUI ? data.itemOwner() : null;
        ItemOwner rootOwner = owner;
        while (rootOwner instanceof ItemOwner.OffsetFromOwner offset) rootOwner = offset.owner();
        if (rootOwner instanceof Entity actor) IEntity.of(actor).kltyton_bedrock_runtime$sampleAnimationLifetime(partialTick);
        ItemDisplayContext display = relatedObject instanceof GeoItemRenderer.RenderData data
                ? data.renderPerspective() : null;
        try (RenderItemContext ignored = RenderItemContext.enter(item, owner, display)) {
            return original.call(animatable, relatedObject, renderState, partialTick);
        }
    }
}
