package com.kltyton.bedrockruntime.client.particle.access;

import com.geckolib.cache.animation.keyframeevent.ParticleKeyframeData;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.molang.VariableTable;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangVersion;

public interface IPSParticleKeyframeData {
    Identifier kltyton_bedrock_runtime$getParticle();
    MolangExp kltyton_bedrock_runtime$getExpression(VariableTable variableTable);
    void kltyton_bedrock_runtime$setMolangVersion(MolangVersion version);
    void kltyton_bedrock_runtime$setBedrockItemNames(boolean bedrockItemNames);
    @Nullable Boolean kltyton_bedrock_runtime$getBindToActor();
    void kltyton_bedrock_runtime$setSource(@Nullable String script, @Nullable Boolean bindToActor);

    static IPSParticleKeyframeData of(ParticleKeyframeData data) {
        return (IPSParticleKeyframeData) data;
    }
}
