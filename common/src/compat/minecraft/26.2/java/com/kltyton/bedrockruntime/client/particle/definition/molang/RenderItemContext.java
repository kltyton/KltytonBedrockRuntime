package com.kltyton.bedrockruntime.client.particle.definition.molang;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus;
import com.kltyton.bedrockruntime.client.particle.api.ParticleEmitterAttachable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/** Keeps item render data local to the active animation evaluation. */
public final class RenderItemContext implements AutoCloseable {
    private static final ThreadLocal<Deque<RenderItemContext>> SCOPES = new ThreadLocal<>();
    private static final Snapshot ABSENT = new Snapshot(false, null, null);
    private final @Nullable ItemStack item;
    private final @Nullable ItemOwner owner;
    private final @Nullable ItemDisplayContext display;
    private final boolean provided;
    private @Nullable Object particleBindingsSnapshot;
    private @Nullable Level particleBindingsLevel;
    private @Nullable VariableTable particleBindingsVariables;
    private @Nullable ParticleEmitterAttachable particleBindingsAttachment;
    private @Nullable ParticleBindingSelection particleBindingSelection;
    private @Nullable VariableTable unboundVariables;

    private RenderItemContext(@Nullable ItemStack item, @Nullable ItemOwner owner,
                              @Nullable ItemDisplayContext display, boolean provided) {
        this.item = item;
        this.owner = owner;
        this.display = display;
        this.provided = provided;
    }

    public static RenderItemContext enter(@Nullable ItemStack item) {
        return enter(item, null);
    }

    public static RenderItemContext enter(@Nullable ItemStack item, @Nullable ItemOwner owner) {
        return enter(item, owner, null);
    }

    public static RenderItemContext enter(@Nullable ItemStack item, @Nullable ItemOwner owner,
                                          @Nullable ItemDisplayContext display) {
        return enterState(item, owner, display, true);
    }

    public static RenderItemContext enterSnapshot(Snapshot snapshot) {
        return enterState(snapshot.item(), snapshot.owner(), snapshot.display(), snapshot.provided());
    }

    private static RenderItemContext enterState(@Nullable ItemStack item, @Nullable ItemOwner owner,
                                                @Nullable ItemDisplayContext display, boolean provided) {
        Deque<RenderItemContext> scopes = SCOPES.get();
        if (scopes == null) {
            scopes = new ArrayDeque<>();
            SCOPES.set(scopes);
        }
        RenderItemContext scope = new RenderItemContext(item, owner, display, provided);
        scopes.push(scope);
        return scope;
    }

    public static @Nullable ItemStack currentItem() {
        Deque<RenderItemContext> scopes = SCOPES.get();
        return scopes == null || scopes.isEmpty() || !scopes.peek().provided ? null : scopes.peek().item;
    }

    public static boolean isActive() {
        Deque<RenderItemContext> scopes = SCOPES.get();
        return scopes != null && !scopes.isEmpty() && scopes.peek().provided;
    }

    public static @Nullable ItemOwner currentOwner() {
        Deque<RenderItemContext> scopes = SCOPES.get();
        return scopes == null || scopes.isEmpty() || !scopes.peek().provided ? null : scopes.peek().owner;
    }

    public static @Nullable ItemDisplayContext currentDisplay() {
        Deque<RenderItemContext> scopes = SCOPES.get();
        return scopes == null || scopes.isEmpty() || !scopes.peek().provided ? null : scopes.peek().display;
    }

    @ApiStatus.Internal
    public static VariableTable variablesOrNew() {
        Deque<RenderItemContext> scopes = SCOPES.get();
        if (scopes == null || scopes.isEmpty() || !scopes.peek().provided || scopes.peek().item == null) {
            return new VariableTable(null);
        }
        RenderItemContext scope = scopes.peek();
        if (scope.unboundVariables == null) scope.unboundVariables = new VariableTable(null);
        return scope.unboundVariables;
    }

    @ApiStatus.Internal
    public record ParticleBindingSelection(boolean selected, Map<String, Identifier> effects) {}

    @ApiStatus.Internal
    public static @Nullable ParticleBindingSelection cachedParticleBinding(Object snapshot, Level level,
            VariableTable variables, @Nullable ParticleEmitterAttachable attached) {
        Deque<RenderItemContext> scopes = SCOPES.get();
        if (scopes == null || scopes.isEmpty()) return null;
        RenderItemContext scope = scopes.peek();
        return scope.provided && scope.particleBindingsSnapshot == snapshot && scope.particleBindingsLevel == level
                && scope.particleBindingsVariables == variables && scope.particleBindingsAttachment == attached
                ? scope.particleBindingSelection : null;
    }

    @ApiStatus.Internal
    public static void cacheParticleBinding(Object snapshot, Level level, VariableTable variables,
            @Nullable ParticleEmitterAttachable attached, ParticleBindingSelection selection) {
        Deque<RenderItemContext> scopes = SCOPES.get();
        if (scopes == null || scopes.isEmpty() || !scopes.peek().provided) return;
        RenderItemContext scope = scopes.peek();
        scope.particleBindingsSnapshot = snapshot;
        scope.particleBindingsLevel = level;
        scope.particleBindingsVariables = variables;
        scope.particleBindingsAttachment = attached;
        scope.particleBindingSelection = selection;
    }

    public static Snapshot snapshot() {
        Deque<RenderItemContext> scopes = SCOPES.get();
        return scopes == null || scopes.isEmpty() ? ABSENT
                : new Snapshot(scopes.peek().provided, scopes.peek().item, scopes.peek().owner, scopes.peek().display);
    }

    public record Snapshot(boolean provided, @Nullable ItemStack item, @Nullable ItemOwner owner,
                           @Nullable ItemDisplayContext display) {
        public Snapshot(boolean provided, @Nullable ItemStack item, @Nullable ItemOwner owner) {
            this(provided, item, owner, null);
        }

        public Snapshot(boolean provided, @Nullable ItemStack item) {
            this(provided, item, null, null);
        }
    }

    @Override
    public void close() {
        Deque<RenderItemContext> scopes = SCOPES.get();
        if (scopes == null || scopes.peek() != this) {
            throw new IllegalStateException("Item render contexts must close in stack order");
        }
        scopes.pop();
        if (scopes.isEmpty()) SCOPES.remove();
    }
}
