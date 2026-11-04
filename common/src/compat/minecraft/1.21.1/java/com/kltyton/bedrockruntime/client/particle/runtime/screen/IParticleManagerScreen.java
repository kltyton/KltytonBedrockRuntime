package com.kltyton.bedrockruntime.client.particle.runtime.screen;

import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleManager;

/// Used by `net.minecraft.client.gui.screens.Screen` particle previews.
/// The returned manager owns ticking/rendering for the preview; its emitters must be
/// [ScreenParticleEmitter] instances.
public interface IParticleManagerScreen {
    MolangParticleManager getManager();
}
