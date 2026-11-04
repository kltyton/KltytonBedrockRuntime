package com.kltyton.bedrockruntime.client.model.mesh;

/** Vertex submission shared by the version-specific Minecraft vertex consumers. */
@FunctionalInterface
public interface MeshVertexSink {
    void vertex(float x, float y, float z, int color, float u, float v,
                int overlay, int light, float normalX, float normalY, float normalZ);
}
