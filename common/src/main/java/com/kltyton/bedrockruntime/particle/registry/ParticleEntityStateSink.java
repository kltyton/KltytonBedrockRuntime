package com.kltyton.bedrockruntime.particle.registry;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

/** Loader-specific transport used by {@link MolangEntityStateTracker}. */
public interface ParticleEntityStateSink {
    boolean canSend(ServerPlayer player);

    boolean isTracking(ServerLevel level, LivingEntity entity, ServerPlayer player);

    void sendToPlayer(ServerPlayer player, MolangEntityStateSnapshot snapshot);

    /**
     * Sends to the supplied explicit tracking players, or to all players tracking the entity's
     * chunk when {@code trackedPlayers} is empty.
     */
    void sendToTracking(ServerLevel level, LivingEntity entity, MolangEntityStateSnapshot snapshot,
                        Set<ServerPlayer> trackedPlayers);
}
