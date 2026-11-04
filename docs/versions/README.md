# Versions and Maven coordinates

[English](README.md) | [简体中文](README.zh-CN.md)

All eight loader targets build version `0.4.0` under `com.kltyton`. The common artifact exposes compile-time API types; the loader artifact is the runtime mod JAR.

| Minecraft | Loader | Java | Target project | Common API coordinate | Loader artifact coordinate |
| --- | --- | --- | --- | --- | --- |
| 26.2 | Fabric | 25 | [fabric-26.2](../../targets/fabric-26.2) | `com.kltyton:kltyton_bedrock_runtime-common-26.2:0.4.0` | `com.kltyton:kltyton_bedrock_runtime-fabric-26.2:0.4.0` |
| 26.2 | NeoForge | 25 | [neoforge-26.2](../../targets/neoforge-26.2) | `com.kltyton:kltyton_bedrock_runtime-common-26.2:0.4.0` | `com.kltyton:kltyton_bedrock_runtime-neoforge-26.2:0.4.0` |
| 26.1.2 | Fabric | 25 | [fabric-26.1.2](../../targets/fabric-26.1.2) | `com.kltyton:kltyton_bedrock_runtime-common-26.1.2:0.4.0` | `com.kltyton:kltyton_bedrock_runtime-fabric-26.1.2:0.4.0` |
| 26.1.2 | NeoForge | 25 | [neoforge-26.1.2](../../targets/neoforge-26.1.2) | `com.kltyton:kltyton_bedrock_runtime-common-26.1.2:0.4.0` | `com.kltyton:kltyton_bedrock_runtime-neoforge-26.1.2:0.4.0` |
| 1.21.1 | Fabric | 21 | [fabric-1.21.1](../../targets/fabric-1.21.1) | `com.kltyton:kltyton_bedrock_runtime-common-1.21.1:0.4.0` | `com.kltyton:kltyton_bedrock_runtime-fabric-1.21.1:0.4.0` |
| 1.21.1 | NeoForge | 21 | [neoforge-1.21.1](../../targets/neoforge-1.21.1) | `com.kltyton:kltyton_bedrock_runtime-common-1.21.1:0.4.0` | `com.kltyton:kltyton_bedrock_runtime-neoforge-1.21.1:0.4.0` |
| 1.20.1 | Fabric | 17 | [fabric-1.20.1](../../targets/fabric-1.20.1) | `com.kltyton:kltyton_bedrock_runtime-common-1.20.1:0.4.0` | `com.kltyton:kltyton_bedrock_runtime-fabric-1.20.1:0.4.0` |
| 1.20.1 | Forge | 17 | [forge-1.20.1](../../targets/forge-1.20.1) | `com.kltyton:kltyton_bedrock_runtime-common-1.20.1:0.4.0` | `com.kltyton:kltyton_bedrock_runtime-forge-1.20.1:0.4.0` |

The Maven repository is `https://maven.sighs.cc/repository/maven-public/`. Keep repository filters scoped to the exact common and loader modules used by the target. Fabric requires its matching Fabric API and Forge Config API Port. GeckoLib is required on every target; install the matching version.

## Version-specific API differences

| Minecraft | Resource ID construction | Entity persistence |
| --- | --- | --- |
| 1.20.1 | `new ResourceLocation(namespace, path)` | `CompoundTag` and `NbtOps` |
| 1.21.1 | `ResourceLocation.fromNamespaceAndPath(namespace, path)` | `CompoundTag` and `NbtOps` |
| 26.1.2, 26.2 | `Identifier.fromNamespaceAndPath(namespace, path)` | `ValueInput` and `ValueOutput` |

All four versions expose the namespace through `getNamespace()`. The generated item-model path is `assets/<namespace>/models/item/<item>.json` on 1.20.1 and 1.21.1, and `assets/<namespace>/items/<item>.json` on 26.x. `ModelShapeCache.buildForElements(JsonArray, Direction)` and its result types are public only on 26.2.

| Minecraft | GeckoLib | GeckoLib package and render callbacks | Off-screen controller update |
| --- | --- | --- | --- |
| 1.20.1 | 4.8.4 | `software.bernie.geckolib`; `GeoRenderEvent.Entity.CompileRenderLayers` | `GeoModel.handleAnimations(animatable, instanceId, state)` at partial tick `1.0` |
| 1.21.1 | 4.9.2 | `software.bernie.geckolib`; `GeoRenderEvent.Entity.CompileRenderLayers` | `GeoModel.handleAnimations(animatable, instanceId, state, 1)` |
| 26.1.2 | 5.5.2 | `com.geckolib`; `CompileEntityRenderLayersEvent` and `CompileEntityRenderStateEvent` | `EntityRenderDispatcher.extractEntity(entity, 1.0F)` |
| 26.2 | 5.5.3 | `com.geckolib`; `CompileEntityRenderLayersEvent` and `CompileEntityRenderStateEvent` | `EntityRenderDispatcher.extractEntity(entity, 0.0F)` normally and `1.0F` for frozen entities |

GeckoLib is required on every target. The neutral entity, registration, packet and server APIs do not expose GeckoLib types. Nearby culled-model sampling and frozen-entity partial-tick handling are 26.2-specific; earlier targets keep their previous capture behavior.

## Build

Each target directory contains its own Gradle Wrapper. From the repository root, build a target directly or use the KltytonTemple root controller:

```powershell
cd .\targets\neoforge-26.2
.\gradlew.bat build
```

The root controller also provides `build_neoforge_26_2` for that target and `buildAllTargets` for all eight targets. Build outputs are under each target's `build/libs/`. Install the regular mod JAR, not `-sources.jar` or `-javadoc.jar`.

See the main [README](../../README.md) for feature descriptions, integration examples, configuration, API documentation and acknowledgements.
