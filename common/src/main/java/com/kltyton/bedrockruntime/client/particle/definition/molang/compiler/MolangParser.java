package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler;

import com.kltyton.bedrockruntime.client.particle.definition.molang.VariableTable;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compat.mochafloats.v611.MochaProgram;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.Constant;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.EvaluationScope;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.UndefinedValueScope;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.Variable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/** Compiles particle expressions through the pinned mochafloats parser and KBR host bindings. */
public final class MolangParser {
    private final VariableTable table;
    private final MolangVersion version;
    private final boolean bedrockItemNames;

    public MolangParser(VariableTable table) { this(table, MolangVersion.LATEST); }
    public MolangParser(VariableTable table, MolangVersion version) { this(table, version, false); }
    public MolangParser(VariableTable table, MolangVersion version, boolean bedrockItemNames) {
        this.table = Objects.requireNonNull(table, "table");
        this.version = Objects.requireNonNull(version, "version");
        this.bedrockItemNames = bedrockItemNames;
    }

    public MathValue compileMolang(String expression) { return compile(expression, true); }
    public MathValue compileExpression(String expression) { return compile(expression, false); }

    private MathValue compile(String expression, boolean script) {
        Objects.requireNonNull(expression, "expression");
        if (expression.isBlank()) return new Constant(0.0);
        String normalized = normalizeCase(expression);
        return UndefinedValueScope.boundary(new EvaluationScope(
                new MochaProgram(normalized, version, bedrockItemNames, script && hasTopLevelStatement(normalized))));
    }

    public Variable getVariableFor(String name) {
        String canonical = MolangQueries.applyQueryAliases(name.toLowerCase(Locale.ROOT));
        if (canonical.startsWith("query.")) return MolangQueries.getQueryFor(canonical, version);
        canonical = MolangQueries.applyPrefixAliases(canonical, "temp.", "t.");
        if (canonical.startsWith("temp.")) {
            String temporary = canonical;
            return new Variable(temporary, instance -> EvaluationScope.getTemporary(temporary));
        }
        String variable = MolangQueries.applyPrefixAliases(canonical, "variable.", "v.");
        return new Variable(variable, instance -> instance.getVars().getValue(variable, instance));
    }

    public boolean isFunctionRegistered(String name) {
        String canonical = MolangQueries.applyQueryAliases(name.toLowerCase(Locale.ROOT));
        return canonical.equals("loop") || canonical.equals("for_each") || MolangFunctions.contains(canonical);
    }

    public <T extends MathFunction> T buildFunction(String name, MathValue... values) {
        String canonical = MolangQueries.applyQueryAliases(name.toLowerCase(Locale.ROOT));
        if (!MolangFunctions.contains(canonical)) return null;
        MathValue function = MolangFunctions.create(canonical, Arrays.asList(values), version, bedrockItemNames);
        if (function instanceof MathFunction math) {
            @SuppressWarnings("unchecked") T result = (T) math;
            return result;
        }
        return null;
    }

    public static void registerFunction(String name, MathFunction.Factory<?> factory) {
        MolangFunctions.register(name, factory);
    }

    public VariableTable getTable() { return table; }

    private static String normalizeCase(String expression) {
        StringBuilder result = new StringBuilder(expression.length());
        boolean quoted = false;
        for (int offset = 0; offset < expression.length(); offset++) {
            char character = expression.charAt(offset);
            result.append(!quoted && character >= 'A' && character <= 'Z'
                    ? (char) (character + ('a' - 'A')) : character);
            if (character == '\\' && offset + 1 < expression.length()) result.append(expression.charAt(++offset));
            else if (character == '\'') quoted = !quoted;
        }
        return result.toString();
    }

    private static boolean hasTopLevelStatement(String expression) {
        boolean quoted = false;
        int parentheses = 0;
        int blocks = 0;
        for (int offset = 0; offset < expression.length(); offset++) {
            char character = expression.charAt(offset);
            if (character == '\\' && offset + 1 < expression.length()) { offset++; continue; }
            if (character == '\'') { quoted = !quoted; continue; }
            if (quoted) continue;
            if (character == '(') parentheses++;
            else if (character == ')') parentheses--;
            else if (character == '{') blocks++;
            else if (character == '}') blocks--;
            else if (character == ';' && parentheses == 0 && blocks == 0) return true;
        }
        return false;
    }
}
