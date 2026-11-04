package com.kltyton.bedrockruntime.network;

import com.kltyton.bedrockruntime.network.payload.selection.ObbPartSelectionPayload;
import com.kltyton.bedrockruntime.network.payload.entity.ObbEntityPartsPayload;
import com.kltyton.bedrockruntime.network.payload.contact.ObbContactReportPayload;
import com.kltyton.bedrockruntime.network.payload.skill.GeoKeyframeSkillPayload;
import com.kltyton.bedrockruntime.server.network.BoneHitboxServerNetwork;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * CN: Fabric 网络包注册。
 * EN: Fabric networking registration.
 */
public final class KltytonBedrockRuntimeFabricNetworking {
    private KltytonBedrockRuntimeFabricNetworking() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ObbPartSelectionPayload.TYPE, ObbPartSelectionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ObbEntityPartsPayload.TYPE, ObbEntityPartsPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ObbContactReportPayload.TYPE, ObbContactReportPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(GeoKeyframeSkillPayload.TYPE, GeoKeyframeSkillPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ObbPartSelectionPayload.TYPE,
                (payload, context) -> BoneHitboxServerNetwork.handlePartSelection(payload, context.player()));
        ServerPlayNetworking.registerGlobalReceiver(ObbEntityPartsPayload.TYPE,
                (payload, context) -> BoneHitboxServerNetwork.handleEntityParts(payload, context.player()));
        ServerPlayNetworking.registerGlobalReceiver(ObbContactReportPayload.TYPE,
                (payload, context) -> BoneHitboxServerNetwork.handleContactReport(payload, context.player()));
        ServerPlayNetworking.registerGlobalReceiver(GeoKeyframeSkillPayload.TYPE,
                (payload, context) -> BoneHitboxServerNetwork.handleGeoKeyframeSkill(payload, context.player()));
    }
}
