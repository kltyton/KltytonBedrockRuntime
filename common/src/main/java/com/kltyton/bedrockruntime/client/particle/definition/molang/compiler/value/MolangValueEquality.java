package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value;

import net.minecraft.world.entity.Entity;
import com.kltyton.bedrockruntime.client.particle.definition.molang.BedrockItemIdentifiers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Compares values using the representation used by Molang's all and any queries. */
public final class MolangValueEquality {
    /** The native default identifier has empty text but a different hash from an empty literal. */
    private static final String DEFAULT_IDENTIFIER = new String(new char[0]);

    private MolangValueEquality() {
    }

    public static String defaultIdentifier() {
        return DEFAULT_IDENTIFIER;
    }

    public static Object snapshot(Object value) {
        if (value instanceof MolangStruct structure) return structure.comparisonSnapshot();
        if (value instanceof Map<?, ?> fields) return MolangStruct.fromMap(fields).comparisonSnapshot();
        if (value instanceof MolangStruct.DynamicNumber number) return number.floatValue();
        if (value instanceof List<?> list && isActorArray(list)) return new ArrayList<>(list);
        return value;
    }

    public static boolean same(Object left, Object right) {
        if (left instanceof Map<?, ?> fields) left = MolangStruct.fromMap(fields);
        if (right instanceof Map<?, ?> fields) right = MolangStruct.fromMap(fields);
        if (left instanceof Number a && right instanceof Number b) {
            return Float.floatToRawIntBits(a.floatValue()) == Float.floatToRawIntBits(b.floatValue());
        }
        if (left instanceof String a && right instanceof String b) return sameString(a, b);
        if (left instanceof MolangStruct a && right instanceof MolangStruct b) return a.sameMembers(b);
        if (left instanceof List<?> a && right instanceof List<?> b && isActorArray(a) && isActorArray(b)) {
            if (a.size() != b.size()) return false;
            for (int i = 0; i < a.size(); i++) if (a.get(i) != b.get(i)) return false;
            return true;
        }
        return left == right;
    }

    private static boolean isActorArray(List<?> values) {
        for (Object value : values) if (value != null && !(value instanceof Entity)) return false;
        return true;
    }

    public static boolean sameString(String left, String right) {
        if (left == DEFAULT_IDENTIFIER || right == DEFAULT_IDENTIFIER) return hash(left) == hash(right);
        if (left.equals(right) || hash(left) == hash(right)) return true;
        String leftAlias = BedrockItemIdentifiers.bedrockQueryPath(left);
        String rightAlias = BedrockItemIdentifiers.bedrockQueryPath(right);
        return leftAlias != null && (hash(leftAlias) == hash(right)
                || rightAlias != null && hash(leftAlias) == hash(rightAlias))
                || rightAlias != null && hash(left) == hash(rightAlias);
    }

    public static long hash(String value) {
        if (value == DEFAULT_IDENTIFIER) return 0xcbf29ce484222325L;
        if (value.isEmpty() || value.charAt(0) == 0) return 0;
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == 0) break;
            int point = c;
            if (Character.isHighSurrogate(c) && i + 1 < value.length()
                    && Character.isLowSurrogate(value.charAt(i + 1))) {
                point = Character.toCodePoint(c, value.charAt(++i));
            } else if (Character.isSurrogate(c)) {
                point = 0xfffd;
            }
            if (point <= 0x7f) {
                hash = byteHash(hash, point);
            } else if (point <= 0x7ff) {
                hash = byteHash(hash, 0xc0 | point >>> 6);
                hash = byteHash(hash, 0x80 | point & 0x3f);
            } else if (point <= 0xffff) {
                hash = byteHash(hash, 0xe0 | point >>> 12);
                hash = byteHash(hash, 0x80 | point >>> 6 & 0x3f);
                hash = byteHash(hash, 0x80 | point & 0x3f);
            } else {
                hash = byteHash(hash, 0xf0 | point >>> 18);
                hash = byteHash(hash, 0x80 | point >>> 12 & 0x3f);
                hash = byteHash(hash, 0x80 | point >>> 6 & 0x3f);
                hash = byteHash(hash, 0x80 | point & 0x3f);
            }
        }
        return hash;
    }

    private static long byteHash(long hash, int value) {
        return hash * 0x100000001b3L ^ value;
    }
}
