package com.kltyton.bedrockruntime.server.sync.contact;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

final class ServerObbGroundCompat {
    private ServerObbGroundCompat() { }

    static void setOnGround(Entity entity, Vec3 movement) {
        entity.setOnGroundWithMovement(true, entity.horizontalCollision, movement);
    }
}
