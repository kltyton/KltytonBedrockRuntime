package com.kltyton.bedrockruntime.particle.network;

import com.kltyton.bedrockruntime.client.particle.access.IPlayerPersistentData;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public record EmitterSynchronizePacket(int id, CompoundTag tag) {
    public static final String KEY = BedrockParticles.MODID + ":emitters";

    public EmitterSynchronizePacket(FriendlyByteBuf buf) {
        this(buf.readInt(), java.util.Objects.requireNonNull(buf.readNbt(), "Missing emitter data"));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(id);
        buf.writeNbt(tag);
    }

    public static void syncToClient(ServerPlayer player, int id) {
        CompoundTag emitters = getEmitterData(player, false);
        if (emitters.contains(Integer.toString(id))) {
            EmitterSynchronizePacketS2C.sendToClient(player, id, emitters.getCompound(Integer.toString(id)));
        } else {
            BedrockParticles.LOGGER.warn("No persisted emitter {} for player {}", id, player.getGameProfile());
        }
    }

    public static void syncSavedEmitters(ServerPlayer player) {
        CompoundTag emitters = getEmitterData(player, false);
        for (String id : emitters.getAllKeys()) {
            try {
                EmitterSynchronizePacketS2C.sendToClient(player, Integer.parseInt(id), emitters.getCompound(id));
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
