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
import com.kltyton.bedrockruntime.particle.network.ParticleClientDispatch;
import com.kltyton.bedrockruntime.particle.network.ParticleNetwork;
import com.kltyton.bedrockruntime.particle.registry.ParticleRegistry;
import com.kltyton.bedrockruntime.client.particle.model.BedrockModelParticles;
import com.kltyton.bedrockruntime.client.particle.model.loading.NeoForgeGeometryModelLoading;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** NeoForge 1.21.1 client particle wiring. */
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
        eventBus.addListener(this::registerSpriteSourceTypes);
        eventBus.addListener(this::registerParticleProviders);
        eventBus.addListener(this::addReloadListener);
        NeoForge.EVENT_BUS.addListener(this::clientTick);
        NeoForge.EVENT_BUS.addListener(this::loggingOut);
        BedrockBlockLight.initialize();
        PSClientConfigs.setConfigDirectory(FMLPaths.CONFIGDIR.get());
        PSClientConfigs.onLoad();
        BedrockModelParticles.initialize();
        NeoForgeGeometryModelLoading.register(eventBus);
        ParticleNetwork.installClient(new NeoClientTransport());
        ParticleClientDispatch.install((payload, context) ->
                handleClientPayload(payload, (IPayloadContext) context));
    }

    private static void handleClientPayload(Object payload, IPayloadContext context) {
        if (payload instanceof EmitterCreationPacketS2C creation) {
            PSClientPacketHandler.handleEmitterCreation(creation, context.player());
        } else if (payload instanceof EmitterAttachPacketS2C attach) {
            PSClientPacketHandler.handleEmitterAttach(attach, context.player());
        } else if (payload instanceof EmitterRemovalPacketS2C removal) {
            PSClientPacketHandler.handleEmitterRemoval(removal.id(), context.player());
        } else if (payload instanceof EmitterRemovalPacket removal) {
            PSClientPacketHandler.handleEmitterRemoval(removal.id(), context.player());
        } else if (payload instanceof EmitterSynchronizePacket synchronize) {
            PSClientPacketHandler.handleEmitterSynchronize(synchronize, context.player());
        } else if (payload instanceof MolangEntityStatePacketS2C state) {
            PSClientPacketHandler.handleMolangEntityState(state, context.player());
        } else {
            throw new IllegalArgumentException("Unhandled particle payload: " + payload.getClass().getName());
        }
    }

    private void registerSpriteSourceTypes(RegisterSpriteSourceTypesEvent event) {
        SpriteSourceType bedrock = event.register(BedrockParticles.asResource("bedrock_particles"),
                BedrockSpriteSource.CODEC);
        SpriteSourceType terrain = event.register(BedrockParticles.asResource("bedrock_terrain"),
                BedrockTerrainSpriteSource.CODEC);
        BedrockSpriteSource.bindType(bedrock);
        BedrockTerrainSpriteSource.bindType(terrain);
    }

    private void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpecial(BedrockParticles.MOLANG, new MolangParticleInstance.Provider());
    }

    private void addReloadListener(RegisterClientReloadListenersEvent event) {
        ParticleRegistry.initializeDefaults();
        event.registerReloadListener(MolangParticleEngine.INSTANCE);
        event.registerReloadListener(BedrockModelParticles.reloadListener());
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
                Camera camera = minecraft.gameRenderer.getMainCamera();
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
            PacketDistributor.sendToServer(payload);
        }
    }
}
