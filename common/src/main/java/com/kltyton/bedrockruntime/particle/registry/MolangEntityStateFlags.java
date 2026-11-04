package com.kltyton.bedrockruntime.particle.registry;

/** Flags shared by the server tracker and the version-specific entity-state payload. */
public final class MolangEntityStateFlags {
    public static final int HAS_TARGET = 1;
    public static final int LOVE_KNOWN = 2;
    public static final int IN_LOVE = 4;
    public static final int INITIAL = 8;
    public static final int TARGET_KNOWN = 16;
    public static final int ITEM_USE_ACTIVE = 32;
    public static final int RECOVERY_KNOWN = 64;

    private MolangEntityStateFlags() {
    }
}
