package com.kltyton.bedrockruntime.api.geckolib.skill.registration;

import java.util.Objects;

import com.kltyton.bedrockruntime.api.geckolib.skill.handler.GeoKeyframeSkillHandler;

/**
 * CN: 一个单点通知或由开始/结束 marker 构成的状态窗口注册。
 * EN: Registration for a point notification or a state window formed by begin/end markers.
 */
public record GeoKeyframeSkillRegistration(
        String skillId,
        String beginMarker,
        String endMarker,
        GeoKeyframeSkillHandler handler) {

    public GeoKeyframeSkillRegistration {
        Objects.requireNonNull(skillId, "skillId");
        Objects.requireNonNull(beginMarker, "beginMarker");
        endMarker = endMarker == null ? "" : endMarker;
        Objects.requireNonNull(handler, "handler");
        if (skillId.isBlank() || beginMarker.isBlank()) {
            throw new IllegalArgumentException("Geo keyframe skill id and begin marker must not be blank");
        }
    }

    public boolean window() {
        return !endMarker.isBlank();
    }
}
