package com.kltyton.bedrockruntime.examples.client;

import com.kltyton.bedrockruntime.examples.ExampleScene;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class ExampleRenderers {
    @FunctionalInterface
    public interface Registrar {
        void register(EntityType type, EntityRendererProvider provider);
    }

    private ExampleRenderers() {
    }

    public static void registerAll(Registrar registrar) {
        registrar.register(EntityType.PIG, context -> new ExampleRenderer(context, ExampleScene.SPEED_1D, EntityType.PIG));
        registrar.register(EntityType.ARMOR_STAND, context -> new ExampleRenderer(context, ExampleScene.AIM_2D, EntityType.ARMOR_STAND));
        registrar.register(EntityType.COW, context -> new ExampleRenderer(context, ExampleScene.LEGS_AIM, EntityType.COW));
        registrar.register(EntityType.SHEEP, context -> new ExampleRenderer(context, ExampleScene.ADDITIVE, EntityType.SHEEP));
        registrar.register(EntityType.CHICKEN, context -> new ExampleRenderer(context, ExampleScene.HERMITE, EntityType.CHICKEN));
        registrar.register(EntityType.ZOMBIE, context -> new ExampleRenderer(context, ExampleScene.MESH, EntityType.ZOMBIE));
        registrar.register(EntityType.SKELETON, context -> new ExampleRenderer(context, ExampleScene.TREE, EntityType.SKELETON));
        registrar.register(EntityType.HUSK, context -> new ExampleRenderer(context, ExampleScene.BAKED, EntityType.HUSK));
    }
}
