package com.kltyton.bedrockruntime.client.compat.geckolib;

import com.kltyton.bedrockruntime.client.compat.geckolib.layer.GeckoBoneSelectionLayer;
import com.kltyton.bedrockruntime.client.compat.geckolib.loading.EnhancedGeometryLoader;
import software.bernie.geckolib.event.GeoRenderEvent;

/**
 * Fabric GeckoLib geometry and selection registration.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class KltytonBedrockRuntimeFabricGeckoClient {
    private static boolean eventsRegistered;

    private KltytonBedrockRuntimeFabricGeckoClient() {
    }

    public static void registerRenderers() {
        registerEvents();
    }

    private static void registerEvents() {
        if (eventsRegistered) {
            return;
        }

        eventsRegistered = true;
        EnhancedGeometryLoader.register();
        GeoRenderEvent.Entity.CompileRenderLayers.EVENT.register(KltytonBedrockRuntimeFabricGeckoClient::addEntityLayers);
    }

    private static void addEntityLayers(GeoRenderEvent.Entity.CompileRenderLayers event) {
        event.addLayer(new GeckoBoneSelectionLayer(event.getRenderer()));
    }

}
