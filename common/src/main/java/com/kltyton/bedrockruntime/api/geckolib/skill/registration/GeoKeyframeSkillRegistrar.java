package com.kltyton.bedrockruntime.api.geckolib.skill.registration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.kltyton.bedrockruntime.api.geckolib.skill.handler.GeoKeyframeSkillHandler;

/**
 * CN: GeckoLib 关键帧技能注册器。marker 匹配忽略大小写及末尾分号。
 * EN: GeckoLib keyframe-skill registrar. Marker matching ignores case and trailing semicolons.
 */
public final class GeoKeyframeSkillRegistrar {
    private final List<GeoKeyframeSkillRegistration> registrations = new ArrayList<>();

    /** CN: 注册一个单点 marker，技能 id 默认等于规范化 marker。EN: Registers a point marker using its normalized marker as the skill id. */
    public GeoKeyframeSkillRegistration point(String marker, GeoKeyframeSkillHandler handler) {
        return point(normalizeMarker(marker), marker, handler);
    }

    public GeoKeyframeSkillRegistration register(String marker, GeoKeyframeSkillHandler handler) {
        return point(marker, handler);
    }

    public GeoKeyframeSkillRegistration point(String skillId, String marker, GeoKeyframeSkillHandler handler) {
        return add(new GeoKeyframeSkillRegistration(
                normalizeSkillId(skillId),
                normalizeMarker(marker),
                "",
                handler));
    }

    /** CN: 注册每 tick 回调的开始/结束关键帧窗口。EN: Registers a begin/end keyframe window that receives a callback each server tick. */
    public GeoKeyframeSkillRegistration window(String skillId, String beginMarker, String endMarker,
            GeoKeyframeSkillHandler handler) {
        String normalizedEnd = normalizeMarker(endMarker);
        if (normalizedEnd.isBlank()) {
            throw new IllegalArgumentException("Geo keyframe skill end marker must not be blank");
        }
        return add(new GeoKeyframeSkillRegistration(
                normalizeSkillId(skillId),
                normalizeMarker(beginMarker),
                normalizedEnd,
                handler));
    }

    public List<GeoKeyframeSkillRegistration> registrations() {
        return List.copyOf(registrations);
    }

    public static String normalizeMarker(String marker) {
        String normalized = Objects.requireNonNullElse(marker, "").trim();
        while (normalized.endsWith(";")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private GeoKeyframeSkillRegistration add(GeoKeyframeSkillRegistration registration) {
        if (registrations.stream().anyMatch(existing -> existing.skillId().equals(registration.skillId()))) {
            throw new IllegalArgumentException("Duplicate Geo keyframe skill id: " + registration.skillId());
        }
        registrations.add(registration);
        return registration;
    }

    private static String normalizeSkillId(String skillId) {
        String normalized = Objects.requireNonNullElse(skillId, "").trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Geo keyframe skill id must not be blank");
        }
        return normalized;
    }
}
