package com.kltyton.bedrockruntime.particle.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import com.kltyton.bedrockruntime.particle.BedrockParticles;

public record EmitterAttachPacketS2C(int particleId, int entityId) implements CustomPacketPayload {
    public static final Type<EmitterAttachPacketS2C> TYPE = new Type<>(BedrockParticles.asResource("emitter_attach"));
    public static final StreamCodec<ByteBuf, EmitterAttachPacketS2C> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, p -> p.particleId,
            ByteBufCodecs.INT, p -> p.entityId,
            EmitterAttachPacketS2C::new
    );

    @Override
    public Type<EmitterAttachPacketS2C> type() {
        return TYPE;
    }

    public static void sendToClient(ServerPlayer serverPlayer, int particleId, Entity entity) {
        ParticleNetwork.sendToPlayer(serverPlayer, new EmitterAttachPacketS2C(particleId, entity.getId()));
    }
}
