package com.kltyton.bedrockruntime.examples.client;

import com.kltyton.bedrockruntime.examples.ExampleScene;
import net.minecraft.resources.ResourceLocation;

public final class ExampleIds {
    private ExampleIds() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ExampleScene.MOD_ID, path);
    }
}
