package com.kltyton.bedrockruntime.client.particle.runtime.screen;

import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleManager;

/// 用于 `net.minecraft.client.gui.screens.Screen` 的粒子预览。
/// 该界面返回的管理器负责独立 tick/渲染粒子，其发射器必须继承自 [ScreenParticleEmitter]。
public interface IParticleManagerScreen {
    MolangParticleManager getManager();
}
