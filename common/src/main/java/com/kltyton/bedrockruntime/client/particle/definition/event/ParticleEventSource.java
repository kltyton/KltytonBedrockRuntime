package com.kltyton.bedrockruntime.client.particle.definition.event;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Supplies the triggering particle's world-space sample to its event and child effects. */
public interface ParticleEventSource {
    Vec3 eventPosition();

    Vector3f eventVelocity();
}
