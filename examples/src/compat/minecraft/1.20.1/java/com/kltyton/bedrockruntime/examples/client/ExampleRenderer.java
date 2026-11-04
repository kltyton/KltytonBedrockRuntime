package com.kltyton.bedrockruntime.examples.client;

import com.kltyton.bedrockruntime.client.compat.geckolib.layer.GeckoBoneSelectionLayer;
import com.kltyton.bedrockruntime.examples.ExampleScene;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class ExampleRenderer<E extends Entity> extends GeoReplacedEntityRenderer<E, ExampleAnimatable> {
    public ExampleRenderer(EntityRendererProvider.Context context, ExampleScene scene, EntityType<?> type) {
        super(context, new ExampleModel(scene), new ExampleAnimatable(scene, type));
        if (getRenderLayers().stream().noneMatch(layer -> layer instanceof GeckoBoneSelectionLayer)) {
            addRenderLayer((GeoRenderLayer) new GeckoBoneSelectionLayer((GeoRenderer) this));
        }
    }
}
