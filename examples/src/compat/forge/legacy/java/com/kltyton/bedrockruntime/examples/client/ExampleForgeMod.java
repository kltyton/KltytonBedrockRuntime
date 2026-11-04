package com.kltyton.bedrockruntime.examples.client;

import com.kltyton.bedrockruntime.examples.ExampleCommonInit;
import com.kltyton.bedrockruntime.examples.ExampleScene;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(ExampleScene.MOD_ID)
public final class ExampleForgeMod {
    public ExampleForgeMod() {
        ExampleCommonInit.register();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ExampleForgeClient.init();
        }
    }
}
