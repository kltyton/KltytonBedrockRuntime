package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;
import com.kltyton.bedrockruntime.client.particle.runtime.attachment.WithBlockParticleEmitter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

/** Reads a named property from the block associated with the particle emitter. */
public final class BlockStateQuery extends MathFunction {
    private static final Set<String> REPORTED_ERRORS = ConcurrentHashMap.newKeySet();

    public enum Kind {
        BLOCK_STATE("query.block_state", false),
        HAS_BLOCK_STATE("query.has_block_state", true),
        BLOCK_PROPERTY("query.block_property", false),
        HAS_BLOCK_PROPERTY("query.has_block_property", true);

        private final String name;
        private final boolean presence;

        Kind(String name, boolean presence) {
            this.name = name;
            this.presence = presence;
        }
    }

    private final Kind kind;
    private final MathValue[] arguments;

    public BlockStateQuery(Kind kind, MathValue... arguments) {
        super(arguments);
        this.kind = java.util.Objects.requireNonNull(kind, "kind");
        this.arguments = arguments.clone();
    }

    @Override
    public Object getRaw(MolangInstance instance) {
        if (kind.presence) return get(instance);
        if (!validArguments(instance)) return 0.0;
        BlockState state = attachedState(instance);
        if (state == null) return 0.0;
        Property<?> property = findProperty(instance, state);
        if (property == null) return 0.0;
        return rawValue(state, property);
    }

    @Override
    public double compute(MolangInstance instance) {
        if (!kind.presence) {
            Object raw = getRaw(instance);
            return raw instanceof Number number ? number.doubleValue() : 0.0;
        }
        if (!validArguments(instance)) return 0.0;
        BlockState state = attachedState(instance);
        if (state == null) return 0.0;
        return findProperty(instance, state) == null ? 0.0 : 1.0;
    }

    private @Nullable BlockState attachedState(MolangInstance instance) {
        if (instance.getReferencedItem() != null) {
            contentError(instance, "missing associated block");
            return null;
        }
        var emitter = instance.getEmitter();
        if (emitter == null) {
            contentError(instance, "missing associated block");
            return null;
        }
        BlockPos origin;
        var attachedBlock = emitter.getAttachedBlock();
        if (attachedBlock != null) {
            if (attachedBlock.isRemoved()) {
                contentError(instance, "attached block entity is removed");
                return null;
            }
            origin = attachedBlock.getBlockPos();
        } else if (emitter instanceof WithBlockParticleEmitter blockEmitter) {
            WithBlockParticleEmitter.BlockData blockData = blockEmitter.getBlockData();
            if (blockData == null) {
                contentError(instance, "missing associated block");
                return null;
            }
            origin = blockData.pos();
        } else {
            contentError(instance, "missing associated block");
            return null;
        }

        Level level = instance.getLevel();
        if (!level.isLoaded(origin)) {
            contentError(instance, "associated block position is not loaded");
            return null;
        }
        return level.getBlockState(origin);
    }

    private @Nullable Property<?> findProperty(MolangInstance instance, BlockState state) {
        String propertyName = arguments[0].getString(instance);
        if (propertyName == null || propertyName.isEmpty()) {
            contentError(instance, "property name must be a non-empty string");
            return null;
        }
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals(propertyName)) return property;
        }
        if (!kind.presence) contentError(instance, "associated block has no property named '" + propertyName + "'");
        return null;
    }

    private static <T extends Comparable<T>> Object rawValue(BlockState state, Property<T> property) {
        T value = state.getValue(property);
        if (value instanceof Boolean bool) return bool ? 1.0 : 0.0;
        if (value instanceof Number number) return number.floatValue();
        return property.getName(value);
    }

    private double contentError(MolangInstance instance, String reason) {
        String key = instance.getIdentity() + ":" + kind.name + ":" + reason;
        if (REPORTED_ERRORS.add(key)) {
            BedrockParticles.LOGGER.warn("Molang {} in particle effect {}: {}", kind.name, instance.getIdentity(), reason);
        }
        return 0.0;
    }

    private boolean validArguments(MolangInstance instance) {
        if (arguments.length == 1) return true;
        contentError(instance, "requires exactly one property name");
        return false;
    }

    @Override public String getName() { return kind == null ? "query.block_state" : kind.name; }
    @Override public int getMinArgs() { return 0; }
    @Override public MathValue[] getArgs() { return arguments.clone(); }
    @Override public boolean isMutable(MathValue... values) { return true; }
}
