package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.random;

import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * {@link MathFunction} value supplier
 *
 * <p>
 * <b>Contract:</b>
 * <br>
 * Returns a random integer value based on the input values:
 * <ul>
 *     <li>A single input generates a value between 0 and that input (exclusive)</li>
 *     <li>Two inputs generates a random value between the first (inclusive) and second input (inclusive)</li>
 *     <li>Three inputs generates a random value between the first (inclusive) and second input (inclusive), seeded by the third input</li>
 * </ul>
 */
public final class RandomIntegerFunction extends MathFunction {
    private final MathValue valueA;
    @Nullable
    private final MathValue valueB;
    @Nullable
    private final MathValue seed;
    @Nullable
    private final Random random;

    public RandomIntegerFunction(MathValue... values) {
        super(values);

        this.valueA = values[0];
        this.valueB = values.length >= 2 ? values[1] : null;
        this.seed = values.length >= 3 ? values[2] : null;
        this.random = this.seed != null ? new Random() : null;
    }

    @Override
    public String getName() {
        return "math.random_integer";
    }

    @Override
    public double compute(MolangInstance instance) {
        if (this.valueB == null) {
            int max = (int) Math.round(this.valueA.get(instance));
            Random random = ThreadLocalRandom.current();
            return max >= 0 && max < Integer.MAX_VALUE ? random.nextInt(0, max + 1)
                    : randomInclusive(random, Math.min(0, max), Math.max(0, max));
        }
        float first = (float) this.valueA.get(instance);
        float second = (float) this.valueB.get(instance);
        float min = first <= second ? first : second;
        float max = first <= second ? second : first;
        Random random;
        if (this.random != null) {
            this.random.setSeed((long) this.seed.get(instance));
            random = this.random;
        } else {
            random = ThreadLocalRandom.current();
        }
        return sampleInteger(random, min, max, upperBound(max));
    }

    static float upperBound(float max) {
        return -0x1.0p-23F * max + 1.0F + max;
    }

    static float sampleInteger(Random random, float min, float max, float upper) {
        float unit = random.nextFloat();
        float sample = (float) StrictMath.floor(upper * unit + (1.0F - unit) * min);
        float clamped = sample <= min ? min : sample;
        return max < sample ? max : clamped;
    }

    static int randomInclusive(Random random, int min, int max) {
        long width = (long) max - min + 1L;
        return width <= Integer.MAX_VALUE
                ? min + random.nextInt((int) width)
                : (int) (min + random.nextLong(width));
    }

    @Override
    public boolean isMutable(MathValue... values) {
        if (values.length < 3)
            return true;

        return super.isMutable(values);
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public MathValue[] getArgs() {
        if (this.seed != null)
            return new MathValue[] {this.valueA, this.valueB, this.seed};

        if (this.valueB != null)
            return new MathValue[] {this.valueA, this.valueB};

        return new MathValue[] {this.valueA};
    }
}
