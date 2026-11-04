package com.kltyton.bedrockruntime.client.particle.api.geckolib;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4x3f;
import org.joml.Quaternionf;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.ParticleEmitterAttachable;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockParticleBindings;
import com.kltyton.bedrockruntime.client.particle.runtime.geckolib.ActorAnimationContext;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.molang.RenderItemContext;
import com.kltyton.bedrockruntime.client.particle.definition.molang.VariableTable;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import com.kltyton.bedrockruntime.client.particle.access.IBlockEntity;
import com.kltyton.bedrockruntime.client.particle.access.IEntity;
import com.kltyton.bedrockruntime.client.particle.access.IPSAnimatableInstanceCache;
import com.kltyton.bedrockruntime.client.particle.access.IPSAnimationController;
import com.kltyton.bedrockruntime.client.particle.access.IPSGeoBone;
import com.kltyton.bedrockruntime.client.particle.access.IPSParticleKeyframeData;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleEngine;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.keyframe.event.data.ParticleKeyframeData;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class GeckoLibHelper {
    private static final Map<AnimatableInstanceCache, Map<LocatorParticleKey, Binding>> LOCATOR_EMITTERS = new WeakHashMap<>();
    private static final List<Runnable> RELOAD_CALLBACKS = new ArrayList<>();
    private static final List<WeakReference<Runnable>> WEAK_RELOAD_CALLBACKS = new ArrayList<>();
    private static final List<PendingEmitter> PENDING_EMITTERS = new ArrayList<>();

    private GeckoLibHelper() {}

    public static void postEvent() { RegisterLocatorPreTransformerEvent.postEvent(); }
    public static void addReloadCallback(Runnable callback) { RELOAD_CALLBACKS.add(callback); }
    public static void addWeakReloadCallback(Runnable callback) {
        pruneWeakReloadCallbacks();
        WEAK_RELOAD_CALLBACKS.add(new WeakReference<>(callback));
    }
    private static void pruneWeakReloadCallbacks() { WEAK_RELOAD_CALLBACKS.removeIf(reference -> reference.get() == null); }

    public static void clearReloadCallbacks() {
        RELOAD_CALLBACKS.clear();
        WEAK_RELOAD_CALLBACKS.clear();
        LOCATOR_EMITTERS.clear();
        PENDING_EMITTERS.clear();
    }

    public static void afterReload() {
        LOCATOR_EMITTERS.clear();
        PENDING_EMITTERS.clear();
        for (Runnable callback : List.copyOf(RELOAD_CALLBACKS)) callback.run();
        for (WeakReference<Runnable> reference : List.copyOf(WEAK_RELOAD_CALLBACKS)) {
            Runnable callback = reference.get();
            if (callback != null) callback.run();
        }
        pruneWeakReloadCallbacks();
    }

    public static void processParticleEffect(@Nullable GeoAnimatable animatable, AnimationController<?> controller, ParticleKeyframeData keyframeData) {
        try {
            processParticleEffectInternal(animatable, controller, keyframeData);
        } catch (RuntimeException exception) {
            BedrockParticles.LOGGER.error("Failed to process GeckoLib particle keyframe controller={} target={} effect={}",
                    controller, animatable, keyframeData.getEffect(), exception);
            throw exception;
        }
    }

    private static void processParticleEffectInternal(@Nullable GeoAnimatable animatable, AnimationController<?> controller,
                                                     ParticleKeyframeData keyframeData) {
        if (animatable == null) return;
        Object target = actorFor(animatable);
        if (target == null) return;
        ParticleEmitterAttachable attached = target instanceof ParticleEmitterAttachable value ? value : null;
        RenderItemContext.Owner owner = RenderItemContext.currentOwner();
        if (attached == null && owner == null) return;
        Level level = attached != null ? attached.getLevel() : owner.level();
        if (level == null) return;

        Vec3 basePos = attached != null ? attached.getPos() : owner.position();
        RenderItemContext.Snapshot itemContext = RenderItemContext.snapshot();
        ParticleContext context = new ParticleContext(target, level, basePos, attached == null
                ? RenderItemContext.variablesOrNew() : attached.getVariableTable(), attached, owner, itemContext);
        IPSParticleKeyframeData keyframe = IPSParticleKeyframeData.of(keyframeData);
        ResourceLocation particleId = keyframe.kltyton_bedrock_runtime$getParticle();
        ItemStack item = RenderItemContext.currentItem();
        VariableTable keyframeVariables = attached != null ? variableTable(attached) : RenderItemContext.variablesOrNew();
        BedrockParticleBindings.ItemResolution itemBinding = item == null ? null
                : BedrockParticleBindings.resolve(item, keyframeData.getEffect(), level, basePos,
                        keyframeVariables, attached);
        ResourceLocation mapped = itemBinding != null && itemBinding.selected() ? itemBinding.particle()
                : target instanceof Entity entity ? BedrockParticleBindings.resolve(entity, keyframeData.getEffect()) : null;
        if (mapped != null) particleId = mapped;
        MolangExp expression = keyframe.kltyton_bedrock_runtime$getExpression(keyframeVariables);
        String locatorName = keyframeData.getLocator();
        boolean bindToActor = !Boolean.FALSE.equals(keyframe.kltyton_bedrock_runtime$getBindToActor());
        if (locatorName == null || locatorName.isBlank()) {
            ParticleEmitter emitter = bindToActor
                    ? createEmitter(level, basePos, particleId, expression, attached)
                    : createUnboundEmitter(level, basePos, particleId, expression);
            if (!bindToActor) emitter.attach(null);
            if (bindToActor) registerRoot(animatable, controller, target, null, particleId, emitter, null, null, true, context);
            return;
        }

        Locator locator = findLocator(controller, locatorName);
        if (locator == null) {
            BedrockParticles.LOGGER.warn("Missing GeckoLib locator {} for particle {}", locatorName, particleId);
            return;
        }
        LocatorParticleKey bindingKey = new LocatorParticleKey(controller, target, locatorName, particleId);
        if (bindToActor && hasActiveRoot(animatable.getAnimatableInstanceCache(), bindingKey, level)) return;
        if (!bindToActor) {
            PENDING_EMITTERS.add(new PendingEmitter(animatable, controller, target, level, basePos, particleId, expression,
                    locatorName, locator.bone(), locator.value(), attached, keyframeVariables,
                    attached == null ? RenderItemContext.currentOwner() : null, itemContext, context));
            return;
        }
        ParticleEmitter emitter = createEmitter(level, basePos, particleId, expression, attached);
        emitter.parentSpace = new Matrix4x3f();
        registerRoot(animatable, controller, target, locatorName, particleId, emitter, locator.bone(), locator.value(), true, context);
    }

    private static VariableTable variableTable(@Nullable ParticleEmitterAttachable attached) {
        return attached == null ? new VariableTable(null) : attached.getVariableTable();
    }

    private static @Nullable Object actorFor(GeoAnimatable animatable) {
        Entity entity = ActorAnimationContext.entity(animatable);
        return entity != null ? entity : animatable;
    }

    private static ParticleEmitter createEmitter(Level level, Vec3 pos, ResourceLocation particleId, MolangExp expression,
                                                 @Nullable ParticleEmitterAttachable attached) {
        RenderItemContext.Owner owner = attached == null ? RenderItemContext.currentOwner() : null;
        VariableTable itemVariables = owner != null ? RenderItemContext.variablesOrNew() : null;
        ParticleEmitter emitter = ParticleEmitter.forAnimation(level, pos, particleId, expression, attached, itemVariables, owner);
        MolangParticleEngine.addEmitterWithPreEffect(emitter);
        return emitter;
    }

    private static ParticleEmitter createUnboundEmitter(Level level, Vec3 pos, ResourceLocation particleId, MolangExp expression) {
        ParticleEmitter emitter = ParticleEmitter.forAnimation(level, pos, particleId, expression, null, null, null);
        MolangParticleEngine.addEmitterWithPreEffect(emitter);
        return emitter;
    }

    private static Locator findLocator(AnimationController<?> controller, String name) {
        for (GeoBone bone : List.copyOf(IPSAnimationController.of(controller).kltyton_bedrock_runtime$getBonesWhichHasLocators())) {
            Map<String, LocatorValue> locators = IPSGeoBone.of(bone).kltyton_bedrock_runtime$getLocators();
            LocatorValue value = locators == null ? null : locators.get(name);
            if (value != null) return new Locator(bone, value);
        }
        return null;
    }

    private static Map<LocatorParticleKey, Binding> bindings(AnimatableInstanceCache cache) {
        return LOCATOR_EMITTERS.computeIfAbsent(cache, ignored -> new HashMap<>());
    }

    private static void registerRoot(GeoAnimatable animatable, AnimationController<?> controller, Object target,
                                     @Nullable String locatorName, ResourceLocation particleId, ParticleEmitter emitter,
                                     @Nullable GeoBone bone, @Nullable LocatorValue locator, boolean stopOnTransition,
                                     ParticleContext context) {
        Map<LocatorParticleKey, Binding> entries = bindings(animatable.getAnimatableInstanceCache());
        LocatorParticleKey key = new LocatorParticleKey(controller, target, locatorName, particleId);
        Binding binding = entries.get(key);
        if (binding == null) {
            binding = new Binding(key, bone, locator, stopOnTransition, context);
            entries.put(key, binding);
        }
        binding.roots().add(emitter);
        if (locator != null) {
            IPSAnimatableInstanceCache.of(animatable.getAnimatableInstanceCache()).kltyton_bedrock_runtime$getCachedId()
                    .computeIfAbsent(locator, ignored -> new IntArrayList()).add(emitter.id);
        }
    }

    private static void removeCachedId(AnimatableInstanceCache cache, @Nullable LocatorValue locator, ParticleEmitter emitter) {
        if (locator == null) return;
        IntArrayList ids = IPSAnimatableInstanceCache.of(cache).kltyton_bedrock_runtime$getCachedId().get(locator);
        if (ids == null) return;
        ids.rem(emitter.id);
        if (ids.isEmpty()) IPSAnimatableInstanceCache.of(cache).kltyton_bedrock_runtime$getCachedId().remove(locator);
    }

    private static boolean hasActiveRoot(AnimatableInstanceCache cache, LocatorParticleKey key, Level level) {
        Binding binding = LOCATOR_EMITTERS.getOrDefault(cache, Map.of()).get(key);
        return binding != null && binding.roots().stream().anyMatch(emitter -> isActive(emitter) && emitter.level == level);
    }
    private static boolean isActive(ParticleEmitter emitter) {
        return !emitter.isRemoved() && MolangParticleEngine.INSTANCE.getEmitter(emitter.id) == emitter;
    }

    public static void setCurrentEntity(GeoAnimatable animatable, @Nullable Entity entity) {
        if (animatable instanceof WithCurrentEntity replaced) replaced.setCurrentEntity(entity);
    }

    public static void removeEmittersWhenAnimationChange(AnimationController.State animationState, AnimationController<?> controller,
                                                          GeoAnimatable animatable) {
        if (animationState != AnimationController.State.TRANSITIONING) return;
        Object target = actorFor(animatable);
        if (target == null) return;
        Map<LocatorParticleKey, Binding> entries = LOCATOR_EMITTERS.get(animatable.getAnimatableInstanceCache());
        if (entries == null) return;
        for (Binding binding : List.copyOf(entries.values())) {
            if (binding.key().controller() != controller || binding.key().target() != target || !binding.stopOnTransition()) continue;
            for (ParticleEmitter root : List.copyOf(binding.roots())) {
                removeCachedId(animatable.getAnimatableInstanceCache(), binding.locator(), root);
                if (isActive(root)) MolangParticleEngine.INSTANCE.removeEmitter(root, false);
            }
        }
        pruneLocatorBindings();
    }

    public static void transformLocator(GeoBone bone, GeoAnimatable animatable, float partialTick) {
        if (usesNativeItemLocator(animatable)) return;
        AnimatableInstanceCache cache = animatable.getAnimatableInstanceCache();
        Object target = actorFor(animatable);
        if (target == null) return;
        Map<LocatorParticleKey, Binding> entries = LOCATOR_EMITTERS.get(cache);
        for (PendingEmitter request : List.copyOf(PENDING_EMITTERS)) {
            if (request.animatable() != animatable || request.target() != target || request.bone() != bone) continue;
            PENDING_EMITTERS.remove(request);
            try {
                Matrix4x3f matrix = locatorMatrix(animatable, bone, request.locator(), partialTick);
                if (request.attached() != null && request.attached().isDiscarded()) continue;
                Vec3 basePos = request.attached() == null ? request.basePos() : request.attached().getPos();
                Vec3 position = basePos.add(matrix.m30(), matrix.m31(), matrix.m32());
                try (RenderItemContext ignored = RenderItemContext.enterSnapshot(request.itemContext())) {
                    ParticleEmitter emitter = ParticleEmitter.forAnimation(request.level(), position, request.particleId(),
                            request.expression(), null, null, null);
                    emitter.attach(null);
                    emitter.setEffectFrame(new org.joml.Matrix3f().set(matrix));
                    MolangParticleEngine.addEmitterWithPreEffect(emitter);
                }
            } catch (RuntimeException exception) {
                BedrockParticles.LOGGER.error("Failed to create unbound GeckoLib locator emitter particle={} locator={} target={}",
                        request.particleId(), request.locatorName(), request.target(), exception);
                throw exception;
            }
        }
        if (entries == null || entries.isEmpty()) return;
        for (Binding binding : List.copyOf(entries.values())) {
            if (binding.key().target() != target || binding.bone() != bone) continue;
            binding.pruneRoots();
            for (ParticleEmitter root : List.copyOf(binding.roots())) {
                if (!isActive(root) && !root.hasLiveBoundEventChildren()) continue;
                try {
                    Matrix4x3f matrix = locatorMatrix(animatable, bone, binding.locator(), partialTick);
                    root.setLocalSpace(matrix);
                    Vec3 basePos = root.getAttached() != null ? root.getAttached().getPos() : Vec3.ZERO;
                    root.setPos(basePos.add(matrix.m30(), matrix.m31(), matrix.m32()));
                } catch (RuntimeException exception) {
                    BedrockParticles.LOGGER.error("Failed to update GeckoLib locator emitter {} locator={} target={}",
                            root.id, binding.key().locator(), binding.key().target(), exception);
                    throw exception;
                }
            }
        }
    }

    public static boolean usesNativeItemLocator(GeoAnimatable animatable) {
        return ActorAnimationContext.isItemRendering(animatable)
                && !RegisterLocatorPreTransformerEvent.hasCustomItemTransformer(animatable);
    }

    public static void captureNativeItemLocator(GeoBone bone, GeoAnimatable animatable, Matrix4f pose, float partialTick) {
        if (!usesNativeItemLocator(animatable) || RenderItemContext.currentDisplay() == ItemDisplayContext.GUI) return;
        AnimatableInstanceCache cache = animatable.getAnimatableInstanceCache();
        Map<LocatorParticleKey, Binding> bindings = LOCATOR_EMITTERS.get(cache);
        List<PendingEmitter> pending = PENDING_EMITTERS.stream()
                .filter(request -> request.animatable() == animatable && request.bone() == bone).toList();
        if ((bindings == null || bindings.isEmpty()) && pending.isEmpty()) return;
        Object actor = actorFor(animatable);
        var renderCamera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 camera = renderCamera.getPosition();
        Matrix4f worldPose = new Matrix4f().rotation(renderCamera.rotation())
                .mul(RenderSystem.getModelViewMatrix()).mul(pose);
        if (bindings != null) {
            for (Binding binding : new ArrayList<>(bindings.values())) {
                if (binding.key().target() != actor || binding.bone() != bone || binding.locator() == null) continue;
                Matrix4x3f frame = locatorTransform(cache, binding.locator(), new Matrix4x3f().set(worldPose));
                Vec3 base = attachmentBase(binding.context(), partialTick);
                frame.m30(frame.m30() + (float) (camera.x - base.x));
                frame.m31(frame.m31() + (float) (camera.y - base.y));
                frame.m32(frame.m32() + (float) (camera.z - base.z));
                for (ParticleEmitter emitter : List.copyOf(binding.roots())) emitter.setLocalSpace(new Matrix4x3f(frame));
            }
        }
        for (PendingEmitter request : pending) {
            PENDING_EMITTERS.remove(request);
            if (request.attached() != null && request.attached().isDiscarded()) continue;
            Matrix4x3f frame = locatorTransform(cache, request.locator(), new Matrix4x3f().set(worldPose));
            Vec3 position = camera.add(frame.m30(), frame.m31(), frame.m32());
            ParticleEmitter emitter = newEmitter(request.context(), position, request.particleId(), request.expression(), false);
            emitter.setEffectFrame(new org.joml.Matrix3f().set(frame));
            emitter.setPos(position);
            addAnimationEmitter(request.context(), emitter);
        }
    }

    private static Vec3 attachmentBase(ParticleContext context, float partialTick) {
        Vec3 position = context.owner() != null ? context.owner().position()
                : context.attached() == null ? context.basePos() : context.attached().getPos();
        return context.attached() instanceof Entity entity
                ? position.add(entity.getPosition(partialTick).subtract(entity.position())) : position;
    }

    private static ParticleEmitter newEmitter(ParticleContext context, Vec3 position, ResourceLocation particle,
            MolangExp expression, boolean bound) {
        try (RenderItemContext ignored = RenderItemContext.enterSnapshot(context.itemContext())) {
            VariableTable itemVariables = bound && context.attached() == null && context.itemContext().item() != null
                    ? context.variables() : null;
            return ParticleEmitter.forAnimation(context.level(), position, particle, expression,
                    bound ? context.attached() : null, itemVariables, bound ? context.owner() : null);
        }
    }

    private static void addAnimationEmitter(ParticleContext context, ParticleEmitter emitter) {
        try (RenderItemContext ignored = RenderItemContext.enterSnapshot(context.itemContext())) {
            MolangParticleEngine.addEmitterWithPreEffect(emitter);
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

    private static Matrix4x3f locatorMatrix(GeoAnimatable animatable, GeoBone bone, LocatorValue locator, float partialTick) {
        PoseStack poseStack = new PoseStack();
        RegisterLocatorPreTransformerEvent.getTransformer(animatable).transform(bone, animatable, poseStack, partialTick);
        double[] offset = locator.locatorClass() == null ? locator.values() : locator.locatorClass().offset();
        double[] rotation = locator.locatorClass() == null ? new double[3] : locator.locatorClass().rotation();
        poseStack.mulPose(new Quaternionf().rotationXYZ((float) Math.toRadians(rotation[0]), (float) Math.toRadians(rotation[1]),
                (float) Math.toRadians(rotation[2])));
        poseStack.translate(-offset[0] / 16.0, offset[1] / 16.0, offset[2] / 16.0);
        Matrix4x3f matrix = new Matrix4x3f();
        poseStack.last().pose().get4x3(matrix);
        return matrix;
    }

    public static void pruneLocatorBindings() {
        for (Map.Entry<AnimatableInstanceCache, Map<LocatorParticleKey, Binding>> cacheEntry
                : new ArrayList<>(LOCATOR_EMITTERS.entrySet())) {
            Map<LocatorParticleKey, Binding> bindings = cacheEntry.getValue();
            for (Map.Entry<LocatorParticleKey, Binding> entry : new ArrayList<>(bindings.entrySet())) {
                Binding binding = entry.getValue();
                if (binding.key().controller() == null || binding.key().target() == null) {
                    for (ParticleEmitter root : List.copyOf(binding.roots())) {
                        removeCachedId(cacheEntry.getKey(), binding.locator(), root);
                        if (isActive(root)) MolangParticleEngine.INSTANCE.removeEmitter(root, false);
                    }
                }
                for (ParticleEmitter root : List.copyOf(binding.roots())) {
                    if (!isActive(root) && !root.hasLiveBoundEventChildren()) {
                        removeCachedId(cacheEntry.getKey(), binding.locator(), root);
                    }
                }
                binding.pruneRoots();
                if (binding.roots().isEmpty()) bindings.remove(entry.getKey(), binding);
            }
            if (bindings.isEmpty()) LOCATOR_EMITTERS.remove(cacheEntry.getKey(), bindings);
        }
        PENDING_EMITTERS.removeIf(PendingEmitter::expired);
        pruneWeakReloadCallbacks();
    }

    private record Locator(GeoBone bone, LocatorValue value) {}
    private static final class LocatorParticleKey {
        private final WeakReference<AnimationController<?>> controller;
        private final WeakReference<Object> target;
        private final int controllerIdentity;
        private final int targetIdentity;
        private final @Nullable String locator;
        private final ResourceLocation particleId;
        private LocatorParticleKey(AnimationController<?> controller, Object target, @Nullable String locator, ResourceLocation particleId) {
            this.controller = new WeakReference<>(controller);
            this.target = new WeakReference<>(target);
            this.controllerIdentity = System.identityHashCode(controller);
            this.targetIdentity = System.identityHashCode(target);
            this.locator = locator;
            this.particleId = particleId;
        }
        private @Nullable AnimationController<?> controller() { return controller.get(); }
        private @Nullable Object target() { return target.get(); }
        private @Nullable String locator() { return locator; }
        @Override public boolean equals(Object other) {
            if (this == other) return true;
            return other instanceof LocatorParticleKey key && controller() != null && target() != null
                    && controller() == key.controller() && target() == key.target()
                    && java.util.Objects.equals(locator, key.locator) && particleId.equals(key.particleId);
        }
        @Override public int hashCode() {
            int result = controllerIdentity;
            result = 31 * result + targetIdentity;
            result = 31 * result + java.util.Objects.hashCode(locator);
            return 31 * result + particleId.hashCode();
        }
    }
    private static final class Binding {
        private final LocatorParticleKey key;
        private final @Nullable GeoBone bone;
        private final @Nullable LocatorValue locator;
        private final boolean stopOnTransition;
        private final ParticleContext context;
        private final List<ParticleEmitter> roots = new ArrayList<>();
        private Binding(LocatorParticleKey key, @Nullable GeoBone bone, @Nullable LocatorValue locator,
                        boolean stopOnTransition, ParticleContext context) {
            this.key = key;
            this.bone = bone;
            this.locator = locator;
            this.stopOnTransition = stopOnTransition;
            this.context = context;
        }
        private LocatorParticleKey key() { return key; }
        private @Nullable GeoBone bone() { return bone; }
        private @Nullable LocatorValue locator() { return locator; }
        private boolean stopOnTransition() { return stopOnTransition; }
        private ParticleContext context() { return context; }
        private List<ParticleEmitter> roots() { return roots; }
        private void pruneRoots() { roots.removeIf(root -> !isActive(root) && !root.hasLiveBoundEventChildren()); }
    }
    private record ParticleContext(Object actor, Level level, Vec3 basePos, VariableTable variables,
                                   @Nullable ParticleEmitterAttachable attached,
                                   @Nullable RenderItemContext.Owner owner,
                                   RenderItemContext.Snapshot itemContext) {}

    private record PendingEmitter(GeoAnimatable animatable, AnimationController<?> controller, Object target, Level level, Vec3 basePos,
                                  ResourceLocation particleId, MolangExp expression, String locatorName, GeoBone bone,
                                  LocatorValue locator, @Nullable ParticleEmitterAttachable attached,
                                  VariableTable variables, @Nullable RenderItemContext.Owner owner,
                                  RenderItemContext.Snapshot itemContext, ParticleContext context) {
        private boolean expired() { return level != Minecraft.getInstance().level || attached != null && (attached.getLevel() != level || attached.isDiscarded()); }
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
