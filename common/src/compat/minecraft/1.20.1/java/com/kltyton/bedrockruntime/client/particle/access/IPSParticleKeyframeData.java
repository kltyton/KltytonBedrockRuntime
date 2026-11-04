package com.kltyton.bedrockruntime.client.particle.access;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.molang.VariableTable;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangVersion;
import software.bernie.geckolib.core.keyframe.event.data.ParticleKeyframeData;

public interface IPSParticleKeyframeData {
    ResourceLocation kltyton_bedrock_runtime$getParticle();

    MolangExp kltyton_bedrock_runtime$getExpression(VariableTable variableTable);
    void kltyton_bedrock_runtime$setMolangVersion(MolangVersion version);
    void kltyton_bedrock_runtime$setBedrockItemNames(boolean bedrockItemNames);

    @Nullable Boolean kltyton_bedrock_runtime$getBindToActor();

    void kltyton_bedrock_runtime$setSource(@Nullable String script, @Nullable Boolean bindToActor);

    static IPSParticleKeyframeData of(ParticleKeyframeData data) {
        return (IPSParticleKeyframeData) data;
    }
}
