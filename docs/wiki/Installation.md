# Installation

[English](https://github.com/kltyton/KltytonBedrockRuntime/wiki/Installation) | [简体中文](https://github.com/kltyton/KltytonBedrockRuntime/wiki/Installation.zh-CN) · [Home](https://github.com/kltyton/KltytonBedrockRuntime/wiki/Home)

## Pick the matching build

| Minecraft | Loaders | Java | GeckoLib |
| --- | --- | --- | --- |
| 1.20.1 | Fabric / Forge | 17 | 4.8.4 |
| 1.21.1 | Fabric / NeoForge | 21 | 4.9.2 |
| 26.1.2 | Fabric / NeoForge | 25 | 5.5.2 |
| 26.2 | Fabric / NeoForge | 25 | 5.5.3 |

Use the exact Minecraft/loader pair shown on the file. GeckoLib's version is target-specific; do not mix GeckoLib 4 packages with GeckoLib 5 examples.

## Player installation

1. Install the required loader for the instance.
2. Add the matching GeckoLib file.
3. On Fabric, also install Fabric API and Forge Config API Port.
4. Add the regular KBR JAR and the mod that uses it. For multiplayer, follow that mod's installation requirements for both the client and server.

A regular filename looks like `kltyton_bedrock_runtime-neoforge-26.2-0.4.0.jar`. Do not put common API, source or Javadoc artifacts into `mods/`.

KBR provides a runtime and APIs. The integrating mod supplies its entities, models, animation choices and effects; installing the library cannot infer new animation layers for every other mod.

## Configuration and resource packs

- Client selection display settings use `kltyton_bedrock_runtime-client.toml`. The hold-to-display key is unbound by default; configure it in Controls.
- Server model-shape settings use `kltyton_bedrock_runtime-server.toml`. Use the file created by the loader; Forge 1.20.1 uses the world's `serverconfig/` directory.
- Resource packs can provide model and particle resources at the paths documented in [Models](https://github.com/kltyton/KltytonBedrockRuntime/wiki/Models) and [Particles](https://github.com/kltyton/KltytonBedrockRuntime/wiki/Particles).
- `F3+T` reloads client resources. A client texture or model override does not itself replace server-authoritative block physics.

## For modpack authors

Keep the loader, Minecraft and GeckoLib pairing consistent. Declare KBR and GeckoLib as required dependencies where your content uses them. Do not include old-version SBM or particle-library JARs merely to supply KBR's integrated runtime capabilities.

See [FAQ](https://github.com/kltyton/KltytonBedrockRuntime/wiki/FAQ) for missing-dependency and resource-path problems.
