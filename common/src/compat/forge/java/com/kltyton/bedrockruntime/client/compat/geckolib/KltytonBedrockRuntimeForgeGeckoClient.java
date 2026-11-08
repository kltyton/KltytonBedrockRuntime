package com.kltyton.bedrockruntime.client.compat.geckolib;

import com.kltyton.bedrockruntime.client.compat.geckolib.layer.GeckoBoneSelectionLayer;
import com.kltyton.bedrockruntime.client.compat.geckolib.loading.EnhancedGeometryLoader;
import com.kltyton.bedrockruntime.client.compat.geckolib.render.ForgeGeckoRenderHook;
import software.bernie.geckolib.event.GeoRenderEvent;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.EntityRenderersEvent;

/**
 * Forge GeckoLib geometry and selection registration.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class KltytonBedrockRuntimeForgeGeckoClient {
    private static boolean eventsRegistered;

    private KltytonBedrockRuntimeForgeGeckoClient() {
    }

    public static void registerEvents() {
        if (eventsRegistered) {
            return;
        }

        ForgeGeckoRenderHook.verifyInstalled();
        eventsRegistered = true;
        EnhancedGeometryLoader.register();
        MinecraftForge.EVENT_BUS.addListener(KltytonBedrockRuntimeForgeGeckoClient::addEntityLayers);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        registerEvents();
    }

    private static void addEntityLayers(GeoRenderEvent.Entity.CompileRenderLayers event) {
        event.addLayer(new GeckoBoneSelectionLayer(event.getRenderer()));
    }

}
