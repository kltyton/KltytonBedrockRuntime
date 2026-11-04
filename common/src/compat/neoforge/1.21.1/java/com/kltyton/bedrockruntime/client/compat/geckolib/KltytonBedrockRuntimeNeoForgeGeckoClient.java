package com.kltyton.bedrockruntime.client.compat.geckolib;

import com.kltyton.bedrockruntime.client.compat.geckolib.layer.GeckoBoneSelectionLayer;
import com.kltyton.bedrockruntime.client.compat.geckolib.loading.EnhancedGeometryLoader;
import software.bernie.geckolib.event.GeoRenderEvent;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * NeoForge GeckoLib geometry and selection registration.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class KltytonBedrockRuntimeNeoForgeGeckoClient {
    private static boolean eventsRegistered;

    private KltytonBedrockRuntimeNeoForgeGeckoClient() {
    }

    public static void registerEvents() {
        if (eventsRegistered) {
            return;
        }

        eventsRegistered = true;
        EnhancedGeometryLoader.register();
        NeoForge.EVENT_BUS.addListener(KltytonBedrockRuntimeNeoForgeGeckoClient::addEntityLayers);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        registerEvents();
    }

    private static void addEntityLayers(GeoRenderEvent.Entity.CompileRenderLayers event) {
        event.addLayer(new GeckoBoneSelectionLayer(event.getRenderer()));
    }

}
