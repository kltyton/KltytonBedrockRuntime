package com.kltyton.bedrockruntime.client.particle.platform;

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
import com.kltyton.bedrockruntime.client.particle.model.loading.NeoForgeGeometryModelLoading;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterSpriteSourcesEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** NeoForge 26.1.2/26.2 client particle wiring. */
public final class NeoForgeParticleClientPlatform implements ParticleClientPlatform {
    private final IEventBus eventBus;
    private boolean initialized;

    public NeoForgeParticleClientPlatform(IEventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void initializeClient() {
        if (initialized) return;
        initialized = true;
        eventBus.addListener(ParticleNeoForgeRenderLayers::register);
        eventBus.addListener(this::registerClientPayloads);
        eventBus.addListener(this::registerSpriteSources);
        eventBus.addListener(this::registerParticleProviders);
        eventBus.addListener(this::addReloadListeners);
        NeoForge.EVENT_BUS.addListener(this::clientTick);
        NeoForge.EVENT_BUS.addListener(this::loggingOut);
        BedrockBlockLight.initialize();
        PSClientConfigs.setConfigDirectory(FMLPaths.CONFIGDIR.get());
        PSClientConfigs.onLoad();
        BedrockModelParticles.initialize();
        NeoForgeGeometryModelLoading.register(eventBus);
        eventBus.addListener(this::clientSetup);
        ParticleNetwork.installClient(new NeoClientTransport());
    }

    private void registerClientPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(EmitterCreationPacketS2C.TYPE, (payload, context) ->
                PSClientPacketHandler.handleEmitterCreation(payload, context.player()));
        event.register(EmitterAttachPacketS2C.TYPE, (payload, context) ->
                PSClientPacketHandler.handleEmitterAttach(payload, context.player()));
        event.register(EmitterRemovalPacketS2C.TYPE, (payload, context) ->
                PSClientPacketHandler.handleEmitterRemoval(payload.id(), context.player()));
        event.register(EmitterRemovalPacket.TYPE, (payload, context) ->
                PSClientPacketHandler.handleEmitterRemoval(payload.id(), context.player()));
        event.register(EmitterSynchronizePacket.TYPE, (payload, context) ->
                PSClientPacketHandler.handleEmitterSynchronize(payload, context.player()));
        event.register(MolangEntityStatePacketS2C.TYPE, (payload, context) ->
                PSClientPacketHandler.handleMolangEntityState(payload, context.player()));
    }

    private void registerSpriteSources(RegisterSpriteSourcesEvent event) {
        event.register(BedrockParticles.asResource("bedrock_particles"), BedrockSpriteSource.CODEC);
        event.register(BedrockParticles.asResource("bedrock_terrain"), BedrockTerrainSpriteSource.CODEC);
    }

    private void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpecial(BedrockParticles.MOLANG, new MolangParticleInstance.Provider());
    }

    private void clientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(ParticleRegistry::initializeDefaults);
    }

    private void addReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(MolangParticleEngine.RELOADER_ID, MolangParticleEngine.INSTANCE);
        event.addListener(BedrockParticles.asResource("model_particles"), BedrockModelParticles.reloadListener());
    }

    private void clientTick(ClientTickEvent.Pre event) {
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

    private void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        MolangParticleEngine.INSTANCE.removeAll();
        EmitterAttachHandler.clearEmitters();
        EmitterAttachHandler.ignoreRangeEmitters.clear();
        if (BedrockParticles.GECKOLIB_LOADED) {
            com.kltyton.bedrockruntime.client.particle.api.geckolib.GeckoLibHelper.clearReloadCallbacks();
        }
    }

    private static final class NeoClientTransport implements ParticleNetwork.ClientTransport {
        @Override
        public boolean canSend(CustomPacketPayload.Type<?> type) {
            Minecraft minecraft = Minecraft.getInstance();
            return minecraft.getConnection() != null && NetworkRegistry.hasChannel(
                    minecraft.getConnection().getConnection(), ConnectionProtocol.PLAY, type.id());
        }

        @Override
        public void sendToServer(CustomPacketPayload payload) {
            ClientPacketDistributor.sendToServer(payload);
        }
    }
}
