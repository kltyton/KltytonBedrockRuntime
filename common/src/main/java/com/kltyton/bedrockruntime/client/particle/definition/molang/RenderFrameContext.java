package com.kltyton.bedrockruntime.client.particle.definition.molang;

import java.util.Arrays;

/** Frame identity and partial-tick scopes for client rendering. */
public final class RenderFrameContext {
    private static final ThreadLocal<FrameStack> FRAMES = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> UI_ENTITY = new ThreadLocal<>();
    private static long frameIndex;
    private static float framePartialTick = 1.0F;

    private RenderFrameContext() {
    }

    public static void beginFrame() {
        beginFrame(1.0F);
    }

    public static void beginFrame(float partialTick) {
        frameIndex++;
        framePartialTick = partialTick;
    }

    public static float framePartialTick() {
        return currentOr(framePartialTick);
    }

    public static long frameIndex() {
        return frameIndex;
    }

    public static void enter(float partialTick) {
        FrameStack frames = FRAMES.get();
        if (frames == null) {
            frames = new FrameStack();
            FRAMES.set(frames);
        }
        if (frames.depth == frames.values.length) frames.values = Arrays.copyOf(frames.values, frames.depth * 2);
        frames.values[frames.depth++] = partialTick;
    }

    public static void exit() {
        FRAMES.get().depth--;
    }

    public static boolean isActive() {
        FrameStack frames = FRAMES.get();
        return frames != null && frames.depth > 0;
    }

    public static boolean isUiEntity() {
        return Boolean.TRUE.equals(UI_ENTITY.get());
    }

    public static boolean enterUiEntity() {
        boolean enclosing = isUiEntity();
        UI_ENTITY.set(true);
        return enclosing;
    }

    public static void exitUiEntity(boolean enclosing) {
        if (!enclosing) UI_ENTITY.remove();
    }

    public static float currentOr(float fallback) {
        FrameStack frames = FRAMES.get();
        return frames == null || frames.depth == 0 ? fallback : frames.values[frames.depth - 1];
    }

    public static float ageInTicks(int age) {
        FrameStack frames = FRAMES.get();
        if (frames == null || frames.depth == 0) return age;
        return Math.max(0.0F, age - 1.0F + frames.values[frames.depth - 1]);
    }

    private static final class FrameStack {
        private float[] values = new float[4];
        private int depth;
    }
}
