package com.kltyton.bedrockruntime;

import com.kltyton.bedrockruntime.event.KltytonBedrockRuntimeFabricEvents;
import com.kltyton.bedrockruntime.particle.platform.FabricParticlePlatform;
import com.kltyton.bedrockruntime.particle.platform.ParticlePlatforms;
import com.kltyton.bedrockruntime.particle.platform.ParticleServerBootstrap;
import com.kltyton.bedrockruntime.network.KltytonBedrockRuntimeFabricNetworking;

import com.kltyton.bedrockruntime.config.common.BoneHitboxConfig;

import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.neoforged.fml.config.ModConfig;

/**
 * CN: Fabric loader 入口。
 * EN: Fabric loader entrypoint.
 */
public final class KltytonBedrockRuntime implements ModInitializer {

    /**
     * CN: 创建 Fabric 入口。
     * EN: Creates the Fabric entrypoint.
     */
    public KltytonBedrockRuntime() {
    }

    @Override
    public void onInitialize() {
        NeoForgeConfigRegistry.INSTANCE.register(Constants.MOD_ID, ModConfig.Type.CLIENT, BoneHitboxConfig.CLIENT_SPEC);
        NeoForgeConfigRegistry.INSTANCE.register(Constants.MOD_ID, ModConfig.Type.SERVER, BoneHitboxConfig.SERVER_SPEC);
        KltytonBedrockRuntimeFabricNetworking.register();
        KltytonBedrockRuntimeFabricEvents.register();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(
                server -> com.kltyton.bedrockruntime.api.block.shape.VanillaBlockShapes.initializeModBlocks());
        ParticlePlatforms.installCommon(new FabricParticlePlatform());
        ParticleServerBootstrap.initializeCommon();
        KltytonBedrockRuntimeCommon.init();
    }
}
