package com.kltyton.bedrockruntime;

import com.kltyton.bedrockruntime.client.KltytonBedrockRuntimeForgeClient;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import com.kltyton.bedrockruntime.network.KltytonBedrockRuntimeForgeNetworking;
import com.kltyton.bedrockruntime.particle.platform.ForgeParticlePlatform;
import com.kltyton.bedrockruntime.particle.platform.ParticlePlatforms;
import com.kltyton.bedrockruntime.particle.platform.ParticleServerBootstrap;

import com.kltyton.bedrockruntime.config.common.BoneHitboxConfig;
import com.kltyton.bedrockruntime.server.hook.vanilla.BoneHitboxServerHooks;

import net.minecraft.world.InteractionResult;

/**
 * CN: Forge loader 入口。
 * EN: Forge loader entrypoint.
 */
@Mod(Constants.MOD_ID)
public final class KltytonBedrockRuntime {

    public KltytonBedrockRuntime() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, BoneHitboxConfig.CLIENT_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, BoneHitboxConfig.SERVER_SPEC);
        ParticlePlatforms.installCommon(new ForgeParticlePlatform(eventBus));
        ParticleServerBootstrap.initializeCommon();
        KltytonBedrockRuntimeForgeNetworking.register();
        MinecraftForge.EVENT_BUS.addListener(KltytonBedrockRuntime::interactEntity);
        MinecraftForge.EVENT_BUS.addListener(KltytonBedrockRuntime::serverTick);
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.server.ServerStoppingEvent event) ->
                com.kltyton.bedrockruntime.server.sync.ObbServerLifecycle.clear());
        if (FMLEnvironment.dist == Dist.CLIENT) {
            registerClient(eventBus);
        }
        eventBus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) ->
                event.enqueueWork(com.kltyton.bedrockruntime.api.block.shape.VanillaBlockShapes::initializeModBlocks));
        KltytonBedrockRuntimeCommon.init();
    }

    private static void interactEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        InteractionResult result = BoneHitboxServerHooks.handlePlayerInteract(event.getEntity(), event.getHand(), event.getTarget());
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    private static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) { com.kltyton.bedrockruntime.server.sync.ObbServerLifecycle.tick(event.getServer()); }
    }

    private static void registerClient(IEventBus eventBus) {
        try {
            Class<?> clientClass = Class.forName("com.kltyton.bedrockruntime.client.KltytonBedrockRuntimeForgeClient");
            clientClass.getMethod("register", IEventBus.class).invoke(null, eventBus);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to register KltytonBedrockRuntime Forge client hooks", exception);
        }
    }
}
