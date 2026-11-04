package com.kltyton.bedrockruntime.particle.network;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import com.kltyton.bedrockruntime.particle.BedrockParticles;

import java.util.Optional;
import java.util.UUID;

public record MolangEntityStatePacketS2C(int entityId, UUID entityUuid, int flags, int itemUseRemaining,
                                         Optional<GlobalPos> recoveryTarget) implements FabricPacket {
    public static final int HAS_TARGET = 1;
    public static final int LOVE_KNOWN = 2;
    public static final int IN_LOVE = 4;
    public static final int INITIAL = 8;
    public static final int TARGET_KNOWN = 16;
    public static final int ITEM_USE_ACTIVE = 32;
    public static final int RECOVERY_KNOWN = 64;
    public static final PacketType<MolangEntityStatePacketS2C> TYPE =
            PacketType.create(BedrockParticles.asResource("molang_entity_state"), MolangEntityStatePacketS2C::new);

    public MolangEntityStatePacketS2C(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readUUID(), buf.readVarInt(), buf.readVarInt(), readGlobalPos(buf));
    }

    private static Optional<GlobalPos> readGlobalPos(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) return Optional.empty();
        ResourceLocation dimension = buf.readResourceLocation();
        return Optional.of(GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dimension), buf.readBlockPos()));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeUUID(entityUuid);
        buf.writeVarInt(flags);
        buf.writeVarInt(itemUseRemaining);
        if (recoveryTarget.isPresent()) {
            buf.writeBoolean(true);
            GlobalPos target = recoveryTarget.get();
            buf.writeResourceLocation(target.dimension().location());
            buf.writeBlockPos(target.pos());
        } else {
            buf.writeBoolean(false);
        }
    }

    @Override
    public PacketType<MolangEntityStatePacketS2C> getType() {
        return TYPE;
    }

    public net.minecraft.network.protocol.Packet<?> toPacket() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        write(buf);
        return ServerPlayNetworking.createS2CPacket(TYPE.getId(), buf);
    }
}
