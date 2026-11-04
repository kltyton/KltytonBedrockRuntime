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

/// Particles expire when in a block of the type in the list.
///
/// Note: this component can exist alongside particle_lifetime_expression.
public final class ParticleExpireIfInBlocks implements IParticleComponent {
    public static final net.minecraft.resources.Identifier ID = net.minecraft.resources.Identifier.withDefaultNamespace("particle_expire_if_in_blocks");
    public static final Codec<ParticleExpireIfInBlocks> CODEC = Codec.list(Codec.STRING).xmap(
            states -> new ParticleExpireIfInBlocks(new HashSet<>(states)),
            blocks -> List.copyOf(blocks.ids)
    );
    public final Set<String> ids;
    public final Set<BlockState> states;

    public ParticleExpireIfInBlocks(Set<String> ids) {
        this.ids = ids;
        this.states = new HashSet<>();
    }

    @Override
    public Codec<ParticleExpireIfInBlocks> codec() {
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
        if (emitter.matchesExpirationBlock(states, position)) {
            instance.discard();
        }
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    @Override
    public String toString() {
        return "ParticleExpireIfInBlocks[" +
                "blocks=" + ids + ']';
    }
}
