package com.kltyton.bedrockruntime.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Minecraft 1.20.1 includes the camera view in entity poses; geometry stores camera-relative world axes. */
public final class WorldRenderPose {
    private static final ThreadLocal<View> ACTIVE = new ThreadLocal<>();

    private WorldRenderPose() { }

    public static void begin(PoseStack.Pose pose) {
        ACTIVE.set(new View(new Matrix4f(pose.pose()), new Matrix3f(pose.normal()),
                new Matrix4f(pose.pose()).invert()));
    }

    public static void end() { ACTIVE.remove(); }

    public static Matrix4f cameraRelative(Matrix4f pose) {
        View view = ACTIVE.get();
        return view == null ? new Matrix4f(pose) : new Matrix4f(view.inverse()).mul(pose);
    }

    public static PoseStack poseStack() {
        PoseStack pose = new PoseStack();
        View view = ACTIVE.get();
        if (view != null) {
            pose.last().pose().set(view.pose());
            pose.last().normal().set(view.normal());
        }
        return pose;
    }

    private record View(Matrix4f pose, Matrix3f normal, Matrix4f inverse) { }
}
