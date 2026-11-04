package com.kltyton.bedrockruntime.particle.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record EmitterSynchronizePacketS2C(int id, CompoundTag tag) {
    public EmitterSynchronizePacketS2C(FriendlyByteBuf buf) {
        this(buf.readInt(), java.util.Objects.requireNonNull(buf.readNbt(), "Missing emitter data"));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(id);
        buf.writeNbt(tag);
    }

    public static void sendToClient(ServerPlayer player, int id, CompoundTag tag) {
        ParticleForgeNetworking.sendToPlayer(player, new EmitterSynchronizePacketS2C(id, tag));
    }
}
