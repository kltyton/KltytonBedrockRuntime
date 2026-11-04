package com.kltyton.bedrockruntime.geometry.mesh;

import com.kltyton.bedrockruntime.geometry.mesh.MeshFittingOptions;
import com.kltyton.bedrockruntime.geometry.mesh.MeshObbFitter;
import com.kltyton.bedrockruntime.geometry.mesh.MeshObbRegion;
import com.kltyton.bedrockruntime.geometry.mesh.TriangleMesh;

import java.util.List;

/** Resource-time collision fitting, shared by all instances of the mesh. */
public record MeshCaptureData(TriangleMesh mesh, List<MeshObbRegion> regions) {
    public MeshCaptureData {
        regions = List.copyOf(regions);
    }

    public static MeshCaptureData of(TriangleMesh mesh) {
        return new MeshCaptureData(mesh, MeshObbFitter.fit(mesh,
                new MeshFittingOptions(8, 64, 1.0 / 64.0, 0)).regions());
    }
}
