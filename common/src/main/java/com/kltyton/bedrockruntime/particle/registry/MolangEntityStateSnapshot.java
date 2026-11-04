package com.kltyton.bedrockruntime.particle.registry;

import net.minecraft.core.GlobalPos;

import java.util.Optional;
import java.util.UUID;

/** Immutable server-side snapshot delivered to clients for particle Molang queries. */
public record MolangEntityStateSnapshot(int entityId, UUID entityUuid, int flags, int itemUseRemaining,
                                        Optional<GlobalPos> recoveryTarget) {
}
