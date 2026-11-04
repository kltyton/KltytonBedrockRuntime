package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.round;

import net.minecraft.util.Mth;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

/**
 * {@link MathFunction} value supplier
 *
 * <p>
 * <b>Contract:</b>
 * <br>
 * Returns the first value plus the difference between the first and second input values multiplied by the third input value, wrapping the end result as a degrees value
 */
public final class LerpRotFunction extends MathFunction {
    private final MathValue min;
    private final MathValue max;
    private final MathValue delta;

    public LerpRotFunction(MathValue... values) {
        super(values);

        this.min = values[0];
        this.max = values[1];
        this.delta = values[2];
    }

    @Override
    public String getName() {
        return "math.lerprotate";
    }

    @Override
    public double compute(MolangInstance instance) {
        float min = (float) this.min.get(instance);
        float max = (float) this.max.get(instance);
        float delta = (float) this.delta.get(instance);
        return com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.math.AngleWrapping.degrees(max - min) * delta + min;
    }

    @Override
    public int getMinArgs() {
        return 3;
    }

    @Override
    public MathValue[] getArgs() {
        return new MathValue[] {this.min, this.max, this.delta};
    }

    public static double lerpYaw(double delta, double start, double end) {
        start = Mth.wrapDegrees(start);
        end = Mth.wrapDegrees(end);
        double diff = start - end;
        end = diff > 180 || diff < -180 ? start + Math.copySign(360 - Math.abs(diff), diff) : end;

        return Mth.lerp(delta, start, end);
    }
}
