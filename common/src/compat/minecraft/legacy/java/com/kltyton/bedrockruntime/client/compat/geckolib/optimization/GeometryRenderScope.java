package com.kltyton.bedrockruntime.client.compat.geckolib.optimization;

/** Invocation-local guard: per-bone layers and re-render passes keep their original geometry. */
public final class GeometryRenderScope {
    private static final ThreadLocal<Boolean> PRIMARY = ThreadLocal.withInitial(() -> false);

    private GeometryRenderScope() { }

    public static boolean primary() { return PRIMARY.get(); }

    public static boolean enter(boolean primary) {
        boolean previous = PRIMARY.get();
        PRIMARY.set(primary);
        return previous;
    }

    public static void exit(boolean previous) {
        if (previous) { PRIMARY.set(true); }
        else { PRIMARY.remove(); }
    }
}
