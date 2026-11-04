package com.kltyton.bedrockruntime.client.particle.runtime.screen;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.api.RegisterCustomParticleTypeEvent;
import com.kltyton.bedrockruntime.client.particle.runtime.ExtendMutableSpriteSet;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleManager;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Routes nested particle creation to the preview that is executing the callback. */
public final class PreviewParticleScope {
    public record Context(MolangParticleManager manager, ClientLevel level, @Nullable Camera camera) {}

    private static final class ScopeFrame {
        private final Context context;
        private RegistrationLease registrationLease;

        private ScopeFrame(Context context, RegistrationLease registrationLease) {
            this.context = context;
            this.registrationLease = registrationLease;
        }
    }

    public static final class RegistrationState {
        private final Map<ParticleType<?>, RegisterCustomParticleTypeEvent.Provider<?>> providers;
        private final ExtendMutableSpriteSet sprites;
        private final Set<ParticleType<?>> spriteTypes;
        private @Nullable List<ResourceLocation> boundSpriteIds;

        private RegistrationState(Map<ParticleType<?>, RegisterCustomParticleTypeEvent.Provider<?>> providers,
                                  ExtendMutableSpriteSet sprites, Set<ParticleType<?>> spriteTypes,
                                  @Nullable List<ResourceLocation> boundSpriteIds) {
            this.providers = providers;
            this.sprites = sprites;
            this.spriteTypes = spriteTypes;
            this.boundSpriteIds = boundSpriteIds;
        }

        private boolean sameRegistries(RegistrationState other) {
            return providers == other.providers && sprites == other.sprites && spriteTypes == other.spriteTypes
                    && boundSpriteIds == other.boundSpriteIds;
        }
    }

    public static final class RegistrationToken {
        private final RegistrationState previous;

        private RegistrationToken(RegistrationState previous) {
            this.previous = previous;
        }
    }

    public static final class RegistrationLease implements AutoCloseable {
        private @Nullable RegistrationState previous;

        private RegistrationLease(@Nullable RegistrationState previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            RegistrationState restore = previous;
            if (restore != null) {
                previous = null;
                installRegistrationState(restore);
            }
        }
    }

    private static final ThreadLocal<ArrayDeque<ScopeFrame>> ACTIVE = new ThreadLocal<>();
    private static final ThreadLocal<ArrayDeque<TextureAtlas>> CONSTRUCTION_ATLASES = new ThreadLocal<>();
    private static final ThreadLocal<Integer> NATIVE_ENGINE_CONSTRUCTION_DEPTH = ThreadLocal.withInitial(() -> 0);

    private PreviewParticleScope() {}

    public static void enter(MolangParticleManager manager, ClientLevel level, @Nullable Camera camera) {
        ArrayDeque<ScopeFrame> stack = ACTIVE.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            ACTIVE.set(stack);
        }
        RegistrationLease registrationLease = useRegistrationState(manager.previewRegistrationState());
        stack.push(new ScopeFrame(new Context(manager, level, camera), registrationLease));
    }

    public static void exit() {
        ArrayDeque<ScopeFrame> stack = ACTIVE.get();
        ScopeFrame frame = stack.pop();
        try {
            frame.registrationLease.close();
        } finally {
            if (stack.isEmpty()) ACTIVE.remove();
        }
    }

    public static @Nullable Context current() {
        ArrayDeque<ScopeFrame> stack = ACTIVE.get();
        ScopeFrame frame = stack == null ? null : stack.peek();
        return frame == null ? null : frame.context;
    }

    public static boolean isActive(MolangParticleManager manager, ClientLevel level, @Nullable Camera camera) {
        Context context = current();
        return context != null && context.manager() == manager && context.level() == level && context.camera() == camera;
    }

    public static void refreshRegistrationState(MolangParticleManager manager) {
        ArrayDeque<ScopeFrame> stack = ACTIVE.get();
        ScopeFrame frame = stack == null ? null : stack.peek();
        if (frame != null && frame.context.manager() == manager) {
            RegistrationState desired = manager.previewRegistrationState();
            if (desired == null || desired.sameRegistries(captureRegistrationState())) return;
            frame.registrationLease.close();
            frame.registrationLease = useRegistrationState(desired);
        }
    }

    public static void enterAtlasConstruction(TextureAtlas atlas) {
        ArrayDeque<TextureAtlas> stack = CONSTRUCTION_ATLASES.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            CONSTRUCTION_ATLASES.set(stack);
        }
        stack.push(atlas);
    }

    public static void exitAtlasConstruction() {
        ArrayDeque<TextureAtlas> stack = CONSTRUCTION_ATLASES.get();
        stack.pop();
        if (stack.isEmpty()) CONSTRUCTION_ATLASES.remove();
    }

    public static @Nullable TextureAtlas constructionAtlas() {
        ArrayDeque<TextureAtlas> stack = CONSTRUCTION_ATLASES.get();
        return stack == null ? null : stack.peek();
    }

    public static RegistrationToken beginNativeEngineConstruction() {
        RegistrationState previous = captureRegistrationState();
        installRegistrationState(new RegistrationState(new HashMap<>(), new ExtendMutableSpriteSet(), new LinkedHashSet<>(), null));
        NATIVE_ENGINE_CONSTRUCTION_DEPTH.set(NATIVE_ENGINE_CONSTRUCTION_DEPTH.get() + 1);
        return new RegistrationToken(previous);
    }

    public static RegistrationState endNativeEngineConstruction(RegistrationToken token) {
        try {
            RegistrationState captured = captureRegistrationState();
            captured.boundSpriteIds = token.previous.boundSpriteIds == null ? null : List.copyOf(token.previous.boundSpriteIds);
            return captured;
        } finally {
            installRegistrationState(token.previous);
            int depth = NATIVE_ENGINE_CONSTRUCTION_DEPTH.get() - 1;
            if (depth == 0) NATIVE_ENGINE_CONSTRUCTION_DEPTH.remove();
            else NATIVE_ENGINE_CONSTRUCTION_DEPTH.set(depth);
        }
    }

    public static boolean isNativeEngineConstruction() {
        return NATIVE_ENGINE_CONSTRUCTION_DEPTH.get() > 0;
    }

    public static RegistrationState captureRegistrationState() {
        return new RegistrationState(new HashMap<>(RegisterCustomParticleTypeEvent.providers()),
                RegisterCustomParticleTypeEvent.sprites(),
                new LinkedHashSet<>(RegisterCustomParticleTypeEvent.spriteTypes()),
                RegisterCustomParticleTypeEvent.boundSpriteIds());
    }

    public static void copyBoundSpriteIds(RegistrationState target, RegistrationState source) {
        target.boundSpriteIds = source.boundSpriteIds == null ? null : List.copyOf(source.boundSpriteIds);
    }

    public static RegistrationLease useRegistrationState(@Nullable RegistrationState desired) {
        if (desired == null) return new RegistrationLease(null);
        RegistrationState current = captureRegistrationState();
        if (desired.sameRegistries(current)) return new RegistrationLease(null);
        installRegistrationState(desired);
        return new RegistrationLease(current);
    }

    public static void withRegistrationState(RegistrationState state, Runnable action) {
        try (RegistrationLease ignored = useRegistrationState(state)) {
            action.run();
        }
    }

    private static void installRegistrationState(RegistrationState state) {
        RegisterCustomParticleTypeEvent.setProviders(new HashMap<>(state.providers));
        RegisterCustomParticleTypeEvent.setSprites(state.sprites);
        RegisterCustomParticleTypeEvent.setSpriteTypes(state.spriteTypes);
        RegisterCustomParticleTypeEvent.setBoundSpriteIds(state.boundSpriteIds);
    }
}
