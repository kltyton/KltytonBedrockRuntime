# FAQ and troubleshooting

[English](https://github.com/kltyton/KltytonBedrockRuntime/wiki/FAQ) | [简体中文](https://github.com/kltyton/KltytonBedrockRuntime/wiki/FAQ.zh-CN) · [Home](https://github.com/kltyton/KltytonBedrockRuntime/wiki/Home)

## Dependencies and scope

**GeckoLib is required.** KBR extends its resource and renderer ecosystem. Install the release belonging to your Minecraft/loader target, not an arbitrary GeckoLib major version.

KBR supplies runtime APIs. New creatures, animation layers and authored particle effects come from the integrating mod or its resources. KBR does not replace every installed mod's models or turn arbitrary Bedrock add-ons into Java gameplay content.

The visual particle editor is not included. You do not need to install donor projects' old-version mod JARs to supply KBR's integrated runtime.

## Resources and animation

| Symptom | What to check |
| --- | --- |
| Model not found | Full resource ID, namespace, and GeckoLib 4 `geo/` versus GeckoLib 5 `geckolib/models/` path |
| Clip not found | The animation node's `animation` value must be a key in the GeckoLib animation file |
| Inline graph not found | Model registration, `kltyton_bedrock_runtime.graph` and the current baked resource |
| Graph reference error | Node names, output, cycles and `velocity_input` used only as a Hermite velocity |
| A masked bone is rejected | Real bone names and the current skeleton generation |
| Geometry visible from only one side | Authored winding/normals and the model's explicit `double_sided` choice |
| Baked mode does not merge a group | `static_bones`, preserved/consumed bones, and at least two eligible geometry bones |
| Particle not found | `particle_definitions/<id>.json` and `description.identifier` |
| Model particle is missing | Descriptor ID, `model_id`, texture sprite and the chosen variant's registered/baked geometry |

Client resources reload with F3+T. Capture game parameters before deferred rendering; do not read mutable world state from repeated bone draw callbacks.

## Events and collision

Graph sampling produces poses. Keep sound, particle and custom instruction events in the native GeckoLib handler, and append the KBR keyframe bridge where skills are used. Do not emit gameplay actions from each sample or render pass.

For part queries, implement `BoneHitboxEntity` and register matching selectors. The selection display is opt-in, and not every entity in the game implements the contract.

Mesh ray selection and movement collision are separate: enhanced `poly_mesh` can provide triangle hits, while motion uses OBBs. Register server model data/entity bindings when exact mesh lookup is needed.

Server geometry is based on recent client poses. An empty snapshot means no current report is available. Skills and damage still need server-owned authorization.

## Versions and migration

KBR uses `kltyton_bedrock_runtime`, `com.kltyton.bedrockruntime` and Maven artifacts beginning with `kltyton_bedrock_runtime-`. It does not publish aliases for the former BoneHitboxLib package or coordinates. Consumers update their imports, dependency and resource declarations.

Minecraft 26.x uses `Identifier`, 1.21.1 uses `ResourceLocation.fromNamespaceAndPath`, and 1.20.1 uses the `ResourceLocation` constructor. GeckoLib 4 uses `software.bernie.geckolib`; GeckoLib 5 uses `com.geckolib`. Version-specific APIs are listed on the corresponding page.

## Reporting an issue

Use the [issue tracker](https://github.com/kltyton/KltytonBedrockRuntime/issues). Include your Minecraft, loader, Java, KBR and GeckoLib versions; the relevant resource ID; a short reproduction; and the associated log or resource error. Remove credentials and unrelated private data.

## License and source acknowledgements

KBR is LGPL-3.0-only. GeckoLib remains an external dependency. ParticleStorm and ThreeDimensionParticle provide particle-runtime reference work; mochafloats provides MIT-licensed lexer/parser/interpreter sources adapted for the particle path. SimpleBedrockModel's model concepts and MAE's animation work are also acknowledged.

See the [source and license notes](https://github.com/kltyton/KltytonBedrockRuntime/blob/main/docs/dependencies/animation-models.md) and [project license](https://github.com/kltyton/KltytonBedrockRuntime/blob/main/LICENSE).
