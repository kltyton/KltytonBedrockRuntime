package com.kltyton.bedrockruntime.api.geckolib.skill.context;

/**
 * CN: GeckoLib 关键帧技能通知的生命周期阶段。
 * EN: Lifecycle phase of a GeckoLib keyframe skill notification.
 */
public enum GeoKeyframeSkillPhase {
    /** CN: 单点关键帧。EN: One-shot keyframe marker. */
    POINT,
    /** CN: 状态关键帧窗口开始。EN: State-keyframe window begins. */
    BEGIN,
    /** CN: 窗口存续期间的服务端 tick。EN: Server tick while a window remains active. */
    TICK,
    /** CN: 正常收到结束关键帧。EN: The closing keyframe was received normally. */
    END,
    /** CN: 实体、动画或注册失效导致窗口取消。EN: Window cancelled because its entity, animation, or registration became invalid. */
    CANCEL
}
