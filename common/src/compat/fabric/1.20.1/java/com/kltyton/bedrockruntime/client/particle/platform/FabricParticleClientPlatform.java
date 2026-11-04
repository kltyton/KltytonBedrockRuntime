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
import com.kltyton.bedrockruntime.particle.network.MolangEntityStatePacketS2C;
import com.kltyton.bedrockruntime.particle.registry.ParticleRegistry;
import com.kltyton.bedrockruntime.client.particle.model.BedrockModelParticles;
import com.kltyton.bedrockruntime.client.particle.model.loading.FabricGeometryModelLoading;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.server.packs.PackType;

/** Fabric 1.20.1 client particle wiring. */
public final class FabricParticleClientPlatform implements ParticleClientPlatform {
    private static boolean initialized;
    private static ShaderInstance particleNoDiscard;
    private static ShaderInstance particleAlpha;

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
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            context.register(BedrockParticles.asResource("particle_no_discard"),
                    com.mojang.blaze3d.vertex.DefaultVertexFormat.PARTICLE,
                    shader -> particleNoDiscard = shader);
            context.register(BedrockParticles.asResource("particle_alpha_test"),
                    com.mojang.blaze3d.vertex.DefaultVertexFormat.PARTICLE,
                    shader -> particleAlpha = shader);
            com.kltyton.bedrockruntime.client.particle.runtime.render.ParticleRenderLayers.installShaders(
                    () -> particleNoDiscard, () -> particleAlpha, FabricLoader.getInstance().isModLoaded("iris"));
        });
        PSClientConfigs.setConfigDirectory(FabricLoader.getInstance().getConfigDir());
        PSClientConfigs.onLoad();
        BedrockModelParticles.initialize();
        FabricGeometryModelLoading.initialize();
        ParticleRegistry.initializeDefaults();
        ParticleFactoryRegistry.getInstance().register(BedrockParticles.MOLANG, new MolangParticleInstance.Provider());
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new ForwardingParticleReloadListener(
                BedrockParticles.asResource("particle_effects"), MolangParticleEngine.INSTANCE));
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new ForwardingParticleReloadListener(
                BedrockParticles.asResource("particle_models"), BedrockModelParticles.reloadListener()));
        registerClientReceivers();
        ClientTickEvents.START_CLIENT_TICK.register(client -> tick());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clearClientState());
    }

    private static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(EmitterCreationPacketS2C.TYPE, (payload, player, responseSender) ->
                Minecraft.getInstance().execute(() -> PSClientPacketHandler.handleEmitterCreation(payload, player)));
        ClientPlayNetworking.registerGlobalReceiver(EmitterAttachPacketS2C.TYPE, (payload, player, responseSender) ->
                PSClientPacketHandler.handleEmitterAttach(payload, player));
        ClientPlayNetworking.registerGlobalReceiver(EmitterRemovalPacketS2C.TYPE, (payload, player, responseSender) ->
                PSClientPacketHandler.handleEmitterRemoval(payload.id(), player));
        ClientPlayNetworking.registerGlobalReceiver(EmitterRemovalPacket.TYPE, (payload, player, responseSender) ->
                PSClientPacketHandler.handleEmitterRemoval(payload.id(), player));
        ClientPlayNetworking.registerGlobalReceiver(EmitterSynchronizePacket.TYPE, (payload, player, responseSender) ->
                PSClientPacketHandler.handleEmitterSynchronize(payload, player));
        ClientPlayNetworking.registerGlobalReceiver(MolangEntityStatePacketS2C.TYPE, (payload, player, responseSender) ->
                PSClientPacketHandler.handleMolangEntityState(payload, player));
    }

    private static void tick() {
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

    private static void clearClientState() {
        MolangParticleEngine.INSTANCE.removeAll();
        EmitterAttachHandler.clearEmitters();
        EmitterAttachHandler.ignoreRangeEmitters.clear();
        if (BedrockParticles.GECKOLIB_LOADED) {
            com.kltyton.bedrockruntime.client.particle.api.geckolib.GeckoLibHelper.clearReloadCallbacks();
        }
    }
}
