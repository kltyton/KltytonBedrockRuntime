package com.kltyton.bedrockruntime.network.protocol;

import com.kltyton.bedrockruntime.network.payload.contact.ObbContactReportPayload;
import com.kltyton.bedrockruntime.network.payload.entity.ObbEntityPartsPayload;
import com.kltyton.bedrockruntime.network.payload.selection.ObbPartSelectionPayload;
import com.kltyton.bedrockruntime.network.payload.skill.GeoKeyframeSkillPayload;

import java.util.function.Consumer;

/**
 * CN: 客户端到服务端 OBB 网络包的 loader 桥接。
 * EN: Loader bridge for client-to-server OBB packets.
 */
public final class BoneHitboxNetworking {
    private static Consumer<ObbPartSelectionPayload> clientSelectionSender = payload -> {
    };
    private static Consumer<ObbEntityPartsPayload> clientEntityPartsSender = payload -> {
    };
    private static Consumer<ObbContactReportPayload> clientContactReportSender = payload -> {
    };
    private static Consumer<GeoKeyframeSkillPayload> clientGeoKeyframeSkillSender = payload -> {
    };

    private BoneHitboxNetworking() {
    }

    public static void setClientSelectionSender(Consumer<ObbPartSelectionPayload> sender) {
        clientSelectionSender = sender == null ? payload -> {
        } : sender;
    }

    public static void setClientEntityPartsSender(Consumer<ObbEntityPartsPayload> sender) {
        clientEntityPartsSender = sender == null ? payload -> {
        } : sender;
    }

    public static void setClientContactReportSender(Consumer<ObbContactReportPayload> sender) {
        clientContactReportSender = sender == null ? payload -> {
        } : sender;
    }

    public static void setClientGeoKeyframeSkillSender(Consumer<GeoKeyframeSkillPayload> sender) {
        clientGeoKeyframeSkillSender = sender == null ? payload -> {
        } : sender;
    }

    public static void sendClientSelection(ObbPartSelectionPayload payload) {
        clientSelectionSender.accept(payload);
    }

    public static void sendClientEntityParts(ObbEntityPartsPayload payload) {
        clientEntityPartsSender.accept(payload);
    }

    public static void sendClientContactReport(ObbContactReportPayload payload) {
        clientContactReportSender.accept(payload);
    }

    public static void sendClientGeoKeyframeSkill(GeoKeyframeSkillPayload payload) {
        clientGeoKeyframeSkillSender.accept(payload);
    }
}
