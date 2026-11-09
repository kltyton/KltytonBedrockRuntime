package com.kltyton.bedrockruntime.animation.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.kltyton.bedrockruntime.animation.blend.BlendSpace1D;
import com.kltyton.bedrockruntime.animation.blend.BlendSpace2D;
import com.kltyton.bedrockruntime.animation.pose.BoneMask;
import com.kltyton.bedrockruntime.animation.pose.PoseBlender;
import com.kltyton.bedrockruntime.animation.pose.PoseBuffer;
import com.kltyton.bedrockruntime.animation.pose.SkeletonBinding;
import com.kltyton.bedrockruntime.animation.transition.PoseTransition;

/**
 * Per-instance graph evaluator.
 *
 * <p>Runtime-owned node buffers and layout objects are reused across frames. The returned pose
 * buffer is owned by this runtime and is overwritten by the next evaluation.</p>
 *
 * <p>Every pose-valued graph node produces absolute local transforms. An additive node therefore
 * converts its additive child from absolute local space to bind-relative deltas before composition:
 * translation keeps pixels (bind translation is zero), rotation subtracts the skeleton bind Euler
 * on each axis, and scale keeps its ratio (bind scale is one). Hermite velocity children are
 * already deltas and are never passed through this conversion.</p>
 *
 * <p>Velocity input nodes read caller-supplied velocity poses through
 * {@link #setVelocityInput(String, PoseBuffer)}. Translation is pixels per second, rotation is
 * parent-space angular velocity in radians per second, and scale is ratio per second. Sparse
 * channel flags mean zero velocity; unset inputs are explicit zero velocity.</p>
 */
public final class GraphRuntime {
    private final GraphDefinition definition;
    private final Map<String, Double> parameters = new LinkedHashMap<>();
    private final Map<String, PoseBuffer> nodePoses = new HashMap<>();
    private final Map<String, BlendSpace1D> blendSpaces1D = new HashMap<>();
    private final Map<String, BlendSpace2D> blendSpaces2D = new HashMap<>();
    private final Map<String, float[]> weightScratch = new HashMap<>();
    private final Map<String, PoseBuffer[]> poseScratch = new HashMap<>();
    private final Map<String, PoseBuffer> additiveDeltaScratch = new HashMap<>();
    private final Map<String, PoseBuffer> velocityInputs = new HashMap<>();
    private final Map<String, BoneMask> maskCache = new HashMap<>();
    private final Set<String> evaluatedNodes = new HashSet<>();
    private final Map<String, Double> timeCache = new HashMap<>();
    private SkeletonBinding binding;
    private PoseBuffer output;
    private PoseBuffer emptyPose;
    private long cachedFrameId = Long.MIN_VALUE;
    private long cachedSecondsBits = Long.MIN_VALUE;
    private boolean hasFrameCache;

    public GraphRuntime(GraphDefinition definition) {
        this.definition = Objects.requireNonNull(definition, "definition");
    }

    public GraphDefinition definition() {
        return definition;
    }

    public GraphRuntime setParameter(String name, double value) {
        String safeName = requireParameterName(name);
        requireFinite(value, "parameter '" + safeName + "'");
        parameters.put(safeName, value);
        invalidateEvaluation();
        return this;
    }

    public GraphRuntime setParameters(Map<String, ? extends Number> values) {
        Objects.requireNonNull(values, "values");
        for (Map.Entry<String, ? extends Number> entry : values.entrySet()) {
            String name = requireParameterName(entry.getKey());
            Number number = Objects.requireNonNull(entry.getValue(), "parameter '" + name + "'");
            double value = number.doubleValue();
            requireFinite(value, "parameter '" + name + "'");
            parameters.put(name, value);
        }
        invalidateEvaluation();
        return this;
    }

    public double parameter(String name) {
        String safeName = requireParameterName(name);
        Double value = parameters.get(safeName);
        if (value != null) {
            return value;
        }
        return definition.defaultParameter(safeName);
    }

    public void clearParameters() {
        parameters.clear();
        invalidateEvaluation();
    }

    /**
     * Copies a caller-owned velocity pose into runtime-owned storage for a velocity input key.
     *
     * <p>Translation channels are pixels per second, rotation channels are parent-space angular
     * velocity in radians per second, and scale channels are ratio per second. Sparse channel flags
     * mean zero velocity for absent channels. The input is copied, never retained or mutated.</p>
     */
    public GraphRuntime setVelocityInput(String inputKey, PoseBuffer input) {
        String safeKey = requireVelocityInputKey(inputKey);
        Objects.requireNonNull(input, "input");

        PoseBuffer owned = velocityInputs.get(safeKey);

        if (owned == null || !owned.binding().sameSkeleton(input.binding())) {
            owned = new PoseBuffer(input.binding());
            velocityInputs.put(safeKey, owned);
        }

        owned.copyFrom(input);
        invalidateEvaluation();
        return this;
    }

    /** Removes a velocity input key; its velocity input nodes return zero velocity. */
    public void clearVelocityInput(String inputKey) {
        if (velocityInputs.remove(requireVelocityInputKey(inputKey)) != null) {
            invalidateEvaluation();
        }
    }

    /** Removes all velocity input keys; velocity input nodes return zero velocity. */
    public void clearVelocityInputs() {
        if (!velocityInputs.isEmpty()) {
            velocityInputs.clear();
            invalidateEvaluation();
        }
    }



    public PoseBuffer evaluate(long frameId, double seconds, SkeletonBinding binding, AnimationSampler sampler) {
        Objects.requireNonNull(binding, "binding");
        Objects.requireNonNull(sampler, "sampler");
        requireFinite(seconds, "seconds");
        ensureBinding(binding);
        long secondsBits = Double.doubleToLongBits(seconds);
        if (!hasFrameCache || frameId != cachedFrameId || secondsBits != cachedSecondsBits) {
            evaluatedNodes.clear();
            timeCache.clear();
            cachedFrameId = frameId;
            cachedSecondsBits = secondsBits;
            hasFrameCache = true;
        }
        PoseBuffer result = evaluateNode(definition.output(), seconds, sampler);
        output.copyFrom(result);
        return output;
    }



    private PoseBuffer evaluateNode(String id, double seconds, AnimationSampler sampler) {
        if (evaluatedNodes.contains(id)) {
            return nodePose(id);
        }
        GraphDefinition.Node node = definition.node(id);
        PoseBuffer out = nodePose(id);

        if (node instanceof GraphDefinition.AnimationNode animation) {
            out.resetToBind();
            double clipDuration = 0.0;
            if (animation.timeMode() == GraphDefinition.TimeMode.PHASE) {
                double sampledDuration = sampler.duration(animation.animation());
                if (Double.isFinite(sampledDuration) && sampledDuration > 0.0) {
                    clipDuration = sampledDuration;
                }
            }
            double localSeconds = animation.localSeconds(seconds, clipDuration);
            timeCache.put(id, localSeconds);
            sampler.sample(animation.animation(), localSeconds, animation.loop(), out);
            evaluatedNodes.add(id);
            return out;
        }
        if (node instanceof GraphDefinition.VelocityInputNode velocityInput) {
            PoseBuffer input = velocityInputs.get(velocityInput.inputKey());

            if (input == null || !out.sameSkeleton(input)) {
                out.clear();
            } else {
                out.copyFrom(input);
            }

            evaluatedNodes.add(id);
            return out;
        }
        if (node instanceof GraphDefinition.Blend1DNode blend) {
            BlendSpace1D space = blendSpaces1D.computeIfAbsent(id, ignored -> {
                float[] positions = new float[blend.entries().size()];
                for (int i = 0; i < positions.length; i++) {
                    positions[i] = blend.entries().get(i).position();
                }
                return new BlendSpace1D(positions);
            });
            float[] weights = weightScratch.computeIfAbsent(id, ignored -> new float[blend.entries().size()]);
            space.evaluate((float) parameter(blend.parameter()), weights);
            PoseBuffer[] poses = poseScratch.computeIfAbsent(id, ignored -> new PoseBuffer[blend.entries().size()]);
            for (int i = 0; i < poses.length; i++) {
                poses[i] = evaluateNode(blend.entries().get(i).node(), seconds, sampler);
            }
            PoseBlender.blendWeighted(poses, weights, poses.length, out);
            evaluatedNodes.add(id);
            return out;
        }
        if (node instanceof GraphDefinition.Blend2DNode blend) {
            BlendSpace2D space = blendSpaces2D.computeIfAbsent(id, ignored -> {
                float[] xs = new float[blend.entries().size()];
                float[] ys = new float[blend.entries().size()];
                for (int i = 0; i < xs.length; i++) {
                    GraphDefinition.Blend2DEntry entry = blend.entries().get(i);
                    xs[i] = entry.x();
                    ys[i] = entry.y();
                }
                return new BlendSpace2D(xs, ys);
            });
            float[] weights = weightScratch.computeIfAbsent(id, ignored -> new float[blend.entries().size()]);
            space.evaluate((float) parameter(blend.parameterX()), (float) parameter(blend.parameterY()), weights);
            PoseBuffer[] poses = poseScratch.computeIfAbsent(id, ignored -> new PoseBuffer[blend.entries().size()]);
            for (int i = 0; i < poses.length; i++) {
                poses[i] = evaluateNode(blend.entries().get(i).node(), seconds, sampler);
            }
            PoseBlender.blendWeighted(poses, weights, poses.length, out);
            evaluatedNodes.add(id);
            return out;
        }
        if (node instanceof GraphDefinition.AdditiveNode additive) {
            PoseBuffer base = evaluateNode(additive.base(), seconds, sampler);
            GraphDefinition.MaskNode mask = definition.node(additive.additive()) instanceof GraphDefinition.MaskNode layerMask ? layerMask : null;
            PoseBuffer absoluteLayer = evaluateNode(mask == null ? additive.additive() : mask.node(), seconds, sampler);
            PoseBuffer deltaLayer = additiveDeltaScratch.computeIfAbsent(id, ignored -> new PoseBuffer(binding));
            absoluteToBindDelta(absoluteLayer, deltaLayer);
            double weight = additive.weightParameter() == null
                    ? additive.weight()
                    : parameter(additive.weightParameter());
            if (mask == null) {
                PoseBlender.additive(base, deltaLayer, (float) weight, out);
            } else {
                PoseBlender.additiveMasked(base, deltaLayer, maskCache.computeIfAbsent(mask.id(), ignored -> buildMask(mask)), (float) weight, out);
            }
            evaluatedNodes.add(id);
            return out;
        }
        if (node instanceof GraphDefinition.BlendNode blend) {
            PoseBuffer base = evaluateNode(blend.base(), seconds, sampler);
            GraphDefinition.MaskNode mask = definition.node(blend.input()) instanceof GraphDefinition.MaskNode layerMask ? layerMask : null;
            PoseBuffer input = evaluateNode(mask == null ? blend.input() : mask.node(), seconds, sampler);
            double weight = blend.weightParameter() == null
                    ? blend.weight()
                    : parameter(blend.weightParameter());
            if (mask == null) {
                PoseBlender.lerp(base, input, (float) weight, out);
            } else {
                PoseBlender.lerpMasked(base, input, maskCache.computeIfAbsent(mask.id(), ignored -> buildMask(mask)), (float) weight, out);
            }
            evaluatedNodes.add(id);
            return out;
        }
        if (node instanceof GraphDefinition.HermiteNode hermite) {
            PoseBuffer from = evaluateNode(hermite.from(), seconds, sampler);
            PoseBuffer to = evaluateNode(hermite.to(), seconds, sampler);
            PoseBuffer fromVelocity = hermite.fromVelocity() == null
                    ? emptyPose
                    : evaluateNode(hermite.fromVelocity(), seconds, sampler);
            PoseBuffer toVelocity = hermite.toVelocity() == null
                    ? emptyPose
                    : evaluateNode(hermite.toVelocity(), seconds, sampler);
            double phase = hermite.phaseParameter() == null
                    ? hermite.phase()
                    : parameter(hermite.phaseParameter());
            PoseTransition.hermite(from, fromVelocity, to, toVelocity, hermite.duration(), phase, out);
            evaluatedNodes.add(id);
            return out;
        }
        if (node instanceof GraphDefinition.MaskNode mask) {
            PoseBuffer source = evaluateNode(mask.node(), seconds, sampler);
            BoneMask boneMask = maskCache.computeIfAbsent(id, ignored -> buildMask(mask));
            PoseBlender.mask(source, boneMask, out);
            evaluatedNodes.add(id);
            return out;
        }
        throw new IllegalStateException("Unsupported animation graph node " + node.getClass().getName());
    }

    private void absoluteToBindDelta(PoseBuffer absolute, PoseBuffer delta) {
        int size = delta.size();
        for (int i = 0; i < size; i++) {
            if (absolute.hasTranslation(i)) {
                delta.setTranslation(i, absolute.translationX(i), absolute.translationY(i), absolute.translationZ(i));
            } else {
                delta.clearTranslation(i);
            }
            if (absolute.hasRotation(i)) {
                SkeletonBinding.Bone bone = binding.bone(i);
                delta.setRotation(i,
                        absolute.rotationX(i) - bone.bindRotX(),
                        absolute.rotationY(i) - bone.bindRotY(),
                        absolute.rotationZ(i) - bone.bindRotZ());
            } else {
                delta.clearRotation(i);
            }
            if (absolute.hasScale(i)) {
                delta.setScale(i, absolute.scaleX(i), absolute.scaleY(i), absolute.scaleZ(i));
            } else {
                delta.clearScale(i);
            }
        }
    }

    private BoneMask buildMask(GraphDefinition.MaskNode node) {
        BoneMask.Builder builder = BoneMask.builder(binding);
        float weight = node.weight();
        for (String name : node.names()) {
            builder.set(name, weight);
        }
        for (Integer index : node.indices()) {
            builder.set(index, weight);
        }
        for (String subtree : node.subtrees()) {
            builder.subtree(subtree, weight);
        }
        return builder.build();
    }

    private PoseBuffer nodePose(String id) {
        return nodePoses.computeIfAbsent(id, ignored -> new PoseBuffer(binding));
    }

    private void migrateVelocityInputs(SkeletonBinding newBinding) {
        if (velocityInputs.isEmpty()) {
            return;
        }
        velocityInputs.entrySet().removeIf(entry -> !entry.getValue().binding().sameSkeleton(newBinding));
        for (Map.Entry<String, PoseBuffer> entry : velocityInputs.entrySet()) {
            PoseBuffer input = entry.getValue();

            if (input.binding() != newBinding) {
                PoseBuffer migrated = new PoseBuffer(newBinding);
                migrated.copyFrom(input);
                entry.setValue(migrated);
            }
        }
    }

    private void ensureBinding(SkeletonBinding newBinding) {
        if (binding == newBinding) {
            return;
        }
        migrateVelocityInputs(newBinding);
        binding = newBinding;
        nodePoses.clear();
        blendSpaces1D.clear();
        blendSpaces2D.clear();
        weightScratch.clear();
        poseScratch.clear();
        additiveDeltaScratch.clear();
        maskCache.clear();
        evaluatedNodes.clear();
        timeCache.clear();
        output = new PoseBuffer(binding);
        emptyPose = new PoseBuffer(binding);
        emptyPose.clear();
        hasFrameCache = false;
    }

    private void invalidateEvaluation() {
        evaluatedNodes.clear();
        timeCache.clear();
        hasFrameCache = false;
    }

    private static String requireParameterName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Animation parameter name must not be blank");
        }
        return name;
    }

    private static String requireVelocityInputKey(String inputKey) {
        if (inputKey == null || inputKey.isBlank()) {
            throw new IllegalArgumentException("Velocity input key must not be blank");
        }
        return inputKey;
    }

    private static void requireFinite(double value, String field) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(field + " must be finite");
        }
    }
}
