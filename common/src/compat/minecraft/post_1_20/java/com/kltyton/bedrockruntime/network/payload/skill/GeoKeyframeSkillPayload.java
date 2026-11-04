package com.kltyton.bedrockruntime.network.payload.skill;

import com.kltyton.bedrockruntime.Constants;
import com.kltyton.bedrockruntime.api.geckolib.state.GeoObbAnimationState;
import com.kltyton.bedrockruntime.network.payload.ObbPayloadIO;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** CN: 客户端 GeckoLib 自定义关键帧到服务端技能分发器的通知包。EN: Client GeckoLib custom-keyframe notification for the server skill dispatcher. */
public record GeoKeyframeSkillPayload(
        int entityId,
        long clientSequence,
        long observedGameTime,
        String marker,
        GeoObbAnimationState animationState,
        double markerTimeSeconds,
        double animationSpeed) implements CustomPacketPayload {

    public static final int MAX_FIELD_LENGTH = 128;
    public static final Type<GeoKeyframeSkillPayload> TYPE = new Type<>(Constants.id("geo_keyframe_skill"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GeoKeyframeSkillPayload> CODEC =
            StreamCodec.ofMember(GeoKeyframeSkillPayload::write, GeoKeyframeSkillPayload::read);

    public GeoKeyframeSkillPayload {
        marker = bounded(marker);
        animationState = animationState == null
                ? GeoObbAnimationState.NONE
                : new GeoObbAnimationState(
                        bounded(animationState.controllerName()),
                        bounded(animationState.animationName()),
                        animationState.animationTimeSeconds(),
                        animationState.timelineTimeSeconds(),
                        animationState.triggered(),
                        animationState.transitioning(),
                        animationState.finished());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        ObbPayloadIO.writeSkill(buffer, this);
    }

    private static GeoKeyframeSkillPayload read(RegistryFriendlyByteBuf buffer) {
        return ObbPayloadIO.readSkill(buffer);
    }

    private static String bounded(String value) {
        String normalized = value == null ? "" : value;
        return normalized.substring(0, Math.min(normalized.length(), MAX_FIELD_LENGTH));
    }

}
