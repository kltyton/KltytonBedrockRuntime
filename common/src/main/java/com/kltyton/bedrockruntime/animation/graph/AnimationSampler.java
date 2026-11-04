package com.kltyton.bedrockruntime.animation.graph;

import com.kltyton.bedrockruntime.animation.pose.PoseBuffer;

/**
 * Host-provided animation sampling hook.
 *
 * <p>The sampler writes sparse or complete channels into the supplied target. The target is
 * reset to the skeleton bind pose (with all channel flags clear) before each call.
 * Implementations must not replace the target or change its skeleton binding.</p>
 */
@FunctionalInterface
public interface AnimationSampler {
    void sample(String animation, double seconds, boolean loop, PoseBuffer target);

    /**
     * Returns the clip duration in seconds, or a non-positive value when unknown.
     */
    default double duration(String animation) {
        return 0.0;
    }
}
