package com.kltyton.bedrockruntime.client.particle.definition.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import com.kltyton.bedrockruntime.client.particle.api.IEventNode;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;


public record SoundEffect(Holder<SoundEvent> soundEffect) implements IEventNode {
    public static final Codec<SoundEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("event_name").forGetter(sound -> sound.soundEffect().value().location())
    ).apply(instance, id -> new SoundEffect(BuiltInRegistries.SOUND_EVENT.get(id)
            .<Holder<SoundEvent>>map(holder -> holder)
            .orElseGet(() -> Holder.direct(SoundEvent.createVariableRangeEvent(id))))));


    @Override
    public void execute(MolangInstance instance) {
        Vec3 position = EventResolver.position(instance);
        instance.getLevel().playLocalSound(position.x, position.y, position.z, soundEffect.value(), SoundSource.AMBIENT, 1.0F, 1.0F, true);
    }
}
