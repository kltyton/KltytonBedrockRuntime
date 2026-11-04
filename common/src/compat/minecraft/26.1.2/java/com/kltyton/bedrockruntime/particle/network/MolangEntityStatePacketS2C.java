package com.kltyton.bedrockruntime.particle.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.kltyton.bedrockruntime.particle.BedrockParticles;

import java.util.UUID;
import java.util.Optional;

public record MolangEntityStatePacketS2C(int entityId, UUID entityUuid, int flags, int itemUseRemaining,
                                         Optional<GlobalPos> recoveryTarget) implements CustomPacketPayload {
    public static final int HAS_TARGET = 1;
    public static final int LOVE_KNOWN = 2;
    public static final int IN_LOVE = 4;
    public static final int INITIAL = 8;
    public static final int TARGET_KNOWN = 16;
    public static final int ITEM_USE_ACTIVE = 32;
    public static final int RECOVERY_KNOWN = 64;
    public static final Type<MolangEntityStatePacketS2C> TYPE = new Type<>(BedrockParticles.asResource("molang_entity_state"));
    public static final StreamCodec<ByteBuf, MolangEntityStatePacketS2C> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MolangEntityStatePacketS2C::entityId,
            UUIDUtil.STREAM_CODEC, MolangEntityStatePacketS2C::entityUuid,
            ByteBufCodecs.VAR_INT, MolangEntityStatePacketS2C::flags,
            ByteBufCodecs.VAR_INT, MolangEntityStatePacketS2C::itemUseRemaining,
            GlobalPos.STREAM_CODEC.apply(ByteBufCodecs::optional), MolangEntityStatePacketS2C::recoveryTarget,
            MolangEntityStatePacketS2C::new
    );

    @Override
    public Type<MolangEntityStatePacketS2C> type() {
        return TYPE;
    }

}
