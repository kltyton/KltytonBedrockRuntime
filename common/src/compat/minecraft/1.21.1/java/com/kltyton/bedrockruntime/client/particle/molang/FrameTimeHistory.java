package com.kltyton.bedrockruntime.client.particle.molang;

/** Recent completed frame intervals, measured by the client timer. */
public final class FrameTimeHistory {
    public enum Kind { LAST, AVERAGE, MINIMUM, MAXIMUM }

    private static final float[] INTERVALS = new float[30];
    private static int next;
    private static int count;

    private FrameTimeHistory() {
    }

    public static void record(float seconds) {
        INTERVALS[next] = seconds;
        next = (next + 1) % INTERVALS.length;
        if (count < INTERVALS.length) count++;
    }

    public static double query(Kind kind, int previousFrames) {
        if (count == 0) return 0.0;
        int offset = Math.min(Math.max(previousFrames, 0), count - 1);
        if (kind == Kind.LAST) return interval(offset);
        float result = interval(0);
        for (int i = 1; i <= offset; i++) {
            float sample = interval(i);
            result = switch (kind) {
                case AVERAGE -> result + sample;
                case MINIMUM -> Math.min(result, sample);
                case MAXIMUM -> Math.max(result, sample);
                case LAST -> throw new IllegalStateException("Unexpected frame aggregation");
            };
        }
        return kind == Kind.AVERAGE ? result / (offset + 1) : result;
    }

    private static float interval(int offset) {
        return INTERVALS[(next + INTERVALS.length - 1 - offset) % INTERVALS.length];
    }
}
