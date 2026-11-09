package com.kltyton.bedrockruntime.client;

import com.kltyton.bedrockruntime.client.particle.platform.NeoForgeParticleClientPlatform;
import com.kltyton.bedrockruntime.client.particle.platform.ParticleClientBootstrap;
import com.kltyton.bedrockruntime.client.particle.platform.ParticleClientPlatforms;
import com.kltyton.bedrockruntime.client.compat.geckolib.KltytonBedrockRuntimeNeoForgeGeckoClient;

import com.kltyton.bedrockruntime.client.config.BoneHitboxClientOptions;
import com.kltyton.bedrockruntime.client.selection.service.BonePartSelectionClient;
import com.kltyton.bedrockruntime.network.protocol.BoneHitboxNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * CN: NeoForge 客户端入口。
 * EN: NeoForge client bootstrap.
 */
public final class KltytonBedrockRuntimeNeoForgeClient {

    private KltytonBedrockRuntimeNeoForgeClient() {
    }

    public static void register(IEventBus modEventBus) {
        ParticleClientPlatforms.install(new NeoForgeParticleClientPlatform(modEventBus));
        ParticleClientBootstrap.initializeClient();
        KltytonBedrockRuntimeNeoForgeGeckoClient.registerEvents();
        BoneHitboxNetworking.setClientSelectionSender(payload -> sendToServerWhenConnected(payload));
        BoneHitboxNetworking.setClientEntityPartsSender(payload -> sendToServerWhenConnected(payload));
        BoneHitboxNetworking.setClientContactReportSender(payload -> sendToServerWhenConnected(payload));
        BoneHitboxNetworking.setClientGeoKeyframeSkillSender(payload -> sendToServerWhenConnected(payload));
        modEventBus.addListener(KltytonBedrockRuntimeNeoForgeClient::registerRenderers);
        modEventBus.addListener(KltytonBedrockRuntimeNeoForgeClient::registerKeys);
        NeoForge.EVENT_BUS.addListener(KltytonBedrockRuntimeNeoForgeClient::clientTick);
        NeoForge.EVENT_BUS.addListener(KltytonBedrockRuntimeNeoForgeClient::attackEntity);
        NeoForge.EVENT_BUS.addListener(KltytonBedrockRuntimeNeoForgeClient::interactEntity);
        NeoForge.EVENT_BUS.addListener(KltytonBedrockRuntimeNeoForgeClient::beforeInteractionInput);
    }

    private static void sendToServerWhenConnected(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if (Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(payload);
        }
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        KltytonBedrockRuntimeNeoForgeGeckoClient.registerRenderers(event);
    }

    private static void clientTick(ClientTickEvent.Post event) {
        BonePartSelectionClient.clientTick(Minecraft.getInstance());
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(BoneHitboxClientOptions.HOLD_OBB_KEY);
    }

    private static void attackEntity(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide()) {
            BonePartSelectionClient.recordAttack(event.getTarget());
        }
    }

    // Entity interaction and attack events occur after their vanilla packets have been sent.
    private static void beforeInteractionInput(InputEvent.InteractionKeyMappingTriggered event) {
        if ((event.isAttack() || event.isUseItem())
                && Minecraft.getInstance().hitResult instanceof EntityHitResult) {
            BonePartSelectionClient.syncCurrentSelection();
        }
    }

    private static void interactEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            BonePartSelectionClient.syncCurrentSelection();
        }
    }

}
