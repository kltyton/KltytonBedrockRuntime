package com.kltyton.bedrockruntime.client.particle.platform;

import com.kltyton.bedrockruntime.client.particle.PSClientConfigs;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockBlockLight;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockSpriteSource;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockTerrainSpriteSource;
import com.kltyton.bedrockruntime.client.particle.network.PSClientPacketHandler;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleEngine;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.runtime.attachment.EmitterAttachHandler;
import com.kltyton.bedrockruntime.mixin.client.particle.SpriteSourcesAccessor;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.particle.network.EmitterAttachPacketS2C;
import com.kltyton.bedrockruntime.particle.network.EmitterCreationPacketS2C;
import com.kltyton.bedrockruntime.particle.network.EmitterRemovalPacket;
import com.kltyton.bedrockruntime.particle.network.EmitterRemovalPacketS2C;
import com.kltyton.bedrockruntime.particle.network.EmitterSynchronizePacket;
import com.kltyton.bedrockruntime.particle.network.EmitterSynchronizePacketS2C;
import com.kltyton.bedrockruntime.particle.network.MolangEntityStatePacketS2C;
import com.kltyton.bedrockruntime.particle.network.ParticleForgeNetworking;
import com.kltyton.bedrockruntime.particle.registry.ParticleRegistry;
import com.kltyton.bedrockruntime.client.particle.model.BedrockModelParticles;
import com.kltyton.bedrockruntime.client.particle.model.loading.ForgeGeometryModelLoading;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ShaderInstance;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/** Forge 1.20.1 client particle wiring. */
public final class ForgeParticleClientPlatform implements ParticleClientPlatform {
    private final IEventBus eventBus;
    private static ShaderInstance particleNoDiscard;
    private static ShaderInstance particleAlpha;
    private boolean initialized;

    public ForgeParticleClientPlatform(IEventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void initializeClient() {
        if (initialized) return;
        initialized = true;
        SpriteSourceType bedrock = new SpriteSourceType(BedrockSpriteSource.codec());
        SpriteSourceType terrain = new SpriteSourceType(BedrockTerrainSpriteSource.CODEC);
        if (SpriteSourcesAccessor.kltyton_bedrock_runtime$getTypes()
                .putIfAbsent(BedrockParticles.asResource("bedrock_particles"), bedrock) != null) {
            throw new IllegalStateException("Duplicate Bedrock particle sprite source registration");
        }
        if (SpriteSourcesAccessor.kltyton_bedrock_runtime$getTypes()
                .putIfAbsent(BedrockParticles.asResource("bedrock_terrain"), terrain) != null) {
            throw new IllegalStateException("Duplicate Bedrock terrain sprite source registration");
        }
        BedrockSpriteSource.bindType(bedrock);
        BedrockTerrainSpriteSource.bindType(terrain);
        BedrockBlockLight.initialize();
        PSClientConfigs.setConfigDirectory(FMLPaths.CONFIGDIR.get());
        PSClientConfigs.onLoad();
        BedrockModelParticles.initialize();
        ForgeGeometryModelLoading.register(eventBus);
        eventBus.addListener(this::clientSetup);
        eventBus.addListener(this::registerShaders);
        eventBus.addListener(this::registerParticleProviders);
        eventBus.addListener(this::addReloadListener);
        MinecraftForge.EVENT_BUS.addListener(this::clientTick);
        ParticleForgeNetworking.setClientConsumer((payload, context) -> Minecraft.getInstance().execute(() -> {
            if (payload instanceof EmitterCreationPacketS2C creation) {
                PSClientPacketHandler.handleEmitterCreation(creation, Minecraft.getInstance().player);
            } else if (payload instanceof EmitterAttachPacketS2C attach) {
                PSClientPacketHandler.handleEmitterAttach(attach, Minecraft.getInstance().player);
            } else if (payload instanceof EmitterRemovalPacketS2C removal) {
                PSClientPacketHandler.handleEmitterRemoval(removal.id(), Minecraft.getInstance().player);
            } else if (payload instanceof EmitterRemovalPacket removal) {
                PSClientPacketHandler.handleEmitterRemoval(removal.id(), Minecraft.getInstance().player);
            } else if (payload instanceof EmitterSynchronizePacket synchronize) {
                PSClientPacketHandler.handleEmitterSynchronize(synchronize, Minecraft.getInstance().player);
            } else if (payload instanceof EmitterSynchronizePacketS2C synchronize) {
                PSClientPacketHandler.handleEmitterSynchronize(
                        new EmitterSynchronizePacket(synchronize.id(), synchronize.tag()), Minecraft.getInstance().player);
            } else if (payload instanceof MolangEntityStatePacketS2C state) {
                PSClientPacketHandler.handleMolangEntityState(state, Minecraft.getInstance().player);
            }
            context.get().setPacketHandled(true);
        }));
    }

    private void clientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(ParticleRegistry::initializeDefaults);
    }

    private void registerShaders(RegisterShadersEvent event) {
        try {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                        BedrockParticles.asResource("particle_no_discard"), DefaultVertexFormat.PARTICLE),
                shader -> particleNoDiscard = shader);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                        BedrockParticles.asResource("particle_alpha_test"), DefaultVertexFormat.PARTICLE),
                shader -> particleAlpha = shader);
        com.kltyton.bedrockruntime.client.particle.runtime.render.ParticleRenderLayers.installShaders(
                () -> particleNoDiscard, () -> particleAlpha,
                net.minecraftforge.fml.ModList.get().isLoaded("iris"));
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException("Cannot load KBR particle shaders", exception);
        }
    }

    private void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpecial(BedrockParticles.MOLANG, new MolangParticleInstance.Provider());
    }

    private void addReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(MolangParticleEngine.INSTANCE);
        event.registerReloadListener(BedrockModelParticles.reloadListener());
    }

    private void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            MolangParticleEngine.INSTANCE.removeAll();
        } else if (!minecraft.isPaused()) {
            MolangParticleEngine.INSTANCE.tick(player);
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
}
