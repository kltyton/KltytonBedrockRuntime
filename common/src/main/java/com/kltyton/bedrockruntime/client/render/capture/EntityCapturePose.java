package com.kltyton.bedrockruntime.client.render.capture;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Root reference sampled at the same interpolation time as the captured model. */
public record EntityCapturePose(Vec3 position, float yaw) {
    public static EntityCapturePose sample(Entity entity, float partialTick) {
        return new EntityCapturePose(entity.getPosition(partialTick),
                Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot()));
    }
}
