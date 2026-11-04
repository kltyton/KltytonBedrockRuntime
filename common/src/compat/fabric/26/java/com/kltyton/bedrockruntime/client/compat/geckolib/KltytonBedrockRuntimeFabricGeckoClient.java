package com.kltyton.bedrockruntime.client.compat.geckolib;

import com.kltyton.bedrockruntime.client.compat.geckolib.layer.GeckoBoneSelectionLayer;
import com.kltyton.bedrockruntime.client.compat.geckolib.animation.GraphAnimationLayer;
import com.kltyton.bedrockruntime.client.compat.geckolib.loading.EnhancedGeometryLoader;

import com.geckolib.constant.DataTickets;
import com.geckolib.event.entity.CompileEntityRenderLayersEvent;
import com.geckolib.event.entity.CompileEntityRenderStateEvent;
import com.geckolib.renderer.base.GeoRenderer;
import com.kltyton.bedrockruntime.client.selection.service.BonePartSelectionClient;

import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;

/**
 * Fabric GeckoLib geometry, graph and selection registration.
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
        CompileEntityRenderLayersEvent.EVENT.register(KltytonBedrockRuntimeFabricGeckoClient::addEntityLayers);
        CompileEntityRenderStateEvent.EVENT.register(KltytonBedrockRuntimeFabricGeckoClient::suppressFullRedOverlay);
    }

    private static void addEntityLayers(CompileEntityRenderLayersEvent event) {
        event.addLayer(renderer -> new GraphAnimationLayer((GeoRenderer) renderer));
        event.addLayer(renderer -> new GeckoBoneSelectionLayer((GeoRenderer) renderer));
    }

    private static void suppressFullRedOverlay(CompileEntityRenderStateEvent event) {
        if (event.getAnimatable() instanceof LivingEntity livingEntity
                && (livingEntity.hurtTime > 0 || livingEntity.deathTime > 0)
                && BonePartSelectionClient.shouldSuppressFullEntityRed(livingEntity)) {
            event.addData(DataTickets.PACKED_OVERLAY, OverlayTexture.NO_OVERLAY);
        }
    }
}
