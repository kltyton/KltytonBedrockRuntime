package com.kltyton.bedrockruntime.client.particle.molang;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import com.kltyton.bedrockruntime.client.particle.access.IMolangEntityState;

import java.util.concurrent.ThreadLocalRandom;

/** Tracks an actor's compass frame for particle expressions. */
public final class ItemCompassFrameState {
    private final Direction direction = new Direction();
    private int lastUpdatedAt;

    public int frame(Entity entity, boolean recovery) {
        if (entity.isRemoved()) return 0;
        if (lastUpdatedAt == entity.tickCount) return direction.frame;
        lastUpdatedAt = entity.tickCount;
        ClientLevel level = (ClientLevel) entity.level();
        BlockPos destination = null;
        if (recovery && entity instanceof Player player) {
            GlobalPos lastDeath = player.getLastDeathLocation().orElse(null);
            if (!player.isLocalPlayer() && entity instanceof IMolangEntityState state && state.kltyton_bedrock_runtime$recoveryStateKnown()) {
                lastDeath = state.kltyton_bedrock_runtime$recoveryTarget();
            }
            if (lastDeath != null && lastDeath.dimension() == level.dimension()) destination = lastDeath.pos();
        } else if (!recovery && level.dimension() == Level.OVERWORLD) {
            destination = level.getSharedSpawnPos();
        }
        if (destination != null) {
            float x = (float) destination.getX() - (float) entity.getX();
            float z = (float) destination.getZ() - (float) entity.getZ();
            direction.point(x, z, (int) entity.getYRot());
        } else {
            direction.spin();
        }
        return direction.frame;
    }

    static final class Direction {
        private static final float PI = (float) Math.PI;
        private static final float TWO_PI = PI * 2.0F;
        private float rotation;
        private float velocity;
        private int frame;

        void point(float x, float z, int yawDegrees) {
            float bearing = (float) Math.atan2(z, x);
            float yaw = (yawDegrees % 360 - 90) * PI / 180.0F;
            advance(-(yaw - bearing));
        }

        void spin() {
            float random = ThreadLocalRandom.current().nextFloat() * PI;
            advance(random + random);
        }

        void copyFrom(Direction other) {
            rotation = other.rotation;
            velocity = other.velocity;
            frame = other.frame;
        }

        int frame() {
            return frame;
        }

        private void advance(float target) {
            float difference = (target - rotation + PI) % TWO_PI;
            if (difference < 0.0F) difference += TWO_PI;
            difference -= PI;
            difference = Math.max(-1.0F, Math.min(1.0F, difference));
            velocity = (velocity + difference * 0.1F) * 0.8F;
            rotation += velocity;
            frame = Math.floorMod((int) ((rotation / TWO_PI + 1.0F) * 32.0F), 32);
        }
    }
}
