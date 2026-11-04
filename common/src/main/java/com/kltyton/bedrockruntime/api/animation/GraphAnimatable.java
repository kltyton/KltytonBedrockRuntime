package com.kltyton.bedrockruntime.api.animation;

import com.kltyton.bedrockruntime.animation.graph.GraphDefinition;
import com.kltyton.bedrockruntime.animation.graph.GraphRuntime;

/** Explicit animation-graph opt-in. Runtime state belongs to GeckoLib's animatable instance manager. */
public interface GraphAnimatable {
    GraphDefinition animationGraph();

    /** Called while extracting game state, before any deferred pose or rendering callback. */
    default void captureGraphParameters(GraphRuntime runtime, Object relatedObject, float partialTick) { }
}
