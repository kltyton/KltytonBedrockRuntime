package com.kltyton.bedrockruntime.particle.platform;

import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.particle.registry.ModelParticleTypes;
import com.kltyton.bedrockruntime.particle.network.EmitterSynchronizePacket;
import com.kltyton.bedrockruntime.particle.network.MolangEntityStatePacketS2C;
import com.kltyton.bedrockruntime.particle.network.ParticleForgeNetworking;
import com.kltyton.bedrockruntime.particle.registry.MolangEntityStateSnapshot;
import com.kltyton.bedrockruntime.particle.registry.MolangEntityStateTracker;
import com.kltyton.bedrockruntime.particle.registry.ParticleEntityStateSink;
import com.kltyton.bedrockruntime.particle.command.BedrockParticleCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

import java.util.Set;

/** Forge 1.20.1 common/server particle wiring. */
public final class ForgeParticlePlatform implements ParticlePlatform {
    private static boolean commonInitialized;
    private final net.minecraftforge.registries.DeferredRegister<net.minecraft.core.particles.ParticleType<?>> particleTypes;

    public ForgeParticlePlatform(net.minecraftforge.eventbus.api.IEventBus eventBus) {
        particleTypes = net.minecraftforge.registries.DeferredRegister.create(
                net.minecraft.core.registries.Registries.PARTICLE_TYPE, BedrockParticles.MODID);
        particleTypes.register("molang", () -> BedrockParticles.MOLANG);
        particleTypes.register(ModelParticleTypes.ID_PATH, ModelParticleTypes::modelParticleType);
        particleTypes.register(eventBus);
    }

    @Override
    public void initializeCommon() {
        if (commonInitialized) return;
        commonInitialized = true;
        ParticleForgeNetworking.register();
        MolangEntityStateTracker.install(new ForgeEntityStateSink());
        MinecraftForge.EVENT_BUS.addListener(this::playerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(this::startTracking);
        MinecraftForge.EVENT_BUS.addListener(this::stopTracking);
        MinecraftForge.EVENT_BUS.addListener(this::playerLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(this::entityLeft);
        MinecraftForge.EVENT_BUS.addListener(this::serverStopped);
        MinecraftForge.EVENT_BUS.addListener(this::serverTick);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        BedrockParticleCommand.register(event.getDispatcher());
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

    private void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            MolangEntityStateTracker.tick(event.getServer());
        }
    }

    private static final class ForgeEntityStateSink implements ParticleEntityStateSink {
        @Override
        public boolean canSend(ServerPlayer player) {
            return ParticleForgeNetworking.canSendTo(player);
        }

        @Override
        public boolean isTracking(ServerLevel level, LivingEntity entity, ServerPlayer player) {
            return level.getChunkSource().chunkMap.getPlayers(new ChunkPos(entity.blockPosition()), false)
                    .contains(player);
        }

        @Override
        public void sendToPlayer(ServerPlayer player, MolangEntityStateSnapshot snapshot) {
            if (canSend(player)) {
                ParticleForgeNetworking.sendToPlayer(player, new MolangEntityStatePacketS2C(snapshot.entityId(),
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
