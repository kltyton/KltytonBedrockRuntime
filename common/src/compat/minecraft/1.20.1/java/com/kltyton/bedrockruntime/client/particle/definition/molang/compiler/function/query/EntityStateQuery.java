package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinArmPose;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.raid.Raider;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.RenderFrameContext;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;
import com.kltyton.bedrockruntime.client.particle.access.IEntityFireAnimation;
import com.kltyton.bedrockruntime.client.particle.access.IMolangEntityState;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Reads entity state exposed by vanilla's synchronized data or client-side animation state. */
public final class EntityStateQuery extends MathFunction {
    public enum Kind {
        HAS_TARGET("query.has_target", EntityStateQuery::targetState),
        IS_IN_LOVE("query.is_in_love", EntityStateQuery::loveState),
        TARGET_X_ROTATION("query.target_x_rotation", entity ->
                rotationLerp(RenderFrameContext.currentOr(0.0F), entity.xRotO, entity.getXRot())),
        TARGET_Y_ROTATION("query.target_y_rotation", EntityStateQuery::targetYRotation),
        IS_CRITICAL("query.is_critical", entity -> entity instanceof AbstractArrow arrow && arrow.isCritArrow() ? 1.0 : 0.0),
        IS_SNEEZING("query.is_sneezing", entity -> entity instanceof Panda panda && panda.isSneezing() ? 1.0 : 0.0),
        IS_CELEBRATING("query.is_celebrating", entity -> entity instanceof Raider raider && raider.isCelebrating() ? 1.0 : 0.0),
        IS_ELDER("query.is_elder", entity -> entity instanceof ElderGuardian ? 1.0 : 0.0),
        IS_SCARED("query.is_scared", entity -> entity instanceof Panda panda && panda.isScared() ? 1.0 : 0.0),
        IS_STUNNED("query.is_stunned", entity -> entity instanceof Ravager ravager && ravager.getStunnedTick() > 0 ? 1.0 : 0.0),
        IS_ADMIRING("query.is_admiring", entity -> entity instanceof Piglin piglin && piglin.getArmPose() == PiglinArmPose.ADMIRING_ITEM ? 1.0 : 0.0),
        IS_TRANSFORMING("query.is_transforming", entity -> entity instanceof Zombie zombie && zombie.isUnderWaterConverting()
                || entity instanceof ZombieVillager villager && villager.isConverting()
                || entity instanceof Hoglin hoglin && hoglin.isConverting() ? 1.0 : 0.0),
        IS_WALL_CLIMBING("query.is_wall_climbing", entity -> entity instanceof Spider spider && spider.isClimbing() ? 1.0 : 0.0),
        ON_FIRE_TIME("query.on_fire_time", entity -> entity instanceof IEntityFireAnimation fire
                ? fire.kltyton_bedrock_runtime$getFireAnimationTime() : 0.0),
        SWIM_AMOUNT("query.swim_amount", entity -> entity instanceof LivingEntity living
                ? (double) living.getSwimAmount(RenderFrameContext.currentOr(0.0F)) : 0.0),
        SWELL_AMOUNT("query.swell_amount", entity -> entity instanceof Creeper creeper
                ? (double) creeper.getSwelling(RenderFrameContext.currentOr(0.0F)) : 0.0),
        STANDING_SCALE("query.standing_scale", entity -> entity instanceof PolarBear bear
                ? bear.getStandingAnimationScale(RenderFrameContext.currentOr(0.0F))
                : entity instanceof AbstractHorse horse ? horse.getStandAnim(RenderFrameContext.currentOr(0.0F)) : 0.0),
        MODEL_SCALE("query.model_scale", entity -> entity instanceof LivingEntity living ? (double) living.getScale() : 0.0),
        HURT_TIME("query.hurt_time", entity -> entity instanceof LivingEntity living ? living.hurtTime : 0.0),
        HURT_DIRECTION("query.hurt_direction", entity -> entity instanceof LivingEntity living ? living.getHurtDir() : 0.0),
        INVULNERABLE_TICKS("query.invulnerable_ticks", entity -> entity instanceof WitherBoss wither
                ? wither.getInvulnerableTicks() : entity.invulnerableTime);

        private final String name;
        private final java.util.function.ToDoubleFunction<Entity> value;

        Kind(String name, java.util.function.ToDoubleFunction<Entity> value) {
            this.name = name;
            this.value = value;
        }
    }

    private static final Set<EntityType<?>> REPORTED_MISSING_TARGET_STATE = ConcurrentHashMap.newKeySet();
    private static final Set<EntityType<?>> REPORTED_MISSING_LOVE_STATE = ConcurrentHashMap.newKeySet();

    private static float wrapRotation(float value) {
        float wrapped = (value + 180.0F) % 360.0F;
        if (wrapped < 0.0F) wrapped += 360.0F;
        return wrapped - 180.0F;
    }

    private static float rotationLerp(float frame, float previous, float current) {
        return wrapRotation(current - previous) * frame + previous;
    }

    private static double targetYRotation(Entity entity) {
        float frame = RenderFrameContext.currentOr(0.0F);
        if (!(entity instanceof LivingEntity living)) return 0.0;
        float head = rotationLerp(frame, living.yHeadRotO, living.yHeadRot);
        float body = rotationLerp(frame, living.yBodyRotO, living.yBodyRot);
        return Mth.clamp(wrapRotation(head - body), -85.0F, 85.0F);
    }

    private static double targetState(Entity entity) {
        if (!entity.level().isClientSide() && entity instanceof Mob mob) return mob.getTarget() != null ? 1.0 : 0.0;
        if (!(entity instanceof IMolangEntityState state) || !state.kltyton_bedrock_runtime$targetStateKnown()) {
            if (REPORTED_MISSING_TARGET_STATE.add(entity.getType())) {
                BedrockParticles.LOGGER.warn("No synchronized target state for entity type {}", entity.getType());
            }
            return 0.0;
        }
        return state.kltyton_bedrock_runtime$hasTarget() ? 1.0 : 0.0;
    }

    private static double loveState(Entity entity) {
        if (!entity.level().isClientSide() && entity instanceof Animal animal) return animal.isInLove() ? 1.0 : 0.0;
        if (!(entity instanceof IMolangEntityState state) || !state.kltyton_bedrock_runtime$loveStateKnown()) {
            if (REPORTED_MISSING_LOVE_STATE.add(entity.getType())) {
                BedrockParticles.LOGGER.warn("No synchronized love state for entity type {}", entity.getType());
            }
            return 0.0;
        }
        return state.kltyton_bedrock_runtime$isInLove() ? 1.0 : 0.0;
    }

    private final Kind kind;
    private final MathValue[] arguments;

    public EntityStateQuery(Kind kind, MathValue... arguments) {
        super(arguments);
        this.kind = java.util.Objects.requireNonNull(kind, "kind");
        this.arguments = arguments.clone();
    }

    @Override
    public double compute(MolangInstance instance) {
        Entity entity = instance.getAttachedEntity();
        if (entity == null && kind == Kind.MODEL_SCALE) return 1.0;
        return entity == null ? 0.0 : kind.value.applyAsDouble(entity);
    }

    @Override public String getName() { return kind == null ? "query.entity_state" : kind.name; }
    @Override public int getMinArgs() { return 0; }
    @Override public MathValue[] getArgs() { return arguments.clone(); }
    @Override public boolean isMutable(MathValue... values) { return true; }

    @Override
    public void validate(MathValue... inputs) {
        if (inputs.length != 0) {
            throw new IllegalArgumentException(getName() + " takes no arguments");
        }
    }
}
