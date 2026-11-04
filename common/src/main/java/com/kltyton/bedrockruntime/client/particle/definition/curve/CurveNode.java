package com.kltyton.bedrockruntime.client.particle.definition.curve;

import com.kltyton.bedrockruntime.client.particle.definition.CodecCompat;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.particle.BedrockParticles;

import java.util.Optional;

/** A bezier-chain node with optional values and slopes for each side. */
public record CurveNode(float value, float slope, boolean hasValue, boolean hasSlope,
                        @Nullable Float leftValue, @Nullable Float rightValue,
                        @Nullable Float leftSlope, @Nullable Float rightSlope) {
    private static final Codec<Float> NODE_NUMBER = Codec.either(Codec.FLOAT, Codec.STRING).comapFlatMap(
            number -> number.map(DataResult::success, text -> {
                try {
                    return DataResult.success(Float.parseFloat(text.trim()));
                } catch (NumberFormatException exception) {
                    return DataResult.error(() -> "Invalid bezier chain number: " + text);
                }
            }), Either::left);
    public static final Codec<CurveNode> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CodecCompat.lenientOptionalFieldOf(NODE_NUMBER, "value").forGetter(node -> node.hasValue ? Optional.of(node.value) : Optional.empty()),
            CodecCompat.lenientOptionalFieldOf(NODE_NUMBER, "slope").forGetter(node -> node.hasSlope ? Optional.of(node.slope) : Optional.empty()),
            CodecCompat.lenientOptionalFieldOf(NODE_NUMBER, "left_value").forGetter(node -> Optional.ofNullable(node.leftValue)),
            CodecCompat.lenientOptionalFieldOf(NODE_NUMBER, "right_value").forGetter(node -> Optional.ofNullable(node.rightValue)),
            CodecCompat.lenientOptionalFieldOf(NODE_NUMBER, "left_slope").forGetter(node -> Optional.ofNullable(node.leftSlope)),
            CodecCompat.lenientOptionalFieldOf(NODE_NUMBER, "right_slope").forGetter(node -> Optional.ofNullable(node.rightSlope))
    ).apply(instance, CurveNode::fromFields));

    public CurveNode(float value, float slope) {
        this(value, slope, true, true, null, null, null, null);
    }

    private static CurveNode fromFields(Optional<Float> value, Optional<Float> slope,
                                        Optional<Float> leftValue, Optional<Float> rightValue,
                                        Optional<Float> leftSlope, Optional<Float> rightSlope) {
        if (value.isEmpty() && (leftValue.isEmpty() || rightValue.isEmpty())) {
            BedrockParticles.LOGGER.warn("Bezier chain node requires value or both left_value and right_value");
        }
        if (slope.isEmpty() && (leftSlope.isEmpty() || rightSlope.isEmpty())) {
            BedrockParticles.LOGGER.warn("Bezier chain node requires slope or both left_slope and right_slope");
        }
        return new CurveNode(value.orElseGet(() -> leftValue.orElseGet(() -> rightValue.orElse(0.0F))),
                slope.orElseGet(() -> leftSlope.orElseGet(() -> rightSlope.orElse(0.0F))),
                value.isPresent(), slope.isPresent(), leftValue.orElse(null), rightValue.orElse(null),
                leftSlope.orElse(null), rightSlope.orElse(null));
    }

    public float resolvedLeftValue() { return hasValue ? value : leftValue == null ? 0.0F : leftValue; }
    public float resolvedRightValue() { return hasValue ? value : rightValue == null ? 0.0F : rightValue; }
    public float resolvedLeftSlope() { return hasSlope ? slope : leftSlope == null ? 0.0F : leftSlope; }
    public float resolvedRightSlope() { return hasSlope ? slope : rightSlope == null ? 0.0F : rightSlope; }

    public CurveNode withValue(float value) {
        return new CurveNode(value, slope, true, hasSlope, leftValue, rightValue, leftSlope, rightSlope);
    }

    public CurveNode withSlope(float slope) {
        return new CurveNode(value, slope, hasValue, true, leftValue, rightValue, leftSlope, rightSlope);
    }
}
