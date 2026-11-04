package com.kltyton.bedrockruntime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.ResourceLocation;

/**
 * CN: mod 共享常量。
 * EN: Shared mod constants.
 */
public final class Constants {

    /**
     * CN: 所有 loader 使用的 mod id。
     * EN: Mod identifier used by all loaders.
     */
    public static final String MOD_ID = "kltyton_bedrock_runtime";

    /**
     * CN: 人类可读 mod 名称。
     * EN: Human-readable mod name.
     */
    public static final String MOD_NAME = "KltytonBedrockRuntime";

    /**
     * CN: 共享日志器。
     * EN: Shared logger.
     */
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    private Constants() {
    }

    /**
     * CN: 在当前 mod 命名空间下创建 identifier。
     * EN: Builds an identifier in this mod's namespace.
     *
     * @param path the resource path
     * @return a namespaced identifier
     */
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
