package com.kltyton.bedrockruntime.client.particle.model.runtime;

import net.minecraft.client.renderer.SubmitNodeCollector;

/** Model geometry owned by a single extracted particle render state. */
public interface ModelParticleSubmissionAccess {
    void kltyton_bedrock_runtime$addModelGeometry(ModelParticleGeometry geometry);

    void kltyton_bedrock_runtime$submitModelGeometry(SubmitNodeCollector collector);
}

