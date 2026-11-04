package com.kltyton.bedrockruntime.client;

import com.kltyton.bedrockruntime.client.particle.platform.ForgeParticleClientPlatform;
import com.kltyton.bedrockruntime.client.particle.platform.ParticleClientBootstrap;
import com.kltyton.bedrockruntime.client.particle.platform.ParticleClientPlatforms;
import com.kltyton.bedrockruntime.client.compat.geckolib.KltytonBedrockRuntimeForgeGeckoClient;

import com.kltyton.bedrockruntime.client.config.BoneHitboxClientOptions;
import com.kltyton.bedrockruntime.client.selection.service.BonePartSelectionClient;
import com.kltyton.bedrockruntime.network.protocol.BoneHitboxNetworking;

import net.minecraft.client.Minecraft;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import com.kltyton.bedrockruntime.network.KltytonBedrockRuntimeForgeNetworking;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * CN: Forge 客户端入口。
 * EN: Forge client bootstrap.
 */
public final class KltytonBedrockRuntimeForgeClient {

    private KltytonBedrockRuntimeForgeClient() {
    }

    public static void register(IEventBus modEventBus) {
        ParticleClientPlatforms.install(new ForgeParticleClientPlatform(modEventBus));
        ParticleClientBootstrap.initializeClient();
        KltytonBedrockRuntimeForgeGeckoClient.registerEvents();
        BoneHitboxNetworking.setClientSelectionSender(payload -> sendToServerWhenConnected(payload));
        BoneHitboxNetworking.setClientEntityPartsSender(payload -> sendToServerWhenConnected(payload));
        BoneHitboxNetworking.setClientContactReportSender(payload -> sendToServerWhenConnected(payload));
        BoneHitboxNetworking.setClientGeoKeyframeSkillSender(payload -> sendToServerWhenConnected(payload));
        modEventBus.addListener(KltytonBedrockRuntimeForgeClient::registerRenderers);
        modEventBus.addListener(KltytonBedrockRuntimeForgeClient::registerKeys);
        MinecraftForge.EVENT_BUS.addListener(KltytonBedrockRuntimeForgeClient::clientTick);
        MinecraftForge.EVENT_BUS.addListener(KltytonBedrockRuntimeForgeClient::attackEntity);
        MinecraftForge.EVENT_BUS.addListener(KltytonBedrockRuntimeForgeClient::interactEntity);
    }

    private static void sendToServerWhenConnected(Object payload) {
        if (Minecraft.getInstance().getConnection() != null) {
            KltytonBedrockRuntimeForgeNetworking.CHANNEL.sendToServer(payload);
        }
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        KltytonBedrockRuntimeForgeGeckoClient.registerRenderers(event);
    }

    private static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) { BonePartSelectionClient.clientTick(Minecraft.getInstance()); }
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(BoneHitboxClientOptions.HOLD_OBB_KEY);
    }

    private static void attackEntity(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide()) {
            BonePartSelectionClient.recordAttack(event.getTarget());
        }
    }

    private static void interactEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            BonePartSelectionClient.syncCurrentSelection();
        }
    }

}
