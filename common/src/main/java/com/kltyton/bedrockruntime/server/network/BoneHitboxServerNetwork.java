package com.kltyton.bedrockruntime.server.network;

import com.kltyton.bedrockruntime.server.selection.ServerPartSelectionStore;

import com.kltyton.bedrockruntime.network.payload.selection.ObbPartSelectionPayload;
import com.kltyton.bedrockruntime.network.payload.entity.ObbEntityPartsPayload;
import com.kltyton.bedrockruntime.network.payload.contact.ObbContactReportPayload;
import com.kltyton.bedrockruntime.network.payload.skill.GeoKeyframeSkillPayload;
import com.kltyton.bedrockruntime.server.skill.keyframe.GeoKeyframeSkillDispatcher;
import com.kltyton.bedrockruntime.server.sync.contact.ServerObbContactStore;
import com.kltyton.bedrockruntime.server.sync.snapshot.ServerObbStore;

import net.minecraft.server.level.ServerPlayer;

/**
 * CN: loader 专用网络注册调用的通用服务端包处理器。
 * EN: Common server packet handlers called by loader-specific network registrations.
 */
public final class BoneHitboxServerNetwork {
    private BoneHitboxServerNetwork() {
    }

    public static void handlePartSelection(ObbPartSelectionPayload payload, ServerPlayer player) {
        if (!ObbReportValidation.consume(player, 16)) { return; }
        ServerPartSelectionStore.update(player, payload);
    }

    public static void handleEntityParts(ObbEntityPartsPayload payload, ServerPlayer player) {
        if (!ObbReportValidation.consume(player, 16 + payload.parts().size() + payload.controllerStates().size() * 4)) { return; }
        ServerObbStore.update(player, payload);
    }

    public static void handleContactReport(ObbContactReportPayload payload, ServerPlayer player) {
        if (!ObbReportValidation.consume(player, 16 + payload.contacts().size() * 4)
                || !ObbReportValidation.recent(player, payload.observedGameTime())) { return; }
        ServerObbContactStore.update(player, payload);
    }

    public static void handleGeoKeyframeSkill(GeoKeyframeSkillPayload payload, ServerPlayer player) {
        if (!ObbReportValidation.consume(player, 128)
                || !ObbReportValidation.recent(player, payload.observedGameTime())) { return; }
        GeoKeyframeSkillDispatcher.handle(payload, player);
    }
}
