package com.kltyton.bedrockruntime.client.render.vanilla;

import com.kltyton.bedrockruntime.client.render.item.HeldItemCapture;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;

/** Observes the pose used by EMF's independent model-part drawing path. */
public final class EmfModelPartCapture {
    private EmfModelPartCapture() { }

    public static void afterDraw(ModelPart part, PoseStack stack) {
        if (!VanillaModelCapture.isActive() && !HeldItemCapture.isActive()) { return; }
        ObservableModelPart observable = (ObservableModelPart) (Object) part;
        observable.kltyton_bedrock_runtime$refreshChildPaths();
        if (!part.visible || part.skipDraw) { return; }
        observable.kltyton_bedrock_runtime$observe(stack.last());
    }
}
