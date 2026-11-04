package com.kltyton.bedrockruntime.compat.geckolib;

/**
 * CN: GeckoLib 可选依赖检测工具，不直接引用 GeckoLib 类型。
 * EN: GeckoLib optional-dependency detector that does not directly reference GeckoLib types.
 */
public final class GeckoLibCompat {
    private static final boolean LOADED = classExists("software.bernie.geckolib.animatable.GeoEntity");

    private GeckoLibCompat() {
    }

    public static boolean isLoaded() {
        return LOADED;
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className, false, GeckoLibCompat.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }
}
