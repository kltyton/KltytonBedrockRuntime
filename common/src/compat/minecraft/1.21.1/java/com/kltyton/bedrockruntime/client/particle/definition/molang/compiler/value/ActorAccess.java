package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.api.ParticleEmitterAttachable;
import com.kltyton.bedrockruntime.client.particle.definition.molang.VariableTable;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangQueries;
import com.kltyton.bedrockruntime.client.particle.access.IEntity;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/** Evaluates the right-hand expression in the referenced entity or item context. */
public record ActorAccess(MathValue actor, MathValue expression) implements MathValue {
    @Override
    public Object getRaw(MolangInstance instance) {
        Object value = actor.getRaw(instance);
        if (value instanceof ItemStack item) return expression.getRaw(new ItemInstance(instance, item));
        if (!(value instanceof Entity entity)) return 0.0;
        ActorInstance target = new ActorInstance(instance, entity);
        if (expression instanceof Variable variable && variable.name().startsWith("variable.")) {
            Object raw = variable.getRawWithStructure(target);
            if (!target.getVars().containsVariable(variable.name()) && !(raw instanceof MolangStruct)) return 0.0;
            return raw;
        }
        return expression.getRaw(target);
    }

    @Override
    public double get(MolangInstance instance) {
        Object value = getRaw(instance);
        return value instanceof Number number ? number.floatValue() : 0.0;
    }

    @Override
    public void markImmutable() {
    }

    public static MolangInstance referenceOwner(MolangInstance instance) {
        while (instance instanceof ItemInstance item) instance = item.source;
        return instance;
    }

    /**
     * Creates an independent actor context for a valid entity or item target.
     * Removed entities and empty items have no Molang actor context.
     */
    public static @Nullable MolangInstance reference(MolangInstance source, Object target) {
        if (target instanceof ItemStack item) {
            return item.isEmpty() ? null : new ItemInstance(source, item);
        }
        if (target instanceof Entity entity) {
            return entity.isRemoved() ? null : new ActorInstance(source, entity);
        }
        return null;
    }

    /** Writes through a registered actor writer and refreshes its current public snapshot. */
    public static boolean writeReference(MolangInstance instance, String name, Object value) {
        if (!(instance instanceof ActorInstance actor)) return false;
        if (!MolangQueries.writePublicActorVariable(actor.entity, name, value)) return false;
        actor.variables.refresh(actor.entity);
        return true;
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public static boolean isActorReference(MolangInstance instance) {
        return instance instanceof ActorInstance;
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public static float actorAnimationTime(MolangInstance instance) {
        return ((ActorInstance) instance).animationTime;
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public static float actorAnimationLifetime(MolangInstance instance) {
        return ((ActorInstance) instance).animationLifetime;
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public static float actorAnimationDeltaTime(MolangInstance instance) {
        return ((ActorInstance) instance).animationDeltaTime;
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public static float actorAnimationFrameAlpha(MolangInstance instance) {
        return ((ActorInstance) instance).animationFrameAlpha;
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public static float actorKeyframeLerpTime(MolangInstance instance) {
        return ((ActorInstance) instance).keyframeLerpTime;
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public static boolean hasBlockSource(MolangInstance instance) {
        if (instance instanceof ActorInstance actor) return actor.blockSourceAvailable;
        return instance.getReferencedItem() == null && instance.getEmitter() == null
                && instance.getAttachedEntity() instanceof IEntity entity && entity.kltyton_bedrock_runtime$hasAnimationContext();
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public static MolangInstance contextOwner(MolangInstance instance) {
        while (true) {
            if (instance instanceof ItemInstance item) instance = item.source;
            else if (instance instanceof ActorInstance actor) instance = actor.source;
            else return instance;
        }
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public static boolean isInitializingDeferredParticle(MolangInstance instance) {
        ParticleEmitter emitter = instance instanceof ParticleEmitter root ? root
                : instance instanceof IMolangParticleInstance particle ? particle.getEmitter() : null;
        return emitter != null && emitter.isInitializingDeferredEffect();
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public static boolean isInitializingDeferredComponents(MolangInstance instance) {
        ParticleEmitter emitter = instance instanceof ParticleEmitter root ? root
                : instance instanceof IMolangParticleInstance particle ? particle.getEmitter() : null;
        return emitter != null && emitter.isInitializingDeferredComponents();
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public static boolean hasEmptyBaseRenderContext(MolangInstance instance) {
        while (true) {
            if (instance.getReferencedItem() != null) return true;
            if (instance instanceof ActorInstance actor) instance = actor.source;
            else return isInitializingDeferredParticle(instance);
        }
    }

    private static final class ItemInstance implements MolangInstance {
        private final MolangInstance source;
        private final ItemStack item;
        private final VariableTable variables = new PublicVariables(Map.of());

        private ItemInstance(MolangInstance source, ItemStack item) {
            this.source = source;
            this.item = item;
        }

        @Override public VariableTable getVars() { return variables; }
        @Override public Map<String, ?> getMolangContext() {
            Map<String, ?> context = source.getMolangContext();
            if (!context.containsKey("this")) return context;
            Map<String, Object> independent = new HashMap<>(context);
            independent.remove("this");
            return independent;
        }
        @Override public ItemStack getReferencedItem() { return item; }
        @Override public Level getLevel() { return source.getLevel(); }
        @Override public float tickAge() { return 0; }
        @Override public float tickLifetime() { return 0; }
        @Override public double getRandom1() { return 0; }
        @Override public double getRandom2() { return 0; }
        @Override public double getRandom3() { return 0; }
        @Override public double getRandom4() { return 0; }
        @Override public ResourceLocation getIdentity() { return source.getIdentity(); }
        @Override public Vec3 getPosition() { return Vec3.ZERO; }
        @Override public @Nullable Entity getAttachedEntity() { return null; }
        @Override public @Nullable ParticleEmitterAttachable getAttached() { return null; }
        @Override public float getInvTickRate() { return source.getInvTickRate(); }
        @Override public @Nullable ParticleEmitter getEmitter() { return null; }
    }

    private static final class ActorInstance implements MolangInstance {
        private final MolangInstance source;
        private final Entity entity;
        private final PublicVariables variables;
        private final float animationTime;
        private final float animationLifetime;
        private final float animationDeltaTime;
        private final float animationFrameAlpha;
        private final float keyframeLerpTime;
        private final boolean blockSourceAvailable;

        private ActorInstance(MolangInstance source, Entity entity) {
            this.source = source;
            this.entity = entity;
            this.animationTime = entity instanceof IEntity actor ? actor.kltyton_bedrock_runtime$getAnimationTime() : 0.0F;
            this.animationLifetime = entity instanceof IEntity actor ? actor.kltyton_bedrock_runtime$getAnimationLifetime() : 0.0F;
            this.animationDeltaTime = entity instanceof IEntity actor ? actor.kltyton_bedrock_runtime$getAnimationDeltaTime() : 0.0F;
            this.animationFrameAlpha = entity instanceof IEntity actor ? actor.kltyton_bedrock_runtime$getAnimationFrameAlpha() : 0.0F;
            this.keyframeLerpTime = entity instanceof IEntity actor ? actor.kltyton_bedrock_runtime$getKeyframeLerpTime() : 0.0F;
            this.blockSourceAvailable = entity instanceof IEntity actor && actor.kltyton_bedrock_runtime$hasAnimationContext();
            this.variables = new PublicVariables(new HashMap<>());
            this.variables.refresh(entity);
        }

        @Override public VariableTable getVars() { return variables; }
        @Override public Map<String, ?> getMolangContext() {
            Map<String, ?> context = source.getMolangContext();
            if (!context.containsKey("this")) return context;
            Map<String, Object> independent = new HashMap<>(context);
            independent.remove("this");
            return independent;
        }
        @Override public Level getLevel() { return entity.level(); }
        @Override public float tickAge() { return source.tickAge(); }
        @Override public float tickLifetime() { return source.tickLifetime(); }
        @Override public double getRandom1() { return source.getRandom1(); }
        @Override public double getRandom2() { return source.getRandom2(); }
        @Override public double getRandom3() { return source.getRandom3(); }
        @Override public double getRandom4() { return source.getRandom4(); }
        @Override public ResourceLocation getIdentity() { return source.getIdentity(); }
        @Override public Vec3 getPosition() { return entity.position(); }
        @Override public Entity getAttachedEntity() { return entity; }
        @Override public @Nullable ParticleEmitterAttachable getAttached() { return IEntity.of(entity); }
        @Override public float getInvTickRate() { return source.getInvTickRate(); }
        @Override public @Nullable ParticleEmitter getEmitter() { return null; }
    }

    private static final class PublicVariables extends VariableTable {
        private PublicVariables(Map<String, Variable> values) { super(values, null); }

        private void refresh(Entity entity) {
            Map<String, Variable> values = new HashMap<>();
            MolangQueries.getPublicActorVariables(entity).forEach((name, value) -> {
                Variable variable;
                if (value instanceof Number number) variable = new Variable(name, number.doubleValue());
                else if (value instanceof String string) variable = new Variable(name, string);
                else {
                    variable = new Variable(name, 0.0);
                    variable.setRaw(value instanceof Map<?, ?> structure ? MolangStruct.fromMap(structure) : value);
                }
                values.put(name, variable);
            });
            table.clear();
            table.putAll(values);
        }

        private boolean missingRoot(String name) {
            return name.startsWith("variable.") && name.indexOf('.', "variable.".length()) < 0
                    && !containsVariable(name);
        }
        @Override public double getValue(String name, MolangInstance instance) {
            return missingRoot(name) ? 0.0 : super.getValue(name, instance);
        }
        @Override public Object getRawValue(String name, MolangInstance instance) {
            return missingRoot(name) ? 0.0 : super.getRawValue(name, instance);
        }
        private static UnsupportedOperationException readOnly() {
            return new UnsupportedOperationException("Other actors' public Molang variables are read-only");
        }
        @Override public void setValue(String name, double value) { throw readOnly(); }
        @Override public void setValue(String name, String value) { throw readOnly(); }
        @Override public void setValue(String name, ToDoubleFunction<MolangInstance> value) { throw readOnly(); }
        @Override public void setValue(String name, Variable value) { throw readOnly(); }
        @Override public void setRawValue(String name, Object value) { throw readOnly(); }
        @Override public void setRawValue(String name, Object value, MolangInstance instance) { throw readOnly(); }
    }
}
