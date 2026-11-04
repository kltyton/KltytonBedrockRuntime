package com.kltyton.bedrockruntime.client.particle.runtime.screen;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** A preview without caller-specified culling. */
public final class UnboundedFrustum extends Frustum {
    public UnboundedFrustum(Camera camera) {
        super(new Matrix4f(), new Matrix4f());
        Vec3 position = camera.getPosition();
        prepare(position.x, position.y, position.z);
    }

    @Override
    public boolean isVisible(AABB box) {
        return true;
    }
}
