package com.kltyton.bedrockruntime.particle.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.access.IPlayerPersistentData;

public record EmitterSynchronizePacket(int id, CompoundTag tag) implements FabricPacket {
    public static final PacketType<EmitterSynchronizePacket> TYPE = PacketType.create(BedrockParticles.asResource("emitter_synchronize"), EmitterSynchronizePacket::new);
    public static final String KEY = BedrockParticles.MODID + ":emitters";


    public EmitterSynchronizePacket(FriendlyByteBuf buf) {
        this(buf.readInt(), java.util.Objects.requireNonNull(buf.readNbt(), "Missing emitter data"));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(id);
        buf.writeNbt(tag);
    }

    @Override
    public PacketType<EmitterSynchronizePacket> getType() {
        return TYPE;
    }

    public static void handleServer(EmitterSynchronizePacket payload, ServerPlayer player, PacketSender responseSender) {
        CompoundTag emitters = getEmitterData(player, true);
        emitters.put(Integer.toString(payload.id), payload.tag.copy());
    }

    public static void syncToClient(ServerPlayer player, int id) {
        CompoundTag emitters = getEmitterData(player, false);
        if (emitters.contains(Integer.toString(id))) {
            ServerPlayNetworking.send(player, new EmitterSynchronizePacket(id, emitters.getCompound(Integer.toString(id))));
        } else {
            BedrockParticles.LOGGER.warn("No persisted emitter {} for player {}", id, player.getGameProfile());
        }
    }

    public static void syncSavedEmitters(ServerPlayer player) {
        CompoundTag emitters = getEmitterData(player, false);
        for (String id : emitters.getAllKeys()) {
            try {
                ServerPlayNetworking.send(player, new EmitterSynchronizePacket(Integer.parseInt(id), emitters.getCompound(id)));
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
