package com.kltyton.bedrockruntime.particle.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.access.IPlayerPersistentData;

public record EmitterSynchronizePacket(int id, CompoundTag tag) implements CustomPacketPayload {
    public static final Type<EmitterSynchronizePacket> TYPE = new Type<>(BedrockParticles.asResource("emitter_synchronize"));

    public static final StreamCodec<ByteBuf, EmitterSynchronizePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, p -> p.id,
            ByteBufCodecs.COMPOUND_TAG, p -> p.tag,
            EmitterSynchronizePacket::new
    );
    public static final String KEY = BedrockParticles.MODID + ":emitters";

    @Override
    public Type<EmitterSynchronizePacket> type() {
        return TYPE;
    }

    public static void syncToClient(ServerPlayer player, int id) {
        CompoundTag emitters = getEmitterData(player, false);
        if (emitters.contains(Integer.toString(id))) {
            ParticleNetwork.sendToPlayer(player, new EmitterSynchronizePacket(id, emitters.getCompound(Integer.toString(id))));
        } else {
            BedrockParticles.LOGGER.warn("No persisted emitter {} for player {}", id, player.getGameProfile());
        }
    }

    public static void syncSavedEmitters(ServerPlayer player) {
        CompoundTag emitters = getEmitterData(player, false);
        for (String id : emitters.getAllKeys()) {
            try {
                ParticleNetwork.sendToPlayer(player, new EmitterSynchronizePacket(Integer.parseInt(id), emitters.getCompound(id)));
            } catch (NumberFormatException exception) {
                BedrockParticles.LOGGER.warn("Invalid persisted emitter id '{}' for player {}", id, player.getGameProfile());
            }
        }
    }

    public static CompoundTag getEmitterData(Player player, boolean create) {
        CompoundTag persistentData = IPlayerPersistentData.of(player).kltyton_bedrock_runtime$getPersistentData();
        if (!persistentData.contains(KEY) && create) {
            persistentData.put(KEY, new CompoundTag());
        }
        return persistentData.getCompound(KEY);
    }
}
