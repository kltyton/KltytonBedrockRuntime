package com.kltyton.bedrockruntime.particle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record EmitterRemovalPacketS2C(int id) {
    public EmitterRemovalPacketS2C(FriendlyByteBuf buf) {
        this(buf.readVarInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(id);
    }

    public static void sendToClient(ServerPlayer player, int id) {
        EmitterSynchronizePacket.getEmitterData(player, false).remove(Integer.toString(id));
        ParticleForgeNetworking.sendToPlayer(player, new EmitterRemovalPacketS2C(id));
    }
}
