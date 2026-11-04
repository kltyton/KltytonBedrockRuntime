package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

final class ActorQueryPosition {
    private ActorQueryPosition() {}

    static float x(Entity entity) {
        return (float) entity.getX();
    }

    static float y(Entity entity) {
        float y = (float) entity.getY();
        if (entity instanceof Player player) {
            y += player.isSleeping() && player.hasPose(Pose.SLEEPING) ? 0.2F : 1.62001F;
        } else if (entity instanceof Boat) {
            y += 0.375F;
        } else if (entity instanceof AbstractMinecart || entity instanceof ItemEntity
                || entity instanceof FallingBlockEntity || entity instanceof PrimedTnt) {
            y += entity.getBbHeight() * 0.5F;
        }
        return y;
    }

    static float z(Entity entity) {
        return (float) entity.getZ();
    }
}
