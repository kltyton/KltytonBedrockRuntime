# Kltyton Bedrock Runtime

**Bedrock models, layered animation, particles and model-part interaction, built on GeckoLib.**

Kltyton Bedrock Runtime (KBR) is a library for mods that need more control over animated models and effects. It keeps GeckoLib's model resources, renderers and animation ecosystem, and adds animation graphs, polygon geometry, Bedrock particle effects and model-derived selection and collision.

## For players and modpack authors

Install KBR when a mod in your pack requires it. The creatures, equipment and effects that use these APIs are supplied by the mods using the library.

1. Choose the file for your exact Minecraft version and loader.
2. Install the matching **GeckoLib** release. Fabric also requires **Fabric API** and **Forge Config API Port**.
3. Install the regular KBR JAR in the appropriate instance. Files ending in `-sources.jar` or `-javadoc.jar` are developer resources.

KBR does not require a separate installation of SimpleBedrockModel, ParticleStorm or ThreeDimensionParticle.

## What developers can build

- **Continuous animation blending:** combine idle, walk and run with a 1D BlendSpace, or drive directional movement and aiming with a 2D BlendSpace.
- **Layered poses:** target individual bones or entire subtrees with masks, combine ordinary and additive poses, and use optional Hermite transitions with explicit endpoint velocities.
- **Tree and Baked models:** retain logical bones and locators while organizing geometry and explicitly identifying static structures that can be compiled together.
- **Bedrock `poly_mesh`:** use polygon geometry alongside cubes, attach it to animated bones, and use mesh triangles for selection and ray queries.
- **Bedrock particles and Molang:** load JSON effect definitions, expressions, curves and events. Render billboards or model particles using cube, GeckoLib or enhanced polygon geometry.
- **Model-part selection and collision:** register bones, select and highlight parts, handle attack and contact events, store per-part data, and query recent geometry on the server. Movement collision uses oriented bounding boxes (OBBs).
- **Model-based blocks:** use model-derived AABB/OBB shapes and automatic multi-block structures through the version-specific block APIs.

Enhanced animation and model features are enabled explicitly by the consuming mod. Existing GeckoLib animation files remain the clip source; GeckoLib continues to handle its own animation Molang and keyframe callbacks. KBR's particle Molang follows the particle runtime path.

## Builds

| Minecraft | Loaders | Java | GeckoLib baseline |
| --- | --- | --- | --- |
| 1.20.1 | Fabric, Forge | 17 | 4.8.4 |
| 1.21.1 | Fabric, NeoForge | 21 | 4.9.2 |
| 26.1.2 | Fabric, NeoForge | 25 | 5.5.2 |
| 26.2 | Fabric, NeoForge | 25 | 5.5.3 |

Choose the matching file and its declared dependencies. The Wiki identifies version-specific APIs.

## Documentation and development

Start with the [Wiki](https://github.com/kltyton/KltytonBedrockRuntime/wiki), including [installation](https://github.com/kltyton/KltytonBedrockRuntime/wiki/Installation) and [developer setup](https://github.com/kltyton/KltytonBedrockRuntime/wiki/Getting-Started).

Source code and issue tracking are available in the [project repository](https://github.com/kltyton/KltytonBedrockRuntime). Developer dependencies use the `com.kltyton` Maven group and `com.kltyton.bedrockruntime` Java package.

## License and credits

KBR is licensed under **LGPL-3.0-only**. It uses GeckoLib as a required external dependency and draws on the model, animation and particle work in SimpleBedrockModel, MAE, ParticleStorm, ThreeDimensionParticle and mochafloats. Original notices and license information are retained in the source and distribution.
