package com.kltyton.bedrockruntime.examples.client;

import com.kltyton.bedrockruntime.examples.ExampleScene;
import com.kltyton.bedrockruntime.examples.registry.ExampleEntityTypes;
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
        registrar.register(ExampleEntityTypes.PIG, context -> new ExampleRenderer(context, ExampleScene.SPEED_1D));
        registrar.register(ExampleEntityTypes.ARMOR_STAND, context -> new ExampleRenderer(context, ExampleScene.AIM_2D));
        registrar.register(ExampleEntityTypes.COW, context -> new ExampleRenderer(context, ExampleScene.LEGS_AIM));
        registrar.register(ExampleEntityTypes.SHEEP, context -> new ExampleRenderer(context, ExampleScene.ADDITIVE));
        registrar.register(ExampleEntityTypes.CHICKEN, context -> new ExampleRenderer(context, ExampleScene.HERMITE));
        registrar.register(ExampleEntityTypes.ZOMBIE, context -> new ExampleRenderer(context, ExampleScene.MESH));
        registrar.register(ExampleEntityTypes.SKELETON, context -> new ExampleRenderer(context, ExampleScene.TREE));
        registrar.register(ExampleEntityTypes.HUSK, context -> new ExampleRenderer(context, ExampleScene.BAKED));
    }
}
