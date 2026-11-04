# 安装

[English](https://github.com/kltyton/KltytonBedrockRuntime/wiki/Installation) | [简体中文](https://github.com/kltyton/KltytonBedrockRuntime/wiki/Installation.zh-CN) · [首页](https://github.com/kltyton/KltytonBedrockRuntime/wiki/Home.zh-CN)

## 选择对应构建

| Minecraft | 加载器 | Java | GeckoLib |
| --- | --- | --- | --- |
| 1.20.1 | Fabric / Forge | 17 | 4.8.4 |
| 1.21.1 | Fabric / NeoForge | 21 | 4.9.2 |
| 26.1.2 | Fabric / NeoForge | 25 | 5.5.2 |
| 26.2 | Fabric / NeoForge | 25 | 5.5.3 |

文件的 Minecraft 与加载器必须和实例一致。GeckoLib 按目标分别适配；GeckoLib 4 的包名和 GeckoLib 5 的示例不能混用。

## 玩家安装步骤

1. 为实例安装所需加载器。
2. 加入对应版本的 GeckoLib。
3. Fabric 还需安装 Fabric API 与 Forge Config API Port。
4. 加入普通 KBR JAR 和使用它的模组。多人游戏按内容模组要求配置客户端与服务端。

普通文件名形如 `kltyton_bedrock_runtime-neoforge-26.2-0.4.0.jar`。common API、源码包和 Javadoc 包不应放入 `mods/`。

KBR 提供运行能力和 API。实体、模型、动作选择及特效由接入模组提供；安装库本身不会推导出其他所有模组所需的动画分层。

## 配置与资源包

- 客户端选择显示使用 `kltyton_bedrock_runtime-client.toml`。按住显示的键位默认未绑定，可在控制设置中配置。
- 服务端模型形状使用 `kltyton_bedrock_runtime-server.toml`，以加载器实际生成的位置为准；Forge 1.20.1 位于世界的 `serverconfig/` 目录。
- 资源包可按[模型](https://github.com/kltyton/KltytonBedrockRuntime/wiki/Models.zh-CN)和[粒子](https://github.com/kltyton/KltytonBedrockRuntime/wiki/Particles.zh-CN)页面的路径提供资源。
- `F3+T` 可重载客户端资源。客户端的贴图或模型替换不会自行替换服务端方块物理。

## 整合包作者

保持加载器、Minecraft 与 GeckoLib 的版本配对。内容模组使用本库时，应声明 KBR 与 GeckoLib 的必需依赖。不需要为了提供 KBR 已整合的运行能力而额外放入旧版 SBM 或粒子库 JAR。

缺少前置、资源路径错误等问题见[常见问题](https://github.com/kltyton/KltytonBedrockRuntime/wiki/FAQ.zh-CN)。
