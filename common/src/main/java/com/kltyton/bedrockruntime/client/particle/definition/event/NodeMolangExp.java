package com.kltyton.bedrockruntime.client.particle.definition.event;

import com.kltyton.bedrockruntime.client.particle.definition.CodecCompat;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.IEventNode;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangParser;

import java.util.function.Function;

public final class NodeMolangExp extends MolangExp implements IEventNode {
    public static final Codec<NodeMolangExp> DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("exp").forGetter(NodeMolangExp::getExpStr),
            CodecCompat.lenientOptionalFieldOf(Codec.BOOL, "log", false).forGetter(NodeMolangExp::shouldLog)
    ).apply(instance, NodeMolangExp::new));
    public static final Codec<NodeMolangExp> CODEC = Codec.either(DIRECT_CODEC, MolangExp.CODEC).xmap(
            either -> either.map(Function.identity(), expression -> new NodeMolangExp(expression.getExpStr(), false)),
            Either::left
    );
    private final boolean log;

    public NodeMolangExp(String expStr, boolean log) {
        super(expStr);
        this.log = log;
    }

    public boolean shouldLog() {
        return log;
    }

    @Override
    public void execute(MolangInstance instance) {
        if (variable == null && !expStr.isEmpty() && !expStr.isBlank()) {
            var preset = instance.getEmitter().getPreset();
            MolangParser parser = new MolangParser(instance.getVars(), preset.molangVersion, preset.bedrockItemNames);
            this.variable = parser.compileMolang(expStr);
        }
        if (variable != null) {
            double v = calculate(instance);
            if (log) {
                Vec3 position = EventResolver.position(instance);
                BedrockParticles.LOGGER.info("{}[{},{},{}]: {}={}", instance.getIdentity(),
                        position.x, position.y, position.z, expStr, v);
            }
        }
    }
}
