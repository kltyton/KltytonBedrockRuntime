package com.kltyton.bedrockruntime.client.particle.api.geckolib;

import com.kltyton.bedrockruntime.client.particle.api.event.ParticleEvents;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4x3f;
import org.joml.Matrix4f;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.ParticleEmitterAttachable;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.molang.RenderItemContext;
import com.kltyton.bedrockruntime.client.particle.definition.molang.VariableTable;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockParticleBindings;
import com.kltyton.bedrockruntime.client.particle.runtime.geckolib.ActorAnimationContext;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleEngine;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;
import com.kltyton.bedrockruntime.client.particle.access.IBlockEntity;
import com.kltyton.bedrockruntime.client.particle.access.IEntity;
import com.kltyton.bedrockruntime.client.particle.access.IPSAnimatableInstanceCache;
import com.kltyton.bedrockruntime.client.particle.access.IPSAnimationController;
import com.kltyton.bedrockruntime.client.particle.access.IPSGeoBone;
import com.kltyton.bedrockruntime.client.particle.access.IPSParticleKeyframeData;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.keyframe.event.data.ParticleKeyframeData;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

public final class GeckoLibHelper {
    private static final Map<AnimatableInstanceCache, Map<LocatorParticleKey, BoundEmitter>> BINDINGS = new WeakHashMap<>();
    private static final Map<AnimatableInstanceCache, List<PendingEmitter>> PENDING = new WeakHashMap<>();
    private static final List<Runnable> RELOAD_CALLBACKS = new ArrayList<>();
    private static final List<WeakReloadCallback<?>> WEAK_RELOAD_CALLBACKS = new ArrayList<>();

    private GeckoLibHelper() {}

    public static void postEvent() {
        ParticleEvents.post(new RegisterLocatorPreTransformerEvent());
    }

    public static void addReloadCallback(Runnable callback) {
        RELOAD_CALLBACKS.add(callback);
    }

    public static <T> void addWeakReloadCallback(T owner, Consumer<T> callback) {
        WEAK_RELOAD_CALLBACKS.add(new WeakReloadCallback<>(new WeakReference<>(owner), callback));
    }

    public static void clearReloadCallbacks() {
        RELOAD_CALLBACKS.clear();
        WEAK_RELOAD_CALLBACKS.clear();
        BINDINGS.clear();
        PENDING.clear();
    }

    public static void afterReload() {
        BINDINGS.clear();
        PENDING.clear();
        for (Runnable callback : List.copyOf(RELOAD_CALLBACKS)) callback.run();
        for (WeakReloadCallback<?> callback : List.copyOf(WEAK_RELOAD_CALLBACKS)) {
            if (!callback.run()) WEAK_RELOAD_CALLBACKS.remove(callback);
        }
    }

    public static boolean processParticleEffect(@Nullable GeoAnimatable animatable, AnimationController<?> controller,
                                                ParticleKeyframeData data) {
        if (animatable == null) return true;
        String locatorName = data.getLocator();
        try {
            ParticleContext context = createContext(animatable);
            if (context == null) return true;
            Object actor = context.actor();
            IPSParticleKeyframeData keyframe = IPSParticleKeyframeData.of(data);
            var item = context.itemContext().item();
            BedrockParticleBindings.ItemResolution itemBinding = item == null ? null
                    : BedrockParticleBindings.resolve(item, data.getEffect(), context.level(),
                            context.basePos(), context.variables(), context.attached());
            ResourceLocation mapped = itemBinding != null && itemBinding.selected() ? itemBinding.particle()
                    : context.attached() instanceof Entity entity
                            ? BedrockParticleBindings.resolve(entity, data.getEffect()) : null;
            ResourceLocation particle = mapped == null ? keyframe.kltyton_bedrock_runtime$getParticle() : mapped;
            MolangExp expression = keyframe.kltyton_bedrock_runtime$getExpression(context.variables());
            boolean bound = !Boolean.FALSE.equals(keyframe.kltyton_bedrock_runtime$getBindToActor());
            AnimatableInstanceCache cache = animatable.getAnimatableInstanceCache();
            if (!bound) {
                if (locatorName == null || locatorName.isBlank()) {
                    ParticleEmitter emitter = newEmitter(context, context.basePos(), particle, expression, false);
                    addAnimationEmitter(context, emitter);
                    return false;
                }
                for (GeoBone bone : IPSAnimationController.of(controller).kltyton_bedrock_runtime$getBonesWhichHasLocators()) {
                    Map<String, LocatorValue> locators = IPSGeoBone.of(bone).kltyton_bedrock_runtime$getLocators();
                    LocatorValue locator = locators == null ? null : locators.get(locatorName);
                    if (locator != null) {
                        PENDING.computeIfAbsent(cache, ignored -> new ArrayList<>())
                                .add(new PendingEmitter(actor, context, particle, expression, bone, locator));
                        return false;
                    }
                }
                BedrockParticles.LOGGER.warn("Missing GeckoLib locator {} for particle {}", locatorName, particle);
                return true;
            }

            String keyLocator = locatorName == null || locatorName.isBlank() ? "" : locatorName;
            LocatorParticleKey key = new LocatorParticleKey(controller, actor, keyLocator, particle);
            Map<LocatorParticleKey, BoundEmitter> bindings = BINDINGS.computeIfAbsent(cache, ignored -> new HashMap<>());
            BoundEmitter existing = bindings.get(key);
            if (!keyLocator.isEmpty() && existing != null) {
                for (ParticleEmitter root : List.copyOf(existing.roots)) {
                    if (!active(root)) removeCachedId(cache, existing, root);
                }
                existing.prune();
                if (existing.roots.stream().anyMatch(emitter -> active(emitter) && emitter.level == context.level())) return false;
            }

            GeoBone foundBone = null;
            LocatorValue foundLocator = null;
            if (!keyLocator.isEmpty()) {
                for (GeoBone bone : IPSAnimationController.of(controller).kltyton_bedrock_runtime$getBonesWhichHasLocators()) {
                    Map<String, LocatorValue> locators = IPSGeoBone.of(bone).kltyton_bedrock_runtime$getLocators();
                    LocatorValue candidate = locators == null ? null : locators.get(keyLocator);
                    if (candidate != null) { foundBone = bone; foundLocator = candidate; break; }
                }
                if (foundBone == null) {
                    BedrockParticles.LOGGER.warn("Missing GeckoLib locator {} for particle {}", keyLocator, particle);
                    return true;
                }
            }
            ParticleEmitter emitter = newEmitter(context, context.basePos(), particle, expression, true);
            if (foundLocator != null) {
                emitter.parentSpace = new Matrix4x3f();
                IPSAnimatableInstanceCache view = IPSAnimatableInstanceCache.of(cache);
                if (view.kltyton_bedrock_runtime$getLocatorState(foundLocator) == null) {
                    view.kltyton_bedrock_runtime$createLocatorState(foundLocator);
                }
            }
            addAnimationEmitter(context, emitter);
            if (foundLocator != null) {
                IPSAnimatableInstanceCache.of(cache).kltyton_bedrock_runtime$getCachedId()
                        .computeIfAbsent(foundLocator, ignored -> new IntArrayList()).add(emitter.id);
            }
            if (existing == null) {
                existing = new BoundEmitter(foundBone, foundLocator, emitter, context);
                bindings.put(key, existing);
            } else {
                existing.roots.add(emitter);
            }
            if (foundLocator != null) existing.indexedIds.put(emitter, emitter.id);
            return false;
        } catch (RuntimeException exception) {
            BedrockParticles.LOGGER.error("GeckoLib particle keyframe failed effect={} locator={}", data.getEffect(), locatorName, exception);
            throw exception;
        }
    }

    private static @Nullable ParticleContext createContext(GeoAnimatable animatable) {
        boolean item = animatable instanceof software.bernie.geckolib.animatable.GeoItem;
        if (item && RenderItemContext.currentDisplay() == net.minecraft.world.item.ItemDisplayContext.GUI) return null;
        Object actor = actorFor(animatable);
        ParticleEmitterAttachable attached = attachable(actor);
        RenderItemContext.Owner owner = item ? RenderItemContext.currentOwner() : null;
        if (attached == null && owner == null) return null;
        Level actorLevel = attached == null ? null : attached.getLevel();
        if (attached != null && actorLevel == null) return null;
        Level level = owner == null ? actorLevel : owner.level();
        Vec3 position = owner == null ? attached.getPos() : owner.position();
        VariableTable variables = attached == null ? RenderItemContext.variablesOrNew() : attached.getVariableTable();
        return new ParticleContext(actor, level, position, variables, attached, owner, RenderItemContext.snapshot());
    }

    private static ParticleEmitter newEmitter(ParticleContext context, Vec3 position, ResourceLocation particle,
            MolangExp expression, boolean bound) {
        try (var ignored = RenderItemContext.enterSnapshot(context.itemContext())) {
            VariableTable itemVariables = bound && context.attached() == null && context.itemContext().item() != null
                    ? context.variables() : null;
            return ParticleEmitter.forAnimation(context.level(), position, particle, expression,
                    bound ? context.attached() : null, itemVariables, bound ? context.owner() : null);
        }
    }

    private static void addAnimationEmitter(ParticleContext context, ParticleEmitter emitter) {
        try (var ignored = RenderItemContext.enterSnapshot(context.itemContext())) {
            MolangParticleEngine.addEmitterWithPreEffect(emitter);
        }
    }

    private static Vec3 attachmentBase(ParticleContext context, float partialTick) {
        Vec3 position = context.owner() != null ? context.owner().position()
                : context.attached() == null ? context.basePos() : context.attached().getPos();
        return context.attached() instanceof Entity entity
                ? position.add(entity.getPosition(partialTick).subtract(entity.position())) : position;
    }

    private static @Nullable Object actorFor(GeoAnimatable animatable) {
        Entity entity = ActorAnimationContext.entity(animatable);
        if (entity != null) return entity;
        if (animatable instanceof software.bernie.geckolib.animatable.GeoItem) {
            var owner = RenderItemContext.currentOwner();
            if (owner != null) {
                Entity itemEntity = owner.entity();
                if (itemEntity != null) return itemEntity;
            }
        }
        return animatable;
    }

    private static @Nullable ParticleEmitterAttachable attachable(Object actor) {
        if (actor instanceof Entity entity) return IEntity.of(entity);
        if (actor instanceof BlockEntity block) return IBlockEntity.of(block);
        return actor instanceof ParticleEmitterAttachable value ? value : null;
    }

    private static boolean active(ParticleEmitter emitter) {
        return !emitter.isRemoved() && MolangParticleEngine.INSTANCE.getEmitter(emitter.id) == emitter;
    }

    public static void setCurrentEntity(Object animatable, @Nullable Entity entity) {
        if (animatable instanceof WithCurrentEntity replaced) replaced.setCurrentEntity(entity);
    }

    public static @Nullable Entity animationEntity(Object animatable) {
        return animatable instanceof WithCurrentEntity replaced ? replaced.getCurrentEntity()
                : animatable instanceof Entity entity ? entity : null;
    }

    public static void removeEmittersWhenAnimationChange(AnimationController.State state, AnimationController<?> controller,
                                                         AnimatableInstanceCache cache, GeoAnimatable animatable) {
        if (state != AnimationController.State.TRANSITIONING) return;
        Object actor = actorFor(animatable);
        if (actor == null) return;
        Map<LocatorParticleKey, BoundEmitter> bindings = BINDINGS.get(cache);
        if (bindings == null) return;
        for (Map.Entry<LocatorParticleKey, BoundEmitter> entry : new ArrayList<>(bindings.entrySet())) {
            if (entry.getKey().controller != controller || entry.getKey().actor != actor) continue;
            BoundEmitter bound = entry.getValue();
            for (ParticleEmitter emitter : List.copyOf(bound.roots)) {
                removeCachedId(cache, bound, emitter);
                if (active(emitter)) {
                    MolangParticleEngine.INSTANCE.removeEmitter(emitter, false);
                }
            }
            bound.prune();
            if (bound.roots.isEmpty()) bindings.remove(entry.getKey(), bound);
        }
        if (bindings.isEmpty()) BINDINGS.remove(cache, bindings);
    }

    public static void removeEmitters(AnimatableInstanceCache cache) {
        Map<LocatorParticleKey, BoundEmitter> bindings = BINDINGS.get(cache);
        if (bindings == null) return;
        for (Map.Entry<LocatorParticleKey, BoundEmitter> entry : new ArrayList<>(bindings.entrySet())) {
            BoundEmitter bound = entry.getValue();
            for (ParticleEmitter emitter : List.copyOf(bound.roots)) {
                removeCachedId(cache, bound, emitter);
                if (MolangParticleEngine.INSTANCE.getEmitter(emitter.id) == emitter) {
                    MolangParticleEngine.INSTANCE.removeEmitter(emitter, false);
                }
            }
            bound.prune();
            if (bound.roots.isEmpty()) bindings.remove(entry.getKey(), bound);
        }
        if (bindings.isEmpty()) BINDINGS.remove(cache, bindings);
    }

    public static void transformLocator(GeoBone bone, GeoAnimatable animatable, float partialTick) {
        if (usesNativeItemLocator(animatable)) return;
        AnimatableInstanceCache cache = animatable.getAnimatableInstanceCache();
        Object actor = actorFor(animatable);
        if (actor == null) return;
        Map<LocatorParticleKey, BoundEmitter> bindings = BINDINGS.get(cache);
        List<PendingEmitter> pending = PENDING.get(cache);
        if ((bindings == null || bindings.isEmpty()) && (pending == null || pending.isEmpty())) return;
        Matrix4x3f boneTransform = new Matrix4x3f();
        RegisterLocatorPreTransformerEvent.getTransformer(animatable).transform(bone, animatable, boneTransform, partialTick);
        if (bindings != null) {
            for (Map.Entry<LocatorParticleKey, BoundEmitter> entry : new ArrayList<>(bindings.entrySet())) {
                BoundEmitter bound = entry.getValue();
                if (entry.getKey().actor != actor || bound.bone != bone || bound.locator == null) continue;
                Matrix4x3f transform = locatorTransform(cache, bound.locator, boneTransform);
                for (ParticleEmitter emitter : List.copyOf(bound.roots)) emitter.setLocalSpace(new Matrix4x3f(transform));
            }
        }
        if (pending != null) {
            for (PendingEmitter request : List.copyOf(pending)) {
                if (request.actor != actor || request.bone != bone) continue;
                pending.remove(request);
                if (request.context.attached() != null && request.context.attached().isDiscarded()) continue;
                Matrix4x3f transform = locatorTransform(cache, request.locator, boneTransform);
                Vec3 base = attachmentBase(request.context, partialTick);
                ParticleEmitter emitter = newEmitter(request.context, base, request.particle, request.expression, false);
                emitter.setEffectFrame(new org.joml.Matrix3f().set(transform));
                emitter.setPos(base.add(transform.m30(), transform.m31(), transform.m32()));
                addAnimationEmitter(request.context, emitter);
            }
            if (pending.isEmpty()) PENDING.remove(cache, pending);
        }
    }

    public static boolean usesNativeItemLocator(GeoAnimatable animatable) {
        return ActorAnimationContext.isItemRendering(animatable)
                && !RegisterLocatorPreTransformerEvent.hasCustomItemTransformer(animatable);
    }

    public static void captureNativeItemLocator(GeoBone bone, GeoAnimatable animatable, Matrix4f pose, float partialTick) {
        if (!usesNativeItemLocator(animatable) || RenderItemContext.currentDisplay() == net.minecraft.world.item.ItemDisplayContext.GUI) return;
        AnimatableInstanceCache cache = animatable.getAnimatableInstanceCache();
        Map<LocatorParticleKey, BoundEmitter> bindings = BINDINGS.get(cache);
        List<PendingEmitter> pending = PENDING.get(cache);
        if ((bindings == null || bindings.isEmpty()) && (pending == null || pending.isEmpty())) return;
        Object actor = actorFor(animatable);
        var renderCamera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 camera = renderCamera.getPosition();
        Matrix4f worldPose = new Matrix4f().rotation(renderCamera.rotation())
                .mul(RenderSystem.getModelViewMatrix()).mul(pose);
        if (bindings != null) {
            for (Map.Entry<LocatorParticleKey, BoundEmitter> entry : new ArrayList<>(bindings.entrySet())) {
                BoundEmitter bound = entry.getValue();
                if (entry.getKey().actor != actor || bound.bone != bone || bound.locator == null) continue;
                Matrix4x3f frame = locatorTransform(cache, bound.locator, new Matrix4x3f().set(worldPose));
                Vec3 base = attachmentBase(bound.context, partialTick);
                frame.m30(frame.m30() + (float) (camera.x - base.x));
                frame.m31(frame.m31() + (float) (camera.y - base.y));
                frame.m32(frame.m32() + (float) (camera.z - base.z));
                for (ParticleEmitter emitter : List.copyOf(bound.roots)) emitter.setLocalSpace(new Matrix4x3f(frame));
            }
        }
        if (pending != null) {
            for (PendingEmitter request : List.copyOf(pending)) {
                if (request.actor != actor || request.bone != bone) continue;
                pending.remove(request);
                if (request.context.attached() != null && request.context.attached().isDiscarded()) continue;
                Matrix4x3f frame = locatorTransform(cache, request.locator, new Matrix4x3f().set(worldPose));
                Vec3 position = camera.add(frame.m30(), frame.m31(), frame.m32());
                ParticleEmitter emitter = newEmitter(request.context, position, request.particle, request.expression, false);
                emitter.setEffectFrame(new org.joml.Matrix3f().set(frame));
                emitter.setPos(position);
                addAnimationEmitter(request.context, emitter);
            }
            if (pending.isEmpty()) PENDING.remove(cache, pending);
        }
    }

    private static Matrix4x3f locatorTransform(AnimatableInstanceCache cache, LocatorValue locator, Matrix4x3f base) {
        IPSAnimatableInstanceCache view = IPSAnimatableInstanceCache.of(cache);
        LocatorState state = view.kltyton_bedrock_runtime$getLocatorState(locator);
        if (state == null) {
            view.kltyton_bedrock_runtime$createLocatorState(locator);
            state = view.kltyton_bedrock_runtime$getLocatorState(locator);
        }
        return new Matrix4x3f(base).rotateXYZ(state.rx, state.ry, state.rz).translate(state.px, state.py, state.pz);
    }

    public static void pruneLocatorBindings() {
        for (Map.Entry<AnimatableInstanceCache, Map<LocatorParticleKey, BoundEmitter>> cacheEntry : new ArrayList<>(BINDINGS.entrySet())) {
            Map<LocatorParticleKey, BoundEmitter> bindings = cacheEntry.getValue();
            for (Map.Entry<LocatorParticleKey, BoundEmitter> entry : new ArrayList<>(bindings.entrySet())) {
                BoundEmitter bound = entry.getValue();
                for (ParticleEmitter emitter : List.copyOf(bound.roots)) {
                    if (!active(emitter)) removeCachedId(cacheEntry.getKey(), bound, emitter);
                }
                bound.prune();
                if (bound.roots.isEmpty()) bindings.remove(entry.getKey(), bound);
            }
            if (bindings.isEmpty()) BINDINGS.remove(cacheEntry.getKey(), bindings);
        }
        for (Map.Entry<AnimatableInstanceCache, List<PendingEmitter>> entry : new ArrayList<>(PENDING.entrySet())) {
            List<PendingEmitter> requests = entry.getValue();
            requests.removeIf(request -> request.context.attached() != null && request.context.attached().isDiscarded());
            if (requests.isEmpty()) PENDING.remove(entry.getKey(), requests);
        }
        WEAK_RELOAD_CALLBACKS.removeIf(callback -> callback.owner.get() == null);
    }

    private record LocatorParticleKey(AnimationController<?> controller, Object actor, String locator, ResourceLocation particle) {
        @Override public boolean equals(Object other) {
            return other instanceof LocatorParticleKey key && controller == key.controller && actor == key.actor
                    && locator.equals(key.locator) && particle.equals(key.particle);
        }
        @Override public int hashCode() {
            int result = 31 * System.identityHashCode(controller) + System.identityHashCode(actor);
            return 31 * (31 * result + locator.hashCode()) + particle.hashCode();
        }
    }

    private static final class BoundEmitter {
        private final @Nullable GeoBone bone;
        private final @Nullable LocatorValue locator;
        private final ParticleContext context;
        private final List<ParticleEmitter> roots = new ArrayList<>();
        private final Map<ParticleEmitter, Integer> indexedIds = new IdentityHashMap<>();
        private BoundEmitter(@Nullable GeoBone bone, @Nullable LocatorValue locator, ParticleEmitter root, ParticleContext context) {
            this.bone = bone;
            this.locator = locator;
            this.context = context;
            roots.add(root);
        }
        private void prune() {
            roots.removeIf(emitter -> !active(emitter) && !emitter.hasLiveBoundEventChildren());
        }
    }

    private static void removeCachedId(AnimatableInstanceCache cache, BoundEmitter bound, ParticleEmitter emitter) {
        Integer id = bound.indexedIds.remove(emitter);
        if (id == null || bound.locator == null) return;
        var ids = IPSAnimatableInstanceCache.of(cache).kltyton_bedrock_runtime$getCachedId().get(bound.locator);
        if (ids != null) {
            ids.rem(id.intValue());
            if (ids.isEmpty()) IPSAnimatableInstanceCache.of(cache).kltyton_bedrock_runtime$getCachedId().remove(bound.locator, ids);
        }
    }

    private record ParticleContext(Object actor, Level level, Vec3 basePos, VariableTable variables,
            @Nullable ParticleEmitterAttachable attached, @Nullable RenderItemContext.Owner owner,
            RenderItemContext.Snapshot itemContext) {}

    private record PendingEmitter(Object actor, ParticleContext context, ResourceLocation particle,
            MolangExp expression, GeoBone bone, LocatorValue locator) {}

    private record WeakReloadCallback<T>(WeakReference<T> owner, Consumer<T> callback) {
        private boolean run() {
            T target = owner.get();
            if (target == null) return false;
            callback.accept(target);
            return true;
        }
    }

    public static class LocatorState {
        private final float px, py, pz;
        private final float rx, ry, rz;

        public LocatorState(LocatorValue locator) {
            double[] offset = locator.locatorClass() == null ? locator.values() : locator.locatorClass().offset();
            double[] rotation = locator.locatorClass() == null ? new double[3] : locator.locatorClass().rotation();
            px = -(float) (offset[0] / 16.0);
            py = (float) (offset[1] / 16.0);
            pz = (float) (offset[2] / 16.0);
            rx = (float) Math.toRadians(rotation[0]);
            ry = (float) Math.toRadians(rotation[1]);
            rz = (float) Math.toRadians(rotation[2]);
        }
    }
}
