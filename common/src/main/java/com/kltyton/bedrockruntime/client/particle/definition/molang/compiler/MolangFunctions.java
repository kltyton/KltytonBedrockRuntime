package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler;

import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.generic.*;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.limit.ClampFunction;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.limit.MaxFunction;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.limit.MinFunction;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.misc.IsBlockFunction;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.misc.PiFunction;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.misc.ToDegFunction;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.misc.ToRadFunction;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query.*;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.random.DieRollFunction;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.random.DieRollIntegerFunction;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.random.RandomFunction;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.random.RandomIntegerFunction;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.round.*;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.Variable;
import com.kltyton.bedrockruntime.client.particle.molang.query.SurfaceParticleQuery;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Owns the built-in Molang function registry and the bridge into the query dispatcher.
 *
 * <p>This class contains no tokenizer or parser. The Mocha-backed parser resolves names,
 * wraps arguments as {@link MathValue}s and asks this class to instantiate the function.</p>
 */
public final class MolangFunctions {
    private static final Map<String, MathFunction.Factory<?>> FACTORIES = new ConcurrentHashMap<>(128);

    static {
        register("math.abs", AbsFunction::new);
        register("math.acos", ACosFunction::new);
        register("math.asin", ASinFunction::new);
        register("math.atan", ATanFunction::new);
        register("math.atan2", ATan2Function::new);
        register("math.ceil", CeilFunction::new);
        register("math.clamp", ClampFunction::new);
        register("math.copy_sign", CopySignFunction::new);
        register("math.cos", CosFunction::new);
        register("math.die_roll", DieRollFunction::new);
        register("math.die_roll_integer", DieRollIntegerFunction::new);
        register("math.exp", ExpFunction::new);
        register("math.floor", FloorFunction::new);
        register("math.inverse_lerp", InverseLerpFunction::new);
        register("math.lerprotate", LerpRotFunction::new);
        register("math.hermite_blend", HermiteBlendFunction::new);
        register("math.lerp", LerpFunction::new);
        register("math.ln", LogFunction::new);
        register("math.max", MaxFunction::new);
        register("math.min", MinFunction::new);
        register("math.min_angle", MinAngleFunction::new);
        register("math.mod", ModFunction::new);
        register("math.pi", PiFunction::new);
        register("math.pow", PowFunction::new);
        register("math.random", RandomFunction::new);
        register("math.random_integer", RandomIntegerFunction::new);
        register("math.round", RoundFunction::new);
        register("math.sign", SignFunction::new);
        register("math.sin", SinFunction::new);
        register("math.sqrt", SqrtFunction::new);
        register("math.to_deg", ToDegFunction::new);
        register("math.to_rad", ToRadFunction::new);
        register("math.trunc", TruncateFunction::new);
        for (EasingFunction.Family family : EasingFunction.Family.values()) {
            for (EasingFunction.Direction direction : EasingFunction.Direction.values()) {
                register(EasingFunction.name(family, direction),
                        values -> new EasingFunction(family, direction, values));
            }
        }
        register("query.is_block", IsBlockFunction::new);
        register("query.item_is_charged", ItemChargedFunction::new);
        register("query.is_item_equipped", ItemEquippedFunction::new);
        register("query.position", values -> new EntityAxisFunction(EntityAxisFunction.Kind.POSITION, values));
        register("query.position_delta", values -> new EntityAxisFunction(EntityAxisFunction.Kind.POSITION_DELTA, values));
        register("query.movement_direction", values -> new EntityAxisFunction(EntityAxisFunction.Kind.MOVEMENT_DIRECTION, values));
        register("query.has_armor_slot", values -> new ArmorSlotQuery(ArmorSlotQuery.Kind.HAS_ARMOR, values));
        register("query.camera_rotation", CameraRotationQuery::new);
        register("query.rotation_to_camera", RotationToCameraQuery::new);
        register("query.camera_distance_range_lerp", values -> new CameraDistanceQuery(
                CameraDistanceQuery.Kind.RANGE_LERP, MolangQueries.getQueryFor("query.distance_from_camera"), values));
        register("query.lod_index", values -> new CameraDistanceQuery(
                CameraDistanceQuery.Kind.LOD_INDEX, MolangQueries.getQueryFor("query.distance_from_camera"), values));
        register("query.get_name", values -> new EntityNameQuery(EntityNameQuery.Kind.GET_NAME, values));
        register("query.is_name_any", values -> new EntityNameQuery(EntityNameQuery.Kind.IS_NAME_ANY, values));
        register("query.is_riding_any_entity_of_type", EntityTypeRelationQuery::new);
        register("query.is_owner_identifier_any", values -> new EntityTypeRelationQuery(EntityTypeRelationQuery.Kind.OWNER, values));
        register("query.owner_identifier", values -> new EntityTypeRelationQuery(EntityTypeRelationQuery.Kind.OWNER_IDENTIFIER, values));
        register("query.get_equipped_item_name", values -> new ItemNameQuery(ItemNameQuery.Kind.GET_EQUIPPED_NAME, values));
        register("query.is_item_name_any", values -> new ItemNameQuery(ItemNameQuery.Kind.IS_NAME_ANY, values));
        register("query.all_tags", values -> new ItemBlockTagQuery(ItemBlockTagQuery.Kind.ALL, values));
        register("query.any_tag", values -> new ItemBlockTagQuery(ItemBlockTagQuery.Kind.ANY, values));
        register("query.equipped_item_all_tags", values -> new ItemBlockTagQuery(ItemBlockTagQuery.Kind.EQUIPPED_ALL, values));
        register("query.equipped_item_any_tag", values -> new ItemBlockTagQuery(ItemBlockTagQuery.Kind.EQUIPPED_ANY, values));
        register("query.item_in_use_duration", ItemUseDurationQuery::new);
        register("query.get_animation_frame", ItemAnimationFrameQuery::new);
        register("query.item_remaining_use_duration", ItemRemainingUseDurationQuery::new);
        register("query.block_has_all_tags", values -> new BlockTagQuery(BlockTagQuery.Query.BLOCK_HAS_ALL_TAGS, values));
        register("query.block_has_any_tag", values -> new BlockTagQuery(BlockTagQuery.Query.BLOCK_HAS_ANY_TAG, values));
        register("query.relative_block_has_all_tags", values -> new BlockTagQuery(BlockTagQuery.Query.RELATIVE_BLOCK_HAS_ALL_TAGS, values));
        register("query.relative_block_has_any_tag", values -> new BlockTagQuery(BlockTagQuery.Query.RELATIVE_BLOCK_HAS_ANY_TAG, values));
        register("query.block_neighbor_has_all_tags", values -> new BlockTagQuery(BlockTagQuery.Query.BLOCK_NEIGHBOR_HAS_ALL_TAGS, values));
        register("query.block_neighbor_has_any_tag", values -> new BlockTagQuery(BlockTagQuery.Query.BLOCK_NEIGHBOR_HAS_ANY_TAG, values));
        register("query.entity_biome_has_all_tags", values -> new EntityBiomeQuery(EntityBiomeQuery.Kind.ALL_TAGS, values));
        register("query.entity_biome_has_any_tags", values -> new EntityBiomeQuery(EntityBiomeQuery.Kind.ANY_TAGS, values));
        register("query.entity_biome_has_any_identifier", values -> new EntityBiomeQuery(EntityBiomeQuery.Kind.ANY_IDENTIFIER, values));
        register("query.has_target", values -> new EntityStateQuery(EntityStateQuery.Kind.HAS_TARGET, values));
        register("query.is_in_love", values -> new EntityStateQuery(EntityStateQuery.Kind.IS_IN_LOVE, values));
        register("query.target_x_rotation", values -> new EntityStateQuery(EntityStateQuery.Kind.TARGET_X_ROTATION, values));
        register("query.target_y_rotation", values -> new EntityStateQuery(EntityStateQuery.Kind.TARGET_Y_ROTATION, values));
        register("query.is_critical", values -> new EntityStateQuery(EntityStateQuery.Kind.IS_CRITICAL, values));
        register("query.is_sneezing", values -> new EntityStateQuery(EntityStateQuery.Kind.IS_SNEEZING, values));
        register("query.is_celebrating", values -> new EntityStateQuery(EntityStateQuery.Kind.IS_CELEBRATING, values));
        register("query.is_elder", values -> new EntityStateQuery(EntityStateQuery.Kind.IS_ELDER, values));
        register("query.is_scared", values -> new EntityStateQuery(EntityStateQuery.Kind.IS_SCARED, values));
        register("query.is_stunned", values -> new EntityStateQuery(EntityStateQuery.Kind.IS_STUNNED, values));
        register("query.is_admiring", values -> new EntityStateQuery(EntityStateQuery.Kind.IS_ADMIRING, values));
        register("query.is_transforming", values -> new EntityStateQuery(EntityStateQuery.Kind.IS_TRANSFORMING, values));
        register("query.is_wall_climbing", values -> new EntityStateQuery(EntityStateQuery.Kind.IS_WALL_CLIMBING, values));
        register("query.on_fire_time", values -> new EntityStateQuery(EntityStateQuery.Kind.ON_FIRE_TIME, values));
        register("query.swim_amount", values -> new EntityStateQuery(EntityStateQuery.Kind.SWIM_AMOUNT, values));
        register("query.swell_amount", values -> new EntityStateQuery(EntityStateQuery.Kind.SWELL_AMOUNT, values));
        register("query.standing_scale", values -> new EntityStateQuery(EntityStateQuery.Kind.STANDING_SCALE, values));
        register("query.model_scale", values -> new EntityStateQuery(EntityStateQuery.Kind.MODEL_SCALE, values));
        register("query.hurt_time", values -> new EntityStateQuery(EntityStateQuery.Kind.HURT_TIME, values));
        register("query.hurt_direction", values -> new EntityStateQuery(EntityStateQuery.Kind.HURT_DIRECTION, values));
        register("query.invulnerable_ticks", values -> new EntityStateQuery(EntityStateQuery.Kind.INVULNERABLE_TICKS, values));
        register("query.block_state", values -> new BlockStateQuery(BlockStateQuery.Kind.BLOCK_STATE, values));
        register("query.has_block_state", values -> new BlockStateQuery(BlockStateQuery.Kind.HAS_BLOCK_STATE, values));
        register("query.block_property", values -> new BlockStateQuery(BlockStateQuery.Kind.BLOCK_PROPERTY, values));
        register("query.has_block_property", values -> new BlockStateQuery(BlockStateQuery.Kind.HAS_BLOCK_PROPERTY, values));
        register("query.last_frame_time", values -> new FrameTimeQuery(FrameTimeQuery.Kind.LAST, values));
        register("query.average_frame_time", values -> new FrameTimeQuery(FrameTimeQuery.Kind.AVERAGE, values));
        register("query.minimum_frame_time", values -> new FrameTimeQuery(FrameTimeQuery.Kind.MINIMUM, values));
        register("query.maximum_frame_time", values -> new FrameTimeQuery(FrameTimeQuery.Kind.MAXIMUM, values));
        register("query.armor_damage_slot", values -> new ArmorSlotQuery(ArmorSlotQuery.Kind.DAMAGE, values));
        register("query.all", values -> new ComparisonQuery(ComparisonQuery.Kind.ALL, values));
        register("query.any", values -> new ComparisonQuery(ComparisonQuery.Kind.ANY, values));
        register("query.approx_eq", values -> new ComparisonQuery(ComparisonQuery.Kind.APPROX_EQ, values));
        register("query.in_range", values -> new ComparisonQuery(ComparisonQuery.Kind.IN_RANGE, values));
        register("query.count", CountQuery::new);
        register("query.log", LogQuery::new);
        register("query.noise", NoiseQuery::new);
        register("query.combine_entities", CombineEntitiesQuery::new);
        register("query.spellcolor", SpellColorQuery::new);
        register("query.surface_particle_color", values -> new SurfaceParticleQuery(
                SurfaceParticleQuery.Kind.COLOR, MolangVersion.LATEST, values));
        register("query.surface_particle_texture_coordinate", values -> new SurfaceParticleQuery(
                SurfaceParticleQuery.Kind.COORDINATE, MolangVersion.LATEST, values));
        register("query.surface_particle_texture_size", values -> new SurfaceParticleQuery(
                SurfaceParticleQuery.Kind.SIZE, MolangVersion.LATEST, values));
        register("query.above_top_solid", AboveTopSolidQuery::new);
        register("query.heightmap", HeightmapQuery::new);
        register("query.get_nearby_entities", values -> new NearbyEntitiesQuery(false, values));
        register("query.get_nearby_entities_except_self", values -> new NearbyEntitiesQuery(true, values));
    }

    private MolangFunctions() {
    }

    /** Registers or replaces a built-in function factory. */
    public static void register(String name, MathFunction.Factory<?> factory) {
        FACTORIES.put(MolangQueries.applyQueryAliases(name), factory);
    }

    public static boolean contains(String name) {
        String canonical = MolangQueries.applyQueryAliases(name);
        return FACTORIES.containsKey(canonical) || MolangQueries.isExistingFunction(canonical);
    }

    public static MathValue create(String canonicalName, List<MathValue> args, MolangVersion version) {
        return create(canonicalName, args, version, false);
    }

    /**
     * Creates a function for the parser. The special item-name, remaining-use-duration and
     * surface-particle queries keep their version/bedrock-name sensitive construction.
     */
    public static MathValue create(String canonicalName, List<MathValue> args, MolangVersion version,
                                   boolean bedrockItemNames) {
        String name = MolangQueries.applyQueryAliases(canonicalName);
        MathValue[] values = args.toArray(new MathValue[0]);
        if (name.startsWith("query.")) {
            Supplier<MathValue> fallback = values.length == 0 && MolangQueries.isExistingVariable(name)
                    ? () -> MolangQueries.getQueryFor(name, version)
                    : builtIn(name, values, version, bedrockItemNames);
            MathValue created = MolangQueries.createFunction(name, args, fallback);
            if (created == null) {
                throw new IllegalArgumentException("No dispatchable Molang query: " + name);
            }
            return created;
        }
        Supplier<MathValue> factory = builtIn(name, values, version, bedrockItemNames);
        MathValue created = factory == null ? null : factory.get();
        if (created == null) {
            throw new IllegalArgumentException("No registered Molang function: " + name);
        }
        return created;
    }

    private static Supplier<MathValue> builtIn(String name, MathValue[] values, MolangVersion version,
                                               boolean bedrockItemNames) {
        if (name.equals("query.is_item_name_any")) {
            return () -> new ItemNameQuery(ItemNameQuery.Kind.IS_NAME_ANY, bedrockItemNames, values);
        }
        if (name.equals("query.get_equipped_item_name")) {
            return () -> new ItemNameQuery(ItemNameQuery.Kind.GET_EQUIPPED_NAME, bedrockItemNames, values);
        }
        if (name.equals("query.item_remaining_use_duration")) {
            return () -> new ItemRemainingUseDurationQuery(version, values);
        }
        if (name.equals("query.surface_particle_color")) {
            return () -> new SurfaceParticleQuery(SurfaceParticleQuery.Kind.COLOR, version, values);
        }
        if (name.equals("query.surface_particle_texture_coordinate")) {
            return () -> new SurfaceParticleQuery(SurfaceParticleQuery.Kind.COORDINATE, version, values);
        }
        if (name.equals("query.surface_particle_texture_size")) {
            return () -> new SurfaceParticleQuery(SurfaceParticleQuery.Kind.SIZE, version, values);
        }
        MathFunction.Factory<?> factory = FACTORIES.get(name);
        if (factory == null) {
            return () -> null;
        }
        return () -> factory.create(values);
    }

    public static Variable getQueryFor(String name, MolangVersion version) {
        return MolangQueries.getQueryFor(name, version);
    }
}
