package com.kltyton.bedrockruntime.client.particle.definition.curve;

import com.kltyton.bedrockruntime.client.particle.definition.CodecCompat;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.definition.Tuple;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.FloatMolangExp;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public final class ParticleCurve {
    private static final java.util.Set<String> REPORTED_NONFINITE = java.util.concurrent.ConcurrentHashMap.newKeySet();
    public static final Codec<ParticleCurve> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CodecCompat.lenientOptionalFieldOf(CurveType.CODEC, "type", CurveType.LINEAR).forGetter(curve -> curve.type),
            CodecCompat.lenientOptionalFieldOf(FloatMolangExp.CODEC, "input").xmap(input -> {
                if (input.isEmpty()) BedrockParticles.LOGGER.warn("Particle curve input is missing or invalid; using zero");
                return input.orElse(FloatMolangExp.ZERO);
            }, java.util.Optional::of).forGetter(curve -> curve.input),
            CodecCompat.lenientOptionalFieldOf(FloatMolangExp.CODEC, "horizontal_range", FloatMolangExp.ONE).forGetter(curve -> curve.horizontalRange),
            Codec.either(CurveNodes.MAP_CODEC, CurveNodes.LIST_CODEC).xmap(
                    either -> either.map(nodes -> nodes, nodes -> nodes),
                    nodes -> nodes.isLeft ? Either.left(nodes) : Either.right(nodes)
            ).fieldOf("nodes").forGetter(curve -> curve.nodes)
    ).apply(instance, ParticleCurve::new));
    public static final Tuple<Float, CurveNode> FIRST = new Tuple<>(0.0F, new CurveNode(0.0F, 0.0F));
    public static final Tuple<Float, CurveNode> LAST = new Tuple<>(1.0F, new CurveNode(0.0F, 0.0F));
    public static final float ONE_THREE = 1.0F / 3.0F;
    public final CurveType type;
    public final FloatMolangExp input;
    public final FloatMolangExp horizontalRange;
    public final CurveNodes nodes;

    private final Map<String, float[]> cachedNodes;
    private final boolean literalNodes;
    private final List<BezierSegment> segments;

    public ParticleCurve(CurveType type, FloatMolangExp input, FloatMolangExp horizontalRange, CurveNodes nodes) {
        this.type = type;
        this.input = input;
        this.horizontalRange = horizontalRange;
        this.nodes = nodes;

        this.cachedNodes = new Hashtable<>();
        this.literalNodes = !nodes.isLeft && nodes.either.right().orElseThrow().stream()
                .allMatch(node -> node.getExpStr().isEmpty());
        ArrayList<BezierSegment> segments = new ArrayList<>();
        if (type == CurveType.BEZIER_CHAIN) {
            for (int index = 1; index < nodes.nodeList.size(); index++) {
                Tuple<Float, CurveNode> previous = nodes.nodeList.get(index - 1);
                Tuple<Float, CurveNode> next = nodes.nodeList.get(index);
                float p0 = previous.getB().resolvedRightValue();
                float p3 = next.getB().resolvedLeftValue();
                segments.add(new BezierSegment(previous.getA(), next.getA(), p0,
                        previous.getB().resolvedRightSlope() / 3.0F + p0,
                        p3 - next.getB().resolvedLeftSlope() / 3.0F, p3));
            }
        }
        this.segments = List.copyOf(segments);
    }

    public void evaluate(MolangInstance instance, String name) {
        if (type == CurveType.BEZIER_CHAIN && segments.isEmpty()) return;
        float value = calculate(instance, name);
        if (!Float.isFinite(value)) {
            String key = instance.getIdentity() + ":" + name;
            if (REPORTED_NONFINITE.add(key)) {
                BedrockParticles.LOGGER.warn("Non-finite particle curve {} in effect {}", name, instance.getIdentity());
            }
            value = 0.0F;
        }
        instance.getVars().setValue(name, value);
    }

    public float calculate(MolangInstance instance, String name) {
        float value = input.calculate(instance);
        if (type == CurveType.BEZIER_CHAIN) return sampleChain(value);
        float range = horizontalRange.calculate(instance);
        if (range <= Math.ulp(1.0F)) range = Math.ulp(1.0F);
        float t = value / range;
        List<FloatMolangExp> values = nodes.either.right().orElseThrow();
        float[] constants = literalNodes ? cachedNodes.computeIfAbsent(name, key -> {
            float[] result = new float[values.size()];
            for (int index = 0; index < result.length; index++) result[index] = values.get(index).getConstant();
            return result;
        }) : null;
        return switch (type) {
            case LINEAR -> sampleLinear(t, instance, values, constants);
            case BEZIER -> sampleBezier(t, instance, values, constants);
            case CATMULL_ROM -> sampleCatmullRom(t, instance, values, constants);
            case BEZIER_CHAIN -> throw new IllegalStateException("Bezier chain has no horizontal range");
        };
    }

    private static float node(int index, MolangInstance instance, List<FloatMolangExp> values,
                              @org.jetbrains.annotations.Nullable float[] constants) {
        if (index >= values.size()) return 0.0F;
        return constants == null ? values.get(index).calculate(instance) : constants[index];
    }

    private static float sampleLinear(float t, MolangInstance instance, List<FloatMolangExp> values,
                                      @org.jetbrains.annotations.Nullable float[] constants) {
        int size = values.size();
        if (size == 0) return 0.0F;
        if (t <= 0.0F || size == 1) return node(0, instance, values, constants);
        if (t >= 1.0F) return node(size - 1, instance, values, constants);
        float position = t * (size - 1);
        int index = Mth.floor(position);
        float next = node(index + 1, instance, values, constants);
        float current = node(index, instance, values, constants);
        return (position - index) * (next - current) + current;
    }

    private static float sampleBezier(float t, MolangInstance instance, List<FloatMolangExp> values,
                                      @org.jetbrains.annotations.Nullable float[] constants) {
        float p3 = node(3, instance, values, constants);
        float p2 = node(2, instance, values, constants);
        float p1 = node(1, instance, values, constants);
        float p0 = node(0, instance, values, constants);
        if (t <= 0.0F) return p0;
        if (t >= 1.0F) return p3;
        return bezier(t, p0, p1, p2, p3);
    }

    private static float bezier(float t, float p0, float p1, float p2, float p3) {
        float inverse = 1.0F - t;
        return t * t * t * p3 + inverse * inverse * inverse * p0
                + t * 3.0F * inverse * inverse * p1 + t * t * 3.0F * inverse * p2;
    }

    private static float sampleCatmullRom(float t, MolangInstance instance, List<FloatMolangExp> values,
                                         @org.jetbrains.annotations.Nullable float[] constants) {
        int size = values.size();
        if (size < 4) return 0.0F;
        if (t <= 0.0F) return node(1, instance, values, constants);
        if (t >= 1.0F) return node(size - 2, instance, values, constants);
        float position = t * (size - 3);
        int index = Mth.floor(position);
        float p0 = node(index, instance, values, constants);
        float p1 = node(index + 1, instance, values, constants);
        float p2 = node(index + 2, instance, values, constants);
        float p3 = node(index + 3, instance, values, constants);
        t = position - index;
        return ((((p1 - p2) * 3.0F + (p3 - p0)) * t
                + ((p2 * 4.0F - 5.0F * p1 + p0 + p0) - p3)) * t
                + (p2 - p0)) * t * 0.5F + p1;
    }

    private float sampleChain(float input) {
        if (segments.isEmpty()) return 0.0F;
        BezierSegment first = segments.get(0);
        if (input <= first.start()) return first.p0();
        BezierSegment last = segments.get(segments.size() - 1);
        if (last.end() <= input) return last.p3();
        for (BezierSegment segment : segments) {
            if (segment.start() <= input && input <= segment.end()) {
                float range = segment.end() - segment.start();
                if (range < Math.ulp(1.0F)) return segment.p0();
                return bezier((input - segment.start()) / range, segment.p0(), segment.p1(),
                        segment.p2(), segment.p3());
            }
        }
        return 0.0F;
    }

    private record BezierSegment(float start, float end, float p0, float p1, float p2, float p3) {}

    @Override
    public String toString() {
        return "ParticleCurve[" +
                "type=" + type + ", " +
                "input=" + input + ", " +
                "horizontalRange=" + horizontalRange + ", " +
                "nodes=" + nodes + ']';
    }
}
