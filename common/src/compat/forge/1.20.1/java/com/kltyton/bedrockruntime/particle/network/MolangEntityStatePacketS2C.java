package com.kltyton.bedrockruntime.particle.network;

import com.kltyton.bedrockruntime.particle.registry.MolangEntityStateFlags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.UUID;

public record MolangEntityStatePacketS2C(int entityId, UUID entityUuid, int flags, int itemUseRemaining,
                                         Optional<GlobalPos> recoveryTarget) {
    public static final int HAS_TARGET = MolangEntityStateFlags.HAS_TARGET;
    public static final int LOVE_KNOWN = MolangEntityStateFlags.LOVE_KNOWN;
    public static final int IN_LOVE = MolangEntityStateFlags.IN_LOVE;
    public static final int INITIAL = MolangEntityStateFlags.INITIAL;
    public static final int TARGET_KNOWN = MolangEntityStateFlags.TARGET_KNOWN;
    public static final int ITEM_USE_ACTIVE = MolangEntityStateFlags.ITEM_USE_ACTIVE;
    public static final int RECOVERY_KNOWN = MolangEntityStateFlags.RECOVERY_KNOWN;

    public MolangEntityStatePacketS2C(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readUUID(), buf.readVarInt(), buf.readVarInt(), readGlobalPos(buf));
    }

    private static Optional<GlobalPos> readGlobalPos(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) {
            return Optional.empty();
        }
        ResourceLocation dimension = buf.readResourceLocation();
        return Optional.of(GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dimension), buf.readBlockPos()));
    }

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
}
