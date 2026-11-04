package com.kltyton.bedrockruntime.examples.client;

import com.kltyton.bedrockruntime.examples.ExampleTelemetry;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class ExampleForgeClient {
    private ExampleForgeClient() {
    }

    public static void init() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(ExampleForgeClient::registerRenderers);
        MinecraftForge.EVENT_BUS.addListener(ExampleForgeClient::clientTick);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ExampleRenderers.registerAll((type, provider) -> event.registerEntityRenderer(type, provider));
    }

    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ExampleTelemetry.tick();
        }
    }
}
