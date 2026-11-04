package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;
import com.kltyton.bedrockruntime.client.particle.runtime.attachment.WithBlockParticleEmitter;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Implements the six Bedrock block-tag queries against Java block tags. */
public final class BlockTagQuery extends MathFunction {
    private static final Set<String> REPORTED_ERRORS = ConcurrentHashMap.newKeySet();
    public enum Query {
        BLOCK_HAS_ALL_TAGS("query.block_has_all_tags", Origin.WORLD, Match.ALL),
        BLOCK_HAS_ANY_TAG("query.block_has_any_tag", Origin.WORLD, Match.ANY),
        RELATIVE_BLOCK_HAS_ALL_TAGS("query.relative_block_has_all_tags", Origin.ENTITY, Match.ALL),
        RELATIVE_BLOCK_HAS_ANY_TAG("query.relative_block_has_any_tag", Origin.ENTITY, Match.ANY),
        BLOCK_NEIGHBOR_HAS_ALL_TAGS("query.block_neighbor_has_all_tags", Origin.BLOCK, Match.ALL),
        BLOCK_NEIGHBOR_HAS_ANY_TAG("query.block_neighbor_has_any_tag", Origin.BLOCK, Match.ANY);

        private final String name;
        private final Origin origin;
        private final Match match;

        Query(String name, Origin origin, Match match) {
            this.name = name;
            this.origin = origin;
            this.match = match;
        }
    }

    private enum Origin { WORLD, ENTITY, BLOCK }
    private enum Match { ALL, ANY }

    private final Query query;
    private final MathValue[] arguments;

    public BlockTagQuery(Query query, MathValue... arguments) {
        super(arguments);
        this.query = query;
        this.arguments = arguments.clone();
    }

    @Override
    public String getName() {
        return query == null ? "query.block_tag" : query.name;
    }

    @Override
    public int getMinArgs() {
        return 0;
    }

    @Override
    public MathValue[] getArgs() {
        return arguments.clone();
    }

    @Override
    public boolean isMutable(MathValue... values) {
        return true;
    }

    @Override
    public double compute(MolangInstance instance) {
        if (arguments.length < 4) return contentError(instance, "requires x, y, z and at least one block tag");
        if (instance.getReferencedItem() != null) return contentError(instance, "missing block source");
        Level level = instance.getLevel();
        BlockPos pos = targetPosition(instance);
        if (pos == null) return contentError(instance, "missing attachment for relative position");
        if (!level.isLoaded(pos)) {
            return contentError(instance, "block position is outside loaded world data");
        }

        BlockState state = level.getBlockState(pos);
        for (int i = 3; i < arguments.length; i++) {
            String name = arguments[i].getString(instance);
            Identifier identifier = name == null ? null : Identifier.tryParse(name);
            if (identifier == null) return contentError(instance, "invalid block tag identifier");
            TagKey<Block> tag = TagKey.create(Registries.BLOCK, identifier);
            boolean hasTag = state.is(tag);
            if (query.match == Match.ALL && !hasTag) return 0.0;
            if (query.match == Match.ANY && hasTag) return 1.0;
        }
        return query.match == Match.ALL ? 1.0 : 0.0;
    }

    private @Nullable BlockPos targetPosition(MolangInstance instance) {
        if (query.origin == Origin.ENTITY) {
            int z = coordinate(arguments[2], instance);
            int y = coordinate(arguments[1], instance);
            int x = coordinate(arguments[0], instance);
            Entity entity = instance.getAttachedEntity();
            if (entity == null) return null;
            return BlockPos.containing(ActorQueryPosition.x(entity), ActorQueryPosition.y(entity), ActorQueryPosition.z(entity)).offset(x, y, z);
        }
        BlockPos origin = query.origin == Origin.WORLD ? BlockPos.ZERO : attachedBlockPosition(instance);
        if (origin == null) return null;

        int x, y, z;
        if (query.origin == Origin.BLOCK) {
            x = blockCoordinate(arguments[0], instance);
            y = blockCoordinate(arguments[1], instance);
            z = blockCoordinate(arguments[2], instance);
        } else {
            z = coordinate(arguments[2], instance);
            y = coordinate(arguments[1], instance);
            x = coordinate(arguments[0], instance);
        }
        return origin.offset(x, y, z);
    }

    private static int coordinate(MathValue argument, MolangInstance instance) {
        return coordinate((float) argument.get(instance));
    }

    private int blockCoordinate(MathValue argument, MolangInstance instance) {
        Object value = MathValue.resolveRaw(argument, instance);
        if (value instanceof Number number) return coordinate(number.floatValue());
        contentError(instance, "block neighbor coordinate must be numeric");
        return 0;
    }

    private static int coordinate(float value) {
        return value >= 0x1.0p31F || value < -0x1.0p31F || Float.isNaN(value)
                ? Integer.MIN_VALUE : (int) value;
    }

    private @Nullable BlockPos attachedBlockPosition(MolangInstance instance) {
        var emitter = instance.getEmitter();
        if (emitter == null) return null;
        var blockEntity = emitter.getAttachedBlock();
        if (blockEntity != null) return blockEntity.getBlockPos();
        if (emitter instanceof WithBlockParticleEmitter blockEmitter) {
            WithBlockParticleEmitter.BlockData blockData = blockEmitter.getBlockData();
            if (blockData != null) return blockData.pos();
        }
        return null;
    }

    private double contentError(MolangInstance instance, String reason) {
        String key = instance.getIdentity() + ":" + query.name + ":" + reason;
        if (REPORTED_ERRORS.add(key)) {
            BedrockParticles.LOGGER.warn("Molang {} in particle effect {}: {}", query.name, instance.getIdentity(), reason);
        }
        return 0.0;
    }
}
