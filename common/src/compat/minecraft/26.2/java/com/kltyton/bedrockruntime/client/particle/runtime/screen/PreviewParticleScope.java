package com.kltyton.bedrockruntime.client.particle.runtime.screen;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleManager;

import java.util.ArrayDeque;

/** Routes nested particle creation to the preview that is executing the callback. */
public final class PreviewParticleScope {
    public record Context(MolangParticleManager manager, ClientLevel level, @Nullable Camera camera) {}

    private static final ThreadLocal<ArrayDeque<Context>> ACTIVE = new ThreadLocal<>();

    private PreviewParticleScope() {}

    public static void enter(MolangParticleManager manager, ClientLevel level, @Nullable Camera camera) {
        ArrayDeque<Context> stack = ACTIVE.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            ACTIVE.set(stack);
        }
        stack.push(new Context(manager, level, camera));
    }

    public static void exit() {
        ArrayDeque<Context> stack = ACTIVE.get();
        stack.pop();
        if (stack.isEmpty()) ACTIVE.remove();
    }

    public static @Nullable Context current() {
        ArrayDeque<Context> stack = ACTIVE.get();
        return stack == null ? null : stack.peek();
    }
}
