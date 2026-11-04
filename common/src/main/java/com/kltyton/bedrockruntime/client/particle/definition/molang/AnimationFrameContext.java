package com.kltyton.bedrockruntime.client.particle.definition.molang;

import java.util.ArrayDeque;
import java.util.Deque;

/** Supplies animation-local seconds during animation particle initialization. */
public final class AnimationFrameContext implements AutoCloseable {
    private static final ThreadLocal<Deque<AnimationFrameContext>> SCOPES = new ThreadLocal<>();
    private final double animationSeconds;

    private AnimationFrameContext(double animationSeconds) {
        this.animationSeconds = animationSeconds;
    }

    public static AnimationFrameContext enter(double animationSeconds) {
        Deque<AnimationFrameContext> scopes = SCOPES.get();
        if (scopes == null) {
            scopes = new ArrayDeque<>();
            SCOPES.set(scopes);
        }
        AnimationFrameContext scope = new AnimationFrameContext(animationSeconds);
        scopes.push(scope);
        return scope;
    }

    public static double currentTime() {
        Deque<AnimationFrameContext> scopes = SCOPES.get();
        return scopes == null || scopes.isEmpty() ? 0.0 : scopes.peek().animationSeconds;
    }

    @Override
    public void close() {
        Deque<AnimationFrameContext> scopes = SCOPES.get();
        if (scopes == null || scopes.peek() != this) {
            throw new IllegalStateException("Animation frame contexts must close in stack order");
        }
        scopes.pop();
        if (scopes.isEmpty()) SCOPES.remove();
    }
}
