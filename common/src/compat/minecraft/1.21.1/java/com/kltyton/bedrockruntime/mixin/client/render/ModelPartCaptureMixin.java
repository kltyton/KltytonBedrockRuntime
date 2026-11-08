package com.kltyton.bedrockruntime.mixin.client.render;

import com.kltyton.bedrockruntime.client.render.item.HeldItemCapture;
import com.kltyton.bedrockruntime.client.render.vanilla.NamedModelPart;
import com.kltyton.bedrockruntime.client.render.vanilla.ObservableModelPart;
import com.kltyton.bedrockruntime.client.render.vanilla.VanillaModelCapture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes model-part geometry at the draw call and direct compile entry. */
@Mixin(ModelPart.class)
public abstract class ModelPartCaptureMixin implements NamedModelPart, ObservableModelPart {
    @Shadow @Final private List<ModelPart.Cube> cubes;
    @Shadow @Final private Map<String, ModelPart> children;
    @Unique private String kltyton_bedrock_runtime$path = "root";
    @Unique private String kltyton_bedrock_runtime$childPrefix = "";

    @Inject(method = "<init>", at = @At("TAIL"))
    private void kltyton_bedrock_runtime$nameTree(List<ModelPart.Cube> cubes, Map<String, ModelPart> children, CallbackInfo callback) {
        kltyton_bedrock_runtime$setPath("");
    }

    @Override
    public void kltyton_bedrock_runtime$setPath(String path) {
        kltyton_bedrock_runtime$setOwnPath(path);
        children.forEach((name, child) -> ((NamedModelPart) (Object) child).kltyton_bedrock_runtime$setPath(path.isEmpty() ? name : path + "/" + name));
    }

    @Override
    public void kltyton_bedrock_runtime$setOwnPath(String path) {
        kltyton_bedrock_runtime$path = path.isEmpty() ? "root" : path;
        kltyton_bedrock_runtime$childPrefix = path;
    }

    @Override
    public void kltyton_bedrock_runtime$refreshChildPaths() {
        children.forEach((name, child) -> ((ObservableModelPart) (Object) child).kltyton_bedrock_runtime$setOwnPath(
                kltyton_bedrock_runtime$childPrefix.isEmpty() ? name : kltyton_bedrock_runtime$childPrefix + "/" + name));
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V", at = @At("HEAD"))
    private void kltyton_bedrock_runtime$refreshNames(CallbackInfo callback) {
        if (VanillaModelCapture.isActive() || HeldItemCapture.isActive()) { kltyton_bedrock_runtime$refreshChildPaths(); }
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;compile(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
    private void kltyton_bedrock_runtime$captureBeforeDraw(PoseStack stack, VertexConsumer output, int light, int overlay,
            int color, CallbackInfo callback) {
        kltyton_bedrock_runtime$observe(stack.last());
    }

    @Inject(method = "compile", at = @At("HEAD"))
    private void kltyton_bedrock_runtime$capture(PoseStack.Pose pose, VertexConsumer output, int light, int overlay, int color,
            CallbackInfo callback) {
        kltyton_bedrock_runtime$observe(pose);
    }

    @Override
    public void kltyton_bedrock_runtime$observe(PoseStack.Pose pose) {
        ModelPart part = (ModelPart) (Object) this;
        VanillaModelCapture.record(part, pose, kltyton_bedrock_runtime$path, cubes);
        HeldItemCapture.modelPart(part, pose, cubes);
    }
}
