package com.kltyton.bedrockruntime.particle.platform;

import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.particle.registry.ModelParticleTypes;
import com.kltyton.bedrockruntime.particle.network.EmitterAttachPacketS2C;
import com.kltyton.bedrockruntime.particle.network.EmitterCreationPacketS2C;
import com.kltyton.bedrockruntime.particle.network.EmitterRemovalPacket;
import com.kltyton.bedrockruntime.particle.network.EmitterRemovalPacketS2C;
import com.kltyton.bedrockruntime.particle.network.EmitterSynchronizePacket;
import com.kltyton.bedrockruntime.particle.network.MolangEntityStatePacketS2C;
import com.kltyton.bedrockruntime.particle.network.ParticleNetwork;
import com.kltyton.bedrockruntime.particle.network.ParticleServerPacketHandler;
import com.kltyton.bedrockruntime.particle.registry.MolangEntityStateSnapshot;
import com.kltyton.bedrockruntime.particle.registry.MolangEntityStateTracker;
import com.kltyton.bedrockruntime.particle.registry.ParticleEntityStateSink;
import com.kltyton.bedrockruntime.particle.command.BedrockParticleCommand;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;
import java.util.function.Predicate;

/** NeoForge 26.1.2/26.2 common/server particle wiring. */
public final class NeoForgeParticlePlatform implements ParticlePlatform {
    private static boolean commonInitialized;
    private final IEventBus eventBus;
    private final DeferredRegister<ParticleType<?>> particleTypes;

    public NeoForgeParticlePlatform(IEventBus eventBus) {
        this.eventBus = eventBus;
        this.particleTypes = DeferredRegister.create(Registries.PARTICLE_TYPE, BedrockParticles.MODID);
        this.particleTypes.register("molang", () -> BedrockParticles.MOLANG);
        this.particleTypes.register(ModelParticleTypes.ID_PATH, ModelParticleTypes::modelParticleType);
        this.particleTypes.register(eventBus);
    }

    @Override
    public void initializeCommon() {
        if (commonInitialized) return;
        commonInitialized = true;
        eventBus.addListener(this::registerPayloads);
        ParticleNetwork.installServer(new NeoServerTransport());
        MolangEntityStateTracker.install(new NeoEntityStateSink());
        NeoForge.EVENT_BUS.addListener(this::playerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::startTracking);
        NeoForge.EVENT_BUS.addListener(this::stopTracking);
        NeoForge.EVENT_BUS.addListener(this::playerLoggedOut);
        NeoForge.EVENT_BUS.addListener(this::entityLeft);
        NeoForge.EVENT_BUS.addListener(this::serverStopped);
        NeoForge.EVENT_BUS.addListener(this::serverTick);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        BedrockParticleCommand.register(event.getDispatcher());
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(EmitterCreationPacketS2C.TYPE, EmitterCreationPacketS2C.STREAM_CODEC)
                .playToClient(EmitterAttachPacketS2C.TYPE, EmitterAttachPacketS2C.STREAM_CODEC)
                .playToClient(EmitterRemovalPacketS2C.TYPE, EmitterRemovalPacketS2C.STREAM_CODEC)
                .playBidirectional(EmitterRemovalPacket.TYPE, EmitterRemovalPacket.STREAM_CODEC,
                        (payload, context) -> ParticleServerPacketHandler.handleRemoval((ServerPlayer) context.player(), payload),
                        null)
                .playBidirectional(EmitterSynchronizePacket.TYPE, EmitterSynchronizePacket.STREAM_CODEC,
                        (payload, context) -> ParticleServerPacketHandler.handleSynchronize((ServerPlayer) context.player(), payload),
                        null);
        event.registrar("1").optional().playToClient(MolangEntityStatePacketS2C.TYPE,
                MolangEntityStatePacketS2C.STREAM_CODEC);
    }

    private void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            EmitterSynchronizePacket.syncSavedEmitters(player);
        }
    }

    private void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MolangEntityStateTracker.onStartTracking(player, event.getTarget());
        }
    }

    private void stopTracking(PlayerEvent.StopTracking event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MolangEntityStateTracker.onStopTracking(player, event.getTarget());
        }
    }

    private void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MolangEntityStateTracker.onPlayerLoggedOut(player);
        }
    }

    private void entityLeft(EntityLeaveLevelEvent event) {
        MolangEntityStateTracker.onEntityLeft(event.getEntity());
    }

    private void serverStopped(ServerStoppedEvent event) {
        MolangEntityStateTracker.onServerStopped(event.getServer());
    }

    private void serverTick(ServerTickEvent.Post event) {
        MolangEntityStateTracker.tick(event.getServer());
    }

    private static final class NeoServerTransport implements ParticleNetwork.ServerTransport {
        @Override
        public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
            return player.connection.hasChannel(type);
        }

        @Override
        public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
            PacketDistributor.sendToPlayer(player, payload);
        }

        @Override
        public void sendToTracking(ServerLevel level, Entity entity, CustomPacketPayload payload,
                                   Predicate<ServerPlayer> filter) {
            level.getChunkSource().chunkMap.sendToTrackingPlayersFiltered(entity,
                    new ClientboundCustomPayloadPacket(payload),
                    player -> filter.test(player) && canSend(player, payload.type()));
        }
    }

    private static final class NeoEntityStateSink implements ParticleEntityStateSink {
        @Override
        public boolean canSend(ServerPlayer player) {
            return player.connection.hasChannel(MolangEntityStatePacketS2C.TYPE);
        }

        @Override
        public boolean isTracking(ServerLevel level, LivingEntity entity, ServerPlayer player) {
            return level.getChunkSource().chunkMap.getPlayers(ChunkPos.containing(entity.blockPosition()), false)
                    .contains(player);
        }

        @Override
        public void sendToPlayer(ServerPlayer player, MolangEntityStateSnapshot snapshot) {
            if (canSend(player)) {
                PacketDistributor.sendToPlayer(player, new MolangEntityStatePacketS2C(snapshot.entityId(),
                        snapshot.entityUuid(), snapshot.flags(), snapshot.itemUseRemaining(),
                        snapshot.recoveryTarget()));
            }
        }

        @Override
        public void sendToTracking(ServerLevel level, LivingEntity entity, MolangEntityStateSnapshot snapshot,
                                   Set<ServerPlayer> trackedPlayers) {
            for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(
                    ChunkPos.containing(entity.blockPosition()), false)) {
                if ((trackedPlayers.isEmpty() || trackedPlayers.contains(player)) && canSend(player)) {
                    sendToPlayer(player, snapshot);
                }
            }
        }
    }
}
