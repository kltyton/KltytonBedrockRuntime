package com.kltyton.bedrockruntime.particle.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import com.kltyton.bedrockruntime.particle.BedrockParticles;

public record EmitterRemovalPacketS2C(int id) implements CustomPacketPayload {
    public static final Type<EmitterRemovalPacketS2C> TYPE = new Type<>(BedrockParticles.asResource("emitter_removal_s2c"));
    public static final StreamCodec<ByteBuf, EmitterRemovalPacketS2C> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EmitterRemovalPacketS2C::id,
            EmitterRemovalPacketS2C::new
    );

    @Override
    public Type<EmitterRemovalPacketS2C> type() {
        return TYPE;
    }

    public static void sendToClient(ServerPlayer player, int id) {
        EmitterSynchronizePacket.getEmitterData(player, false).remove(Integer.toString(id));
        ParticleNetwork.sendToPlayer(player, new EmitterRemovalPacketS2C(id));
    }

}
