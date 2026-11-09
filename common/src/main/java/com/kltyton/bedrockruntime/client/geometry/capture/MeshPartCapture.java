package com.kltyton.bedrockruntime.client.geometry.capture;

import com.kltyton.bedrockruntime.api.geckolib.state.GeoObbAnimationState;
import com.kltyton.bedrockruntime.client.geometry.bounds.PartBounds;
import com.kltyton.bedrockruntime.client.render.capture.EntityCapturePose;
import com.kltyton.bedrockruntime.geometry.mesh.MeshCaptureData;
import com.kltyton.bedrockruntime.client.selection.model.VisualPartId;
import com.kltyton.bedrockruntime.client.selection.service.BonePartSelectionClient;
import org.joml.Matrix4f;

import java.util.function.BiConsumer;

/** Keeps mesh selection and fitted movement bounds on the existing registered-part pipeline. */
public final class MeshPartCapture {
    private MeshPartCapture() { }

    public static void capture(long entityId, String bone, int firstIndex, MeshCaptureData geometry,
                               Matrix4f pose, GeoObbAnimationState animation,
                               BiConsumer<VisualPartId, PartBounds> accepted) {
        capture(entityId, bone, firstIndex, geometry, pose, animation, null, accepted);
    }

    public static void capture(long entityId, String bone, int firstIndex, MeshCaptureData geometry,
                               Matrix4f pose, GeoObbAnimationState animation, EntityCapturePose capturePose,
                               BiConsumer<VisualPartId, PartBounds> accepted) {
        for (int region = 0; region < geometry.regions().size(); region++) {
            PartBounds bounds = PartBounds.fromMesh(pose, geometry.mesh(), geometry.regions().get(region).localBounds());
            if (bounds != null) {
                VisualPartId id = new VisualPartId("gecko", "entity:" + entityId, bone, firstIndex + region);
                if (BonePartSelectionClient.record(id, bounds, animation, capturePose)) {
                    accepted.accept(id, bounds);
                }
            }
        }
    }
}
