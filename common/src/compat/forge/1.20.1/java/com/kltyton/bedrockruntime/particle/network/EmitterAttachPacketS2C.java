package com.kltyton.bedrockruntime.particle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public record EmitterAttachPacketS2C(int particleId, int entityId) {
    public EmitterAttachPacketS2C(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(particleId);
        buf.writeInt(entityId);
    }

    public static void sendToClient(ServerPlayer player, int particleId, Entity entity) {
        ParticleForgeNetworking.sendToPlayer(player, new EmitterAttachPacketS2C(particleId, entity.getId()));
    }
}
