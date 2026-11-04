package com.kltyton.bedrockruntime.particle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record EmitterRemovalPacket(int id) {
    public EmitterRemovalPacket(FriendlyByteBuf buf) {
        this(buf.readInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(id);
    }

    public static void sendToClient(ServerPlayer player, int id) {
        EmitterRemovalPacketS2C.sendToClient(player, id);
    }
}
