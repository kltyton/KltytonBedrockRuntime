package com.kltyton.bedrockruntime.internal.mochafloats.runtime;

import com.kltyton.bedrockruntime.internal.mochafloats.parser.ast.Expression;
import com.kltyton.bedrockruntime.internal.mochafloats.parser.ast.CallExpression;
import com.kltyton.bedrockruntime.internal.mochafloats.parser.ast.BinaryExpression;
import com.kltyton.bedrockruntime.internal.mochafloats.runtime.value.Value;

/** Host bindings for actor-aware evaluation without changing ordinary Scope behavior. */
public interface ContextualScope extends Scope {
    Value index(Value array, Value index, ExecutionContext<?> context);
    Value binary(BinaryExpression expression, ExecutionContext<?> context);
    boolean truth(Value value);
    Value call(CallExpression expression, ExecutionContext<?> context);
    Value resolve(String name, ExecutionContext<?> context);
    Value access(Value object, String name, ExecutionContext<?> context, boolean callable);
    void assign(Expression target, Value value, ExecutionContext<?> context);
    Object reference(Value value, ExecutionContext<?> context);
    boolean canIterate(Value value);
    float loopLimit();

    @Override
    ContextualScope copy();
}
