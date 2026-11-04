package com.kltyton.bedrockruntime.mixin.client.render;

import java.util.List;
import com.kltyton.bedrockruntime.client.render.item.HeldItemCapture;
import java.util.Map;
import com.kltyton.bedrockruntime.client.render.vanilla.NamedModelPart;
import com.kltyton.bedrockruntime.client.render.vanilla.VanillaModelCapture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.class)
public abstract class ModelPartCaptureMixin implements NamedModelPart {
    @Shadow @Final private List<ModelPart.Cube> cubes;
    @Shadow @Final private Map<String, ModelPart> children;
    @Unique private String kltyton_bedrock_runtime$path = "root";

    @Inject(method = "<init>", at = @At("TAIL"))
    private void kltyton_bedrock_runtime$nameTree(List<ModelPart.Cube> cubes, Map<String, ModelPart> children, CallbackInfo callback) {
        kltyton_bedrock_runtime$setPath("");
    }

    @Override
    public void kltyton_bedrock_runtime$setPath(String path) {
        kltyton_bedrock_runtime$path = path.isEmpty() ? "root" : path;
        children.forEach((name, child) -> ((NamedModelPart) (Object) child).kltyton_bedrock_runtime$setPath(path.isEmpty() ? name : path + "/" + name));
    }

    @Inject(method = "compile", at = @At("HEAD"))
    private void kltyton_bedrock_runtime$capture(PoseStack.Pose pose, VertexConsumer output, int light, int overlay, float red, float green, float blue, float alpha,
            CallbackInfo callback) {
        VanillaModelCapture.record(pose, kltyton_bedrock_runtime$path, cubes);
        HeldItemCapture.modelPart(pose, cubes);
    }
}
