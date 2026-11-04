package com.kltyton.bedrockruntime.examples.client;

import com.kltyton.bedrockruntime.examples.ExampleTelemetry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class ExampleNeoForgeClient {
    private ExampleNeoForgeClient() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(ExampleNeoForgeClient::registerRenderers);
        NeoForge.EVENT_BUS.addListener(ExampleNeoForgeClient::clientTick);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ExampleRenderers.registerAll((type, provider) -> event.registerEntityRenderer(type, provider));
    }

    public static void clientTick(ClientTickEvent.Post event) {
        ExampleTelemetry.tick();
    }
}
