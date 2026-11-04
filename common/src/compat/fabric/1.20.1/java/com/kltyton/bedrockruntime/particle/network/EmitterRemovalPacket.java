package com.kltyton.bedrockruntime.particle.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import com.kltyton.bedrockruntime.particle.BedrockParticles;

public record EmitterRemovalPacket(int id) implements FabricPacket {
    public static final PacketType<EmitterRemovalPacket> TYPE = PacketType.create(BedrockParticles.asResource("emitter_removal"), EmitterRemovalPacket::new);

    public EmitterRemovalPacket(FriendlyByteBuf buf) {
        this(buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(id);
    }

    @Override
    public PacketType<EmitterRemovalPacket> getType() {
        return TYPE;
    }

    public static void handleServer(EmitterRemovalPacket payload, ServerPlayer player, PacketSender responseSender) {
        EmitterSynchronizePacket.getEmitterData(player, false).remove(Integer.toString(payload.id));
    }

    public static void sendToClient(ServerPlayer player, int id) {
        EmitterSynchronizePacket.getEmitterData(player, false).remove(Integer.toString(id));
        ServerPlayNetworking.send(player, new EmitterRemovalPacket(id));
    }
}
