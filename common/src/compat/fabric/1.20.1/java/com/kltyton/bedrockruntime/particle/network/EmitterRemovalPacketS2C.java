package com.kltyton.bedrockruntime.particle.network;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import com.kltyton.bedrockruntime.particle.BedrockParticles;

public record EmitterRemovalPacketS2C(int id) implements FabricPacket {
    public static final PacketType<EmitterRemovalPacketS2C> TYPE = PacketType.create(
            BedrockParticles.asResource("emitter_removal_s2c"), EmitterRemovalPacketS2C::new);

    public EmitterRemovalPacketS2C(FriendlyByteBuf buf) {
        this(buf.readVarInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(id);
    }

    @Override
    public PacketType<EmitterRemovalPacketS2C> getType() {
        return TYPE;
    }

    public static void sendToClient(ServerPlayer player, int id) {
        EmitterSynchronizePacket.getEmitterData(player, false).remove(Integer.toString(id));
        ServerPlayNetworking.send(player, new EmitterRemovalPacketS2C(id));
    }
}
