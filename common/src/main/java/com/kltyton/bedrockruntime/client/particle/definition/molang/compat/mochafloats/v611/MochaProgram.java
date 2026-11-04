package com.kltyton.bedrockruntime.client.particle.definition.molang.compat.mochafloats.v611;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangFunctions;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangQueries;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangVersion;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.UndefinedValueScope;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.Constant;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.StringValue;
import com.kltyton.bedrockruntime.internal.mochafloats.parser.ast.*;
import com.kltyton.bedrockruntime.internal.mochafloats.runtime.ExecutionContext;
import com.kltyton.bedrockruntime.internal.mochafloats.runtime.ExpressionInterpreter;
import com.kltyton.bedrockruntime.internal.mochafloats.runtime.value.Value;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.concurrent.ConcurrentHashMap;

/** Owns the upstream AST and host function bindings for one compiled expression. */
public final class MochaProgram implements MathValue {
    private final String source;
    private final List<Expression> expressions;
    private final MolangVersion version;
    private final boolean bedrockItemNames;
    private final boolean complex;
    private final Map<CallExpression, MathValue> calls = new IdentityHashMap<>();
    private final Map<String, MathValue> queries = new ConcurrentHashMap<>();
    private final Map<String, MathValue> bindings = new ConcurrentHashMap<>();
    private final ThreadLocal<ArrayDeque<ExecutionContext<?>>> contexts = ThreadLocal.withInitial(ArrayDeque::new);
    private boolean immutable;
    private final Function<MolangInstance, Object> evaluateInitial = this::evaluate;

    public MochaProgram(String source, MolangVersion version, boolean bedrockItemNames, boolean complex) {
        this.source = source;
        this.version = version;
        this.bedrockItemNames = bedrockItemNames;
        this.complex = complex;
        try {
            this.expressions = List.copyOf(com.kltyton.bedrockruntime.internal.mochafloats.parser.MolangParser.parseAll(source,
                    new com.kltyton.bedrockruntime.internal.mochafloats.parser.ParserDialect(!version.atLeast(1, 18, 20), !version.atLeast(1, 18, 10))));
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to parse MoLang source", exception);
        }
        expressions.forEach(this::bind);
    }

    MolangVersion version() { return version; }

    boolean mutable(Expression expression) {
        if (expression instanceof FloatExpression || expression instanceof StringExpression) return false;
        if (expression instanceof CallExpression call) {
            MathValue function = calls.get(call);
            return function == null || function.isMutable();
        }
        if (expression instanceof UnaryExpression unary) return mutable(unary.expression());
        if (expression instanceof BinaryExpression binary) return mutable(binary.left()) || mutable(binary.right());
        if (expression instanceof TernaryConditionalExpression branch) {
            return mutable(branch.condition()) || mutable(branch.trueExpression()) || mutable(branch.falseExpression());
        }
        return true;
    }

    private void bind(Expression expression) {
        if (expression instanceof CallExpression call) {
            call.arguments().forEach(this::bind);
            String name = MochaScope.path(call.function());
            if (name != null && !name.equals("loop") && !name.equals("for_each")) {
                String canonical = MolangQueries.applyQueryAliases(name);
                checkVersion(canonical);
                if (!MolangFunctions.contains(canonical)) {
                    throw new IllegalArgumentException("Unknown MoLang function: " + canonical);
                }
                List<MathValue> arguments = call.arguments().stream()
                        .map(this::argument).toList();
                calls.put(call, MolangFunctions.create(canonical, arguments, version, bedrockItemNames));
            }
            return;
        }
        if (expression instanceof AccessExpression access) {
            String name = MochaScope.path(access);
            if (name != null && name.startsWith("query.")) checkVersion(MolangQueries.applyQueryAliases(name));
            bind(access.object());
        } else if (expression instanceof ArrayAccessExpression array) {
            bind(array.array());
            bind(array.index());
        } else if (expression instanceof BinaryExpression binary) {
            bind(binary.left());
            bind(binary.right());
        } else if (expression instanceof UnaryExpression unary) {
            bind(unary.expression());
        } else if (expression instanceof TernaryConditionalExpression ternary) {
            bind(ternary.condition());
            bind(ternary.trueExpression());
            bind(ternary.falseExpression());
        } else if (expression instanceof ExecutionScopeExpression block) {
            block.expressions().forEach(this::bind);
        }
    }

    private MathValue argument(Expression expression) {
        if (expression instanceof FloatExpression literal) return new Constant(literal.value());
        if (expression instanceof StringExpression literal) return new StringValue(literal.value());
        return new Argument(expression);
    }

    private void checkVersion(String name) {
        if (version.atLeast(1, 20, 50) && (name.equals("query.is_scenting")
                || name.equals("query.is_rising") || name.equals("query.is_feeling_happy"))) {
            throw new IllegalArgumentException(name + " is unavailable since MoLang 1.20.50");
        }
    }

    MathValue query(String name) {
        return queries.computeIfAbsent(name, key -> MolangFunctions.contains(key)
                ? MolangFunctions.create(key, List.of(), version, bedrockItemNames)
                : MolangQueries.getQueryFor(key, version));
    }

    Object read(String name, MolangInstance instance) {
        MathValue binding = bindings.computeIfAbsent(name, key -> {
            if (key.startsWith("query.") || key.startsWith("math.")) return query(key);
            if (key.startsWith("context.")) {
                return new com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.ContextValue(key);
            }
            if (key.startsWith("variable.") || key.startsWith("temp.")) {
                return new com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.Variable(
                        key, owner -> owner.getVars().getValue(key, owner));
            }
            throw new IllegalArgumentException("Unknown MoLang namespace: " + key);
        });
        return UndefinedValueScope.evaluate(binding, instance);
    }

    Value call(CallExpression expression, ExecutionContext<?> context) {
        MathValue function = calls.get(expression);
        if (function == null) return null;
        ArrayDeque<ExecutionContext<?>> stack = contexts.get();
        stack.push(context);
        try {
            return MochaValues.encode(UndefinedValueScope.evaluate(function, MochaScope.owner(context)));
        } finally {
            stack.pop();
            if (stack.isEmpty()) contexts.remove();
        }
    }

    @Override
    public Object getRaw(MolangInstance instance) {
        return immutable ? instance.getVars().initialValue(this, instance, evaluateInitial) : evaluate(instance);
    }

    private Object evaluate(MolangInstance instance) {
        MochaScope scope = new MochaScope(this);
        ExpressionInterpreter<MolangInstance> interpreter = new ExpressionInterpreter<>(instance, scope);
        return MochaValues.decode(interpreter.evaluateScript(expressions, complex));
    }

    @Override
    public double get(MolangInstance instance) {
        Object value = getRaw(instance);
        return value instanceof Number number ? number.floatValue() : 0.0;
    }

    @Override public void markImmutable() { immutable = true; }
    @Override public boolean isMutable() { return !immutable; }
    @Override public String toString() { return source; }

    private final class Argument implements MathValue {
        private final Expression expression;
        private boolean frozen;
        private Argument(Expression expression) { this.expression = expression; }

        @Override
        public Object getRaw(MolangInstance instance) {
            ExecutionContext<?> current = contexts.get().peek();
            if (current == null) throw new IllegalStateException("MoLang argument evaluated outside its call");
            if (instance != current.entity() && current instanceof ExpressionInterpreter<?> interpreter) {
                current = interpreter.createChild(instance);
            }
            return MochaValues.decode(current.eval(expression));
        }

        @Override
        public double get(MolangInstance instance) {
            Object value = getRaw(instance);
            return value instanceof Number number ? number.floatValue() : 0.0;
        }

        @Override public boolean isMutable() {
            return !frozen && mutable(expression);
        }
        @Override public void markImmutable() { frozen = true; }
        @Override public String toString() { return expression.toString(); }
    }
}
