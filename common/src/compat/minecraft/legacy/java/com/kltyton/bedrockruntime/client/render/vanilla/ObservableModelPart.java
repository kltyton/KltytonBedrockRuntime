package com.kltyton.bedrockruntime.client.render.vanilla;

import com.mojang.blaze3d.vertex.PoseStack;

/** Connects model-part drawing to the active selection capture. */
public interface ObservableModelPart {
    void kltyton_bedrock_runtime$setOwnPath(String path);
    void kltyton_bedrock_runtime$refreshChildPaths();
    void kltyton_bedrock_runtime$observe(PoseStack.Pose pose);
}
