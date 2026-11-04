package com.kltyton.bedrockruntime.internal.mochafloats.parser;

import com.kltyton.bedrockruntime.internal.mochafloats.parser.ast.BinaryExpression;

/** Resource-declared operator rules; scoped to a parser call on the current thread. */
public record ParserDialect(boolean legacyPrecedence, boolean legacyTernary) {
    public static final ParserDialect MODERN = new ParserDialect(false, false);
    private static final ThreadLocal<ParserDialect> CURRENT = new ThreadLocal<>();

    static ParserDialect current() {
        ParserDialect dialect = CURRENT.get();
        return dialect == null ? MODERN : dialect;
    }
    static ParserDialect enter(ParserDialect dialect) {
        ParserDialect previous = CURRENT.get();
        CURRENT.set(dialect);
        return previous;
    }
    static void restore(ParserDialect dialect) {
        if (dialect == null) CURRENT.remove();
        else CURRENT.set(dialect);
    }
    int precedence(BinaryExpression.Op op) {
        if (legacyPrecedence) {
            return switch (op) {
                case LT, LTE, GT, GTE, EQ, NEQ -> 700;
                case AND -> 200;
                case OR -> 300;
                default -> op.precedence();
            };
        }
        return op.precedence();
    }
}
