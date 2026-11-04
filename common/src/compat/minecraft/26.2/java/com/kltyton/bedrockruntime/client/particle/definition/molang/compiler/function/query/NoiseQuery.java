package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

/** Samples the fixed two-dimensional Molang noise field. */
public final class NoiseQuery extends MathFunction {
    private static final float SKEW = Float.intBitsToFloat(0x3ebb67ae);
    private static final float UNSKEW = Float.intBitsToFloat(0x3e58658c);
    private static final int[][] GRADIENTS = {
            {1, 1}, {-1, 1}, {1, -1}, {-1, -1},
            {1, 0}, {-1, 0}, {1, 0}, {-1, 0},
            {0, 1}, {0, -1}, {0, 1}, {0, -1}
    };
    private static final int[] PERMUTATION = createPermutation();
    private final MathValue[] arguments;

    public NoiseQuery(MathValue... arguments) {
        super(arguments);
        this.arguments = arguments.clone();
    }

    @Override
    public double compute(MolangInstance instance) {
        if (arguments.length != 2) return 0.0;
        float y = (float) arguments[1].get(instance);
        float x = (float) arguments[0].get(instance);
        float skew = (y + x) * SKEW;
        int cellX = nativeFloor(x + skew);
        int cellY = nativeFloor(y + skew);
        float unskew = (float) (cellX + cellY) * UNSKEW;
        float x0 = x - ((float) cellX - unskew);
        float y0 = y - ((float) cellY - unskew);
        int stepX = x0 > y0 ? 1 : 0;
        int stepY = x0 > y0 ? 0 : 1;
        float x1 = UNSKEW + (x0 - stepX);
        float y1 = UNSKEW + (y0 - stepY);
        float x2 = UNSKEW + UNSKEW - 1.0F + x0;
        float y2 = UNSKEW + UNSKEW - 1.0F + y0;
        int ix = cellX & 255;
        int iy = cellY & 255;
        float first = contribution(x0, y0, PERMUTATION[ix + PERMUTATION[iy]]);
        float second = contribution(x1, y1, PERMUTATION[ix + stepX + PERMUTATION[iy + stepY]]);
        float third = contribution(x2, y2, PERMUTATION[ix + 1 + PERMUTATION[iy + 1]]);
        return (first + second + third) * 70.0F;
    }

    private static int nativeFloor(float value) {
        int truncated = value >= 0x1.0p31F || value < -0x1.0p31F || Float.isNaN(value)
                ? Integer.MIN_VALUE : (int) value;
        return truncated - (value <= 0.0F ? 1 : 0);
    }

    private static float contribution(float x, float y, int hash) {
        float weight = (0.5F - x * x) - y * y;
        if (!(weight >= 0.0F)) return 0.0F;
        int[] gradient = GRADIENTS[hash % 12];
        float dot = y * gradient[1] + x * gradient[0];
        return dot * weight * weight * weight * weight;
    }

    private static int[] createPermutation() {
        int[] state = new int[624];
        state[0] = 2345;
        for (int i = 1; i < state.length; i++) {
            int previous = state[i - 1];
            state[i] = 1812433253 * (previous ^ (previous >>> 30)) + i;
        }
        for (int i = 0; i < state.length; i++) {
            int mixed = (state[i] & 0x80000000) | (state[(i + 1) % 624] & 0x7fffffff);
            state[i] = state[(i + 397) % 624] ^ (mixed >>> 1) ^ ((mixed & 1) == 0 ? 0 : 0x9908b0df);
        }
        int[] permutation = new int[512];
        for (int i = 0; i < 256; i++) permutation[i] = i;
        for (int i = 0; i < 256; i++) {
            int random = temper(state[i + 3]);
            int selected = i + Integer.remainderUnsigned(random, 256 - i);
            int old = permutation[i];
            permutation[i] = permutation[selected];
            permutation[selected] = old;
            permutation[i + 256] = permutation[i];
        }
        return permutation;
    }

    private static int temper(int value) {
        value ^= value >>> 11;
        value ^= (value & 0x013a58ad) << 7;
        value ^= (value & 0x0001df8c) << 15;
        return value ^ (value >>> 18);
    }

    @Override public String getName() { return "query.noise"; }
    @Override public int getMinArgs() { return 0; }
    @Override public MathValue[] getArgs() { return arguments.clone(); }
    @Override public boolean isMutable(MathValue... values) { return true; }
}
