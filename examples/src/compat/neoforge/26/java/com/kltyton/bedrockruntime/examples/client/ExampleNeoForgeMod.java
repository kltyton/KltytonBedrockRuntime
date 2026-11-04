package com.kltyton.bedrockruntime.examples.client;

import com.kltyton.bedrockruntime.examples.ExampleCommonInit;
import com.kltyton.bedrockruntime.examples.ExampleScene;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(value = ExampleScene.MOD_ID)
public final class ExampleNeoForgeMod {
    public ExampleNeoForgeMod(IEventBus modEventBus) {
        ExampleCommonInit.register();
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            ExampleNeoForgeClient.init(modEventBus);
        }
    }
}
