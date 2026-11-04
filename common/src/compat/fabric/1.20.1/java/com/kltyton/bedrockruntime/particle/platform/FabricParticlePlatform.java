package com.kltyton.bedrockruntime.particle.platform;

import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.particle.registry.ModelParticleTypes;
import com.kltyton.bedrockruntime.particle.network.EmitterRemovalPacket;
import com.kltyton.bedrockruntime.particle.network.EmitterSynchronizePacket;
import com.kltyton.bedrockruntime.particle.network.MolangEntityStatePacketS2C;
import com.kltyton.bedrockruntime.particle.network.ParticleServerPacketHandler;
import com.kltyton.bedrockruntime.particle.registry.MolangEntityStateSnapshot;
import com.kltyton.bedrockruntime.particle.registry.MolangEntityStateTracker;
import com.kltyton.bedrockruntime.particle.registry.ParticleEntityStateSink;
import com.kltyton.bedrockruntime.particle.command.BedrockParticleCommand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;

import java.util.Set;

/** Fabric 1.20.1 common/server particle wiring. */
public final class FabricParticlePlatform implements ParticlePlatform {
    private static boolean commonInitialized;

    @Override
    public void initializeCommon() {
        if (commonInitialized) return;
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                BedrockParticleCommand.register(dispatcher));
        commonInitialized = true;
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, BedrockParticles.asResource("molang"), BedrockParticles.MOLANG);
        ModelParticleTypes.registerParticleType(BuiltInRegistries.PARTICLE_TYPE);
        ServerPlayNetworking.registerGlobalReceiver(EmitterRemovalPacket.TYPE, (payload, player, responseSender) ->
                ParticleServerPacketHandler.handleRemoval(player, payload));
        ServerPlayNetworking.registerGlobalReceiver(EmitterSynchronizePacket.TYPE, (payload, player, responseSender) ->
                ParticleServerPacketHandler.handleSynchronize(player, payload));
        MolangEntityStateTracker.install(new FabricEntityStateSink());
        ServerPlayConnectionEvents.JOIN.register((listener, sender, server) ->
                EmitterSynchronizePacket.syncSavedEmitters(listener.getPlayer()));
        ServerEntityEvents.ENTITY_LOAD.register(MolangEntityStateTracker::onEntityLoad);
        ServerEntityEvents.ENTITY_UNLOAD.register(MolangEntityStateTracker::onEntityUnload);
        ServerLifecycleEvents.SERVER_STOPPED.register(MolangEntityStateTracker::onServerStopped);
        ServerTickEvents.END_SERVER_TICK.register(MolangEntityStateTracker::tick);
    }

    private static final class FabricEntityStateSink implements ParticleEntityStateSink {
        @Override
        public boolean canSend(ServerPlayer player) {
            return ServerPlayNetworking.canSend(player, MolangEntityStatePacketS2C.TYPE);
        }

        @Override
        public boolean isTracking(ServerLevel level, LivingEntity entity, ServerPlayer player) {
            return level.getChunkSource().chunkMap.getPlayers(new ChunkPos(entity.blockPosition()), false)
                    .contains(player);
        }

        @Override
        public void sendToPlayer(ServerPlayer player, MolangEntityStateSnapshot snapshot) {
            if (canSend(player)) {
                ServerPlayNetworking.send(player, new MolangEntityStatePacketS2C(snapshot.entityId(),
                        snapshot.entityUuid(), snapshot.flags(), snapshot.itemUseRemaining(),
                        snapshot.recoveryTarget()));
            }
        }

        @Override
        public void sendToTracking(ServerLevel level, LivingEntity entity, MolangEntityStateSnapshot snapshot,
                                   Set<ServerPlayer> trackedPlayers) {
            for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(
                    new ChunkPos(entity.blockPosition()), false)) {
                if ((trackedPlayers.isEmpty() || trackedPlayers.contains(player)) && canSend(player)) {
                    sendToPlayer(player, snapshot);
                }
            }
        }
    }
}
