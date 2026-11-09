package com.kltyton.bedrockruntime.client.particle.platform;

import com.geckolib.cache.GeckoLibResources;
import com.kltyton.bedrockruntime.client.particle.PSClientConfigs;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockBlockLight;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockSpriteSource;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockTerrainSpriteSource;
import com.kltyton.bedrockruntime.client.particle.network.PSClientPacketHandler;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleEngine;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.runtime.attachment.EmitterAttachHandler;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.particle.network.EmitterAttachPacketS2C;
import com.kltyton.bedrockruntime.particle.network.EmitterCreationPacketS2C;
import com.kltyton.bedrockruntime.particle.network.EmitterRemovalPacket;
import com.kltyton.bedrockruntime.particle.network.EmitterRemovalPacketS2C;
import com.kltyton.bedrockruntime.particle.network.EmitterSynchronizePacket;
import com.kltyton.bedrockruntime.particle.network.MolangEntityStatePacketS2C;
import com.kltyton.bedrockruntime.particle.network.ParticleNetwork;
import com.kltyton.bedrockruntime.particle.registry.ParticleRegistry;
import com.kltyton.bedrockruntime.client.particle.model.BedrockModelParticles;
import com.kltyton.bedrockruntime.client.particle.model.loading.FabricGeometryModelLoading;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.SpriteSourceRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.ResourceReloaderKeys;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.packs.PackType;

/** Fabric 26.x client particle wiring. */
public final class FabricParticleClientPlatform implements ParticleClientPlatform {
    private static boolean initialized;

    @Override
    public void initializeClient() {
        if (initialized) return;
        initialized = true;
        ParticleFabricRenderLayers.install();
        SpriteSourceRegistry.register(BedrockParticles.asResource("bedrock_particles"), BedrockSpriteSource.CODEC);
        SpriteSourceRegistry.register(BedrockParticles.asResource("bedrock_terrain"), BedrockTerrainSpriteSource.CODEC);
        BedrockBlockLight.initialize();
        PSClientConfigs.setConfigDirectory(FabricLoader.getInstance().getConfigDir());
        PSClientConfigs.onLoad();
        BedrockModelParticles.initialize();
        FabricGeometryModelLoading.initialize();
        ParticleRegistry.initializeDefaults();
        ParticleProviderRegistry.getInstance().register(BedrockParticles.MOLANG, new MolangParticleInstance.Provider());
        ResourceLoader loader = ResourceLoader.get(PackType.CLIENT_RESOURCES);
        loader.registerReloadListener(MolangParticleEngine.RELOADER_ID, MolangParticleEngine.INSTANCE);
        loader.registerReloadListener(BedrockParticles.asResource("model_particles"), BedrockModelParticles.reloadListener());
        loader.addListenerOrdering(GeckoLibResources.RELOAD_LISTENER_ID, BedrockParticles.asResource("model_particles"));
        loader.addListenerOrdering(ResourceReloaderKeys.Client.ATLAS, MolangParticleEngine.RELOADER_ID);
        loader.addListenerOrdering(ResourceReloaderKeys.Client.PARTICLES, MolangParticleEngine.RELOADER_ID);
        ParticleNetwork.installClient(new FabricClientTransport());
        registerClientReceivers();
        ClientTickEvents.START_CLIENT_TICK.register(client -> tick());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clearClientState());
    }

    private static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(EmitterCreationPacketS2C.TYPE, (payload, context) ->
                context.client().execute(() -> PSClientPacketHandler.handleEmitterCreation(payload, context.player())));
        ClientPlayNetworking.registerGlobalReceiver(EmitterAttachPacketS2C.TYPE, (payload, context) ->
                context.client().execute(() -> PSClientPacketHandler.handleEmitterAttach(payload, context.player())));
        ClientPlayNetworking.registerGlobalReceiver(EmitterRemovalPacketS2C.TYPE, (payload, context) ->
                context.client().execute(() -> PSClientPacketHandler.handleEmitterRemoval(payload.id(), context.player())));
        ClientPlayNetworking.registerGlobalReceiver(EmitterRemovalPacket.TYPE, (payload, context) ->
                context.client().execute(() -> PSClientPacketHandler.handleEmitterRemoval(payload.id(), context.player())));
        ClientPlayNetworking.registerGlobalReceiver(EmitterSynchronizePacket.TYPE, (payload, context) ->
                context.client().execute(() -> PSClientPacketHandler.handleEmitterSynchronize(payload, context.player())));
        ClientPlayNetworking.registerGlobalReceiver(MolangEntityStatePacketS2C.TYPE, (payload, context) ->
                context.client().execute(() -> PSClientPacketHandler.handleMolangEntityState(payload, context.player())));
    }

    private static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            MolangParticleEngine.INSTANCE.removeAll();
        } else if (!minecraft.isPaused() && player.level().tickRateManager().runsNormally()) {
            MolangParticleEngine.INSTANCE.tick(minecraft, player);
            if (BedrockParticles.GECKOLIB_LOADED) {
                com.kltyton.bedrockruntime.client.particle.api.geckolib.GeckoLibHelper.pruneLocatorBindings();
            }
            if (PSClientConfigs.emitterAutoRemoveIntervalTick <= 1
                    || player.level().getGameTime() % PSClientConfigs.emitterAutoRemoveIntervalTick == 0) {
                Camera camera = ParticleCameraAccess.camera(minecraft);
                if (camera.isInitialized()) {
                    EmitterAttachHandler.tick(camera);
                }
            }
        } else {
            MolangParticleEngine.INSTANCE.pauseSimulation();
        }
    }

    private static void clearClientState() {
        MolangParticleEngine.INSTANCE.removeAll();
        EmitterAttachHandler.clearEmitters();
        EmitterAttachHandler.ignoreRangeEmitters.clear();
        if (BedrockParticles.GECKOLIB_LOADED) {
            com.kltyton.bedrockruntime.client.particle.api.geckolib.GeckoLibHelper.clearReloadCallbacks();
        }
    }

    private static final class FabricClientTransport implements ParticleNetwork.ClientTransport {
        @Override
        public boolean canSend(CustomPacketPayload.Type<?> type) {
            return ClientPlayNetworking.canSend(type);
        }

        @Override
        public void sendToServer(CustomPacketPayload payload) {
            ClientPlayNetworking.send(payload);
        }
    }
}
