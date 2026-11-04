package com.kltyton.bedrockruntime.examples.client;

import com.kltyton.bedrockruntime.examples.ExampleTelemetry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class ExampleFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ExampleRenderers.registerAll((type, provider) -> EntityRendererRegistry.register(type, provider));
        ClientTickEvents.END_CLIENT_TICK.register(client -> ExampleTelemetry.tick());
    }
}
