package com.kltyton.bedrockruntime.client.render.vanilla;

import com.kltyton.bedrockruntime.client.render.outline.PartOutlineRenderer;

import com.kltyton.bedrockruntime.client.selection.service.BonePartSelectionClient;
import com.kltyton.bedrockruntime.client.geometry.bounds.PartBounds;
import com.kltyton.bedrockruntime.client.selection.model.VisualPartId;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * CN: 在原版动画完成后捕获并高亮 ModelPart cube。
 * EN: Captures and highlights vanilla ModelPart cubes after vanilla animation setup.
 */
public final class VanillaModelPartSelectionLayer<S extends EntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {

    public VanillaModelPartSelectionLayer(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static RenderLayer createRaw(RenderLayerParent renderer) {
        return new VanillaModelPartSelectionLayer(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector renderTasks, int lightCoords, S state, float yRot, float xRot) {
        M model = getParentModel();
        model.root().visit(poseStack, (pose, path, cubeIndex, cube) -> captureCube(renderTasks, state, pose, path, cubeIndex, cube));
    }

    private static void captureCube(SubmitNodeCollector renderTasks, EntityRenderState state, PoseStack.Pose pose, String path, int cubeIndex, ModelPart.Cube cube) {
        PartBounds bounds = PartBounds.fromVanillaCube(pose.pose(), cube);
        if (bounds == null) {
            return;
        }

        String partName = path.isEmpty() ? "root" : path.substring(1);
        VisualPartId id = VisualPartId.vanilla(state, partName, cubeIndex);
        boolean accepted = BonePartSelectionClient.record(id, bounds);
        if (!accepted) {
            return;
        }

        if (BonePartSelectionClient.shouldRenderOutline(id)) {
            PoseStack outlinePose = new PoseStack();
            outlinePose.last().set(pose);
            PartOutlineRenderer.submit(renderTasks, outlinePose, bounds);
        }
        if (BonePartSelectionClient.isDamageFlashing(id)) {
            PoseStack damagePose = new PoseStack();
            damagePose.last().set(pose);
            PartOutlineRenderer.submitDamageFlash(renderTasks, damagePose, bounds);
        }
    }
}
