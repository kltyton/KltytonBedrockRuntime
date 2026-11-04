package com.kltyton.bedrockruntime.animation.pose;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable per-bone blend weight set.
 *
 * <p>Weights are in the inclusive range {@code [0, 1]}. A weight of zero excludes a bone from
 * the masked contribution; a weight of one includes it fully.</p>
 */
public final class BoneMask {
    private final SkeletonBinding binding;
    private final float[] weights;

    private BoneMask(SkeletonBinding binding, float[] weights) {
        this.binding = binding;
        this.weights = weights;
    }

    public static BoneMask none(SkeletonBinding binding) {
        return new BoneMask(Objects.requireNonNull(binding, "binding"), new float[binding.size()]);
    }

    public static BoneMask all(SkeletonBinding binding) {
        Objects.requireNonNull(binding, "binding");
        float[] weights = new float[binding.size()];
        Arrays.fill(weights, 1.0f);
        return new BoneMask(binding, weights);
    }

    public static BoneMask of(SkeletonBinding binding, float[] weights) {
        Objects.requireNonNull(binding, "binding");
        Objects.requireNonNull(weights, "weights");
        if (weights.length != binding.size()) {
            throw new IllegalArgumentException("Skeleton '" + binding.resource()
                    + "': mask length " + weights.length + " does not match bone count " + binding.size());
        }
        float[] copy = Arrays.copyOf(weights, weights.length);
        for (int i = 0; i < copy.length; i++) {
            validateWeight(binding, i, copy[i]);
        }
        return new BoneMask(binding, copy);
    }

    public static Builder builder(SkeletonBinding binding) {
        return new Builder(binding);
    }

    public SkeletonBinding binding() {
        return binding;
    }

    public int size() {
        return weights.length;
    }

    public float weight(int index) {
        checkIndex(index);
        return weights[index];
    }

    public boolean sameSkeleton(BoneMask other) {
        return other != null && binding.sameSkeleton(other.binding);
    }

    public float[] weights() {
        return Arrays.copyOf(weights, weights.length);
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= weights.length) {
            throw new IllegalArgumentException("Skeleton '" + binding.resource()
                    + "': bone index " + index + " is outside [0, " + weights.length + ")");
        }
    }

    private static void validateWeight(SkeletonBinding binding, int index, float weight) {
        if (!Float.isFinite(weight) || weight < 0.0f || weight > 1.0f) {
            throw new IllegalArgumentException("Skeleton '" + binding.resource()
                    + "': mask weight for bone " + index + " must be finite and within [0, 1]");
        }
    }

    public static final class Builder {
        private final SkeletonBinding binding;
        private final float[] weights;

        private Builder(SkeletonBinding binding) {
            this.binding = Objects.requireNonNull(binding, "binding");
            this.weights = new float[binding.size()];
        }

        public Builder setDefault(float weight) {
            validateWeight(binding, -1, weight);
            Arrays.fill(weights, weight);
            return this;
        }

        public Builder set(int index, float weight) {
            if (index < 0 || index >= weights.length) {
                throw new IllegalArgumentException("Skeleton '" + binding.resource()
                        + "': bone index " + index + " is outside [0, " + weights.length + ")");
            }
            validateWeight(binding, index, weight);
            weights[index] = weight;
            return this;
        }

        public Builder set(String name, float weight) {
            return set(binding.indexOfOrThrow(name), weight);
        }

        public Builder subtree(int rootIndex, float weight) {
            if (rootIndex < 0 || rootIndex >= weights.length) {
                throw new IllegalArgumentException("Skeleton '" + binding.resource()
                        + "': bone index " + rootIndex + " is outside [0, " + weights.length + ")");
            }
            validateWeight(binding, rootIndex, weight);
            for (int i = 0; i < weights.length; i++) {
                if (binding.isDescendantOf(i, rootIndex)) {
                    weights[i] = weight;
                }
            }
            return this;
        }

        public Builder subtree(String rootName, float weight) {
            return subtree(binding.indexOfOrThrow(rootName), weight);
        }

        public Builder add(int index, float weight) {
            if (index < 0 || index >= weights.length) {
                throw new IllegalArgumentException("Skeleton '" + binding.resource()
                        + "': bone index " + index + " is outside [0, " + weights.length + ")");
            }
            validateWeight(binding, index, weight);
            weights[index] = Math.min(1.0f, weights[index] + weight);
            return this;
        }

        public Builder add(String name, float weight) {
            return add(binding.indexOfOrThrow(name), weight);
        }

        public Builder addSubtree(int rootIndex, float weight) {
            if (rootIndex < 0 || rootIndex >= weights.length) {
                throw new IllegalArgumentException("Skeleton '" + binding.resource()
                        + "': bone index " + rootIndex + " is outside [0, " + weights.length + ")");
            }
            validateWeight(binding, rootIndex, weight);
            for (int i = 0; i < weights.length; i++) {
                if (binding.isDescendantOf(i, rootIndex)) {
                    weights[i] = Math.min(1.0f, weights[i] + weight);
                }
            }
            return this;
        }

        public Builder addSubtree(String rootName, float weight) {
            return addSubtree(binding.indexOfOrThrow(rootName), weight);
        }

        public BoneMask build() {
            return new BoneMask(binding, Arrays.copyOf(weights, weights.length));
        }
    }
}
