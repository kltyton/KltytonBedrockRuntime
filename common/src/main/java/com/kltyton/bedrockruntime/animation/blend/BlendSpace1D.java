package com.kltyton.bedrockruntime.animation.blend;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable deterministic one-dimensional blend layout.
 *
 * <p>The evaluation order matches the constructor order. Positions must be finite and unique.
 * Values outside the layout range clamp to the first or last sample.</p>
 */
public final class BlendSpace1D {
    private final float[] positions;
    private final int[] order;

    public BlendSpace1D(float[] positions) {
        Objects.requireNonNull(positions, "positions");
        if (positions.length == 0) {
            throw new IllegalArgumentException("Blend space 1D requires at least one position");
        }
        this.positions = Arrays.copyOf(positions, positions.length);
        for (int i = 0; i < positions.length; i++) {
            if (!Float.isFinite(positions[i])) {
                throw new IllegalArgumentException("Blend space 1D position " + i + " must be finite");
            }
        }
        Integer[] sorted = new Integer[positions.length];
        for (int i = 0; i < sorted.length; i++) {
            sorted[i] = i;
        }
        Arrays.sort(sorted, (a, b) -> {
            int compared = Float.compare(this.positions[a], this.positions[b]);
            return compared != 0 ? compared : Integer.compare(a, b);
        });
        this.order = new int[sorted.length];
        for (int i = 0; i < sorted.length; i++) {
            this.order[i] = sorted[i];
            if (i > 0 && this.positions[order[i - 1]] == this.positions[order[i]]) {
                throw new IllegalArgumentException("Blend space 1D contains duplicate position "
                        + this.positions[order[i]]);
            }
        }
    }

    public static BlendSpace1D of(float... positions) {
        return new BlendSpace1D(positions);
    }

    public int size() {
        return positions.length;
    }

    public float position(int index) {
        if (index < 0 || index >= positions.length) {
            throw new IllegalArgumentException("Blend space 1D index " + index + " is outside [0, "
                    + positions.length + ")");
        }
        return positions[index];
    }

    public void evaluate(float value, float[] weights) {
        Objects.requireNonNull(weights, "weights");
        if (weights.length < positions.length) {
            throw new IllegalArgumentException("Blend space 1D weights length " + weights.length
                    + " is smaller than sample count " + positions.length);
        }
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException("Blend space 1D value must be finite");
        }
        for (int i = 0; i < positions.length; i++) {
            weights[i] = 0.0f;
        }
        if (positions.length == 1) {
            weights[0] = 1.0f;
            return;
        }
        int first = order[0];
        int last = order[order.length - 1];
        if (value <= positions[first]) {
            weights[first] = 1.0f;
            return;
        }
        if (value >= positions[last]) {
            weights[last] = 1.0f;
            return;
        }
        for (int i = 1; i < order.length; i++) {
            int left = order[i - 1];
            int right = order[i];
            float leftPosition = positions[left];
            float rightPosition = positions[right];
            if (value == leftPosition) {
                weights[left] = 1.0f;
                return;
            }
            if (value == rightPosition) {
                weights[right] = 1.0f;
                return;
            }
            if (value > leftPosition && value < rightPosition) {
                float alpha = (value - leftPosition) / (rightPosition - leftPosition);
                weights[left] = 1.0f - alpha;
                weights[right] = alpha;
                return;
            }
        }
        weights[last] = 1.0f;
    }

    public float[] evaluate(float value) {
        float[] weights = new float[positions.length];
        evaluate(value, weights);
        return weights;
    }

    public float[] positions() {
        return Arrays.copyOf(positions, positions.length);
    }
}
