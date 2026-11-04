package com.kltyton.bedrockruntime.client.particle.definition.molang.compat.mochafloats.v611;

import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.MolangValueEquality;
import com.kltyton.bedrockruntime.internal.mochafloats.parser.ast.BinaryExpression;
import com.kltyton.bedrockruntime.internal.mochafloats.runtime.ExecutionContext;
import com.kltyton.bedrockruntime.internal.mochafloats.runtime.value.NumberValue;
import com.kltyton.bedrockruntime.internal.mochafloats.runtime.value.Value;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import net.minecraft.world.entity.Entity;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Bedrock version rules that differ from the upstream interpreter's generic conversions. */
final class MochaOperators {
    private static final Set<String> REPORTED_TYPES = ConcurrentHashMap.newKeySet();
    private MochaOperators() {}

    static Value evaluate(BinaryExpression expression, ExecutionContext<?> context, MochaProgram program) {
        BinaryExpression.Op op = expression.op();
        if (op == BinaryExpression.Op.ASSIGN || op == BinaryExpression.Op.ARROW
                || op == BinaryExpression.Op.NULL_COALESCE || op == BinaryExpression.Op.CONDITIONAL) return null;
        if (op == BinaryExpression.Op.AND) {
            return Value.of(context.eval(expression.left()).getAsNumber() != 0
                    && context.eval(expression.right()).getAsNumber() != 0);
        }
        if (op == BinaryExpression.Op.OR) {
            return Value.of(context.eval(expression.left()).getAsNumber() != 0
                    || context.eval(expression.right()).getAsNumber() != 0);
        }
        Object left = MochaValues.decode(context.eval(expression.left()));
        Object right = MochaValues.decode(context.eval(expression.right()));
        if (op == BinaryExpression.Op.EQ || op == BinaryExpression.Op.NEQ) {
            boolean equal;
            if (left instanceof String a && right instanceof String b) {
                equal = MolangValueEquality.sameString(a, b);
            } else if (left instanceof String a && right instanceof Number b) {
                equal = Float.intBitsToFloat((int) MolangValueEquality.hash(a)) == b.floatValue();
            } else if (left instanceof Number a && right instanceof String b) {
                equal = Integer.toUnsignedLong(Float.floatToRawIntBits(a.floatValue())) == MolangValueEquality.hash(b);
            } else if (left instanceof Entity || right instanceof Entity) {
                equal = left instanceof Entity && right instanceof Entity && left == right;
            } else {
                equal = number(left) == number(right);
            }
            return Value.of(equal == (op == BinaryExpression.Op.EQ));
        }
        if (program.version().atLeast(1, 17, 40) && (left instanceof String || right instanceof String)) {
            String resource = MochaScope.owner(context).getIdentity().toString();
            if (REPORTED_TYPES.add(resource + ":" + op)) {
                BedrockParticles.LOGGER.warn("Invalid MoLang operands for '{}' in particle effect {}", op, resource);
            }
            return Value.nil();
        }
        float a = number(left);
        float b = number(right);
        if (op == BinaryExpression.Op.DIV && !program.version().atLeast(1, 19, 60)
                && program.mutable(expression.right()) && b < 0) b = -b;
        return switch (op) {
            case ADD -> NumberValue.of(a + b);
            case SUB -> NumberValue.of(a - b);
            case MUL -> NumberValue.of(a * b);
            case DIV -> NumberValue.of(b == 0 ? 0 : a / b);
            case MOD -> NumberValue.of(b == 0 ? a : a % b);
            case POW -> NumberValue.of(Math.pow(a, b));
            case LT -> Value.of(a < b);
            case LTE -> Value.of(a <= b);
            case GT -> Value.of(a > b);
            case GTE -> Value.of(a >= b);
            default -> throw new IllegalArgumentException("Unsupported numeric operator: " + op);
        };
    }

    private static float number(Object value) { return value instanceof Number number ? number.floatValue() : 0; }
}
