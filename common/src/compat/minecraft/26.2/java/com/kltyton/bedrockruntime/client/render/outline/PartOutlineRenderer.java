package com.kltyton.bedrockruntime.client.render.outline;

import com.kltyton.bedrockruntime.client.geometry.bounds.PartBounds;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.AABB;

/**
 * CN: 被选择模型部位轮廓的共享视觉样式。
 * EN: Shared visual style for selected model-part outlines.
 */
public final class PartOutlineRenderer {
    private static final int SELECTED_COLOR = 0xFFFFD21F;
    private static final int DAMAGE_FLASH_COLOR = 0x99FF0000;
    private static final float LINE_WIDTH = 4.0F;
    private static final int ORDER = 1000;
    private static final double DAMAGE_FLASH_INFLATE = 0.002;

    private PartOutlineRenderer() {
    }

    public static void submit(SubmitNodeCollector renderTasks, PoseStack poseStack, PartBounds bounds) {
        renderTasks.submitShapeOutline(poseStack, bounds.localShape(), RenderTypes.linesTranslucent(), SELECTED_COLOR, LINE_WIDTH, false);
    }

    public static void submitDamageFlash(SubmitNodeCollector renderTasks, PoseStack poseStack, PartBounds bounds) {
        AABB box = bounds.localBounds().inflate(DAMAGE_FLASH_INFLATE);
        renderTasks.order(ORDER).submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (pose, buffer) -> {
            quad(buffer, pose, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, box.minX, box.maxY, box.minZ);
            quad(buffer, pose, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, box.maxX, box.maxY, box.maxZ);
            quad(buffer, pose, box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, box.minX, box.maxY, box.maxZ);
            quad(buffer, pose, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, box.maxX, box.maxY, box.minZ);
            quad(buffer, pose, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ);
            quad(buffer, pose, box.minX, box.minY, box.maxZ, box.maxX, box.minY, box.maxZ, box.maxX, box.minY, box.minZ, box.minX, box.minY, box.minZ);
        });
    }

    private static void quad(com.mojang.blaze3d.vertex.VertexConsumer buffer, PoseStack.Pose pose,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            double x3, double y3, double z3,
            double x4, double y4, double z4) {
        vertex(buffer, pose, x1, y1, z1);
        vertex(buffer, pose, x2, y2, z2);
        vertex(buffer, pose, x3, y3, z3);
        vertex(buffer, pose, x4, y4, z4);
    }

    private static void vertex(com.mojang.blaze3d.vertex.VertexConsumer buffer, PoseStack.Pose pose, double x, double y, double z) {
        buffer.addVertex(pose, (float) x, (float) y, (float) z).setColor(DAMAGE_FLASH_COLOR);
    }
}
