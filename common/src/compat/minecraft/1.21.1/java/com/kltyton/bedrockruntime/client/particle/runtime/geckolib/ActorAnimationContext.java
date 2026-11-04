package com.kltyton.bedrockruntime.client.particle.runtime.geckolib;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.api.geckolib.WithCurrentEntity;
import com.kltyton.bedrockruntime.client.particle.access.IEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.keyframe.Keyframe;

import java.util.IdentityHashMap;
import java.util.List;

/** Retains the real replaced actor during controller and bone sampling. */
public final class ActorAnimationContext {
    private static final ThreadLocal<IdentityHashMap<GeoAnimatable, Entity>> ACTORS = new ThreadLocal<>();
    private static final ThreadLocal<GeoAnimatable> ITEM_RENDER = new ThreadLocal<>();

    private ActorAnimationContext() {}

    public static @Nullable Entity entity(GeoAnimatable animatable) {
        if (animatable instanceof Entity entity) return entity;
        if (animatable instanceof WithCurrentEntity replaced) {
            Entity explicit = replaced.getCurrentEntity();
            if (explicit != null) return explicit;
        }
        var actors = ACTORS.get();
        return actors == null ? null : actors.get(animatable);
    }

    public static Scope enter(GeoAnimatable animatable, Entity entity) {
        var actors = ACTORS.get();
        if (actors == null) {
            actors = new IdentityHashMap<>();
            ACTORS.set(actors);
        }
        return new Scope(animatable, actors.put(animatable, entity));
    }

    public static boolean isItemRendering(GeoAnimatable animatable) {
        return ITEM_RENDER.get() == animatable;
    }

    public static ItemScope enterItem(GeoAnimatable animatable) {
        GeoAnimatable previous = ITEM_RENDER.get();
        ITEM_RENDER.set(animatable);
        return new ItemScope(previous);
    }

    public static void recordAnimationTime(GeoAnimatable animatable, float seconds) {
        Entity entity = entity(animatable);
        if (entity != null) IEntity.of(entity).kltyton_bedrock_runtime$setAnimationTime(seconds);
    }

    public static void recordKeyframeLerp(GeoAnimatable animatable, List<? extends Keyframe<?>> frames,
            double ageTicks, double endTicks, Keyframe<?> frame) {
        Entity entity = entity(animatable);
        if (entity == null || frames.getFirst() == frame) return;
        float start = (float) ((endTicks - frame.length()) / 20.0);
        float end = (float) (endTicks / 20.0);
        float interval = end - start;
        float ratio = interval >= 1.0E-6F ? 1.0F - (end - (float) (ageTicks / 20.0)) / interval : 1.0F;
        IEntity.of(entity).kltyton_bedrock_runtime$setKeyframeLerpTime(ratio);
    }

    public static final class ItemScope implements AutoCloseable {
        private final @Nullable GeoAnimatable previous;

        private ItemScope(@Nullable GeoAnimatable previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (previous == null) ITEM_RENDER.remove();
            else ITEM_RENDER.set(previous);
        }
    }

    public static final class Scope implements AutoCloseable {
        private final GeoAnimatable animatable;
        private final @Nullable Entity previous;

        private Scope(GeoAnimatable animatable, @Nullable Entity previous) {
            this.animatable = animatable;
            this.previous = previous;
        }

        @Override
        public void close() {
            var actors = ACTORS.get();
            if (previous == null) actors.remove(animatable);
            else actors.put(animatable, previous);
            if (actors.isEmpty()) ACTORS.remove();
        }
    }
}
