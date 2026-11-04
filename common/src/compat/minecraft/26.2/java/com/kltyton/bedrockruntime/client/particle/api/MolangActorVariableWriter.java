package com.kltyton.bedrockruntime.client.particle.api;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/** Writes a public Molang variable owned by an entity integration. */
@FunctionalInterface
public interface MolangActorVariableWriter {
    /** Returns whether this integration accepted and applied the write. */
    boolean write(Entity entity, String name, @Nullable Object value);
}
