package com.kltyton.bedrockruntime.examples.client;

import com.kltyton.bedrockruntime.examples.ExampleCommonInit;
import net.fabricmc.api.ModInitializer;

public final class ExampleFabricCommon implements ModInitializer {
    @Override
    public void onInitialize() {
        ExampleCommonInit.register();
    }
}
