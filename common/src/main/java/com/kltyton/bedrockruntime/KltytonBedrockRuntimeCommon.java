package com.kltyton.bedrockruntime;

import com.kltyton.bedrockruntime.api.data.builtin.key.ObbBuiltinDataKeys;
import com.kltyton.bedrockruntime.server.combat.damage.ObbPartHealthHooks;

/**
 * CN: 支持的 loader 共享的 common 初始化入口。
 * EN: Common bootstrap shared by supported loaders.
 */
public final class KltytonBedrockRuntimeCommon {

    private KltytonBedrockRuntimeCommon() {
    }

    /**
     * CN: 初始化 common mod 状态。
     * EN: Initializes common mod state.
     */
    public static void init() {
        com.kltyton.bedrockruntime.api.block.shape.VanillaBlockShapes.initialize();
        ObbBuiltinDataKeys.init();
        ObbPartHealthHooks.register();
        Constants.LOG.info("{} initialized.", Constants.MOD_NAME);
    }
}
