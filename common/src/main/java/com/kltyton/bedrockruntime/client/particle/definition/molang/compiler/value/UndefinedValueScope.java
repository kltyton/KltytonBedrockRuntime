package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;

/** Routes missing-value jumps to a coalescing operand or the current expression boundary. */
public final class UndefinedValueScope {
    private static final ThreadLocal<Integer> DEPTH = new ThreadLocal<>();

    private UndefinedValueScope() {
    }

    public static boolean isActive() {
        return DEPTH.get() != null;
    }

    public static MathValue boundary(MathValue expression) {
        return expression instanceof Constant || expression instanceof StringValue ? expression : new Boundary(expression);
    }

    public static Object evaluate(MathValue expression, MolangInstance instance) {
        Integer previous = DEPTH.get();
        DEPTH.set(previous == null ? 1 : previous + 1);
        try {
            return MathValue.resolveRaw(expression, instance);
        } catch (MissingValueSignal missing) {
            return null;
        } finally {
            if (previous == null) DEPTH.remove();
            else DEPTH.set(previous);
        }
    }

    public static Object missing() {
        if (isActive()) throw MissingValueSignal.INSTANCE;
        return null;
    }

    public static double missingNumber() {
        missing();
        return 0.0;
    }

    private static final class Boundary implements MathValue {
        private final MathValue expression;
        private final java.util.concurrent.atomic.AtomicBoolean reported = new java.util.concurrent.atomic.AtomicBoolean();

        private Boundary(MathValue expression) {
            this.expression = expression;
        }

        @Override
        public double get(MolangInstance instance) {
            Integer previous = DEPTH.get();
            DEPTH.set(previous == null ? 1 : previous + 1);
            try {
                return expression.get(instance);
            } catch (MissingValueSignal missing) {
                report(instance);
                return 0.0;
            } finally {
                if (previous == null) DEPTH.remove();
                else DEPTH.set(previous);
            }
        }

        @Override
        public Object getRaw(MolangInstance instance) {
            Integer previous = DEPTH.get();
            DEPTH.set(previous == null ? 1 : previous + 1);
            try {
                return MathValue.resolveRaw(expression, instance);
            } catch (MissingValueSignal missing) {
                report(instance);
                return 0.0;
            } finally {
                if (previous == null) DEPTH.remove();
                else DEPTH.set(previous);
            }
        }

        private void report(MolangInstance instance) {
            if (reported.compareAndSet(false, true)) {
                com.kltyton.bedrockruntime.particle.BedrockParticles.LOGGER.warn("Undefined Molang value in effect {}", instance.getIdentity());
            }
        }

        @Override
        public boolean isMutable() {
            return expression.isMutable();
        }

        @Override
        public void markImmutable() {
            expression.markImmutable();
        }
    }

    private static final class MissingValueSignal extends RuntimeException {
        private static final MissingValueSignal INSTANCE = new MissingValueSignal();

        private MissingValueSignal() {
            super(null, null, false, false);
        }
    }
}
