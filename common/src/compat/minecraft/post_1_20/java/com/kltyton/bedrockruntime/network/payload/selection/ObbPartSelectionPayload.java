package com.kltyton.bedrockruntime.network.payload.selection;

import com.kltyton.bedrockruntime.Constants;
import com.kltyton.bedrockruntime.network.payload.ObbPayloadIO;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * CN: 客户端当前选中 OBB 部位的轻量 UX 同步包。
 * EN: Lightweight UX sync payload carrying the client's currently selected OBB part.
 */
public record ObbPartSelectionPayload(int entityId, String source, String partName, int cubeIndex) implements CustomPacketPayload {
    public static final int NO_ENTITY = -1;

    public static final Type<ObbPartSelectionPayload> TYPE = new Type<>(Constants.id("part_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ObbPartSelectionPayload> CODEC =
            StreamCodec.ofMember(ObbPartSelectionPayload::write, ObbPartSelectionPayload::read);

    public static ObbPartSelectionPayload clear() {
        return new ObbPartSelectionPayload(NO_ENTITY, "", "", -1);
    }

    public boolean isClear() {
        return entityId < 0;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        ObbPayloadIO.writeSelection(buffer, this);
    }

    private static ObbPartSelectionPayload read(RegistryFriendlyByteBuf buffer) {
        return ObbPayloadIO.readSelection(buffer);
    }
}
