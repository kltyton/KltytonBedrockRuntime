# Animation and Model Dependencies

This page records the dependency and source boundaries for model geometry, GeckoLib integration, animation graphs, particle and model-particle rendering, and poly_mesh handling.

## Target pins

All eight target pins remain unchanged. GeckoLib is required on every target and is not embedded; install the matching GeckoLib release for the selected loader and Minecraft version. Exact dependency versions stay in target metadata so version-specific hooks compile against the intended APIs.

| Target | Minecraft | Java | GeckoLib | Loader |
| --- | --- | --- | --- | --- |
| `fabric-1.20.1` | 1.20.1 | 17 | 4.8.4 | Fabric `0.92.11+1.20.1` |
| `forge-1.20.1` | 1.20.1 | 17 | 4.8.4 | Forge `47.4.22` |
| `fabric-1.21.1` | 1.21.1 | 21 | 4.9.2 | Fabric `0.116.15+1.21.1` |
| `neoforge-1.21.1` | 1.21.1 | 21 | 4.9.2 | NeoForge `21.1.248` |
| `fabric-26.1.2` | 26.1.2 | 25 | 5.5.2 | Fabric `0.155.2+26.1.2` |
| `neoforge-26.1.2` | 26.1.2 | 25 | 5.5.2 | NeoForge `26.1.2.95` |
| `fabric-26.2` | 26.2 | 25 | 5.5.3 | Fabric `0.152.1+26.2` |
| `neoforge-26.2` | 26.2 | 25 | 5.5.3 | NeoForge `26.2.0.6-beta` |

Fabric targets also require the matching Fabric API. Forge/NeoForge configuration uses Forge Config API Port where the target declares it. MixinExtras is used by the scoped GeckoLib model-bake, model-read, render, and graph-composition hooks.

## Particle runtime sources

The particle runtime is an independent implementation inside `client.particle` and its version adapters. It does not load ParticleStorm, ThreeDimensionParticle or any particle-editor JAR at runtime.

| Source | Version | License | Use in KBR |
| --- | --- | --- | --- |
| [ParticleStorm](https://github.com/westernat/ParticleStorm) | 1.5.2 | LGPL-3.0-only | Bedrock effect definitions, emitter simulation, Molang query bindings, resource-pack loading, attachment and rendering behavior |
| [ThreeDimensionParticle](https://github.com/westernat/ThreeDimensionParticle) | 1.2.1 | LGPL-3.0-only | Model-particle appearance, frame selection and the `particle_models` descriptor format |
| [mochafloats](https://github.com/PlayerAnimationLibrary/mochafloats) | 6.1.1 | MIT (upstream) | Lexer, parser and interpreter for particle Molang |

ThreeDimensionParticle source baselines are `f9eab801e014a40c568cd4d37fde2aa9caa00fef` for the Forge 1.20.1 line and `593269a462a7d060227fbb0dbd4c1651cb613e6c` for the NeoForge 1.21.1 line. The particle editor and its assets are not copied. The development examples use self-authored cube geometry and reference existing Minecraft particle sprites.

### mochafloats source build

mochafloats is built from pinned source artifacts rather than the upstream Java 24 binary:

- Source baseline: `dcccb30261b5c06e4958c8566f13300032dfbec5`; upstream version `6.1.1`; derived module version `6.1.1-kbr.1`.
- Modules: `libraries/mochafloats/lexer`, `libraries/mochafloats/parser` and `libraries/mochafloats/runtime`; each target nests the rebuilt modules in its JAR (Fabric nested dependency, Forge/NeoForge Jar-in-Jar).
- Java level: the modules are rebuilt for Java 17. The Minecraft target and GeckoLib keep their own required Java level.
- Isolation: derived classes, artifact groups and automatic module names use `com.kltyton.bedrockruntime.internal.mochafloats`. Original MIT notices are retained. Source patches add resource-version parsing, Bedrock numeric operators, contextual evaluation, sparse values and structured control flow. The upstream bytecode compiler is not included.
- GeckoLib Molang: GeckoLib continues to evaluate animation/model expressions through its own engine. KBR's mochafloats adapter is used for the Bedrock particle definition path only.

The integration additions and patches are LGPL-3.0-only. The MIT upstream license, KBR license texts and source notices ship under `libraries/mochafloats/resources/META-INF` and `common/src/main/resources/META-INF/licenses`.

## GeckoLib API/version layer

GeckoLib is a host dependency, not a bundled runtime:

- GeckoLib 4 (`4.8.4`, `4.9.2`, package `software.bernie.geckolib`) uses `GeoBone`, the native baked model, controllers, `AnimationState`, and version-specific render/vertex APIs. On 1.20.1 the graph sampler restores the original Molang `anim_time` supplier through a narrow `LazyVariableAccessor`; no reflection is involved.
- GeckoLib 5 (`5.5.2`, `5.5.3`, package `com.geckolib`) uses render-state extraction, render-pass bone snapshots, `BakedGeoModel`, and controller state. The graph layer evaluates an owned frame lazily and composes it through the native `createBoneSnapshots` path.

The adapters preserve native cubes, locators, logical bones, controller timelines, keyframe callbacks, and model resources. The graph extension does not advance controllers or emit keyframe events.

## GeckoLib 4 geometry sharing

On GeckoLib 4 targets, the enhanced geometry loader and model attachment classes are shared from the legacy compatibility source layer:

- `client.compat.geckolib.loading.EnhancedGeometryLoader`
- `client.compat.geckolib.loading.NativeModelMeshData`
- `client.compat.geckolib.model.MeshGeoBone`

The model-bake hook always calls the original GeckoLib `BakedModelFactory.constructGeoModel` operation exactly once. For opted-in resources it attaches metadata to the original baked bones and optionally compiles certified groups against those current bones. It does not replace the factory or take over a namespace. The poly_mesh reader uses a resource-local Gson clone with a corrected `PolyMesh` adapter while preserving the original Gson read.

## Resource paths

GeckoLib 4 targets (`1.20.1`, `1.21.1`):

```text
assets/<namespace>/geo/<path>.geo.json
assets/<namespace>/animations/<path>.animation.json
```

GeckoLib 5 targets (`26.1.2`, `26.2`):

```text
assets/<namespace>/geckolib/models/<path>.geo.json
assets/<namespace>/geckolib/animations/<path>.animation.json
```

`EnhancedModels.registerResource` takes the full resource ID including folder and extension. Inline animation-graph JSON is parsed through `GraphJsonParser` from a Java string or `JsonElement`; there is no automatic standalone graph-resource loader. A caller that ships optional graph JSON can parse it during setup or reload.

Particle resources use the same paths on every target:

```text
assets/<namespace>/particle_definitions/<id>.json
assets/<namespace>/particle_models/<id>.json
assets/<namespace>/models/<path>.json
```

The `geometry` descriptor variant resolves its `model_id` path to the normal model JSON location. `geckolib` and `poly_mesh` descriptors use the already-baked GeckoLib model (with the enhanced mesh data for `poly_mesh`) and require that model to be available in the baked-model cache at reload time. Particle definitions, descriptors and textures are ordinary resources and are not produced by DataGen.

## Host libraries

The implementation uses host/runtime APIs rather than vendored copies:

- JOML from the host dependency graph for matrices, vectors, and quaternions. `PoseBlender` uses host JOML nlerp; the shortest-path `q`/`-q` behavior was source-verified against JOML 1.10.5.
- Gson for graph JSON and model metadata parsing.
- On 1.20.1, GeckoLib's existing `mclib:20` runtime is also declared `compileOnly` for its exposed keyframe and MoLang value types; no additional copy is packaged.
- Minecraft `VertexConsumer`, `PoseStack`, bounds, and resource types.
- GeckoLib public model, bone, renderer, cache, and controller APIs.
- MixinExtras for scoped loader/mod-render hooks.

No new runtime dependency is introduced by the ANIMATION graph extension.

## MAE attribution

MAE 1.1.4 remains a design and algorithm reference and is MIT-licensed, Copyright (c) 2024 MaydayMemory. The current implementation has no MAE imports, calls no MAE methods, and does not embed or bundle an MAE jar or its runtime libraries. The MIT notice is retained for attribution. Current animation math uses host JOML plus repository-owned quaternion Hermite and mixing code.

## Licenses and attribution

- Kltyton Bedrock Runtime remains `LGPL-3.0-only`; see the repository `LICENSE` and `COPYING`.
- MAE 1.1.4 is MIT-licensed. Its notice is retained as an algorithm/design reference; there is no embedded dependency.
- ParticleStorm 1.5.2 and ThreeDimensionParticle 1.2.1 are LGPL-3.0-only upstream references. Their notices are retained; their JARs and editor assets are not bundled.
- mochafloats 6.1.1 is MIT-licensed upstream. The rebuilt source modules retain the MIT notice; KBR patches and integration additions are LGPL-3.0-only.
- GeckoLib is an upstream MIT-licensed project and remains a required host dependency. Consult the GeckoLib artifact used by each target for its exact license text.
- SimpleBedrockModel's Tree/Baked/poly_mesh design is used as a reference. Upstream SimpleBedrockModel is LGPL-3.0; no proprietary assets are copied.
- Third-party project names and licenses belong to their respective owners.

## Registration boundary

Register enhanced model resources and consumers on the common/server side where trusted mesh rays are needed:

```java
String resource = "examplemod:geckolib/models/entity/kltyton_bedrock_runtime/example_humanoid.geo.json";
ModelConsumers consumers = ModelConsumers.builder()
        .add(ModelConsumer.ANIMATION, "head")
        .build();
EnhancedModels.registerResource(resource, ModModelRegistration.class);
EnhancedModels.registerConsumers(resource, consumers);
EnhancedModels.bindEntity(EntityType.ZOMBIE, resource);
```

`bindEntity` lets server geometry resolution associate an entity type with registered `MeshCaptureData`. Without that binding, a reported client OBB is retained; the exact mesh triangle query is not reconstructed automatically on the server.

## Compatibility notes

- The eight target projects share common code but not loader metadata or dependency pins.
- GeckoLib 4 and GeckoLib 5 use different package names, model classes, and render-entry types; do not mix imports across targets.
- `EnhancedModelData` carries generation, mesh definitions, capture regions, Tree/Baked mode, static/preserved bone declarations, and double-sided declaration.
- `MeshCaptureData` is resource-time collision fitting shared by all instances; it is not rebuilt from the render pose.
- The poly_mesh parser accepts simple planar polygons and rejects holes, self-intersections, nonplanar input, invalid indices, non-finite coordinates, and unsupported input with resource/bone diagnostics.
- Exact mesh rays and OBB motion collision are separate contracts. Movement collision remains OBB SAT/sweep based.
- Animation graph value kinds are validated: `VelocityInputNode` is only valid as a Hermite velocity reference, not as a final pose or ordinary input.
- Model-particle resources use the `kltyton_bedrock_runtime` namespace for `model_particle` and `model_appearance`. The old ThreeDimensionParticle `thr_dim_particle` namespace is not registered as an alias.
