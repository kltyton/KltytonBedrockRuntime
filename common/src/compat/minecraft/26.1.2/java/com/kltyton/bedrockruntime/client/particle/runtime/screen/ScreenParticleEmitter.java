package com.kltyton.bedrockruntime.client.particle.runtime.screen;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.kltyton.bedrockruntime.client.particle.definition.event.ParticleEffect;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;
import com.kltyton.bedrockruntime.client.particle.runtime.EmitterPreset;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticlePreset;
import org.jetbrains.annotations.Nullable;

/// 屏幕预览专用发射器标记。`MolangParticleEngine` 依靠这个类型把粒子/子发射器路由到
/// [com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleManager] 的独立预览实例，而不是世界实例。
public class ScreenParticleEmitter extends ParticleEmitter {
    public ScreenParticleEmitter(Identifier type, Level level, Vec3 pos, Identifier particleId, MolangExp expression) {
        super(type, level, pos, particleId, expression);
    }

    protected ScreenParticleEmitter(Identifier type, Level level, Vec3 pos, Identifier particleId, MolangExp expression,
                                    boolean deferInitialization) {
        super(type, level, pos, particleId, expression, null, deferInitialization);
    }

    public ScreenParticleEmitter(ParticleEmitter parent, ParticleEffect effect) {
        super(parent, effect);
    }

    protected ScreenParticleEmitter(ParticleEmitter parent, ParticleEffect effect,
                                    @Nullable EmitterPreset preset, @Nullable ParticlePreset particlePreset) {
        super(parent, effect, preset, particlePreset);
    }
}
