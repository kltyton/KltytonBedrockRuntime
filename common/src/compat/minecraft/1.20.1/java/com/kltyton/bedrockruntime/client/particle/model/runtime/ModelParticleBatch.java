package com.kltyton.bedrockruntime.client.particle.model.runtime;

import com.kltyton.bedrockruntime.client.particle.model.internal.ModelParticleMeshFrame;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/** Render-thread mesh queues; native buffers are acquired only after the billboard draw completes. */
public final class ModelParticleBatch {
    private static final LinkedHashMap<RenderType, Group> GROUPS = new LinkedHashMap<>();

    private ModelParticleBatch() {
    }

    public static void capture(RenderType type, boolean translucent, ModelParticleMeshFrame frame) {
        GROUPS.computeIfAbsent(type, ignored -> new Group(translucent)).frames.add(frame);
    }

    public static void finish() {
        boolean pending = false;
        for (Group group : GROUPS.values()) {
            if (!group.frames.isEmpty()) {
                pending = true;
                break;
            }
        }
        if (!pending) return;
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        RenderSystem.enableCull();
        try {
            for (int pass = 0; pass < 2; pass++) {
                boolean translucent = pass == 1;
                for (var entry : GROUPS.entrySet()) {
                    Group group = entry.getValue();
                    if (group.translucent != translucent || group.frames.isEmpty()) continue;
                    var buffer = buffers.getBuffer(entry.getKey());
                    for (ModelParticleMeshFrame frame : group.frames) {
                        frame.emit((x, y, z, color, u, vv, overlay, light, nx, ny, nz) ->
                                buffer.vertex(x, y, z).color(color).uv(u, vv).overlayCoords(overlay).uv2(light).normal(nx, ny, nz).endVertex());
                    }
                    buffers.endBatch(entry.getKey());
                }
            }
        } finally {
            for (Group group : GROUPS.values()) group.frames.clear();
            if (!cull) RenderSystem.disableCull();
        }
    }

    private static final class Group {
        private final boolean translucent;
        private final ArrayList<ModelParticleMeshFrame> frames = new ArrayList<>();

        private Group(boolean translucent) {
            this.translucent = translucent;
        }
    }
}

