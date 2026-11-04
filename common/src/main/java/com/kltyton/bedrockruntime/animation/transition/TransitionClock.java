package com.kltyton.bedrockruntime.animation.transition;

/**
 * Explicit transition time source.
 *
 * <p>The clock is independent of any game tick loop. Callers advance it with their own delta and
 * query {@link #phase()} for pose blending.</p>
 */
public final class TransitionClock {
    private final double duration;
    private double elapsed;
    private double direction = 1.0;
    private boolean paused;

    public TransitionClock(double durationSeconds) {
        if (!Double.isFinite(durationSeconds) || durationSeconds < 0.0) {
            throw new IllegalArgumentException("Transition duration must be finite and non-negative");
        }
        this.duration = durationSeconds;
    }

    public double duration() {
        return duration;
    }

    public double elapsed() {
        return elapsed;
    }

    public double remaining() {
        return direction > 0.0 ? duration - elapsed : elapsed;
    }

    public double phase() {
        if (duration == 0.0) {
            return isFinished() ? 1.0 : 0.0;
        }
        return elapsed / duration;
    }

    public double direction() {
        return direction;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isFinished() {
        return direction > 0.0 ? elapsed >= duration : elapsed <= 0.0;
    }

    public TransitionClock reset() {
        elapsed = 0.0;
        direction = 1.0;
        paused = false;
        return this;
    }

    public TransitionClock pause() {
        paused = true;
        return this;
    }

    public TransitionClock resume() {
        paused = false;
        return this;
    }

    public TransitionClock togglePause() {
        paused = !paused;
        return this;
    }

    public TransitionClock seek(double seconds) {
        if (!Double.isFinite(seconds)) {
            throw new IllegalArgumentException("Transition seek time must be finite");
        }
        elapsed = clamp(seconds, 0.0, duration);
        return this;
    }

    public TransitionClock seekPhase(double phase) {
        if (!Double.isFinite(phase)) {
            throw new IllegalArgumentException("Transition seek phase must be finite");
        }
        elapsed = clamp(phase, 0.0, 1.0) * duration;
        return this;
    }

    public TransitionClock reverse() {
        direction = -direction;
        return this;
    }

    public TransitionClock setDirection(double direction) {
        if (direction != 1.0 && direction != -1.0) {
            throw new IllegalArgumentException("Transition direction must be 1 or -1");
        }
        this.direction = direction;
        return this;
    }

    public TransitionClock advance(double deltaSeconds) {
        if (!Double.isFinite(deltaSeconds) || deltaSeconds < 0.0) {
            throw new IllegalArgumentException("Transition delta must be finite and non-negative");
        }
        if (paused || deltaSeconds == 0.0) {
            return this;
        }
        elapsed = clamp(elapsed + direction * deltaSeconds, 0.0, duration);
        return this;
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
