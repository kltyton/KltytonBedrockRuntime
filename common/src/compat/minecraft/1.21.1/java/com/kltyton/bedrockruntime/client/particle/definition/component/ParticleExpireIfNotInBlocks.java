package com.kltyton.bedrockruntime.client.particle.definition.component;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.api.IParticleComponent;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ParticleExpireIfNotInBlocks implements IParticleComponent {
    public static final net.minecraft.resources.ResourceLocation ID = net.minecraft.resources.ResourceLocation.withDefaultNamespace("particle_expire_if_not_in_blocks");
    public static final Codec<ParticleExpireIfNotInBlocks> CODEC = Codec.list(Codec.STRING).xmap(
            states -> new ParticleExpireIfNotInBlocks(new HashSet<>(states)),
            blocks -> List.copyOf(blocks.ids)
    );
    public final Set<String> ids;
    public final Set<BlockState> states;

    public ParticleExpireIfNotInBlocks(Set<String> ids) {
        this.ids = ids;
        this.states = new HashSet<>();
    }

    @Override
    public Codec<ParticleExpireIfNotInBlocks> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of();
    }

    @Override
    public void initialize(Level level) {
        initialize(level, false);
    }

    @Override
    public void initialize(Level level, boolean bedrockBlockNames) {
        Set<BlockState> resolved = BlockStateSelectors.resolve(level, ids, bedrockBlockNames);
        states.clear();
        states.addAll(resolved);
    }

    private static final Vector3f POSITION = new Vector3f();

    @Override
    public void update(IMolangParticleInstance instance) {
        if (ids.isEmpty()) return;
        ParticleEmitter emitter = instance.getEmitter();
        instance.toSimulationSpace(instance.getX(), instance.getY(), instance.getZ(), POSITION);
        Vec3 origin = emitter.getEmissionOrigin();
        POSITION.add((float) origin.x, (float) origin.y, (float) origin.z);
        BlockPos position = BlockPos.containing(POSITION.x, POSITION.y, POSITION.z);
        if (!emitter.matchesExpirationBlock(states, position)) {
            instance.discard();
        }
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    @Override
    public String toString() {
        return "ParticleExpireIfNotInBlocks[" +
                "blocks=" + ids + ']';
    }
}