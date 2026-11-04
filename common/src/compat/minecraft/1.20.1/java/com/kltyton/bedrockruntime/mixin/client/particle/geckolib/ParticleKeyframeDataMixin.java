package com.kltyton.bedrockruntime.mixin.client.particle.geckolib;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.molang.VariableTable;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangParser;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangVersion;
import com.kltyton.bedrockruntime.client.particle.access.IPSParticleKeyframeData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib.core.keyframe.event.data.ParticleKeyframeData;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.core.keyframe.event.data.ParticleKeyframeData", remap = false)
public abstract class ParticleKeyframeDataMixin implements IPSParticleKeyframeData {
    @Shadow public abstract String getEffect();
    @Shadow public abstract String script();

    @Unique private @Nullable Boolean kltyton_bedrock_runtime$bindToActor;
    @Unique private @Nullable ResourceLocation kltyton_bedrock_runtime$particle;
    @Unique private @Nullable MolangExp kltyton_bedrock_runtime$expression;

    @Unique private MolangVersion kltyton_bedrock_runtime$scriptVersion = MolangVersion.LATEST;
    @Unique private boolean kltyton_bedrock_runtime$bedrockItemNames;
    @Unique private @Nullable String kltyton_bedrock_runtime$sourceScript;

    @Override
    public void kltyton_bedrock_runtime$setSource(@Nullable String script, @Nullable Boolean bindToActor) {
        this.kltyton_bedrock_runtime$bindToActor = bindToActor;
        this.kltyton_bedrock_runtime$sourceScript = script;
        kltyton_bedrock_runtime$expression = null;
    }

    @Override
    public void kltyton_bedrock_runtime$setMolangVersion(MolangVersion version) {
        kltyton_bedrock_runtime$scriptVersion = version;
        kltyton_bedrock_runtime$expression = null;
    }

    @Override
    public void kltyton_bedrock_runtime$setBedrockItemNames(boolean bedrockItemNames) {
        kltyton_bedrock_runtime$bedrockItemNames = bedrockItemNames;
        kltyton_bedrock_runtime$expression = null;
    }

    @Override
    public ResourceLocation kltyton_bedrock_runtime$getParticle() {
        if (kltyton_bedrock_runtime$particle == null) kltyton_bedrock_runtime$particle = new ResourceLocation(getEffect());
        return kltyton_bedrock_runtime$particle;
    }

    @Override
    public MolangExp kltyton_bedrock_runtime$getExpression(VariableTable variableTable) {
        if (kltyton_bedrock_runtime$expression == null) {
            String script = kltyton_bedrock_runtime$sourceScript == null ? script() : kltyton_bedrock_runtime$sourceScript;
            if (script == null || script.isBlank()) return MolangExp.EMPTY;
            MolangExp expression = new MolangExp(script);
            expression.compile(new MolangParser(variableTable, kltyton_bedrock_runtime$scriptVersion, kltyton_bedrock_runtime$bedrockItemNames));
            kltyton_bedrock_runtime$expression = expression;
        }
        return kltyton_bedrock_runtime$expression;
    }

    @Override
    public @Nullable Boolean kltyton_bedrock_runtime$getBindToActor() {
        return kltyton_bedrock_runtime$bindToActor;
    }
}
