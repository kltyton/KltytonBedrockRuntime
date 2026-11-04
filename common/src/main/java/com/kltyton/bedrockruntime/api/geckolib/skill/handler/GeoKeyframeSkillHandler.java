package com.kltyton.bedrockruntime.api.geckolib.skill.handler;

import com.kltyton.bedrockruntime.api.geckolib.skill.context.GeoKeyframeSkillContext;

/** CN: 服务端关键帧技能处理器。EN: Server-side keyframe skill handler. */
@FunctionalInterface
public interface GeoKeyframeSkillHandler {
    void handle(GeoKeyframeSkillContext context);
}
