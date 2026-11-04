package com.kltyton.bedrockruntime.client.particle.definition.molang.compat.mochafloats.v611;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.ActorAccess;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.EvaluationScope;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.MolangStruct;
import com.kltyton.bedrockruntime.internal.mochafloats.parser.ast.*;
import com.kltyton.bedrockruntime.internal.mochafloats.runtime.*;
import com.kltyton.bedrockruntime.internal.mochafloats.runtime.value.*;
import net.minecraft.world.entity.Entity;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Per-evaluation bindings. A child interpreter changes only its current actor. */
final class MochaScope implements ContextualScope {
    private final MochaProgram program;
    private final Map<String, ObjectProperty> globals = new LinkedHashMap<>();
    private boolean readOnly;

    MochaScope(MochaProgram program) { this.program = program; }

    @Override
    public Value index(Value array, Value index, ExecutionContext<?> context) {
        if (!(array instanceof ArrayValue values)) return null;
        int selected = MochaValues.arrayIndex(index.getAsNumber(), values.values().length);
        return selected < 0 ? null : values.values()[selected];
    }

    @Override
    public Value binary(BinaryExpression expression, ExecutionContext<?> context) {
        return MochaOperators.evaluate(expression, context, program);
    }

    @Override
    public boolean truth(Value value) { return value.getAsNumber() != 0; }

    @Override
    public Value call(CallExpression expression, ExecutionContext<?> context) {
        return program.call(expression, context);
    }

    @Override
    public Value resolve(String name, ExecutionContext<?> context) {
        String canonical = canonical(name);
        MolangInstance owner = owner(context);
        return switch (canonical) {
            case "variable", "temp", "context", "query", "math" -> new Path(owner, canonical);
            case "this" -> NumberValue.of(EvaluationScope.getThis(owner));
            default -> globals.containsKey(canonical) ? globals.get(canonical).value() : null;
        };
    }

    @Override
    public Value access(Value object, String name, ExecutionContext<?> context, boolean callable) {
        if (object instanceof Path path) {
            String key = path.name + "." + name.toLowerCase(Locale.ROOT);
            return read(path.owner, key);
        }
        if (object instanceof ObjectValue fields) {
            ObjectProperty property = fields.getProperty(name);
            return property == null ? null : property.value();
        }
        return null;
    }

    private Value read(MolangInstance instance, String name) {
        Object raw = raw(instance, name);
        if (raw == null) return null;
        if (raw instanceof MolangStruct || raw instanceof Map<?, ?>) return new Path(instance, name);
        return MochaValues.encode(raw);
    }

    private Object raw(MolangInstance instance, String name) {
        return program.read(name, instance);
    }

    @Override
    public void assign(Expression target, Value value, ExecutionContext<?> context) {
        String path = path(target);
        if (path != null && (path.startsWith("variable.") || path.startsWith("temp."))) {
            write(owner(context), path, MochaValues.decode(value));
            return;
        }
        Target binding = target(target, context);
        if (binding != null) binding.writer.accept(value);
    }

    private record Target(Value value, java.util.function.Consumer<Value> writer) {}

    private Target target(Expression expression, ExecutionContext<?> context) {
        String path = path(expression);
        if (path != null && (path.startsWith("variable.") || path.startsWith("temp."))) {
            MolangInstance instance = owner(context);
            return new Target(read(instance, path), value -> write(instance, path, MochaValues.decode(value)));
        }
        if (expression instanceof ArrayAccessExpression access) {
            Target parent = target(access.array(), context);
            Value index = context.eval(access.index());
            if (parent == null || !(parent.value instanceof ArrayValue array) || array.values().length == 0) return null;
            int offset = MochaValues.arrayIndex(index.getAsNumber(), array.values().length);
            return new Target(array.values()[offset], value -> {
                array.values()[offset] = value;
                parent.writer.accept(array);
            });
        }
        if (expression instanceof BinaryExpression binary && binary.op() == BinaryExpression.Op.ARROW) {
            Value result = context.eval(binary);
            if (result instanceof Path binding) {
                return new Target(result, value -> write(binding.owner, binding.name, MochaValues.decode(value)));
            }
            throw new IllegalArgumentException("Actor assignment requires a public variable reference");
        }
        if (expression instanceof AccessExpression access) {
            Target parent = target(access.object(), context);
            if (parent == null) return null;
            Value container = parent.value;
            if (container instanceof Path binding) {
                String field = binding.name + "." + access.property();
                return new Target(read(binding.owner, field), value -> write(binding.owner, field, MochaValues.decode(value)));
            }
            if (container instanceof ObjectValue fields) {
                ObjectProperty property = fields.getProperty(access.property());
                return new Target(property == null ? null : property.value(), value -> {
                    if (!fields.set(access.property(), value)) {
                        throw new UnsupportedOperationException("MoLang object is read-only: " + access.property());
                    }
                    parent.writer.accept(container);
                });
            }
        }
        throw new IllegalArgumentException("Molang assignment requires a writable variable: " + expression);
    }

    private void write(MolangInstance instance, String name, Object value) {
        if (name.startsWith("temp.")) {
            EvaluationScope.setTemporaryValue(name, value);
        } else if (name.startsWith("variable.")) {
            if (ActorAccess.isActorReference(instance)) {
                if (!ActorAccess.writeReference(instance, name, value)) {
                    throw new UnsupportedOperationException("Actor variable has no public writer: " + name);
                }
            } else {
                instance.getVars().setRawValue(name, value, instance);
            }
        } else {
            throw new IllegalArgumentException("Molang namespace is read-only: " + name);
        }
    }

    @Override
    public Object reference(Value value, ExecutionContext<?> context) {
        return ActorAccess.reference(owner(context), MochaValues.decode(value));
    }

    @Override
    public boolean canIterate(Value value) {
        Object raw = MochaValues.decode(value);
        return raw != null && (!(raw instanceof Entity entity) || !entity.isRemoved());
    }

    @Override
    public float loopLimit() { return program.version().atLeast(1, 26, 50) ? Float.POSITIVE_INFINITY : 1024; }

    @Override
    public ContextualScope copy() {
        MochaScope copy = new MochaScope(program);
        copy.globals.putAll(globals);
        copy.readOnly = readOnly;
        return copy;
    }

    @Override public void readOnly(boolean value) { readOnly = value; }
    @Override public boolean readOnly() { return readOnly; }
    @Override public ObjectProperty getProperty(String name) { return globals.get(canonical(name)); }
    @Override public Map<String, ObjectProperty> entries() { return Map.copyOf(globals); }
    @Override public boolean set(String name, Value value) {
        if (readOnly) return false;
        if (value == null) globals.remove(canonical(name));
        else globals.put(canonical(name), ObjectProperty.property(value, false));
        return true;
    }

    static MolangInstance owner(ExecutionContext<?> context) {
        if (context.entity() instanceof MolangInstance instance) return instance;
        throw new IllegalStateException("Missing Molang instance in the execution frame");
    }

    static String path(Expression expression) {
        if (expression instanceof IdentifierExpression identifier) return canonical(identifier.name());
        if (expression instanceof AccessExpression access) {
            String parent = path(access.object());
            return parent == null ? null : parent + "." + access.property().toLowerCase(Locale.ROOT);
        }
        return null;
    }

    static String canonical(String name) {
        String value = name.toLowerCase(Locale.ROOT);
        return switch (value) {
            case "v" -> "variable";
            case "t" -> "temp";
            case "c" -> "context";
            case "q" -> "query";
            default -> value;
        };
    }

    final class Path implements ObjectValue {
        final MolangInstance owner;
        final String name;
        Path(MolangInstance owner, String name) { this.owner = owner; this.name = name; }
        Object raw() { return MochaScope.this.raw(owner, name); }
        @Override public ObjectProperty getProperty(String field) {
            Value value = read(owner, name + "." + field.toLowerCase(Locale.ROOT));
            return value == null ? null : ObjectProperty.property(value, false);
        }
        @Override public boolean set(String field, Value value) {
            write(owner, name + "." + field, MochaValues.decode(value));
            return true;
        }
        @Override public Map<String, ObjectProperty> entries() {
            Object raw = raw();
            if (!(raw instanceof MolangStruct structure)) return Map.of();
            Map<String, ObjectProperty> result = new LinkedHashMap<>();
            for (String field : structure.fieldNames()) result.put(field, getProperty(field));
            return result;
        }
    }
}
