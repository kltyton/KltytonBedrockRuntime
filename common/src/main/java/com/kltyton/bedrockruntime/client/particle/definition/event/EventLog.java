package com.kltyton.bedrockruntime.client.particle.definition.event;

import com.mojang.serialization.Codec;
import net.minecraft.world.phys.Vec3;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.IEventNode;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;

public record EventLog(String log) implements IEventNode {
    public static final Codec<EventLog> CODEC = Codec.STRING.xmap(EventLog::new, EventLog::log);

    @Override
    public void execute(MolangInstance instance) {
        if (this.log.isEmpty()) return;
        Vec3 position = EventResolver.position(instance);
        BedrockParticles.LOGGER.info("{}[{}]: {}", instance.getIdentity(), position, log);
    }
}
