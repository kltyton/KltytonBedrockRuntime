# 版本与 Maven 坐标

[English](README.md) | [简体中文](README.zh-CN.md)

八个加载器目标均构建为 `com.kltyton` 组下的 `0.4.0` 版本。common 产物提供编译期 API 类型；加载器产物是运行时模组 JAR。

| Minecraft | 加载器 | Java | 目标工程 | 公共 API 坐标 | 加载器产物坐标 |
| --- | --- | --- | --- | --- | --- |
| 26.2 | Fabric | 25 | [fabric-26.2](../../targets/fabric-26.2) | `com.kltyton:kltyton_bedrock_runtime-common-26.2:0.4.0` | `com.kltyton:kltyton_bedrock_runtime-fabric-26.2:0.4.0` |
| 26.2 | NeoForge | 25 | [neoforge-26.2](../../targets/neoforge-26.2) | `com.kltyton:kltyton_bedrock_runtime-common-26.2:0.4.0` | `com.kltyton:kltyton_bedrock_runtime-neoforge-26.2:0.4.0` |
| 26.1.2 | Fabric | 25 | [fabric-26.1.2](../../targets/fabric-26.1.2) | `com.kltyton:kltyton_bedrock_runtime-common-26.1.2:0.4.0` | `com.kltyton:kltyton_bedrock_runtime-fabric-26.1.2:0.4.0` |
| 26.1.2 | NeoForge | 25 | [neoforge-26.1.2](../../targets/neoforge-26.1.2) | `com.kltyton:kltyton_bedrock_runtime-common-26.1.2:0.4.0` | `com.kltyton:kltyton_bedrock_runtime-neoforge-26.1.2:0.4.0` |
| 1.21.1 | Fabric | 21 | [fabric-1.21.1](../../targets/fabric-1.21.1) | `com.kltyton:kltyton_bedrock_runtime-common-1.21.1:0.4.0` | `com.kltyton:kltyton_bedrock_runtime-fabric-1.21.1:0.4.0` |
| 1.21.1 | NeoForge | 21 | [neoforge-1.21.1](../../targets/neoforge-1.21.1) | `com.kltyton:kltyton_bedrock_runtime-common-1.21.1:0.4.0` | `com.kltyton:kltyton_bedrock_runtime-neoforge-1.21.1:0.4.0` |
| 1.20.1 | Fabric | 17 | [fabric-1.20.1](../../targets/fabric-1.20.1) | `com.kltyton:kltyton_bedrock_runtime-common-1.20.1:0.4.0` | `com.kltyton:kltyton_bedrock_runtime-fabric-1.20.1:0.4.0` |
| 1.20.1 | Forge | 17 | [forge-1.20.1](../../targets/forge-1.20.1) | `com.kltyton:kltyton_bedrock_runtime-common-1.20.1:0.4.0` | `com.kltyton:kltyton_bedrock_runtime-forge-1.20.1:0.4.0` |

Maven 仓库地址为 `https://maven.sighs.cc/repository/maven-public/`。仓库过滤应仅包含目标实际使用的 common 与加载器模块。Fabric 需要匹配版本的 Fabric API 与 Forge Config API Port。GeckoLib 为必需依赖，请安装对应版本。

## 版本 API 差异

| Minecraft | 资源 ID 构造 | 实体持久化 |
| --- | --- | --- |
| 1.20.1 | `new ResourceLocation(namespace, path)` | `CompoundTag` 与 `NbtOps` |
| 1.21.1 | `ResourceLocation.fromNamespaceAndPath(namespace, path)` | `CompoundTag` 与 `NbtOps` |
| 26.1.2、26.2 | `Identifier.fromNamespaceAndPath(namespace, path)` | `ValueInput` 与 `ValueOutput` |

四个版本都通过 `getNamespace()` 读取命名空间。自动物品模型资源在 1.20.1、1.21.1 输出到 `assets/<namespace>/models/item/<item>.json`，在 26.x 输出到 `assets/<namespace>/items/<item>.json`。`ModelShapeCache.buildForElements(JsonArray, Direction)` 及其结果类型仅在 26.2 公开。

| Minecraft | GeckoLib | GeckoLib 包名与渲染回调 | 视野外控制器更新 |
| --- | --- | --- | --- |
| 1.20.1 | 4.8.4 | `software.bernie.geckolib`；`GeoRenderEvent.Entity.CompileRenderLayers` | 以 partial tick `1.0` 调用 `GeoModel.handleAnimations(animatable, instanceId, state)` |
| 1.21.1 | 4.9.2 | `software.bernie.geckolib`；`GeoRenderEvent.Entity.CompileRenderLayers` | 调用 `GeoModel.handleAnimations(animatable, instanceId, state, 1)` |
| 26.1.2 | 5.5.2 | `com.geckolib`；`CompileEntityRenderLayersEvent` 和 `CompileEntityRenderStateEvent` | `EntityRenderDispatcher.extractEntity(entity, 1.0F)` |
| 26.2 | 5.5.3 | `com.geckolib`；`CompileEntityRenderLayersEvent` 和 `CompileEntityRenderStateEvent` | 通常传 `0.0F`；实体冻结时传 `1.0F` |

所有目标都将 GeckoLib 作为必需依赖。跨加载器的实体、注册、数据包和服务端 API 不暴露 GeckoLib 类型。附近被剔除模型的采样与冻结实体的 partial tick 处理仅适用于 26.2；更早目标保持原有捕获行为。

## 构建

每个目标目录都包含自己的 Gradle Wrapper。从仓库根目录进入一个目标后运行构建：

```powershell
cd .\targets\neoforge-26.2
.\gradlew.bat build
```

根控制器还提供该目标的 `build_neoforge_26_2` 和构建全部八个目标的 `buildAllTargets`。产物位于各目标的 `build/libs/`。安装普通模组 JAR，不要安装 `-sources.jar` 或 `-javadoc.jar`。

功能说明、接入示例、配置和 API 文档见[主 README](../../README.zh-CN.md)。
