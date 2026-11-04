package com.kltyton.bedrockruntime.client.particle.api.geckolib;

import com.kltyton.bedrockruntime.client.particle.api.event.ParticleEvents;

import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleEngine;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.KeyFrameEvent;
import com.geckolib.cache.animation.keyframeevent.ParticleKeyframeData;
import com.geckolib.cache.model.GeoLocator;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4x3f;
import com.kltyton.bedrockruntime.client.particle.PSDiagnostics;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockParticleBindings;
import com.kltyton.bedrockruntime.client.particle.runtime.geckolib.ActorAnimationContext;
import com.kltyton.bedrockruntime.client.particle.access.IPSParticleKeyframeData;
import com.kltyton.bedrockruntime.client.particle.access.IEntity;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.molang.AnimationFrameContext;
import com.kltyton.bedrockruntime.client.particle.definition.molang.RenderItemContext;
import com.kltyton.bedrockruntime.client.particle.definition.molang.VariableTable;
import com.kltyton.bedrockruntime.client.particle.api.ParticleEmitterAttachable;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class GeckoLibHelper {
    private static final Map<AnimatableManager<?>, Map<LocatorParticleKey, BoundEmitter>> LOCATOR_EMITTERS = new WeakHashMap<>();
    private static final Map<RenderPassInfo<?>, Map<GeoLocator, List<BoundEmitter>>> ACTIVE_LOCATOR_EMITTERS = new WeakHashMap<>();
    private static final Map<GeoRenderState, List<PendingEmitter>> PENDING_UNBOUND_EMITTERS = new WeakHashMap<>();
    private static final Map<RenderPassInfo<?>, Map<GeoLocator, List<PendingEmitter>>> ACTIVE_UNBOUND_EMITTERS = new WeakHashMap<>();
    private static final ThreadLocal<ArrayDeque<RenderPassInfo<?>>> CURRENT_RENDER_PASSES = new ThreadLocal<>();
    private static final List<Runnable> RELOAD_CALLBACKS = new ArrayList<>();

    private GeckoLibHelper() {
    }

    public static void postEvent() {
        ParticleEvents.post(new RegisterLocatorPreTransformerEvent());
    }

    public static void addReloadCallback(Runnable callback) {
        RELOAD_CALLBACKS.add(callback);
    }

    public static void clearReloadCallbacks() {
        RELOAD_CALLBACKS.clear();
        clearLocatorBindings();
    }

    public static void afterReload() {
        ACTIVE_LOCATOR_EMITTERS.clear();
        PENDING_UNBOUND_EMITTERS.clear();
        ACTIVE_UNBOUND_EMITTERS.clear();
        CURRENT_RENDER_PASSES.remove();
        pruneLocatorBindings();
        for (Runnable callback : List.copyOf(RELOAD_CALLBACKS)) {
            callback.run();
        }
    }

    private static void clearLocatorBindings() {
        LOCATOR_EMITTERS.clear();
        ACTIVE_LOCATOR_EMITTERS.clear();
        PENDING_UNBOUND_EMITTERS.clear();
        ACTIVE_UNBOUND_EMITTERS.clear();
        CURRENT_RENDER_PASSES.remove();
    }

    public static void processParticleEffect(KeyFrameEvent<? extends GeoAnimatable, ParticleKeyframeData> event) {
        try {
            ParticleContext context = createContext(event.animatable(), event.renderState());
            if (context == null) {
                return;
            }

            IPSParticleKeyframeData keyframe = IPSParticleKeyframeData.of(event.keyframeData());
            var item = RenderItemContext.currentItem();
            BedrockParticleBindings.ItemResolution itemBinding = item == null ? null
                    : BedrockParticleBindings.resolve(item, event.keyframeData().getEffect(), context.level(),
                            context.basePos(), context.variableTable(), context.attachable());
            Identifier mapped = itemBinding != null && itemBinding.selected() ? itemBinding.particle()
                    : context.attachable() instanceof Entity entity
                            ? BedrockParticleBindings.resolve(entity, event.keyframeData().getEffect()) : null;
            Identifier particleId = mapped == null ? keyframe.kltyton_bedrock_runtime$getParticle() : mapped;
            MolangExp expression = keyframe.kltyton_bedrock_runtime$getExpression(context.variableTable());
            String locator = event.keyframeData().getLocatorName();
            if (Boolean.FALSE.equals(keyframe.kltyton_bedrock_runtime$getBindToActor())) {
                if (locator == null || locator.isBlank()) {
                    createEmitter(context, context.basePos(), particleId, expression, false);
                } else {
                    PendingEmitter pending = new PendingEmitter(context, locator, particleId, expression, event.animatable());
                    RenderPassInfo<?> renderPass = currentRenderPass();
                    if (renderPass == null) PENDING_UNBOUND_EMITTERS.computeIfAbsent(event.renderState(), ignored -> new ArrayList<>()).add(pending);
                    else attachUnboundLocatorListener(renderPass, pending);
                }
                return;
            }
            getOrCreateEmitter(event.renderState(), context, event.controller(), locator, particleId, expression, event.animatable());
        } catch (RuntimeException exception) {
            BedrockParticles.LOGGER.error("GeckoLib particle keyframe failed effect={} locator={}",
                    event.keyframeData().getEffect(),
                    event.keyframeData().getLocatorName(),
                    exception
            );
            throw exception;
        }
    }

    public static void attachLocatorListeners(GeoRenderState renderState, RenderPassInfo<?> renderPassInfo) {
        List<PendingEmitter> pending = PENDING_UNBOUND_EMITTERS.remove(renderState);
        if (pending != null) {
            for (PendingEmitter emitter : pending) attachUnboundLocatorListener(renderPassInfo, emitter);
        }
        AnimatableManager<?> manager = renderState.getGeckolibData(DataTickets.ANIMATABLE_MANAGER);
        if (manager == null) {
            return;
        }

        Map<LocatorParticleKey, BoundEmitter> emitters = LOCATOR_EMITTERS.get(manager);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<LocatorParticleKey, BoundEmitter>> iterator = emitters.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<LocatorParticleKey, BoundEmitter> entry = iterator.next();
            BoundEmitter bound = entry.getValue();
            bound.pruneExpiredRoots();
            if (bound.roots.isEmpty()) {
                iterator.remove();
                continue;
            }

            if (!bound.locator().isBlank()) attachLocatorListener(renderPassInfo, bound);
        }
    }

    public static void enterRenderPass(RenderPassInfo<?> renderPassInfo) {
        ArrayDeque<RenderPassInfo<?>> passes = CURRENT_RENDER_PASSES.get();
        if (passes == null) {
            passes = new ArrayDeque<>();
            CURRENT_RENDER_PASSES.set(passes);
        }
        passes.push(renderPassInfo);
    }

    public static void exitRenderPass(RenderPassInfo<?> renderPassInfo) {
        ArrayDeque<RenderPassInfo<?>> passes = CURRENT_RENDER_PASSES.get();
        if (passes != null && passes.peek() == renderPassInfo) {
            passes.pop();
            if (passes.isEmpty()) CURRENT_RENDER_PASSES.remove();
        }
        ACTIVE_LOCATOR_EMITTERS.remove(renderPassInfo);
        ACTIVE_UNBOUND_EMITTERS.remove(renderPassInfo);
    }

    private static @Nullable RenderPassInfo<?> currentRenderPass() {
        ArrayDeque<RenderPassInfo<?>> passes = CURRENT_RENDER_PASSES.get();
        return passes == null ? null : passes.peek();
    }

    public static void pruneLocatorBindings() {
        for (Map.Entry<AnimatableManager<?>, Map<LocatorParticleKey, BoundEmitter>> managerEntry
                : new ArrayList<>(LOCATOR_EMITTERS.entrySet())) {
            Map<LocatorParticleKey, BoundEmitter> emitters = managerEntry.getValue();
            for (Map.Entry<LocatorParticleKey, BoundEmitter> entry : new ArrayList<>(emitters.entrySet())) {
                BoundEmitter bound = entry.getValue();
                bound.pruneExpiredRoots();
                if (bound.roots.isEmpty()) emitters.remove(entry.getKey(), bound);
            }
            if (emitters.isEmpty()) LOCATOR_EMITTERS.remove(managerEntry.getKey(), emitters);
        }
    }

    public static void removeEmitters(GeoRenderState renderState) {
        removeEmitters(renderState, null);
    }

    public static void removeEmitters(GeoRenderState renderState, @Nullable AnimationController<?> controller) {
        AnimatableManager<?> manager = renderState.getGeckolibData(DataTickets.ANIMATABLE_MANAGER);
        if (manager == null) {
            return;
        }

        Map<LocatorParticleKey, BoundEmitter> emitters = LOCATOR_EMITTERS.get(manager);
        if (emitters != null) {
            for (Map.Entry<LocatorParticleKey, BoundEmitter> entry : new ArrayList<>(emitters.entrySet())) {
                if (controller != null && entry.getKey().controller() != controller) continue;
                BoundEmitter bound = entry.getValue();
                for (ParticleEmitter emitter : List.copyOf(bound.roots)) {
                    if (MolangParticleEngine.INSTANCE._getEmitter(emitter.id) == emitter) {
                        MolangParticleEngine.INSTANCE.removeEmitter(emitter, false);
                    }
                }
                bound.pruneExpiredRoots();
                if (bound.roots.isEmpty()) emitters.remove(entry.getKey(), bound);
            }
            if (emitters.isEmpty()) LOCATOR_EMITTERS.remove(manager, emitters);
        }
    }

    public static void setCurrentEntity(Object animatable, @Nullable Entity entity) {
        if (animatable instanceof WithCurrentEntity withCurrentEntity) {
            withCurrentEntity.setCurrentEntity(entity);
        }
    }

    public static @Nullable Entity animationEntity(Object animatable) {
        return animatable instanceof WithCurrentEntity replaced ? replaced.getCurrentEntity()
                : animatable instanceof Entity entity ? entity : null;
    }

    public static @Nullable Entity animationEntity(Object animatable, GeoRenderState renderState) {
        Entity entity = animationEntity(animatable);
        return entity != null ? entity : renderState.getGeckolibData(ActorAnimationContext.ENTITY);
    }

    private static ParticleEmitter createEmitter(ParticleContext context, Vec3 position,
                                                  Identifier particleId, MolangExp expression, boolean bindToActor) {
        ParticleEmitterAttachable attached = bindToActor ? context.attachable() : null;
        ItemOwner owner = bindToActor ? context.owner() : null;
        VariableTable itemVariables = bindToActor && attached == null && RenderItemContext.currentItem() != null
                ? context.variableTable() : null;
        ParticleEmitter emitter = ParticleEmitter.forAnimation(context.level(), position, particleId, expression,
                attached, itemVariables, owner);
        MolangParticleEngine.addEmitterWithPreEffect(emitter);
        return emitter;
    }

    private static @Nullable ParticleEmitter getOrCreateEmitter(GeoRenderState renderState, ParticleContext context, AnimationController<?> controller, @Nullable String locator, Identifier particleId, MolangExp expression, GeoAnimatable animatable) {
        if (locator == null || locator.isBlank()) {
            ParticleEmitter emitter = createEmitter(context, context.basePos(), particleId, expression, true);
            AnimatableManager<?> manager = renderState.getGeckolibData(DataTickets.ANIMATABLE_MANAGER);
            if (manager != null) {
                Map<LocatorParticleKey, BoundEmitter> emitters = LOCATOR_EMITTERS.computeIfAbsent(manager, ignored -> new Object2ObjectOpenHashMap<>());
                LocatorParticleKey key = new LocatorParticleKey(controller, "", particleId);
                BoundEmitter bound = emitters.get(key);
                if (bound == null) emitters.put(key, new BoundEmitter(emitter, "", particleId, context.basePos(), animatable));
                else {
                    bound.pruneExpiredRoots();
                    bound.roots.add(emitter);
                }
            }
            return emitter;
        }

        AnimatableManager<?> manager = renderState.getGeckolibData(DataTickets.ANIMATABLE_MANAGER);
        if (manager == null) {
            return null;
        }

        Map<LocatorParticleKey, BoundEmitter> emitters = LOCATOR_EMITTERS.computeIfAbsent(manager, ignored -> new Object2ObjectOpenHashMap<>());
        LocatorParticleKey key = new LocatorParticleKey(controller, locator, particleId);
        BoundEmitter bound = emitters.get(key);
        ParticleEmitter emitter = createEmitter(context, context.basePos(), particleId, expression, true);
        emitter.parentSpace = new Matrix4x3f();
        BoundEmitter newBound;
        if (bound == null) {
            newBound = new BoundEmitter(emitter, locator, particleId, context.basePos(), animatable);
        } else {
            bound.pruneExpiredRoots();
            bound.roots.add(emitter);
            newBound = bound;
        }
        emitters.put(key, newBound);
        attachCurrentRenderPassListener(newBound);
        PSDiagnostics.infoFirstN("geckolib-emitter-create:" + locator + ":" + particleId, 8, "GeckoLib locator emitter created runtimeId={} particle={} locator={} basePos={}",
                emitter.id,
                particleId,
                locator,
                context.basePos()
        );
        return emitter;
    }

    private static void updateEmitterPosition(ParticleEmitter emitter, BoundEmitter bound, GeoRenderState renderState,
                                              @Nullable Vec3 worldPos, @Nullable Vec3 modelPos, @Nullable Vec3 localPos) {
        if (RegisterLocatorPreTransformerEvent.getRegisteredTransformer(bound.animatable()) != null) {
            return;
        }
        Vec3 target = worldPos;
        if (target == null && (localPos != null || modelPos != null)) {
            Vec3 base = attachmentBase(emitter.getAttached(), emitter.getItemOwner(),
                    renderState.getOrDefaultGeckolibData(DataTickets.POSITION, bound.basePos()), renderState.getPartialTick());
            target = base.add(localPos != null ? localPos : modelPos.scale(1.0 / 16.0));
        }
        if (target == null) {
            PSDiagnostics.warnFirstN("geckolib-locator-missing:" + bound.locator() + ":" + bound.particleId(), 8, "GeckoLib locator update missing position runtimeId={} particle={} locator={}",
                    emitter.id,
                    bound.particleId(),
                    bound.locator()
            );
            return;
        }

        emitter.setLocatorPosition(target);
        PSDiagnostics.infoFirstN("geckolib-locator-update:" + bound.locator() + ":" + bound.particleId(), 8, "GeckoLib locator update runtimeId={} particle={} locator={} target={} worldPos={} modelPos={} localPos={}",
                emitter.id,
                bound.particleId(),
                bound.locator(),
                target,
                worldPos,
                modelPos,
                localPos
        );
    }

    private static void attachCurrentRenderPassListener(BoundEmitter bound) {
        RenderPassInfo<?> renderPassInfo = currentRenderPass();
        if (renderPassInfo != null) {
            attachLocatorListener(renderPassInfo, bound);
        }
    }

    private static void attachLocatorListener(RenderPassInfo<?> renderPassInfo, BoundEmitter bound) {
        renderPassInfo.model().getLocator(bound.locator()).ifPresent(locator -> {
            List<BoundEmitter> bounds = ACTIVE_LOCATOR_EMITTERS
                    .computeIfAbsent(renderPassInfo, ignored -> new Object2ObjectOpenHashMap<>())
                    .computeIfAbsent(locator, ignored -> new ArrayList<>());
            if (!bounds.contains(bound)) {
                bounds.add(bound);
                renderPassInfo.addLocatorPositionListener(bound.locator(), (worldPos, modelPos, localPos) -> {
                    for (ParticleEmitter emitter : List.copyOf(bound.roots)) {
                        updateEmitterPosition(emitter, bound, renderPassInfo.renderState(), worldPos, modelPos, localPos);
                    }
                });
            }
        });
    }

    private static void attachUnboundLocatorListener(RenderPassInfo<?> renderPassInfo, PendingEmitter pending) {
        var locator = renderPassInfo.model().getLocator(pending.locator);
        if (locator.isEmpty()) {
            BedrockParticles.LOGGER.warn("Particle effect {} references missing GeckoLib locator {}", pending.particleId, pending.locator);
            return;
        }
        ACTIVE_UNBOUND_EMITTERS.computeIfAbsent(renderPassInfo, ignored -> new Object2ObjectOpenHashMap<>())
                .computeIfAbsent(locator.get(), ignored -> new ArrayList<>()).add(pending);
        renderPassInfo.addLocatorPositionListener(pending.locator,
                (worldPos, modelPos, localPos) -> spawnUnboundEmitter(pending, worldPos, modelPos, localPos,
                        renderPassInfo.renderState().getPartialTick()));
    }

    private static void spawnUnboundEmitter(PendingEmitter pending, @Nullable Vec3 worldPos,
                                             @Nullable Vec3 modelPos, @Nullable Vec3 localPos, float partialTick) {
        if (pending.completed) return;
        Matrix4x3f transform = pending.transform;
        if (transform == null) {
            BedrockParticles.LOGGER.warn("No transform captured for particle effect {} at locator {}", pending.particleId, pending.locator);
            return;
        }
        boolean custom = RegisterLocatorPreTransformerEvent.getRegisteredTransformer(pending.animatable) != null;
        Vec3 target;
        if (custom) {
            Vec3 base = attachmentBase(pending.context.attachable(), pending.context.owner(), pending.context.basePos(), partialTick);
            target = base.add(transform.m30(), transform.m31(), transform.m32());
        } else if (worldPos != null) {
            target = worldPos;
        } else if (localPos != null || modelPos != null) {
            Vec3 base = attachmentBase(pending.context.attachable(), pending.context.owner(), pending.context.basePos(), partialTick);
            target = base.add(localPos != null ? localPos : modelPos.scale(1.0 / 16.0));
        } else {
            target = null;
        }
        if (target == null) {
            BedrockParticles.LOGGER.warn("No position captured for particle effect {} at locator {}", pending.particleId, pending.locator);
            return;
        }
        pending.completed = true;
        try (AnimationFrameContext ignoredAnimation = AnimationFrameContext.enter(pending.animationTime);
             RenderItemContext ignoredItem = RenderItemContext.enterSnapshot(pending.renderedItem)) {
            ParticleEmitter emitter = createEmitter(pending.context, target, pending.particleId, pending.expression, false);
            emitter.setEffectFrame(new org.joml.Matrix3f().set(transform));
        } catch (RuntimeException exception) {
            BedrockParticles.LOGGER.error("Failed to create unbound particle effect {} at locator {}", pending.particleId, pending.locator, exception);
            throw exception;
        }
    }

    public static void captureLocatorTransform(GeoLocator locator, PoseStack poseStack, RenderPassInfo<?> renderPassInfo) {
        Map<GeoLocator, List<BoundEmitter>> locators = ACTIVE_LOCATOR_EMITTERS.get(renderPassInfo);
        List<BoundEmitter> bounds = locators == null ? null : locators.get(locator);
        Map<GeoLocator, List<PendingEmitter>> unboundLocators = ACTIVE_UNBOUND_EMITTERS.get(renderPassInfo);
        List<PendingEmitter> pending = unboundLocators == null ? null : unboundLocators.get(locator);
        if ((bounds == null || bounds.isEmpty()) && (pending == null || pending.isEmpty())) return;

        Matrix4f localPose = RenderUtil.extractPoseFromRoot(new Matrix4f(poseStack.last().pose()), renderPassInfo.getPreRenderMatrixState());
        if (bounds != null) for (BoundEmitter bound : bounds) {
            Matrix4x3f transform = new Matrix4x3f();
            RegisterLocatorPreTransformerEvent.Transformer<GeoAnimatable> transformer =
                    RegisterLocatorPreTransformerEvent.getRegisteredTransformer(bound.animatable());
            if (transformer == null) {
                localPose.get4x3(transform);
            } else {
                transformer.transform(locator.parent(), bound.animatable(), transform, renderPassInfo.renderState().getPartialTick());
                transform.translate(locator.offsetX() / 16.0F, locator.offsetY() / 16.0F, locator.offsetZ() / 16.0F)
                        .rotateZ(locator.rotZ()).rotateY(locator.rotY()).rotateX(locator.rotX());
            }
            for (ParticleEmitter emitter : List.copyOf(bound.roots)) {
                emitter.setLocalSpace(transform, false);
                ParticleEmitterAttachable attached = emitter.getAttached();
                Vec3 base = attachmentBase(attached, emitter.getItemOwner(),
                        renderPassInfo.renderState().getOrDefaultGeckolibData(DataTickets.POSITION, bound.basePos()),
                        renderPassInfo.renderState().getPartialTick());
                emitter.setLocatorPosition(base.add(transform.m30(), transform.m31(), transform.m32()), false);
            }
        }
        if (pending != null) for (PendingEmitter request : pending) {
            if (request.completed) continue;
            Matrix4x3f transform = new Matrix4x3f();
            RegisterLocatorPreTransformerEvent.Transformer<GeoAnimatable> transformer =
                    RegisterLocatorPreTransformerEvent.getRegisteredTransformer(request.animatable);
            if (transformer == null) {
                localPose.get4x3(transform);
            } else {
                transformer.transform(locator.parent(), request.animatable, transform, renderPassInfo.renderState().getPartialTick());
                transform.translate(locator.offsetX() / 16.0F, locator.offsetY() / 16.0F, locator.offsetZ() / 16.0F)
                        .rotateZ(locator.rotZ()).rotateY(locator.rotY()).rotateX(locator.rotX());
            }
            request.transform = transform;
        }
    }

    private static Vec3 attachmentBase(@Nullable ParticleEmitterAttachable attached, @Nullable ItemOwner owner,
                                       Vec3 fallback, float partialTick) {
        if (owner != null) {
            Vec3 position = owner.position();
            return attached instanceof Entity entity
                    ? position.add(entity.getPosition(partialTick).subtract(entity.position())) : position;
        }
        if (attached == null) return fallback;
        Vec3 position = attached.getPos();
        return attached instanceof Entity entity
                ? position.add(entity.getPosition(partialTick).subtract(entity.position())) : position;
    }

    private static @Nullable ParticleContext createContext(GeoAnimatable animatable, GeoRenderState renderState) {
        Entity entityTarget = animationEntity(animatable, renderState);
        Object target = entityTarget != null ? entityTarget : animatable;
        ParticleEmitterAttachable attachable = target instanceof ParticleEmitterAttachable value ? value : null;
        ItemOwner owner = attachable == null ? RenderItemContext.currentOwner() : null;
        if (owner != null) {
            ItemOwner root = owner;
            while (root instanceof ItemOwner.OffsetFromOwner offset) root = offset.owner();
            if (root instanceof Entity entity) attachable = IEntity.of(entity);
            else {
                var living = owner.asLivingEntity();
                if (living != null) attachable = IEntity.of(living);
            }
        }
        if (attachable != null && attachable.getLevel() == null) {
            return null;
        }
        Level level;
        Vec3 basePos;
        VariableTable variableTable;

        if (owner != null) {
            level = owner.level();
            basePos = owner.position();
            variableTable = attachable == null ? RenderItemContext.variablesOrNew() : attachable.getVariableTable();
        } else if (attachable != null && attachable.getLevel() != null) {
            level = attachable.getLevel();
            basePos = attachable.getPos();
            variableTable = attachable.getVariableTable();
        } else if (Minecraft.getInstance().level != null) {
            level = Minecraft.getInstance().level;
            basePos = renderState.getGeckolibData(DataTickets.POSITION);
            if (basePos == null) {
                PSDiagnostics.infoOnce("geckolib-no-render-position:" + animatable.getClass().getName(),
                        "GeckoLib particle keyframe has no world position for {}", animatable);
                return null;
            }
            variableTable = RenderItemContext.variablesOrNew();
        } else {
            return null;
        }

        return new ParticleContext(level, basePos, variableTable, attachable, owner);
    }

    private record ParticleContext(Level level, Vec3 basePos, VariableTable variableTable,
                                   @Nullable ParticleEmitterAttachable attachable, @Nullable ItemOwner owner) {
    }

    private static final class PendingEmitter {
        private final ParticleContext context;
        private final String locator;
        private final Identifier particleId;
        private final MolangExp expression;
        private final GeoAnimatable animatable;
        private final double animationTime;
        private final RenderItemContext.Snapshot renderedItem;
        private @Nullable Matrix4x3f transform;
        private boolean completed;

        private PendingEmitter(ParticleContext context, String locator, Identifier particleId, MolangExp expression, GeoAnimatable animatable) {
            this.context = context;
            this.locator = locator;
            this.particleId = particleId;
            this.expression = expression;
            this.animatable = animatable;
            this.animationTime = AnimationFrameContext.currentTime();
            this.renderedItem = RenderItemContext.snapshot();
        }
    }

    private record LocatorParticleKey(AnimationController<?> controller, String locator, Identifier particleId) {
        @Override
        public boolean equals(Object other) {
            return other instanceof LocatorParticleKey key && controller == key.controller
                    && locator.equals(key.locator) && particleId.equals(key.particleId);
        }

        @Override
        public int hashCode() {
            int result = System.identityHashCode(controller);
            result = 31 * result + locator.hashCode();
            return 31 * result + particleId.hashCode();
        }
    }

    private record BoundEmitter(List<ParticleEmitter> roots, String locator, Identifier particleId, Vec3 basePos, GeoAnimatable animatable) {
        private BoundEmitter(ParticleEmitter emitter, String locator, Identifier particleId, Vec3 basePos, GeoAnimatable animatable) {
            this(new ArrayList<>(List.of(emitter)), locator, particleId, basePos, animatable);
        }

        private void pruneExpiredRoots() {
            roots.removeIf(emitter -> (emitter.isRemoved() || MolangParticleEngine.INSTANCE._getEmitter(emitter.id) != emitter)
                    && !emitter.hasLiveLocalParticles() && !emitter.hasLiveBoundEventChildren());
        }
    }
}
