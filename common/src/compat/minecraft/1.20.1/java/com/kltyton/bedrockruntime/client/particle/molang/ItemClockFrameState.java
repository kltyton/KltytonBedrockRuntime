package com.kltyton.bedrockruntime.client.particle.molang;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.math.TrigonometricLookup;

import java.util.concurrent.ThreadLocalRandom;

/** Tracks the actor's clock frame for particle expressions. */
public final class ItemClockFrameState {
    private float rotation;
    private float velocity;

    public int frame(Entity entity) {
        if (entity.isRemoved()) return 0;
        ClientLevel level = (ClientLevel) entity.level();
        float target;
        if (level.dimension() == Level.OVERWORLD) {
            long time = level.getDayTime();
            float day = ((float) (time % 24000L) + 1.0F) / 24000.0F - 0.25F;
            if (day < 0.0F) day += 1.0F;
            if (day > 1.0F) day -= 1.0F;
            float eased = (TrigonometricLookup.cos((float) Math.PI * day) + 1.0F) * -0.5F + 1.0F;
            target = (eased - day) / 3.0F + day;
        } else {
            float random = ThreadLocalRandom.current().nextFloat() * (float) Math.PI;
            target = random + random;
        }

        float difference = target - rotation;
        while (difference < -0.5F) difference += 1.0F;
        while (difference >= 0.5F) difference -= 1.0F;
        velocity = (velocity + Math.max(difference, -1.0F) * 0.1F) * 0.8F;
        rotation += velocity;
        int frame = (int) ((rotation + 1.0F) * 64.0F);
        return Math.floorMod(frame, 64);
    }
}
