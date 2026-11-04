package com.kltyton.bedrockruntime.client.particle.runtime.screen;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** A preview without caller-specified culling. */
public final class UnboundedFrustum extends Frustum {
    public UnboundedFrustum(Camera camera) {
        super(new Matrix4f(), new Matrix4f());
        Vec3 position = camera.position();
        prepare(position.x, position.y, position.z);
    }

    @Override public boolean isVisible(AABB box) { return true; }
    @Override public boolean pointInFrustum(double x, double y, double z) { return true; }
    @Override public int cubeInFrustum(BoundingBox box) { return -2; }
}
