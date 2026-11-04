package com.kltyton.bedrockruntime.particle.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** Server-side handling shared by all loader payload adapters. */
public final class ParticleServerPacketHandler {
    private ParticleServerPacketHandler() {
    }

    public static void handleRemoval(ServerPlayer player, EmitterRemovalPacket payload) {
        EmitterSynchronizePacket.getEmitterData(player, false).remove(Integer.toString(payload.id()));
    }

    public static void handleSynchronize(ServerPlayer player, EmitterSynchronizePacket payload) {
        CompoundTag emitters = EmitterSynchronizePacket.getEmitterData(player, true);
        emitters.put(Integer.toString(payload.id()), payload.tag().copy());
    }
}
