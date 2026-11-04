# Package Layout and Migration / 包结构与迁移

The source tree is grouped by responsibility. Parent packages are namespaces only; implementation classes live in focused leaf packages. The only intentional root classes are mod entrypoints and shared constants. / 源码按职责分类；父包只作为命名空间，具体类位于职责明确的叶子包。根包仅保留 Mod 入口和共享常量。

## Target and Shared Source Layout / 目标与共享源码布局

The root Gradle controller is KltytonTemple `3cfe9ad`. It dispatches builds to eight independent projects under `targets/`; each project has its own Wrapper and loader metadata. Java implementation is shared from `common/src/main` and selected from `common/src/compat` for the active Minecraft family/version and loader. / 根 Gradle 控制器为 KltytonTemple `3cfe9ad`，负责向 `targets/` 下八个独立工程分派构建；每个工程有自己的 Wrapper 和加载器元数据。Java 实现位于 `common/src/main`，并按当前 Minecraft 大版本/版本和加载器从 `common/src/compat` 选取适配层。

```mermaid
flowchart TD
    R[KltytonTemple root controller<br/>per-target tasks and buildAllTargets] --> T[targets/loader-minecraft<br/>independent Gradle project and Wrapper]
    T --> C[common/src/main<br/>shared API and runtime]
    T --> M[common/src/compat/minecraft/family-or-version<br/>Minecraft adapters, including shared legacy GL4 geometry/model classes]
    T --> L[common/src/compat/loader/family-or-version<br/>Fabric, Forge and NeoForge adapters]
    T --> X[common/src/compat/minecraft/post_1_20<br/>shared post-1.20 compatibility layer where selected]
    T --> D[target src/main/resources<br/>loader metadata and target resources]
```

The common build selects the shared, Minecraft family/version, optional `post_1_20`, loader and loader family/version source layers for its target. GeckoLib 4 geometry/model adapters are shared from the `legacy` Minecraft family layer, so 1.20.1 and 1.21.1 do not duplicate the loader and model attachment classes. Loader metadata, plugin configuration, dependency pins, wrapper files and target-specific resources stay in the corresponding `targets/<loader>-<minecraft>` project. The target projects are Fabric and NeoForge for 26.2, 26.1.2 and 1.21.1, plus Fabric and Forge for 1.20.1. / common 构建按目标选择共享、Minecraft 大版本/具体版本、适用时的 `post_1_20`、加载器及加载器大版本/具体版本源码层。加载器元数据、插件配置、依赖版本、Wrapper 文件和目标专用资源保留在对应的 `targets/<loader>-<minecraft>` 工程中。目标组合为 26.2、26.1.2、1.21.1 的 Fabric 与 NeoForge，以及 1.20.1 的 Fabric 与 Forge。

## Public API / 公共 API

- `api.block.shape`, `api.block.shape.geometry`: packaged model extraction, AABB/OBB modes, automatic multicell blocks and convex shape operations / 打包模型提取、AABB/OBB 选择、自动多格方块与凸几何运算。
- `api.block.data`: automatic multipart blockstate and item-model data provider / 自动 multipart blockstate 与物品模型资源生成器。
- `api.bone.attribute`: composable OBB attributes / 可组合 OBB 属性。
- `api.bone.builtin`: built-in model-bone keys / 内置模型骨骼键。
- `api.bone.collision`: collision response policies / 碰撞响应策略。
- `api.bone.key`: stable bone identity / 稳定骨骼标识。
- `api.context.attack`: attack context and origin / 攻击上下文与来源。
- `api.context.collision`: contact context, phase, and kind / 接触上下文、阶段与类别。
- `api.context.damage`: captured vanilla damage data / 捕获的原版伤害数据。
- `api.context.interaction`: part interaction context / 部位交互上下文。
- `api.data.key`: extension-data key and registry infrastructure / 扩展数据键与注册基础设施。
- `api.data.builtin.key`: built-in data keys / 内置数据键。
- `api.data.builtin.health`: built-in per-part health value / 内置逐部位生命值。
- `api.registration.registrar`: fluent registration entrypoints / 链式注册入口。
- `api.registration.selector`: `ALL`, `BASE`, `HUMAN_BASE`, exact, and named selectors / `ALL`、`BASE`、`HUMAN_BASE`、精确与名称选择器。
- `api.registration.definition`: frozen registration definitions / 冻结后的注册定义。
- `api.registration.data`: selector-scoped data definitions / 选择器范围数据定义。
- `api.state.runtime`: tightly coupled entity/bone state and persistence / 紧耦合的实体、骨骼状态与持久化。
- `api.geckolib.entity`: Gecko-independent entity contract and its internal runtime table / 不直接依赖 GeckoLib 类的实体契约及内部运行时表。
- `api.geckolib.state`: public animation snapshot value / 公共动画快照值。
- `api.geckolib.skill.entity|context|handler|registration|event`: Gecko keyframe skill contract, lifecycle context, handlers, registrations, and global events / Gecko 关键帧技能契约、生命周期上下文、处理器、注册与全局事件。
- `api.animation`: explicit `GraphAnimatable` opt-in for animation-graph entities / 动画图实体的显式 `GraphAnimatable` 接入契约。
- `api.geometry.snapshot|query`: immutable server-readable entity/part snapshots and query entrypoints / 服务端可读的不可变实体/部位快照与查询入口。
- `api.entity`, `api.event`: opt-in entity contract and loader-neutral events / 实体接入契约与 loader 无关事件。

## Runtime / 运行时

- `geometry.obb`: OBB transforms, discrete SAT, surface queries, and continuous sweep SAT / OBB 变换、离散 SAT、表面查询与连续 sweep SAT。
- `client.geometry.bounds`: captured visual-cube bounds / 捕获的视觉 cube 边界。
- `client.selection.model`, `client.selection.service`: selection values and orchestration / 选择数据与编排服务。
- `client.render.item`: hand/item-layer capture and first-person world-held pose extraction / 手臂、物品层捕获与第一人称世界持物姿态提取。
- `client.render.outline`, `client.render.vanilla`: render responsibilities / 轮廓与原版模型层。
- `animation.pose`: skeleton bindings, sparse pose buffers, bone masks, and blend composition / 骨架绑定、稀疏姿态缓冲、骨骼遮罩与混合组合。
- `animation.blend`: immutable 1D/2D blend layouts and deterministic boundary handling / 不可变一维/二维混合布局与确定性边界处理。
- `animation.graph`: immutable graph definitions, runtime evaluation, typed pose/velocity nodes, and JSON v1 parsing / 不可变动画图定义、运行时求值、姿态/速度类型节点与 JSON v1 解析。
- `animation.transition`: explicit transition clock and normalized quaternion Hermite pose transition / 显式过渡时钟与归一化四元数 Hermite 姿态过渡。
- `client.compat.geckolib.layer`: GeckoLib render integration / GeckoLib 渲染层。
- `client.compat.geckolib.animation`: GeckoLib 5 graph layer/sampler plus GeckoLib 4 bridges, samplers, and skeleton bindings / GeckoLib 5 动画图层/采样器及 GeckoLib 4 桥接、采样器与骨架绑定。
- `client.compat.geckolib.loading`, `client.compat.geckolib.model`: shared legacy GeckoLib 4 enhanced model reader and bone attachment / 共享 legacy GeckoLib 4 增强模型读取器与骨骼挂接。
- `client.compat.geckolib.skill`, `client.skill.keyframe`: GeckoLib marker extraction, Gecko-independent logical animation ticker, and deferred send queue / GeckoLib marker 提取、不依赖 GeckoLib 的逻辑动画 ticker 与延迟发送队列。
- `network.payload.contact|entity|selection|skill`, `network.protocol`: payloads separated from transport wiring / 按用途拆分的数据包与传输桥。
- `server.combat.attack|damage`, `server.collision.contact|solver`, `server.hook.vanilla`: server hooks and the persistent-manifold impulse solver by gameplay responsibility / 按战斗、伤害、接触、冲量求解和原版挂钩职责拆分。
- `server.sync.contact|snapshot`, `server.network`, `server.selection`: contact deduplication, model snapshots, transport, and selected-part state / 接触去重、模型快照、网络与选择状态。
- `server.skill.keyframe`: marker deduplication, point/window dispatch, and server-tick lifecycle / marker 去重、单点/窗口分发与服务端 tick 生命周期。
- `mixin.block`: model shapes, exact compound intersections and native entity movement integration / 模型形状、凸几何相交与原版实体移动接入。
- `mixin.combat`, `mixin.combat.projectile`, `mixin.entity`, `mixin.entity.player|projectile`: common mixins by target behavior / 按目标行为拆分的公共 Mixin。
- `mixin.client.collision`, `mixin.client.render`: client movement and rendering hooks / 客户端移动与渲染挂钩。
- `mixin.client.geckolib`: scoped GeckoLib model-bake/read, graph composition, mesh render, and GeckoLib 4 animation hooks / 范围受限的 GeckoLib 模型烘焙/读取、图组合、网格渲染与 GeckoLib 4 动画挂钩。
- `client.particle`, `client.particle.definition`, `definition.molang`, `definition.component`, `definition.curve`, `definition.event`, `client.particle.model`: Bedrock particle definitions, Molang compiler/value nodes, emitter and particle components, model-particle descriptors and the public model API / Bedrock 粒子定义、Molang 编译器与值节点、发射器与粒子组件、模型粒子描述和公开模型 API。
- `client.particle.runtime`, `client.particle.bedrock`, `client.particle.platform`, `client.particle.atlas`: emitter simulation, Bedrock resource discovery, loader client bootstrap and particle-atlas revision / 发射器模拟、Bedrock 资源发现、加载器客户端引导和粒子图集版本。
- `particle`, `particle.command`, `particle.network`, `particle.platform`, `particle.registry`: particle types, server command and transport, loader wiring and entity-state tracking / 粒子类型、服务端命令与传输、加载器接线和实体状态跟踪。
- `animation`, `animation.graph`, `animation.pose`, `animation.blend`, `animation.transition`: graph definitions, runtime evaluation and transitions / 动画图定义、运行时求值与过渡。
- Loader-specific hooks and transport wiring are grouped under `common/src/compat/<loader>`. / 加载器专用挂钩和网络接线位于 `common/src/compat/<loader>`。

## Animation Graph Layout / 动画图布局

```mermaid
flowchart LR
    A[GraphAnimatable] --> R[GraphRuntime]
    R --> P[PoseBuffer sparse T/R/S]
    R --> S[AnimationSampler]
    S --> N[Native GeckoLib clip data]
    V[VelocityInputNode] --> R
    C[setVelocityInput copies] --> R
    P --> H[Graph composition hook]
    H --> B[Native bone snapshots]
```

The graph owns its per-instance node buffers, frame output, bindings, and copied velocity inputs. Native controllers remain the owner of controller timelines, keyframe events, and programmatic bone transforms. Velocity nodes are typed separately from pose nodes and are only valid as Hermite velocity references. Inline graph JSON is parsed through `GraphJsonParser`; no automatic graph-resource loader is registered.

## Breaking Package Migration / 破坏性包迁移

The package and artifact migration changes Java imports, the mod identity and Maven coordinates. The current names are Kltyton Bedrock Runtime, `kltyton_bedrock_runtime`, `com.kltyton.bedrockruntime` and `kltyton_bedrock_runtime-*`. No legacy package or artifact-coordinate alias is registered. The current controller-list extension uses entity snapshot payload `kltyton_bedrock_runtime:entity_parts_v3` and NeoForge transport version `3`; server and clients need matching builds. / 包与产物迁移会改变 Java import、模组标识和 Maven 坐标。当前名称为 Kltyton Bedrock Runtime、`kltyton_bedrock_runtime`、`com.kltyton.bedrockruntime` 和 `kltyton_bedrock_runtime-*`；不注册旧包或旧产物坐标别名。本次多控制器扩展使用 `kltyton_bedrock_runtime:entity_parts_v3` 和 NeoForge 传输版本 `3`，两端需使用匹配构建。

| Previous package / 旧包 | New package family / 新包族 |
| --- | --- |
| `api.bone` | `api.bone.attribute|builtin|collision|key` |
| `api.context` | `api.context.attack|collision|damage|interaction` |
| `api.registration` | `api.registration.registrar|selector|definition|data` |
| `api.state` | `api.state.runtime` |
| `api.geckolib` | `api.geckolib.entity|state|skill.*` |
| `client.hitbox`, `client.selection`, `client.render` | `client.geometry.bounds`, `client.selection.model|service`, focused render packages |
| `network` | `network.protocol`, `network.payload.contact|entity|selection|skill` |
| `server`, `server.sync` | focused `server.combat|collision|hook|network|selection|sync` packages |
| root/common client mixin packages | focused `mixin.combat|entity|client` packages |
| `com.kltyton.bonehitboxlib` | `com.kltyton.bedrockruntime` |
| `bonehitboxlib-common-<minecraft>`, `bonehitboxlib-<loader>-<minecraft>` | `kltyton_bedrock_runtime-common-<minecraft>`, `kltyton_bedrock_runtime-<loader>-<minecraft>` |
| `bonehitboxlib-server.toml`, `bonehitboxlib.mixins.json` | `kltyton_bedrock_runtime-server.toml`, `kltyton_bedrock_runtime.mixins.json` |
| `thr_dim_particle` model-particle namespace | `kltyton_bedrock_runtime` (no legacy alias) |
