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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * NeoForge GeckoLib geometry, graph and selection registration.
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
        NeoForge.EVENT_BUS.addListener(KltytonBedrockRuntimeNeoForgeGeckoClient::suppressFullRedOverlay);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        registerEvents();
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
