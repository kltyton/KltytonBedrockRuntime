package com.kltyton.bedrockruntime.network.payload.selection;

import com.kltyton.bedrockruntime.Constants;
import com.kltyton.bedrockruntime.network.payload.ObbPayloadIO;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * CN: 客户端当前选中 OBB 部位的轻量 UX 同步包。
 * EN: Lightweight UX sync payload carrying the client's currently selected OBB part.
 */
public record ObbPartSelectionPayload(int entityId, String source, String partName, int cubeIndex) {
    public static final int NO_ENTITY = -1;

    public static final ResourceLocation TYPE = Constants.id("part_selection");

    public static ObbPartSelectionPayload clear() {
        return new ObbPartSelectionPayload(NO_ENTITY, "", "", -1);
    }

    public boolean isClear() {
        return entityId < 0;
    }

    public void write(FriendlyByteBuf buffer) {
        ObbPayloadIO.writeSelection(buffer, this);
    }

    public static ObbPartSelectionPayload read(FriendlyByteBuf buffer) {
        return ObbPayloadIO.readSelection(buffer);
    }
}
