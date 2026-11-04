package com.kltyton.bedrockruntime.particle.network;

/**
 * Common-side trampoline used by loaders that only expose a common payload registration event.
 * The client entrypoint installs the real handler; no client class is referenced here.
 */
public final class ParticleClientDispatch {
    @FunctionalInterface
    public interface Handler {
        void handle(Object payload, Object context);
    }

    private static volatile Handler handler;

    private ParticleClientDispatch() {
    }

    public static void install(Handler dispatch) {
        if (handler != null && handler != dispatch) {
            throw new IllegalStateException("Particle client payload dispatcher already installed");
        }
        handler = dispatch;
    }

    public static void handle(Object payload, Object context) {
        Handler dispatch = handler;
        if (dispatch == null) {
            throw new IllegalStateException("Particle client payload dispatcher has not been installed");
        }
        dispatch.handle(payload, context);
    }
}
