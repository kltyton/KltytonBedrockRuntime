package com.kltyton.bedrockruntime.particle.network;

import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public record EmitterCreationPacketS2C(ResourceLocation id, Vector3f pos, MolangExp expression, int entityId) {
    public EmitterCreationPacketS2C(FriendlyByteBuf buf) {
        this(buf.readResourceLocation(), new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat()),
                new MolangExp(buf.readUtf()), buf.readVarInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(id);
        buf.writeFloat(pos.x);
        buf.writeFloat(pos.y);
        buf.writeFloat(pos.z);
        buf.writeUtf(expression.getExpStr());
        buf.writeVarInt(entityId);
    }

    public static void sendToAll(ResourceLocation id, Vector3f pos, MolangExp expression, @Nullable Entity entity) {
        if (entity != null && entity.level().getServer() != null) {
            for (ServerPlayer player : entity.level().getServer().getPlayerList().getPlayers()) {
                sendToClient(player, id, pos, expression, entity);
            }
        }
    }

    public static void sendToClient(ServerPlayer player, ResourceLocation id, Vector3f pos,
                                    MolangExp expression, @Nullable Entity entity) {
        BedrockParticles.LOGGER.debug(
                "server sending emitter packet viewer={} particle={} pos=({}, {}, {}) expression={} entity={}",
                player.getScoreboardName(), id, pos.x, pos.y, pos.z,
                expression == null ? "" : expression.getExpStr(),
                entity == null ? "none" : entity.getScoreboardName());
        ParticleForgeNetworking.sendToPlayer(player,
                new EmitterCreationPacketS2C(id, pos, expression, entity == null ? -1 : entity.getId()));
    }
}
