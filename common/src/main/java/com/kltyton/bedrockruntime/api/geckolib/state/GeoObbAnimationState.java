package com.kltyton.bedrockruntime.api.geckolib.state;

/**
 * CN: GeckoLib 渲染实体当前动画状态的 loader 无关快照。
 * EN: Loader-neutral snapshot of the current animation state for a GeckoLib-rendered entity.
 */
public record GeoObbAnimationState(
        String controllerName,
        String animationName,
        double animationTimeSeconds,
        double timelineTimeSeconds,
        boolean triggered,
        boolean transitioning,
        boolean finished) {
    public static final GeoObbAnimationState NONE = new GeoObbAnimationState("", "", 0.0, 0.0, false, false, false);

    public GeoObbAnimationState {
        controllerName = controllerName == null ? "" : controllerName;
        animationName = animationName == null ? "" : animationName;
    }

    /**
     * CN: 是否有可用的当前动画名。
     * EN: Whether this state contains a current animation name.
     */
    public boolean active() {
        return !animationName.isBlank();
    }

    /**
     * CN: 用于判断临时攻击盒是否仍属于同一段动画。
     * EN: Key used to decide whether temporary attack boxes still belong to the same animation segment.
     */
    public String animationKey() {
        return controllerName + ":" + animationName;
    }
}
