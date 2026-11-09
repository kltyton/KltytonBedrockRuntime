package com.kltyton.bedrockruntime.client;

import com.kltyton.bedrockruntime.client.particle.platform.NeoForgeParticleClientPlatform;
import com.kltyton.bedrockruntime.client.particle.platform.ParticleClientBootstrap;
import com.kltyton.bedrockruntime.client.particle.platform.ParticleClientPlatforms;
import com.kltyton.bedrockruntime.client.compat.geckolib.KltytonBedrockRuntimeNeoForgeGeckoClient;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.kltyton.bedrockruntime.Constants;
import com.kltyton.bedrockruntime.client.config.BoneHitboxClientOptions;
import com.kltyton.bedrockruntime.client.render.vanilla.VanillaModelPartSelectionLayer;
import com.kltyton.bedrockruntime.client.selection.service.BonePartSelectionClient;
import com.kltyton.bedrockruntime.network.protocol.BoneHitboxNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.client.event.InputEvent;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
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
    private static final Method ADD_LAYER = findAddLayerMethod();

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
        modEventBus.addListener(KltytonBedrockRuntimeNeoForgeClient::addLayers);
        modEventBus.addListener(KltytonBedrockRuntimeNeoForgeClient::registerKeys);
        NeoForge.EVENT_BUS.addListener(KltytonBedrockRuntimeNeoForgeClient::clientTick);
        NeoForge.EVENT_BUS.addListener(KltytonBedrockRuntimeNeoForgeClient::attackEntity);
        NeoForge.EVENT_BUS.addListener(KltytonBedrockRuntimeNeoForgeClient::interactEntity);
        NeoForge.EVENT_BUS.addListener(KltytonBedrockRuntimeNeoForgeClient::beforeInteractionInput);
    }

    private static void sendToServerWhenConnected(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if (Minecraft.getInstance().getConnection() != null) {
            ClientPacketDistributor.sendToServer(payload);
        }
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        KltytonBedrockRuntimeNeoForgeGeckoClient.registerRenderers(event);
    }

    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (EntityType<?> entityType : event.getEntityTypes()) {
            EntityRenderer<?, ?> renderer = event.getRenderer(entityType);
            if (renderer instanceof LivingEntityRenderer<?, ?, ?> livingRenderer) {
                addVanillaLayer(livingRenderer);
            }
        }

        // CN: NeoForge 将玩家 renderer 按皮肤模型单独保存，不包含在 getEntityTypes() 中。
        // EN: NeoForge stores player renderers by skin model, outside getEntityTypes().
        for (PlayerModelType skinModel : event.getSkins()) {
            LivingEntityRenderer<?, ?, ?> playerRenderer = event.getPlayerRenderer(skinModel);
            if (playerRenderer != null) {
                addVanillaLayer(playerRenderer);
            }
        }
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

    private static void addVanillaLayer(LivingEntityRenderer<?, ?, ?> renderer) {
        try {
            ADD_LAYER.invoke(renderer, VanillaModelPartSelectionLayer.createRaw(renderer));
        } catch (IllegalAccessException | InvocationTargetException exception) {
            Constants.LOG.warn("Failed to add vanilla model part selection layer.", exception);
        }
    }

    private static Method findAddLayerMethod() {
        try {
            Method method = LivingEntityRenderer.class.getDeclaredMethod("addLayer", RenderLayer.class);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("LivingEntityRenderer#addLayer was not found", exception);
        }
    }
}
