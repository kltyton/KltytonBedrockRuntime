package com.kltyton.bedrockruntime.api.geckolib.skill.entity;

import com.kltyton.bedrockruntime.api.geckolib.entity.GeoBoneHitboxEntity;
import com.kltyton.bedrockruntime.api.geckolib.skill.context.GeoKeyframeSkillContext;
import com.kltyton.bedrockruntime.api.geckolib.skill.registration.GeoKeyframeSkillRegistrar;

/**
 * CN: 通过客户端 GeckoLib 自定义关键帧驱动服务端技能的实体接口。客户端逻辑 tick 会主动推进已追踪实体的
 * 渲染状态提取，因此 marker 不依赖实体是否位于视锥内。本接口本身不引用 GeckoLib 类，保持依赖可选。
 * EN: Entity interface for client GeckoLib custom-keyframe driven server skills. Client logic ticks actively advance
 * render-state extraction for tracked entities, so markers do not depend on frustum visibility. This interface has no
 * GeckoLib type dependency, keeping GeckoLib optional.
 */
public interface GeoKeyframeSkillEntity extends GeoBoneHitboxEntity {
    void kltyton_bedrock_runtime$registerGeoKeyframeSkills(GeoKeyframeSkillRegistrar registrar);

    /** Validate the marker against server-owned skill state before deduplication or any state mutation.
     * A tracked model pose alone is not proof that a gameplay skill is authorized. */
    default boolean kltyton_bedrock_runtime$acceptGeoKeyframeSkill(net.minecraft.server.level.ServerPlayer reporter,
            String marker, com.kltyton.bedrockruntime.api.geckolib.state.GeoObbAnimationState animation, double markerTimeSeconds) {
        return true;
    }

    /** CN: 返回 false 可跳过本次技能处理器和全局事件。EN: Return false to skip this invocation's handler and global event. */
    default boolean kltyton_bedrock_runtime$beforeGeoKeyframeSkill(GeoKeyframeSkillContext context) {
        return true;
    }

    /** CN: 处理器和全局事件之后调用。EN: Called after the handler and global event. */
    default void kltyton_bedrock_runtime$afterGeoKeyframeSkill(GeoKeyframeSkillContext context) {
    }
}
