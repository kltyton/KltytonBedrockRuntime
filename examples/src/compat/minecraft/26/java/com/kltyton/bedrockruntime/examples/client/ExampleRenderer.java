package com.kltyton.bedrockruntime.examples.client;

import com.geckolib.renderer.GeoReplacedEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.kltyton.bedrockruntime.client.compat.geckolib.animation.GraphAnimationLayer;
import com.kltyton.bedrockruntime.client.compat.geckolib.layer.GeckoBoneSelectionLayer;
import com.kltyton.bedrockruntime.examples.ExampleScene;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class ExampleRenderer<E extends LivingEntity, R extends LivingEntityRenderState & GeoRenderState>
        extends GeoReplacedEntityRenderer<ExampleAnimatable, E, R> {
    public ExampleRenderer(EntityRendererProvider.Context context, ExampleScene scene) {
        super(context, new ExampleModel(scene), new ExampleAnimatable(scene));
        if (getRenderLayers().stream().noneMatch(layer -> layer instanceof GraphAnimationLayer)) {
            withRenderLayer((GeoRenderLayer) new GraphAnimationLayer((GeoRenderer) this));
        }
        if (getRenderLayers().stream().noneMatch(layer -> layer instanceof GeckoBoneSelectionLayer)) {
            withRenderLayer((GeoRenderLayer) new GeckoBoneSelectionLayer((GeoRenderer) this));
        }
    }
}
