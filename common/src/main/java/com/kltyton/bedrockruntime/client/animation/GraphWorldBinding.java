package com.kltyton.bedrockruntime.client.animation;

import java.lang.ref.WeakReference;

/** Invalidates instance history across client worlds without retaining an unloaded world. */
public final class GraphWorldBinding {
    private final boolean hasWorld;
    private final WeakReference<Object> world;

    public GraphWorldBinding(Object world) {
        this.hasWorld = world != null;
        this.world = new WeakReference<>(world);
    }

    public boolean matches(Object currentWorld) {
        return hasWorld ? currentWorld != null && world.get() == currentWorld : currentWorld == null;
    }
}
