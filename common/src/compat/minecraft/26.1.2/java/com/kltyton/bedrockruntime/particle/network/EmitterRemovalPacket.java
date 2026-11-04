package com.kltyton.bedrockruntime.particle.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import com.kltyton.bedrockruntime.particle.BedrockParticles;

public record EmitterRemovalPacket(int id) implements CustomPacketPayload {
    public static final Type<EmitterRemovalPacket> TYPE = new Type<>(BedrockParticles.asResource("emitter_removal"));
    public static final StreamCodec<ByteBuf, EmitterRemovalPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, EmitterRemovalPacket::id,
            EmitterRemovalPacket::new
    );

    @Override
    public Type<EmitterRemovalPacket> type() {
        return TYPE;
    }

    public static void sendToClient(ServerPlayer player, int id) {
        EmitterSynchronizePacket.getEmitterData(player, false).remove(Integer.toString(id));
        ParticleNetwork.sendToPlayer(player, new EmitterRemovalPacket(id));
    }
}
