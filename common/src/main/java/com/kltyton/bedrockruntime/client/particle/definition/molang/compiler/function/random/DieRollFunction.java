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
 * Returns a random value based on the input values:
 * <ul>
 *     <li>Three inputs generates the sum of <i>n</i> (first input) random values between the second (inclusive) and third input (exclusive)</li>
 *     <li>Four inputs generates the sum of <i>n</i> (first input) random values between the second (inclusive) and third input (exclusive), seeded by the fourth input</li>
 * </ul>
 */
public final class DieRollFunction extends MathFunction {
    private final MathValue rolls;
    private final MathValue min;
    private final MathValue max;
    @Nullable
    private final MathValue seed;
    @Nullable
    private final Random random;

    public DieRollFunction(MathValue... values) {
        super(values);

        this.rolls = values[0];
        this.min = values[1];
        this.max = values[2];
        this.seed = values.length >= 4 ? values[3] : null;
        this.random = this.seed != null ? new Random() : null;
    }

    @Override
    public String getName() {
        return "math.die_roll";
    }

    @Override
    public double compute(MolangInstance instance) {
        float rolls = (float) this.rolls.get(instance);
        float first = (float) StrictMath.floor((float) this.min.get(instance));
        float second = (float) StrictMath.floor((float) this.max.get(instance));
        float min = first <= second ? first : second;
        float max = first <= second ? second : first;
        Random random;
        if (this.random != null) {
            this.random.setSeed((long) this.seed.get(instance));
            random = this.random;
        } else {
            random = ThreadLocalRandom.current();
        }
        int count = rolls >= 1.0F && rolls < 0x1.0p31F ? (int) rolls : 0;
        float sum = 0.0F;
        for (int i = 0; i < count; i++) {
            float unit = random.nextFloat();
            sum = sum + unit * max + (1.0F - unit) * min;
        }
        return sum;
    }

    @Override
    public boolean isMutable(MathValue... values) {
        if (values.length < 4)
            return true;

        return super.isMutable(values);
    }

    @Override
    public int getMinArgs() {
        return 3;
    }

    @Override
    public MathValue[] getArgs() {
        if (this.seed != null)
            return new MathValue[] {this.rolls, this.min, this.max, this.seed};

        return new MathValue[] {this.rolls, this.min, this.max};
    }
}
