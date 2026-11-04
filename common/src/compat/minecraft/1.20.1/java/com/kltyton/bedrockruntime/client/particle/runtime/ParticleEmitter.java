package com.kltyton.bedrockruntime.client.particle.runtime;
import com.kltyton.bedrockruntime.client.particle.runtime.timing.EmitterSimulationTime;
import com.kltyton.bedrockruntime.client.particle.runtime.timing.ParticleSimulationClock;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import com.kltyton.bedrockruntime.client.particle.definition.molang.RenderItemContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4x3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.PSDiagnostics;
import com.kltyton.bedrockruntime.client.particle.api.IEmitterComponent;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.api.ParticleEmitterAttachable;
import com.kltyton.bedrockruntime.client.particle.definition.component.EmitterLifetime;
import com.kltyton.bedrockruntime.client.particle.definition.component.EmitterLifetimeEvents;
import com.kltyton.bedrockruntime.client.particle.definition.component.EmitterInitialization;
import com.kltyton.bedrockruntime.client.particle.definition.component.EmitterMultiShape;
import com.kltyton.bedrockruntime.client.particle.definition.component.EmitterRate;
import com.kltyton.bedrockruntime.client.particle.definition.component.EmitterShape;
import com.kltyton.bedrockruntime.client.particle.definition.event.ParticleEffect;
import com.kltyton.bedrockruntime.client.particle.definition.molang.FloatMolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.molang.AttachedVariableTable;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.molang.RenderFrameContext;
import com.kltyton.bedrockruntime.client.particle.definition.molang.VariableTable;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangParser;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangVersion;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.Variable;
import com.kltyton.bedrockruntime.client.particle.runtime.attachment.EmitterAttachHandler;
import com.kltyton.bedrockruntime.client.particle.runtime.attachment.IgnoreRangeParticleEmitter;
import com.kltyton.bedrockruntime.client.particle.access.IBlockEntity;
import com.kltyton.bedrockruntime.client.particle.access.IEntity;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Queue;

public class ParticleEmitter implements MolangInstance {
    private final EmitterSimulationTime simulationTime = new EmitterSimulationTime(this);
    public static final String TYPE_KEY = "type";
    public static final ResourceLocation TYPE = BedrockParticles.asResource("default");

    public final ResourceLocation type;
    public ResourceLocation particleId;
    public MolangExp expression;

    public transient Matrix4x3f parentSpace;
    public transient ParentMode parentMode = ParentMode.WORLD;
    public transient Vec3 offsetPos = Vec3.ZERO;
    public transient Vector3f offsetRot = new Vector3f();
    public transient Vector3f parentPosition;
    public transient Vector3f parentRotation;
    protected transient EmitterPreset preset;
    protected transient VariableTable vars;
    protected transient List<IEmitterComponent> components;
    public transient ParticleEmitter parent;
    private transient boolean screenOwned;
    private transient boolean independentEventChild;
    private transient boolean boundEventChild;
    private transient Vec3 boundEventWorldOffset = Vec3.ZERO;
    private transient Vec3 boundEventOffset = Vec3.ZERO;
    private transient boolean removalCallbacksRun;
    private @Nullable MolangVersion expressionVersion;
    private boolean expressionBedrockItemNames;
    private transient boolean deferredEffectInitialization;
    private transient boolean deferredPreEffectPending;
    private transient boolean attachmentModifiedDuringInitialization;
    private transient boolean itemOwnerModifiedDuringInitialization;
    private transient boolean installingDeferredBinding;
    private transient boolean preserveDeferredVariableParent;
    private transient long attachmentRevision;
    private transient int attachmentDepth;
    private transient long deferredParentRevision;
    private transient @Nullable ParticleEmitterAttachable deferredAttachment;
    private transient @Nullable VariableTable deferredItemVariables;
    private transient @Nullable RenderItemContext.Owner deferredItemOwner;
    private transient @Nullable ParticleEmitter deferredEventBindingSource;
    private transient long registrationGeneration;
    private transient @Nullable ParticleEffect.SpawnContext eventSpawnContext;
    private transient @Nullable EmitterPreset eventPresetOverride;
    private transient @Nullable ParticlePreset eventParticlePresetOverride;
    private transient @Nullable Map<ManualEventKey, ParticleEmitter> manualEventChildren;
    private transient @Nullable ParticleEmitter manualEventSource;
    private transient @Nullable ManualEventKey manualEventKey;
    private transient @Nullable Queue<ManualEventRequest> manualEventRequests;
    private transient @Nullable Vec3 manualEmissionPosition;
    private transient @Nullable RenderItemContext.Owner itemOwner;
    private transient @Nullable VariableTable itemVariables;
    private transient @Nullable ParticleEmitter inheritedAttachmentSource;
    private transient @Nullable ParticleEmitterAttachable inheritedAttachmentTarget;
    private transient Vec3 previousMovementPosition = Vec3.ZERO;
    private transient float cameraDistance;
    private transient final Vector3f movementVelocity = new Vector3f();
    public transient @Nullable Runnable afterParentInit;
    public transient final List<ParticleEmitter> children = new ArrayList<>();
    /** Velocity supplied by manual emission, in blocks per second. */
    public transient Vector3f inheritedParticleSpeed;
    public transient boolean isManual;

    protected double emitterRandom1;
    protected double emitterRandom2;
    protected double emitterRandom3;
    protected double emitterRandom4;
    public int id;

    public transient float invTickRate;
    public transient int age = 0;
    public transient int lifetime = 0;
    public transient boolean active = true;
    public transient int loopingTime = 0;
    public transient int activeTime = 0;
    public transient int fullLoopTime = 0;
    public transient MutableParticleGroup particleGroup;
    public transient @Nullable EmitterRate.Manual manualRate;
    public transient @Nullable EmitterRate.Steady steadyRate;
    public transient float steadyRatePerSecond;
    public transient float steadyEmissionAccumulator;
    public transient int activeParticleCount = 0;
    private transient int pendingParticleCount;
    public transient float spawnChance;
    public transient int spawnRate = 0;
    public transient boolean spawned = false;
    public boolean hideOutline;
    protected transient ParticleEmitterAttachable attached;
    public transient int lastTimeline = 0;
    public transient float moveDist = 0.0F;
    public transient float moveDistO = 0.0F;
    public transient int lastTravelDist = 0;
    public transient float[] cachedLooping;

    public transient final Level level;
    public Vec3 pos;
    public Vec3 posO = Vec3.ZERO;
    public Vector3f rot = new Vector3f();
    private transient boolean removed = false;
    private transient boolean expired;
    private transient boolean expirationEventFired;
    private transient boolean firstUpdate = true;
    private transient boolean restoredRandoms;
    private transient PerFrameParticleExpressions preparedRenderOwner;
    private transient long preparedRenderPass;
    private transient final org.joml.Matrix3f effectFrame = new org.joml.Matrix3f();
    private transient boolean hasEffectFrame;
    private transient @Nullable Map<Set<BlockState>, Set<BlockState>> expirationBlocks;

    public ParticleEmitter(ResourceLocation type, Level level, Vec3 pos, ResourceLocation particleId, MolangExp expression) {
        this(type, level, pos, particleId, expression, null);
    }

    public ParticleEmitter(ResourceLocation type, Level level, Vec3 pos, ResourceLocation particleId, MolangExp expression,
                           @Nullable ParticleEmitterAttachable attached) {
        this(type, level, pos, particleId, expression, attached, false);
    }

    protected ParticleEmitter(ResourceLocation type, Level level, Vec3 pos, ResourceLocation particleId, MolangExp expression,
                              @Nullable ParticleEmitterAttachable attached, boolean deferInitialization) {
        this(type, level, pos, particleId, expression, attached, null, null, deferInitialization);
    }

    private ParticleEmitter(ResourceLocation type, Level level, Vec3 pos, ResourceLocation particleId, MolangExp expression,
                            @Nullable ParticleEmitterAttachable attached, @Nullable VariableTable itemVariables,
                            @Nullable RenderItemContext.Owner itemOwner, boolean deferInitialization) {
        this(type, level, pos, particleId, expression, attached, itemVariables, itemOwner, deferInitialization, false);
    }

    private ParticleEmitter(ResourceLocation type, Level level, Vec3 pos, ResourceLocation particleId, MolangExp expression,
                            @Nullable ParticleEmitterAttachable attached, @Nullable VariableTable itemVariables,
                            @Nullable RenderItemContext.Owner itemOwner, boolean deferInitialization, boolean animationPreEffect) {
        this.type = type;
        this.level = level;
        setPos(pos);
        this.posO = pos;
        this.particleId = particleId;
        this.expression = expression;
        if (animationPreEffect) {
            this.deferredAttachment = attached;
            this.deferredItemVariables = itemVariables;
            this.deferredItemOwner = itemOwner;
        } else {
            this.attached = attached;
            this.itemVariables = itemVariables;
            this.itemOwner = itemOwner;
        }
        this.deferredEffectInitialization = animationPreEffect;
        this.deferredPreEffectPending = animationPreEffect;
        this.invTickRate = 1.0F / 20.0F;
        simulationTime.setLoopTimes(1_000_000.0F, 0.0F);
        if (!deferInitialization) {
            try {
                init();
            } finally {
                this.deferredEffectInitialization = false;
            }
        }
    }

    public ParticleEmitter(Level level, Vec3 pos, ResourceLocation particleId, MolangExp expression) {
        this(TYPE, level, pos, particleId, expression);
    }

    public ParticleEmitter(Level level, Vec3 pos, ResourceLocation particleId, MolangExp expression,
                           @Nullable ParticleEmitterAttachable attached) {
        this(TYPE, level, pos, particleId, expression, attached);
    }

    @ApiStatus.Internal
    public ParticleEmitter(Level level, Vec3 pos, ResourceLocation particleId, MolangExp expression,
                           @Nullable ParticleEmitterAttachable attached, @Nullable VariableTable itemVariables) {
        this(TYPE, level, pos, particleId, expression, attached, itemVariables, null, false);
    }

    @ApiStatus.Internal
    public ParticleEmitter(Level level, Vec3 pos, ResourceLocation particleId, MolangExp expression,
                           @Nullable ParticleEmitterAttachable attached, @Nullable VariableTable itemVariables,
                           @Nullable RenderItemContext.Owner itemOwner) {
        this(TYPE, level, pos, particleId, expression, attached, itemVariables, itemOwner, false);
    }

    @ApiStatus.Internal
    public static ParticleEmitter forAnimation(Level level, Vec3 pos, ResourceLocation particleId, MolangExp expression,
                                                @Nullable ParticleEmitterAttachable attached,
                                                @Nullable VariableTable itemVariables, @Nullable RenderItemContext.Owner itemOwner) {
        return new ParticleEmitter(TYPE, level, pos, particleId, expression, attached, itemVariables, itemOwner, false, true);
    }

    public ParticleEmitter(Level level, Vec3 pos, ResourceLocation particleId) {
        this(level, pos, particleId, MolangExp.EMPTY);
    }

    public ParticleEmitter(Level level, CompoundTag tag) {
        this(level, tag, false);
    }

    protected ParticleEmitter(Level level, CompoundTag tag, boolean deferInitialization) {
        ResourceLocation type = ResourceLocation.tryParse(tag.getString(TYPE_KEY));
        if (type == null) type = TYPE;
        this.type = type;
        this.level = level;
        deserialize(tag);
        simulationTime.setLoopTimes(1_000_000.0F, 0.0F);
        this.restoredRandoms = true;
        this.invTickRate = 1.0F / 20.0F;
        if (!deferInitialization) init();
    }

    public ParticleEmitter(ParticleEmitter parent, ParticleEffect effect) {
        this(parent, effect, null, null);
    }

    protected ParticleEmitter(ParticleEmitter parent, ParticleEffect effect,
                              @Nullable EmitterPreset presetOverride, @Nullable ParticlePreset particlePresetOverride) {
        this(parent, effect, presetOverride, particlePresetOverride, false);
    }

    protected ParticleEmitter(ParticleEmitter parent, ParticleEffect effect,
                              @Nullable EmitterPreset presetOverride, @Nullable ParticlePreset particlePresetOverride,
                              boolean deferInitialization) {
        this.eventPresetOverride = presetOverride;
        this.eventParticlePresetOverride = particlePresetOverride;
        this.type = parent.type;
        this.hideOutline = parent.hideOutline;
        this.level = parent.level;
        ParticleEffect.SpawnContext spawn = ParticleEffect.spawnContext(parent, effect);
        this.eventSpawnContext = spawn;
        this.deferredPreEffectPending = spawn != null && (parent.usesDefaultEventFactory()
                || com.kltyton.bedrockruntime.client.particle.api.RegisterCustomEmitterTypeEvent.hasManagedEffectFactory(parent.type));
        if (spawn != null && effect.type() == ParticleEffect.Type.EMITTER) setEffectFrame(spawn.basis());
        boolean particleEvent = effect.type() == ParticleEffect.Type.PARTICLE
                || effect.type() == ParticleEffect.Type.PARTICLE_WITH_VELOCITY;
        setPos(particleEvent ? Vec3.ZERO : spawn == null ? parent.pos : spawn.position());
        this.posO = pos;
        this.particleId = effect.effect();
        this.expression = effect.preEffectExpression();
        this.expressionVersion = parent.getPreset().molangVersion;
        this.expressionBedrockItemNames = parent.getPreset().bedrockItemNames;
        this.independentEventChild = true;
        this.boundEventChild = effect.type() == ParticleEffect.Type.EMITTER_BOUND;
        this.boundEventWorldOffset = pos.subtract(parent.pos);
        this.invTickRate = 1.0F / 20.0F;
        simulationTime.setLoopTimes(1_000_000.0F, 0.0F);
        this.afterParentInit = () -> {
            switch (effect.type()) {
                case EMITTER, PARTICLE, PARTICLE_WITH_VELOCITY -> {}
                case EMITTER_BOUND -> {
                    inheritEventBinding(parent);
                    inheritAttachment(parent, parent.getAttached());
                }
            }
        };
        if (!deferInitialization) initializeEvent(parent, effect);
    }

    protected final void initializeEvent(ParticleEmitter parent, ParticleEffect effect) {
        boolean deferred = deferredPreEffectPending;
        if (deferred) deferredEffectInitialization = true;
        try {
            initializeEventComponents(parent, effect, deferred);
        } finally {
            if (deferred) deferredEffectInitialization = false;
        }
    }

    protected final void preserveImmediateEventPreEffect() {
        deferredPreEffectPending = false;
    }

    private void initializeEventComponents(ParticleEmitter parent, ParticleEffect effect, boolean deferred) {
        addParent(parent);
        if (effect.type() == ParticleEffect.Type.EMITTER_BOUND) {
            inheritEventBinding(parent);
            if (deferred) {
                deferredEventBindingSource = parent;
                attached = null;
                itemOwner = null;
                itemVariables = null;
            }
        }
        createVars();
        if (attached != null) attach(attached);
        else if (itemVariables != null) updateAttachmentScope();
        for (ParticleEffect.SharedVar shared : effect.sharedVars()) {
            String name = shared.name();
            Variable variable = parent.getVars().getVariable(name);
            if (variable == null && name.startsWith("variable.") && !parent.getVars().containsVariable(name)
                    && parent.getVars().hasStructureFields(name)) {
                vars.shareStructureField(name, parent.getVars());
                continue;
            }
            if ((variable == null && parent.getVars().hasSharedStructureField(name)
                    || parent.getVars().hasStructuredDefault(name))
                    && parent.getVars().containsVariable(name)) {
                vars.shareStructureField(name, parent.getVars());
                continue;
            }
            int fieldSeparator = name.indexOf('.', "variable.".length());
            FloatMolangExp defaultValue = shared.defaultValue();
            if (variable == null && name.startsWith("variable.") && fieldSeparator >= 0) {
                String rootName = name.substring(0, fieldSeparator);
                if (parent.getVars().containsVariable(rootName)) {
                    Object root = parent.getVars().getRawValue(rootName, parent);
                    if (root instanceof com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.MolangStruct || root instanceof Map<?, ?>) {
                        if (!parent.getVars().containsVariable(name)) {
                            if (defaultValue != null && !defaultValue.getExpStr().isBlank()) {
                                FloatMolangExp expression = new FloatMolangExp(defaultValue.getConstant(), defaultValue.getExpStr());
                                expression.compile(new MolangParser(vars, parent.getPreset().molangVersion,
                                        parent.getPreset().bedrockItemNames));
                                parent.getVars().installStructuredDefault(name, new Variable(name, expression::calculate));
                            } else {
                                parent.getVars().setRawValue(name, defaultValue == null ? 0.0 : defaultValue.getConstant(), parent);
                            }
                        }
                        vars.shareStructureField(name, parent.getVars());
                        continue;
                    }
                }
            }
            if (variable == null) {
                if (defaultValue == null) {
                    variable = new Variable(name, 0.0);
                } else {
                    FloatMolangExp expression = new FloatMolangExp(defaultValue.getConstant(), defaultValue.getExpStr());
                    expression.compile(new MolangParser(vars, parent.getPreset().molangVersion,
                            parent.getPreset().bedrockItemNames));
                    variable = new Variable(name, expression::calculate);
                }
                parent.getVars().table.put(name, variable);
            } else if (parent.getVars().table.get(name) == null
                    && variable == parent.getPreset().vars.getVariable(name)) {
                variable = variable.copy();
                parent.getVars().table.put(name, variable);
            }
            vars.table.put(name, variable);
            vars.shareStructureField(name, parent.getVars());
        }
        createComponents();
        if (!deferred) {
            prepareMolangVariables();
            initVars();
        }
    }

    private void inheritEventBinding(ParticleEmitter source) {
        this.attached = source.getAttached();
        this.itemOwner = source.itemOwner;
        this.itemVariables = source.itemVariables;
        this.offsetRot = source.offsetRot;
        this.parentPosition = source.parentPosition;
        this.parentRotation = source.parentRotation;
        this.parentMode = source.parentMode;
        this.parentSpace = source.parentSpace;
        this.offsetPos = source.offsetPos;
        if (boundEventWorldOffset.lengthSqr() == 0.0) {
            this.boundEventOffset = source.boundEventOffset;
        } else {
            Vector3f delta = boundEventWorldOffset.toVector3f();
            if (source.parentSpace != null) {
                if (source.parentSpace.determinant() == 0.0F) {
                    throw new IllegalStateException("Cannot bind particle event to a singular locator transform: " + particleId);
                }
                source.parentSpace.invert(new Matrix4x3f()).transformDirection(delta);
                this.boundEventOffset = source.boundEventOffset.add(delta.x, delta.y, delta.z);
            } else {
                delta.rotateX(-source.rot.x).rotateY(-source.rot.y).rotateZ(-source.rot.z);
                this.offsetPos = source.offsetPos.add(delta.x, delta.y, delta.z);
            }
        }
    }

    private Vector3f localEmitterPosition(Matrix4x3f space) {
        return space.transformPosition(boundEventOffset.toVector3f());
    }

    public void attachEntity(@Nullable Entity entity) {
        attach(entity == null ? null : IEntity.of(entity));
    }

    public void attachBlock(@Nullable BlockEntity blockEntity) {
        attach(blockEntity == null ? null : IBlockEntity.of(blockEntity));
    }

    public void attach(@Nullable ParticleEmitterAttachable attachable) {
        if (deferredEffectInitialization && deferredPreEffectPending && !installingDeferredBinding) {
            attachmentModifiedDuringInitialization = true;
            itemOwnerModifiedDuringInitialization = true;
        }
        long revision = ++attachmentRevision;
        attachmentDepth++;
        try {
            ParticleEmitter source = inheritedAttachmentSource;
            if (source != null && inheritedAttachmentTarget == attachable) {
                this.itemOwner = source.itemOwner;
                this.itemVariables = source.itemVariables;
            } else {
                if (attachable == null || this.attached != attachable) this.itemOwner = null;
                this.itemVariables = null;
            }
            this.attached = attachable;
            updateAttachmentScope();
            if (attachmentRevision == revision) propagateAttachment(attachable, revision);
        } finally {
            attachmentDepth--;
        }
    }

    private void updateAttachmentScope() {
        long revision = attachmentRevision;
        long parentRevision = vars.parentRevision();
        VariableTable source = attached == null ? itemVariables : attached.getVariableTable();
        if (attachmentRevision != revision || vars.parentRevision() != parentRevision) return;
        if (installingDeferredBinding && preserveDeferredVariableParent && attachmentDepth <= 1) return;
        vars.setParent(source == null ? preset.vars : new AttachedVariableTable(source, preset.vars));
    }

    private void inheritAttachment(ParticleEmitter source, @Nullable ParticleEmitterAttachable target) {
        ParticleEmitter previous = inheritedAttachmentSource;
        ParticleEmitterAttachable previousTarget = inheritedAttachmentTarget;
        inheritedAttachmentSource = source;
        inheritedAttachmentTarget = target;
        try {
            attach(target);
        } finally {
            inheritedAttachmentSource = previous;
            inheritedAttachmentTarget = previousTarget;
        }
    }

    private void propagateAttachment(@Nullable ParticleEmitterAttachable target, long revision) {
        for (ParticleEmitter child : List.copyOf(children)) {
            if (attachmentRevision != revision) return;
            if (child.boundEventChild) child.inheritAttachment(this, target);
        }
    }

    public @Nullable BlockEntity getAttachedBlock() {
        return attached instanceof BlockEntity blockEntity ? blockEntity : null;
    }

    @Override
    public @Nullable ParticleEmitterAttachable getAttached() {
        return attached;
    }

    @ApiStatus.Internal
    public void setItemOwner(@Nullable RenderItemContext.Owner owner) {
        if (deferredEffectInitialization && deferredPreEffectPending && !installingDeferredBinding) {
            itemOwnerModifiedDuringInitialization = true;
        }
        this.itemOwner = owner;
        for (ParticleEmitter child : List.copyOf(children)) {
            if (child.boundEventChild) child.setItemOwner(owner);
        }
    }

    @ApiStatus.Internal
    public @Nullable RenderItemContext.Owner getItemOwner() {
        return itemOwner;
    }

    protected void init() {
        createVars();
        if (attached != null) attach(attached);
        else if (itemVariables != null) updateAttachmentScope();
        if (restoredRandoms) publishRandomVariables();
        if (!deferredEffectInitialization) initVars();
        createComponents();
    }

    @ApiStatus.Internal
    public void applyDeferredPreEffect() {
        if (!deferredPreEffectPending) throw new IllegalStateException("Particle pre-effect already applied");
        deferredEffectInitialization = true;
        try {
            completeDeferredBinding();
            deferredPreEffectPending = false;
            if (prepareMolangVariablesIfBound()) initVars();
        } finally {
            deferredPreEffectPending = false;
            deferredEffectInitialization = false;
            deferredAttachment = null;
            deferredItemVariables = null;
            deferredItemOwner = null;
            deferredEventBindingSource = null;
        }
    }

    private void completeDeferredBinding() {
        ParticleEmitter source = deferredEventBindingSource;
        ParticleEmitterAttachable target = source == null ? deferredAttachment : source.getAttached();
        VariableTable itemScope = source == null ? deferredItemVariables : source.itemVariables;
        RenderItemContext.Owner owner = source == null ? deferredItemOwner : source.itemOwner;
        boolean preserveAttachment = attachmentModifiedDuringInitialization || attached != null;
        boolean preserveOwner = itemOwnerModifiedDuringInitialization || itemOwner != null;
        if (preserveAttachment) {
            if (!preserveOwner) itemOwner = owner;
            return;
        }
        if (target == null && itemScope == null && (owner == null || preserveOwner)) return;

        preserveDeferredVariableParent = vars.parentRevision() != deferredParentRevision;
        attached = target;
        itemVariables = itemScope;
        if (!preserveOwner) itemOwner = owner;
        installingDeferredBinding = true;
        try {
            if (attached != null) attach(attached);
            else if (itemVariables != null) updateAttachmentScope();
        } finally {
            installingDeferredBinding = false;
            preserveDeferredVariableParent = false;
        }
    }

    @ApiStatus.Internal
    public boolean hasDeferredPreEffect() {
        return deferredPreEffectPending;
    }

    @ApiStatus.Internal
    public boolean isInitializingDeferredEffect() {
        return deferredEffectInitialization;
    }

    @ApiStatus.Internal
    public boolean isInitializingDeferredComponents() {
        return deferredEffectInitialization && deferredPreEffectPending;
    }

    @ApiStatus.Internal
    public final void abortInitialization() {
        removed = true;
        if (this instanceof IgnoreRangeParticleEmitter ranged) {
            EmitterAttachHandler.ignoreRangeEmitters.removeIf(value -> value == ranged);
        }
        if (manualEventSource != null && manualEventSource.manualEventChildren != null) {
            manualEventSource.manualEventChildren.remove(manualEventKey, this);
        }
        if (parent != null) parent.children.removeIf(child -> child == this);
    }

    @ApiStatus.Internal
    public final void recordRegistration() {
        registrationGeneration++;
    }

    @ApiStatus.Internal
    public final long registrationGeneration() {
        return registrationGeneration;
    }

    protected void createVars() {
        ResourceLocation requestedId = particleId;
        ResourceLocation resolvedId = MolangParticleEngine.INSTANCE.resolveParticleId(particleId);
        if (resolvedId != null) {
            this.particleId = resolvedId;
        }
        this.preset = getEmitterPreset();
        if (preset == null) {
            throw new IllegalArgumentException("Unknown particle id: '" + particleId + "'!");
        }
        this.vars = new VariableTable(preset.vars);
        if (deferredEffectInitialization) deferredParentRevision = vars.parentRevision();
        PSDiagnostics.infoFirstN("emitter-vars:" + particleId, 32, "emitter vars requested={} resolved={} expression={} presetComponents={} eventKeys={}",
                requestedId,
                particleId,
                expression == null ? "" : expression.getExpStr(),
                preset.components.stream().map(component -> component.getClass().getSimpleName()).toList(),
                preset.events.keySet()
        );
    }

    protected void initVars() {
        if (expression != null) {
            if (expression.getVariable() == null && !expression.getExpStr().isBlank()) {
                expression.compile(new MolangParser(vars, expressionVersion == null ? preset.molangVersion : expressionVersion,
                        expressionBedrockItemNames));
            }
            expression.calculate(this);
        }
    }

    protected void createComponents() {
        for (IEmitterComponent component : preset.components) {
            if (component instanceof EmitterLifetime || component instanceof EmitterRate) component.apply(this);
        }
        if (!restoredRandoms) updateRandoms(level.getRandom());
        restoredRandoms = false;
        for (IEmitterComponent component : preset.components) {
            if (!(component instanceof EmitterLifetime) && !(component instanceof EmitterRate)) component.apply(this);
        }
        this.components = preset.components.stream().filter(IEmitterComponent::requireUpdate).toList();
        PSDiagnostics.infoFirstN("emitter-components:" + particleId, 32, "emitter ready runtimeId={} particle={} active={} lifetime={} rateType={} spawnRate={} particleGroup={} localPosition={} localRotation={} localVelocity={} updateComponents={}",
                id,
                particleId,
                active,
                lifetime,
                preset.emitterRateType,
                spawnRate,
                particleGroup,
                preset.localPosition,
                preset.localRotation,
                preset.localVelocity,
                components.stream().map(component -> component.getClass().getSimpleName()).toList()
        );
    }

    public synchronized void updateRandoms(RandomSource random) {
        this.emitterRandom1 = random.nextFloat();
        this.emitterRandom2 = random.nextFloat();
        this.emitterRandom3 = random.nextFloat();
        this.emitterRandom4 = random.nextFloat();
        publishRandomVariables();
    }

    private void publishRandomVariables() {
        if (vars != null) {
            vars.setValue("variable.emitter_random_1", emitterRandom1);
            vars.setValue("variable.emitter_random_2", emitterRandom2);
            vars.setValue("variable.emitter_random_3", emitterRandom3);
            vars.setValue("variable.emitter_random_4", emitterRandom4);
        }
    }

    public void tick() {
        MolangParticleManager manager = MolangParticleEngine.managerFor(this);
        long deltaNanos = manager == null
                ? ParticleSimulationClock.nanos(ParticleSimulationClock.nominalTickSeconds(level)) : manager.updateDeltaNanos();
        if (!simulationTime.beginFrame(deltaNanos)) return;
        deltaNanos = simulationTime.updateDeltaNanos();
        this.invTickRate = ParticleSimulationClock.seconds(deltaNanos);
        this.posO = pos;

        boolean bindingDiscarded = isBindingDiscarded();
        if (bindingDiscarded) {
            expire();
            if (preset.localPosition && manager != null) manager.discardLocalParticles(this);
        }
        if (!bindingDiscarded && attached instanceof Entity entity) {
            this.pos = entityFramePosition(entity);
        } else if (!bindingDiscarded && (attached != null || itemOwner != null)) {
            Vector3f rotated;
            if (isLocalSpace()) {
                rotated = localEmitterPosition(parentSpace);
            } else {
                if (parentRotation != null) {
                    rot.set(parentRotation).add(offsetRot);
                }
                rotated = offsetPos.toVector3f().rotateZ(rot.z).rotateY(rot.y).rotateX(rot.x);
                if (parentPosition != null) {
                    rotated.add(parentPosition);
                }
            }
            Vec3 base = itemOwner != null ? itemOwner.position() : attached.getPos();
            this.pos = base.add(rotated.x, rotated.y, rotated.z);
        }

        prepareMolangVariables();
        EmitterLifetime.Expression expressionLifetime = null;
        boolean hasLoopingLifetime = false;
        boolean canEmit = true;
        for (IEmitterComponent component : components) {
            if (component instanceof EmitterLifetime value) {
                if (!(value instanceof EmitterLifetime.Once)) hasLoopingLifetime = true;
                if (value instanceof EmitterLifetime.Expression expression) {
                    expressionLifetime = expression;
                    if (canEmit && expression.getActivationExpression().calculate(this) == 0.0F) canEmit = false;
                }
            }
        }
        canEmit &= isInActiveLoopTime() || firstUpdate;
        for (IEmitterComponent component : components) {
            if (component instanceof EmitterInitialization) component.update(this);
        }
        if (!expired && hasExceededLoopTime()) {
            for (IEmitterComponent component : preset.components) {
                if (component instanceof EmitterLifetime) component.apply(this);
            }
            if (attached == null && itemOwner == null || isBindingDiscarded() || !hasLoopingLifetime) expire();
            for (IEmitterComponent component : preset.components) {
                if (!(component instanceof EmitterInitialization) && !(component instanceof EmitterLifetimeEvents)
                        && !(component instanceof EmitterLifetime)) component.apply(this);
            }
            updateRandoms(level.getRandom());
            resetLoopTime();
            lastTimeline = 0;
        }
        if (!expired && expressionLifetime != null
                && expressionLifetime.getExpirationExpression().calculate(this) != 0.0F) expire();
        active = canEmit && (!expired || firstUpdate);

        boolean manualRequestsProcessed = false;
        boolean instantSpawnedAtStart = spawned;
        boolean ordinaryShapeSpawned = false;
        long combinedSpawnRate = 0;
        for (IEmitterComponent component : components) {
            if (component instanceof EmitterLifetime || component instanceof EmitterLifetimeEvents
                    || component instanceof EmitterInitialization) continue;
            if (manualEventSource != null && (component instanceof EmitterShape || component instanceof EmitterMultiShape)) {
                if (!manualRequestsProcessed) {
                    processManualEvents();
                    manualRequestsProcessed = true;
                }
                continue;
            }
            if (active) {
                boolean spawnedBeforeComponent = spawned;
                if (component instanceof EmitterMultiShape && preset.hasInstantRate && !preset.hasSteadyRate
                        && !instantSpawnedAtStart && ordinaryShapeSpawned && spawned) {
                    spawned = false;
                    try {
                        component.update(this);
                    } finally {
                        spawned = true;
                    }
                } else {
                    component.update(this);
                }
                if (component instanceof EmitterShape && !spawnedBeforeComponent && spawned) {
                    ordinaryShapeSpawned = true;
                }
                if (component instanceof EmitterRate) {
                    combinedSpawnRate = Math.min(Integer.MAX_VALUE, combinedSpawnRate + Math.max(0, spawnRate));
                    spawnRate = (int) combinedSpawnRate;
                }
            }
        }
        if (manualEventSource != null && !manualRequestsProcessed) processManualEvents();
        if (firstUpdate) movementVelocity.zero();
        else {
            float dx = (float) pos.x - (float) previousMovementPosition.x;
            float dy = (float) pos.y - (float) previousMovementPosition.y;
            float dz = (float) pos.z - (float) previousMovementPosition.z;
            moveDist += (float) Math.sqrt(dz * dz + dy * dy + dx * dx);
            if (invTickRate > Math.ulp(1.0F)) {
                float inverse = 1.0F / invTickRate;
                movementVelocity.set(dx * inverse, dy * inverse, dz * inverse);
            }
        }
        previousMovementPosition = pos;
        if (preset.lifetimeEvents != null) {
            if (firstUpdate) preset.lifetimeEvents.onCreation(this);
            if (canEmit) preset.lifetimeEvents.update(this);
        }
        firstUpdate = false;
        advanceLoopCounter();
        this.moveDistO = moveDist;
        simulationTime.finishStep();

        if (afterParentInit != null && parent != null) {
            afterParentInit.run();
            this.afterParentInit = null;
        }

        if (parent != null && parent.isRemoved()) {
            if (independentEventChild) parent = null;
            else remove();
        }
        if (manualEventSource != null && manualEventSource.isRemoved()
                && manualEventSource.activeParticleCount == 0 && activeParticleCount == 0) {
            remove();
        }
        if (isManual && manualEventSource == null) {
            expire();
        }
    }

    private Vec3 entityFramePosition(Entity entity) {
        Vector3f offset;
        if (isLocalSpace()) {
            offset = localEmitterPosition(parentSpace);
        } else {
            if (parentRotation != null) {
                rot.set(parentRotation).add(offsetRot.x, offsetRot.y + getAttachedYRot(entity) * Mth.DEG_TO_RAD, offsetRot.z);
            }
            offset = offsetPos.toVector3f().rotateZ(rot.z).rotateY(rot.y).rotateX(rot.x);
            if (parentPosition != null) offset.add(parentPosition);
        }
        Vec3 entityPosition = entity.getPosition(RenderFrameContext.framePartialTick());
        Vec3 base = itemOwner == null ? entityPosition
                : itemOwner.position().add(entityPosition.subtract(entity.position()));
        return base.add(offset.x, offset.y, offset.z);
    }

    private float particleBodyRotation(boolean rotateBody) {
        Entity entity = getAttachedEntity();
        float yaw = 0.0F;
        if (rotateBody && entity != null) {
            float partialTick = RenderFrameContext.framePartialTick();
            yaw = entity instanceof LivingEntity living
                    ? Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot)
                    : Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        } else if (rotateBody && itemOwner != null) {
            yaw = itemOwner.getVisualRotationYInDegrees();
        }
        return Mth.wrapDegrees(180.0F - yaw) * Mth.DEG_TO_RAD;
    }

    private float getAttachedYRot(Entity entity) {
        return entity instanceof LivingEntity living ? -living.yBodyRot : entity.getYRot();
    }

    public void prepareMolangVariables() {
        prepareMolangVariablesIfBound();
    }

    private boolean prepareMolangVariablesIfBound() {
        if (isBindingDiscarded()) return false;
        cameraDistance = MolangParticleEngine.sampleCameraDistance(this, previousMovementPosition);
        vars.setValue("variable.emitter_lifetime", tickLifetime());
        vars.setValue("variable.emitter_age", tickAge());
        Entity entity = getAttachedEntity();
        float scale = entity instanceof LivingEntity living ? living.getScale() : 1.0F;
        vars.setValue("variable.entity_scale", scale);
        return true;
    }

    void prepareRenderVariables(PerFrameParticleExpressions owner, long pass) {
        if (preparedRenderOwner == owner && preparedRenderPass == pass) return;
        prepareMolangVariables();
        if (!hasInvalidLocalFrame()) simulationTime.beginRenderFrame();
        preparedRenderOwner = owner;
        preparedRenderPass = pass;
    }

    public boolean matchesExpirationBlock(Set<BlockState> states, BlockPos position) {
        if (expirationBlocks == null) expirationBlocks = new IdentityHashMap<>();
        return expirationBlocks.computeIfAbsent(states, Set::copyOf).contains(level.getBlockState(position));
    }

    public Vec3 getEmissionOrigin() {
        if (getAttachedEntity() != null) return offsetPos;
        return itemOwner != null && isParticleSimulationWorldSpace() ? Vec3.ZERO : pos;
    }

    public org.joml.Vector3fc getMovementVelocity() {
        return movementVelocity;
    }

    @ApiStatus.Internal
    public boolean isManualEventEmitter() {
        return manualEventSource != null;
    }

    @ApiStatus.Internal
    public @Nullable Vec3 getManualEmissionPosition() {
        return manualEmissionPosition;
    }

    public boolean hasLocalParticlePosition() {
        return preset.localPosition && (getAttachedEntity() != null || itemOwner != null || isLocalSpace());
    }

    public void transformParticleBirth(Vector3f position, Vector3f velocity) {
        if (getAttachedEntity() != null) {
            if (!preset.localPosition) {
                if (isLocalSpace()) {
                    position.mulDirection(parentSpace);
                    velocity.mulDirection(parentSpace);
                } else {
                    float rotation = particleBodyRotation(true);
                    position.rotateY(rotation);
                    velocity.rotateY(rotation);
                }
            } else if (!preset.localRotation) {
                velocity.rotateY(particleBodyRotation(true));
            }
        } else if (itemOwner != null && (!preset.localPosition || !preset.localRotation)) {
            if (isLocalSpace()) {
                position.mulDirection(parentSpace);
                velocity.mulDirection(parentSpace);
            } else {
                float rotation = particleBodyRotation(true);
                position.rotateY(rotation);
                velocity.rotateY(rotation);
            }
        } else if (isLocalSpace() && !preset.localRotation) {
            position.mulDirection(parentSpace);
            velocity.mulDirection(parentSpace);
        }
    }

    public boolean isParticleSimulationWorldSpace() {
        return (getAttachedEntity() != null || itemOwner != null) && !preset.localPosition;
    }

    public Vector3f toParticleSimulationSpace(double x, double y, double z, Vector3f result) {
        if (!isParticleSimulationWorldSpace() && !hasLocalParticlePosition()) {
            Vec3 origin = getPosition();
            return result.set((float) (x - origin.x), (float) (y - origin.y), (float) (z - origin.z));
        }
        return result.set((float) x, (float) y, (float) z);
    }

    public Vec3 fromParticleSimulationSpace(Vector3f position) {
        if (!isParticleSimulationWorldSpace() && !hasLocalParticlePosition()) {
            Vec3 origin = getPosition();
            return new Vec3(origin.x + position.x, origin.y + position.y, origin.z + position.z);
        }
        return new Vec3(position.x, position.y, position.z);
    }

    public boolean hasEffectFrame() {
        return hasEffectFrame;
    }

    public void setEffectFrame(org.joml.Matrix3fc frame) {
        effectFrame.set(frame);
        hasEffectFrame = frame.m00() != 1.0F || frame.m11() != 1.0F || frame.m22() != 1.0F
                || frame.m01() != 0.0F || frame.m02() != 0.0F || frame.m10() != 0.0F
                || frame.m12() != 0.0F || frame.m20() != 0.0F || frame.m21() != 0.0F;
    }

    public void local2World(Vector3f vec, float partialTick) {
        if (!isParticleSimulationWorldSpace() && !hasLocalParticlePosition()) {
            if (!hasEffectFrame && !(isLocalSpace() && preset.localRotation)) return;
            vec.sub((float) pos.x, (float) pos.y, (float) pos.z);
        }
        simulationToWorld(vec, partialTick);
    }

    public void simulationToWorld(Vector3f vec, float partialTick) {
        vec.mul(effectFrame);
        Entity entity = getAttachedEntity();
        if (entity != null) {
            if (hasLocalParticlePosition()) {
                if (isLocalSpace()) vec.mulDirection(parentSpace);
                else vec.rotateY(particleBodyRotation(preset.localRotation));
                Vec3 origin = entityFramePosition(entity);
                vec.add((float) origin.x, (float) origin.y, (float) origin.z);
            } else {
                vec.add((float) offsetPos.x, (float) offsetPos.y, (float) offsetPos.z);
            }
        } else {
            boolean worldSpace = isParticleSimulationWorldSpace();
            if (isLocalSpace() && preset.localRotation && !worldSpace) vec.mulDirection(parentSpace);
            else if (itemOwner != null && !isLocalSpace() && preset.localRotation && hasLocalParticlePosition()) {
                vec.rotateY(particleBodyRotation(true));
            }
            if (hasLocalParticlePosition()) {
                vec.add(
                        (float) Mth.lerp((double) partialTick, posO.x, pos.x),
                        (float) Mth.lerp((double) partialTick, posO.y, pos.y),
                        (float) Mth.lerp((double) partialTick, posO.z, pos.z));
            } else if (!worldSpace) {
                vec.add((float) pos.x, (float) pos.y, (float) pos.z);
            }
        }
    }

    public boolean isLocalSpace() {
        return parentSpace != null;
    }

    public org.joml.Matrix3f getParticleRenderTransform(org.joml.Matrix3f destination) {
        Entity entity = getAttachedEntity();
        if (entity != null && hasLocalParticlePosition()) {
            if (isLocalSpace()) destination.set(parentSpace);
            else destination.rotationY(particleBodyRotation(preset.localRotation));
            return destination.mul(effectFrame);
        }
        if (entity == null && preset != null && preset.localRotation) {
            if (isLocalSpace() && !isParticleSimulationWorldSpace()) return destination.set(parentSpace).mul(effectFrame);
            if (itemOwner != null && hasLocalParticlePosition()) {
                return destination.rotationY(particleBodyRotation(true)).mul(effectFrame);
            }
        }
        return destination.set(effectFrame);
    }

    public org.joml.Matrix3f getParticleRenderBasis(org.joml.Matrix3f destination) {
        getParticleRenderTransform(destination);
        float xScale = 1.0F / (float) Math.sqrt(destination.m00() * destination.m00()
                + destination.m01() * destination.m01() + destination.m02() * destination.m02());
        float yScale = 1.0F / (float) Math.sqrt(destination.m10() * destination.m10()
                + destination.m11() * destination.m11() + destination.m12() * destination.m12());
        float zScale = 1.0F / (float) Math.sqrt(destination.m20() * destination.m20()
                + destination.m21() * destination.m21() + destination.m22() * destination.m22());
        return destination.set(
                destination.m00() * xScale, destination.m01() * xScale, destination.m02() * xScale,
                destination.m10() * yScale, destination.m11() * yScale, destination.m12() * yScale,
                destination.m20() * zScale, destination.m21() * zScale, destination.m22() * zScale);
    }

    public @Nullable Quaternionf getLocalSpaceRotation() {
        boolean attachedRotation = getAttachedEntity() != null && hasLocalParticlePosition()
                || getAttachedEntity() == null && preset != null && preset.localRotation
                && (isLocalSpace() && !isParticleSimulationWorldSpace() || itemOwner != null && hasLocalParticlePosition());
        return hasEffectFrame || attachedRotation
                ? getParticleRenderBasis(new org.joml.Matrix3f()).getNormalizedRotation(new Quaternionf()) : null;
    }

    public final Matrix4x3f getLocalSpace() {
        return parentSpace;
    }

    public final void setLocalSpace(@Nullable Matrix4x3f space) {
        setLocalSpace(space, true);
    }

    public final void setLocalSpace(@Nullable Matrix4x3f space, boolean updatePos) {
        this.parentSpace = space;
        if (updatePos && space != null) {
            ParticleEmitterAttachable attachable = getAttached();
            Vec3 base = itemOwner != null ? itemOwner.position() : attachable != null ? attachable.getPos() : pos;
            Vector3f local = localEmitterPosition(space);
            this.pos = base.add(local.x, local.y, local.z);
        }
        for (ParticleEmitter child : List.copyOf(children)) {
            if (child.boundEventChild) child.setLocalSpace(space, updatePos);
        }
    }

    @ApiStatus.Internal
    public void setLocatorPosition(Vec3 position) {
        setLocatorPosition(position, true);
    }

    @ApiStatus.Internal
    public void setLocatorPosition(Vec3 position, boolean notifyPositionChange) {
        if (notifyPositionChange) setPos(position);
        else this.pos = position;
        for (ParticleEmitter child : List.copyOf(children)) {
            if (!child.boundEventChild) continue;
            Vec3 offset = child.boundEventOffset.subtract(boundEventOffset);
            Vector3f delta = offset.toVector3f();
            if (child.parentSpace != null) child.parentSpace.transformDirection(delta);
            child.setLocatorPosition(this.pos.add(delta.x, delta.y, delta.z), false);
        }
    }

    public void remove() {
        this.removed = true;
    }

    public void expire() {
        if (expired || expirationEventFired) return;
        expirationEventFired = true;
        try {
            if (preset != null && preset.lifetimeEvents != null) preset.lifetimeEvents.onExpiration(this);
        } finally {
            expired = true;
        }
    }

    public boolean isExpired() {
        return expired;
    }

    public boolean isReadyForRemoval() {
        return removed || !isBindingDiscarded() && isRemoved()
                || expired && activeParticleCount == 0 && pendingParticleCount == 0;
    }

    public void onRemove() {
        if (removalCallbacksRun) return;
        this.removalCallbacksRun = true;
        remove();
        if (manualEventSource != null && manualEventSource.manualEventChildren != null) {
            manualEventSource.manualEventChildren.remove(manualEventKey, this);
        }
        children.removeIf(child -> {
            if (child.independentEventChild) {
                if (!child.hasScreenOwnership() && child instanceof IgnoreRangeParticleEmitter ranged
                        && !EmitterAttachHandler.ignoreRangeEmitters.contains(ranged)) {
                    EmitterAttachHandler.ignoreRangeEmitters.add(ranged);
                }
            } else {
                child.parent = null;
                child.remove();
            }
            return !child.boundEventChild;
        });
        expire();
    }

    public void addParent(ParticleEmitter parent) {
        this.screenOwned |= parent.hasScreenOwnership();
        parent.children.add(this);
        this.parent = parent;
        if (parent.isRemoved() && independentEventChild && !screenOwned && this instanceof IgnoreRangeParticleEmitter ranged
                && !EmitterAttachHandler.ignoreRangeEmitters.contains(ranged)) {
            EmitterAttachHandler.ignoreRangeEmitters.add(ranged);
        }
    }

    /** Keeps locator updates alive while event-bound descendants still need this transform. */
    public boolean hasLiveBoundEventChildren() {
        children.removeIf(child -> child.boundEventChild && child.isRemoved()
                && !child.hasLiveLocalParticles() && !child.hasLiveBoundEventChildren());
        for (ParticleEmitter child : children) {
            if (child.boundEventChild) return true;
        }
        return false;
    }

    public boolean hasLiveLocalParticles() {
        return !isBindingDiscarded() && preset.localPosition && activeParticleCount > 0;
    }

    private record SharedVarKey(String name, @Nullable Float constant, @Nullable String expression) {
        private static SharedVarKey of(ParticleEffect.SharedVar shared) {
            FloatMolangExp value = shared.defaultValue();
            return value == null ? new SharedVarKey(shared.name(), null, null)
                    : new SharedVarKey(shared.name(), value.getConstant(), value.getExpStr());
        }
    }
    private record ManualEventKey(ResourceLocation effect, List<?> sharedVars) {}
    private record ManualEventRequest(ParticleEffect effect, Vec3 position, @Nullable Vector3f velocity,
                                      boolean expressionAlreadyApplied) {}

    private ManualEventKey manualEventKey(ParticleEffect effect) {
        List<?> sharedVars = usesDefaultEventFactory()
                ? effect.sharedVars().stream().map(SharedVarKey::of).toList()
                : List.copyOf(effect.sharedVars());
        return new ManualEventKey(manualEventIdentity(effect), sharedVars);
    }

    private boolean usesDefaultEventFactory() {
        return TYPE.equals(type) && !com.kltyton.bedrockruntime.client.particle.api.RegisterCustomEmitterTypeEvent.hasEffectFactory(type);
    }

    @ApiStatus.Internal
    protected ResourceLocation manualEventIdentity(ParticleEffect effect) {
        MolangParticleEngine engine = MolangParticleEngine.INSTANCE;
        ResourceLocation resolved = engine.resolveParticleId(effect.effect());
        if (resolved == null) return effect.effect();
        if (!usesDefaultEventFactory()) return resolved;
        var definition = engine.id2Effect().get(resolved);
        if (definition == null) return resolved;
        ResourceLocation declared = definition.description.identifier();
        return engine.id2Effect().get(declared) == definition ? declared : resolved;
    }

    @ApiStatus.Internal
    public @Nullable ParticleEmitter findManualEventEmitter(ParticleEffect effect) {
        if (manualEventChildren == null) return null;
        ManualEventKey key = manualEventKey(effect);
        ParticleEmitter emitter = manualEventChildren.get(key);
        if (emitter != null && emitter.isRemoved()) {
            manualEventChildren.remove(key, emitter);
            return null;
        }
        return emitter;
    }

    @ApiStatus.Internal
    public void registerManualEventEmitter(ParticleEffect effect, ParticleEmitter emitter) {
        if (manualEventChildren == null) manualEventChildren = new HashMap<>();
        ManualEventKey key = manualEventKey(effect);
        manualEventChildren.put(key, emitter);
        emitter.manualEventSource = this;
        emitter.manualEventKey = key;
    }

    @ApiStatus.Internal
    public void queueManualEvent(ParticleEffect effect, Vec3 position, @Nullable Vector3f velocity,
                                 boolean expressionAlreadyApplied) {
        if (manualEventRequests == null) manualEventRequests = new ArrayDeque<>();
        manualEventRequests.add(new ManualEventRequest(effect, position,
                velocity == null ? null : new Vector3f(velocity), expressionAlreadyApplied));
    }

    private void processManualEvents() {
        if (manualEventRequests == null) return;
        ManualEventRequest request;
        while ((request = manualEventRequests.poll()) != null) {
            emitManualEvent(request.effect(), request.position(), request.velocity(), request.expressionAlreadyApplied());
        }
    }

    @ApiStatus.Internal
    public void emitManualEvent(ParticleEffect effect, Vec3 position, @Nullable Vector3f velocity,
                                 boolean expressionAlreadyApplied) {
        Vec3 previousEmissionPosition = manualEmissionPosition;
        Vector3f previousVelocity = inheritedParticleSpeed;
        boolean previousManual = isManual;
        manualEmissionPosition = position;
        inheritedParticleSpeed = velocity == null ? null : new Vector3f(velocity);
        isManual = true;
        try {
            prepareMolangVariables();
            if (!expressionAlreadyApplied) {
                MolangExp preEffect = effect.preEffectExpression();
                if (preEffect.getVariable() == null && !preEffect.getExpStr().isBlank()) {
                    preEffect.compile(new MolangParser(vars, manualEventSource.getPreset().molangVersion,
                            manualEventSource.getPreset().bedrockItemNames));
                }
                preEffect.calculate(this);
            }
            if (!expired && !isRemoved()) {
                prepareMolangVariables();
                if (manualRate == null) return;
                for (IEmitterComponent component : components) {
                    if (component instanceof EmitterShape || component instanceof EmitterMultiShape) {
                        component.update(this);
                    }
                }
            }
        } finally {
            isManual = previousManual;
            inheritedParticleSpeed = previousVelocity;
            manualEmissionPosition = previousEmissionPosition;
        }
    }

    @ApiStatus.Internal
    public boolean consumeEventSpawnContext(ParticleEffect.SpawnContext context) {
        boolean initialized = eventSpawnContext == context;
        eventSpawnContext = null;
        return initialized;
    }

    public boolean hasScreenOwnership() {
        boolean screen = false;
        ParticleEmitter current = this;
        ParticleEmitter fast = this;
        while (current != null) {
            screen |= current.screenOwned || current instanceof com.kltyton.bedrockruntime.client.particle.runtime.screen.ScreenParticleEmitter;
            current = current.parent;
            fast = fast == null || fast.parent == null ? null : fast.parent.parent;
            if (current != null && current == fast) {
                throw new IllegalStateException("Emitter parent chain contains a cycle for " + particleId);
            }
        }
        return screen;
    }

    public boolean isBindingDiscarded() {
        ParticleEmitterAttachable attachable = getAttached();
        return (attachable != null && attachable.isDiscarded()) || (itemOwner != null && itemOwner.level() != level);
    }

    boolean hasInvalidLocalFrame() {
        return preset.localPosition && isBindingDiscarded();
    }

    public boolean isRemoved() {
        ParticleEmitterAttachable attachable = getAttached();
        return removed || (attachable != null && attachable.isDiscarded()) || (itemOwner != null && itemOwner.level() != level);
    }

    /// Whether this emitter may spawn another particle. Without a particle group it is unlimited; otherwise bounded by the living particle count of this emitter only.
    public boolean hasSpace() {
        return particleGroup == null || activeParticleCount + pendingParticleCount < particleGroup.getLimit();
    }


    @ApiStatus.Internal
    public int reserveParticleSlot() {
        return pendingParticleCount++;
    }

    @ApiStatus.Internal
    public void releaseParticleSlot(int previousPending) {
        if (pendingParticleCount > previousPending) pendingParticleCount--;
    }

    /// Must be called once per particle successfully added to the particle engine for this emitter.
    public void onAdded() {
        if (pendingParticleCount > 0) pendingParticleCount--;
        activeParticleCount++;
    }

    /// Must be called once when a particle owned by this emitter is removed. Clamped to zero to stay safe against redundant removals.
    public void onRemoved() {
        if (activeParticleCount > 0) {
            activeParticleCount--;
        }
    }

    public void setPos(Vec3 pos) {
        this.pos = pos;
    }

    public EmitterPreset getPreset() {
        return preset;
    }

    public @Nullable EmitterPreset getEmitterPreset() {
        return eventPresetOverride != null ? eventPresetOverride : MolangParticleEngine.INSTANCE.id2Emitter().get(particleId);
    }

    public ParticlePreset getParticlePreset() {
        return eventParticlePresetOverride != null ? eventParticlePresetOverride : MolangParticleEngine.INSTANCE.id2Particle().get(particleId);
    }

    public void deserialize(CompoundTag compound) {
        this.hideOutline = compound.getBoolean("hideOutline");
        this.particleId = ResourceLocation.tryParse(compound.getString("particleId"));
        this.expression = new MolangExp(compound.getString("expression"));
        this.emitterRandom1 = compound.getDouble("emitterRandom1");
        this.emitterRandom2 = compound.getDouble("emitterRandom2");
        this.emitterRandom3 = compound.getDouble("emitterRandom3");
        this.emitterRandom4 = compound.getDouble("emitterRandom4");
        this.posO = this.pos = new Vec3(
                compound.getDouble("posX"),
                compound.getDouble("posY"),
                compound.getDouble("posZ")
        );
        this.rot.set(
                compound.getFloat("rotX"),
                compound.getFloat("rotY"),
                compound.getFloat("rotZ")
        );
    }

    public void serialize(CompoundTag compound) {
        compound.putString("particleId", particleId.toString());
        compound.putString("expression", expression.getExpStr());
        compound.putBoolean("hideOutline", hideOutline);
        compound.putDouble("emitterRandom1", emitterRandom1);
        compound.putDouble("emitterRandom2", emitterRandom2);
        compound.putDouble("emitterRandom3", emitterRandom3);
        compound.putDouble("emitterRandom4", emitterRandom4);
        compound.putDouble("posX", pos.x);
        compound.putDouble("posY", pos.y);
        compound.putDouble("posZ", pos.z);
        compound.putFloat("rotX", rot.x);
        compound.putFloat("rotY", rot.y);
        compound.putFloat("rotZ", rot.z);
    }

    public final CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putString(TYPE_KEY, type.toString());
        serialize(tag);
        return tag;
    }

    public double getX() {
        return pos.x;
    }

    public double getY() {
        return pos.y;
    }

    public double getZ() {
        return pos.z;
    }

    public float getCameraDistance() {
        return cameraDistance;
    }

    @Override
    public VariableTable getVars() {
        return vars;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public float tickAge() {
        return simulationTime.ageSeconds();
    }

    @Override
    public float tickLifetime() {
        return simulationTime.activeSeconds();
    }

    @Override
    public double getRandom1() {
        return emitterRandom1;
    }

    @Override
    public double getRandom2() {
        return emitterRandom2;
    }

    @Override
    public double getRandom3() {
        return emitterRandom3;
    }

    @Override
    public double getRandom4() {
        return emitterRandom4;
    }

    @Override
    public ResourceLocation getIdentity() {
        return particleId;
    }

    @Override
    public Vec3 getPosition() {
        return pos;
    }

    @Override
    public @Nullable Entity getAttachedEntity() {
        return attached instanceof Entity entity ? entity : null;
    }

    @Override
    public float getInvTickRate() {
        return invTickRate;
    }

    public float getElapsedTime() { return simulationTime.elapsedSeconds(); }

    public long getElapsedTimeNanos() { return simulationTime.elapsedNanos(); }

    public long getUpdateDeltaNanos() { return simulationTime.updateDeltaNanos(); }

    void advanceRetainedParticleFrame(long deltaNanos) {
        if (simulationTime.beginFrame(deltaNanos, false)) {
            invTickRate = ParticleSimulationClock.seconds(simulationTime.updateDeltaNanos());
        }
    }

    public long getSimulationSequence() { return simulationTime.simulationSequence(); }

    public boolean updatedThisFrame() { return simulationTime.updatedThisFrame(); }

    public float getMotionInterpolation() { return simulationTime.motionInterpolation(); }

    boolean needsAppearanceUpdate() { return simulationTime.appearanceUpdate(); }

    void noteRenderLifetime(long lifetime) { simulationTime.noteRenderLifetime(lifetime); }

    boolean forceWarmupAppearance() { return !hasInvalidLocalFrame() && simulationTime.forceWarmupAppearance(); }

    boolean readyToRender() { return !hasInvalidLocalFrame() && simulationTime.readyToRender(); }

    public float getAppearanceInterpolation() { return simulationTime.appearanceInterpolation(); }

    public long getPreviousElapsedTimeNanos() { return simulationTime.previousElapsedNanos(); }

    public void setLifetimeSeconds(float seconds) { simulationTime.setLifetimeSeconds(seconds); }

    public boolean hasExceededLifetime() { return simulationTime.hasExceededLifetime(); }

    public float getActiveTimeSeconds() { return simulationTime.activeSeconds(); }

    public void setActiveTimeSeconds(float seconds) { simulationTime.setActiveSeconds(seconds); }

    public float getSleepTimeSeconds() { return simulationTime.sleepSeconds(); }

    public void setLoopTimes(float active, float sleep) { simulationTime.setLoopTimes(active, sleep); }

    public boolean hasExceededLoopTime() { return simulationTime.hasExceededLoopTime(); }

    public boolean isInActiveLoopTime() { return simulationTime.isInActiveLoopTime(); }

    public void advanceLoopCounter() { simulationTime.advanceLoopCounter(); }

    public void resetLoopTime() { simulationTime.resetLoop(); }

    @Override
    public ParticleEmitter getEmitter() {
        return this;
    }

    public enum ParentMode {
        LOCATOR,
        WORLD
    }
}