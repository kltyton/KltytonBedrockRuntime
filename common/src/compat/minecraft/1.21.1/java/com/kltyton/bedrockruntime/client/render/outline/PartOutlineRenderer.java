package com.kltyton.bedrockruntime.client.render.outline;

import java.util.ArrayList;
import java.util.List;
import com.kltyton.bedrockruntime.client.geometry.bounds.PartBounds;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

/** Batches overlays after entity rendering so capture never invalidates the active model's vertex buffer. */
public final class PartOutlineRenderer {
    private static final int SELECTED_COLOR = 0xFFFFD21F;
    private static final int DAMAGE_FLASH_COLOR = 0x99FF0000;
    private static final double DAMAGE_FLASH_INFLATE = 0.002;
    private static final RenderType OUTLINES = new RenderType("kltyton_bedrock_runtime_lines",
            DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 1536, false, false,
            () -> { RenderType.lines().setupRenderState(); RenderSystem.lineWidth(4); },
            () -> RenderType.lines().clearRenderState()) { };
    private static final List<Outline> PENDING = new ArrayList<>();
    private PartOutlineRenderer() { }

    public static void beginFrame() { PENDING.clear(); }

    public static void submit(MultiBufferSource output, PoseStack pose, PartBounds bounds) {
        submit(pose.last(), bounds, false);
    }

    public static void submitDamageFlash(MultiBufferSource output, PoseStack pose, PartBounds bounds) {
        submit(pose.last(), bounds, true);
    }

    public static void submit(PoseStack.Pose pose, PartBounds bounds, boolean damage) {
        PoseStack copy = new PoseStack();
        copy.last().pose().set(pose.pose());
        copy.last().normal().set(pose.normal());
        PENDING.add(new Outline(copy, bounds, damage));
    }

    public static void flush(MultiBufferSource.BufferSource output) {
        if (PENDING.isEmpty()) { return; }
        try {
            var lines = output.getBuffer(OUTLINES);
            for (Outline outline : PENDING) {
                if (outline.damage()) { continue; }
                PoseStack.Pose pose = outline.pose().last();
                outline.bounds().localShape().forAllEdges((x1, y1, z1, x2, y2, z2) -> {
                    Vector3f normal = new Vector3f((float)(x2-x1), (float)(y2-y1), (float)(z2-z1)).normalize();
                    lines.addVertex(pose.pose(), (float)x1, (float)y1, (float)z1).setColor(SELECTED_COLOR).setNormal(pose, normal.x(), normal.y(), normal.z());
                    lines.addVertex(pose.pose(), (float)x2, (float)y2, (float)z2).setColor(SELECTED_COLOR).setNormal(pose, normal.x(), normal.y(), normal.z());
                });
            }
            output.endBatch(OUTLINES);
            var buffer = output.getBuffer(RenderType.debugQuads());
            for (Outline outline : PENDING) {
                if (!outline.damage()) { continue; }
                PoseStack.Pose pose = outline.pose().last();
                AABB box = outline.bounds().localBounds().inflate(DAMAGE_FLASH_INFLATE);
            quad(buffer, pose, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, box.minX, box.maxY, box.minZ);
            quad(buffer, pose, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, box.maxX, box.maxY, box.maxZ);
            quad(buffer, pose, box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, box.minX, box.maxY, box.maxZ);
            quad(buffer, pose, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, box.maxX, box.maxY, box.minZ);
            quad(buffer, pose, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ);
            quad(buffer, pose, box.minX, box.minY, box.maxZ, box.maxX, box.minY, box.maxZ, box.maxX, box.minY, box.minZ, box.minX, box.minY, box.minZ);
            }
            output.endBatch(RenderType.debugQuads());
        } finally { PENDING.clear(); }
    }

    private record Outline(PoseStack pose, PartBounds bounds, boolean damage) { }

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
