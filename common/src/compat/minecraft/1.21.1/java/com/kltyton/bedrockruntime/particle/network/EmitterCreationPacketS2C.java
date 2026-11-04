package com.kltyton.bedrockruntime.particle.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;

public record EmitterCreationPacketS2C(ResourceLocation id, Vector3f pos, MolangExp expression, int entityId) implements CustomPacketPayload {
    public static final Type<EmitterCreationPacketS2C> TYPE = new Type<>(BedrockParticles.asResource("emitter_creation"));
    private static final StreamCodec<ByteBuf, Vector3f> VECTOR3F_CODEC = ByteBufCodecs.VECTOR3F.map(Vector3f::new, value -> value);

    public static final StreamCodec<ByteBuf, EmitterCreationPacketS2C> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, EmitterCreationPacketS2C::id,
            VECTOR3F_CODEC, EmitterCreationPacketS2C::pos,
            MolangExp.STREAM_CODEC, EmitterCreationPacketS2C::expression,
            ByteBufCodecs.VAR_INT, EmitterCreationPacketS2C::entityId,
            EmitterCreationPacketS2C::new
    );

    @Override
    public Type<EmitterCreationPacketS2C> type() {
        return TYPE;
    }

    public static void sendToAll(ResourceLocation id, Vector3f pos, MolangExp expression, @Nullable Entity entity) {
        if (entity != null && entity.level().getServer() != null) {
            for (ServerPlayer player : entity.level().getServer().getPlayerList().getPlayers()) {
                sendToClient(player, id, pos, expression, entity);
            }
        }
    }

    public static void sendToClient(ServerPlayer player, ResourceLocation id, Vector3f pos, MolangExp expression, @Nullable Entity entity) {
        BedrockParticles.LOGGER.debug("server sending emitter packet viewer={} particle={} pos=({}, {}, {}) expression={} entity={}",
                player.getScoreboardName(),
                id,
                pos.x,
                pos.y,
                pos.z,
                expression == null ? "" : expression.getExpStr(),
                entity == null ? "none" : entity.getScoreboardName()
        );
        ParticleNetwork.sendToPlayer(player, new EmitterCreationPacketS2C(id, pos, expression, entity == null ? -1 : entity.getId()));
    }
}
