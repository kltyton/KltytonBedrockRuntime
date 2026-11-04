package com.kltyton.bedrockruntime.api.geckolib.skill.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.kltyton.bedrockruntime.api.geckolib.skill.context.GeoKeyframeSkillContext;

/** CN: loader 无关的全局关键帧技能事件。EN: Loader-neutral global keyframe-skill event. */
public final class GeoKeyframeSkillEvents {
    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    private GeoKeyframeSkillEvents() {
    }

    public static void register(Listener listener) {
        LISTENERS.add(listener);
    }

    public static void fire(GeoKeyframeSkillContext context) {
        LISTENERS.forEach(listener -> listener.onGeoKeyframeSkill(context));
    }

    @FunctionalInterface
    public interface Listener {
        void onGeoKeyframeSkill(GeoKeyframeSkillContext context);
    }
}
