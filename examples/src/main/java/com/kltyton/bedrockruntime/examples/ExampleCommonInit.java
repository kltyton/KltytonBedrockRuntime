package com.kltyton.bedrockruntime.examples;

import com.kltyton.bedrockruntime.api.model.EnhancedModels;
import com.kltyton.bedrockruntime.examples.registry.ExampleEntityTypes;

public final class ExampleCommonInit {
    private static boolean registered;

    private ExampleCommonInit() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        ExampleResources.register(ExampleCommonInit.class);
        EnhancedModels.bindEntity(ExampleEntityTypes.PIG, ExampleResources.humanoid());
        EnhancedModels.bindEntity(ExampleEntityTypes.ARMOR_STAND, ExampleResources.humanoid());
        EnhancedModels.bindEntity(ExampleEntityTypes.COW, ExampleResources.humanoid());
        EnhancedModels.bindEntity(ExampleEntityTypes.SHEEP, ExampleResources.humanoid());
        EnhancedModels.bindEntity(ExampleEntityTypes.CHICKEN, ExampleResources.humanoid());
        EnhancedModels.bindEntity(ExampleEntityTypes.ZOMBIE, ExampleResources.humanoid());
        EnhancedModels.bindEntity(ExampleEntityTypes.SKELETON, ExampleResources.tree());
        EnhancedModels.bindEntity(ExampleEntityTypes.HUSK, ExampleResources.baked());
    }
}
