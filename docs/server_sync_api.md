# Registration, Sync, and Events / 注册、同步与事件

## Required Opt-In / 强制接入

EN: Only entities implementing `BoneHitboxEntity` can own Kltyton Bedrock Runtime OBBs. The interface has no global enabled switch. Implementors must override `kltyton_bedrock_runtime$registerObbBones` and register at least one selector.

CN：只有实现 `BoneHitboxEntity` 的实体才能拥有 Kltyton Bedrock Runtime OBB。接口不再提供全局启用开关；实现者必须重写 `kltyton_bedrock_runtime$registerObbBones` 并至少注册一个选择器。

```java
@Override
public void kltyton_bedrock_runtime$registerObbBones(ObbBoneRegistrar registrar) {
    // CN: 六个普通 OBB；无需显式注册 HURT。EN: Six ordinary OBBs; HURT registration is not required.
    registrar.register(ObbBoneRegistrar.BASE);

    // CN: 永久攻击和硬碰撞属性仍可显式注册。EN: Permanent attack and hard-collision attributes remain registerable.
    registrar.register("right_arm")
            .attack()
            .collision(ObbCollisionMode.HARD);
}
```

- `ObbBoneRegistrar.ALL`: every rendered cube / 注册所有渲染 cube。
- `ObbBoneRegistrar.BASE`: head, body, left/right arm, left/right leg / 头、身体、左右手臂、左右腿。
- `ObbBoneRegistrar.HUMAN_BASE`: `BASE` plus left/right held-item cubes for humanoid entities / `BASE` 加人形实体左右手持物品 cube。
- `register(String)`: exact bone name or vanilla path leaf / 精确骨骼名或原版层级路径末段。
- `register(source, name)`: source-specific name, such as `vanilla` or `gecko` / 限定 `vanilla`、`gecko` 等来源。
- `register(ObbBoneKey)`: one exact cube / 注册一个精确 cube。

An empty registration method is a programming error and throws when the entity OBB state is first accessed. / 空注册方法属于编程错误，首次访问该实体 OBB 状态时会抛出异常。

`HUMAN_BASE` is a reusable selector. The built-in player mixin registers it; other humanoids must opt in explicitly. Canonical held-item keys are available through `ObbBuiltinBones.leftHeldItem(cubeIndex)` and `rightHeldItem(cubeIndex)`; common model names such as `leftItem`, `rightItem`, `left_hand_item`, `main_hand_item`, and the `held_item` source are also matched. / `HUMAN_BASE` 是可复用选择器；内置玩家 Mixin 已注册它，其他人形实体需显式接入。规范手持物键可通过 `ObbBuiltinBones.leftHeldItem(cubeIndex)` 与 `rightHeldItem(cubeIndex)` 创建，同时兼容 `leftItem`、`rightItem`、`left_hand_item`、`main_hand_item` 以及 `held_item` 来源等常见命名。

## Attributes, Not Types / 属性而非类型

EN: `ATTACK`, `HURT`, and `COLLISION` are mutable and composable `ObbBoneAttribute` values. They are not mutually exclusive hitbox types. Every registered OBB participates in generic OBB contact events, even with no attributes.

CN：`ATTACK`、`HURT`、`COLLISION` 是可修改、可组合的 `ObbBoneAttribute`，不是互斥碰撞箱类型。所有已注册 OBB 都参与通用 OBB 接触事件，即使没有任何属性。

- `ATTACK`: when touching any registered OBB, emits independent OBB-contact attack/hurt events / 接触任意已注册 OBB 时触发独立的 OBB 接触攻击/受击事件。
- `HURT`: an optional persistent marker; an attack contact automatically adds it temporarily to the target and restores it on `END` / 可选永久标记；攻击接触会自动为目标临时添加，并在 `END` 后恢复。
- `COLLISION`: enables a physical response mode; it is not required for generic contact events / 启用物理响应模式；通用接触事件不要求该属性。

`COLLISION` uses a separate response policy / `COLLISION` 另有响应策略：

- `NONE`: event only / 仅事件。
- `SOFT`: push response / 推挤响应。
- `HARD`: continuous local-player OBB movement clipping plus one persistent-manifold, zero-restitution impulse solve per server entity pair / 本地玩家连续 OBB 位移裁剪，加上服务端每个实体对一次持续接触流形、零恢复系数冲量求解。

When either entity in a pair has opted into physical OBB collision, Kltyton Bedrock Runtime cancels vanilla `Entity.push(Entity)` for that pair. This prevents the original AABB push from being added on top of `SOFT`/`HARD`. A permanently registered collision remains an OBB-physics opt-in when its discovered bones are temporarily disabled; disabling those bones produces no physical collision instead of silently restoring AABB pushing. / 当实体对中的任一方接入 OBB 物理碰撞时，Kltyton Bedrock Runtime 会取消该实体对的原版 `Entity.push(Entity)`，避免原版 AABB 推挤与 `SOFT`/`HARD` 叠加。永久注册碰撞后，即使暂时关闭已发现骨骼，该实体仍保持 OBB 物理接入状态；此时结果是无物理碰撞，而不是静默恢复 AABB 推挤。

Runtime state API / 运行时状态 API：

```java
ObbBoneState bone = entity.kltyton_bedrock_runtime$obbBone(key).orElseThrow();
bone.setAttackBox(true);
bone.setHurtBox(false); // Explicitly disables a registered/default HURT state. / 显式关闭注册默认值。
bone.setTemporaryAttackBox("skill:slash", true);
bone.setTemporaryHurtBox("skill:guard", true);
bone.setTemporaryCollision("skill:barrier", ObbCollisionMode.HARD);
bone.clearTemporaryAttributes("skill:slash");
```

`set(false)` is persisted and suppresses registered defaults and stale client snapshot attributes. A named temporary scope may still overlay the attribute, and clearing that scope restores the prior effective state. / `set(false)` 会持久化，并压过注册默认值与旧客户端快照；命名临时作用域仍可暂时叠加该属性，清除作用域后恢复此前有效状态。

## Client Computation / 客户端计算

EN: The client captures real post-animation vanilla `ModelPart` cubes and GeckoLib cubes. Broad phase uses unions of the OBBs' world-space enclosing bounds, not vanilla entity AABBs. Candidate bone pairs then use exact OBB-vs-OBB SAT.

CN：客户端捕获完成动画后的真实原版 `ModelPart` cube 与 GeckoLib cube。粗筛使用 OBB 自身世界包围范围的并集，而不是原版实体 AABB；候选骨骼对随后执行精确 OBB-vs-OBB SAT。

For local-player `HARD` physics, a client mixin wraps the result of vanilla `Entity.move -> collide(delta)`. Vanilla first clips movement against blocks and ordinary entity shapes; Kltyton Bedrock Runtime then applies translational continuous OBB SAT and returns one final allowed displacement to the rest of the original `move` method. Position, collision flags, grounded state, fall handling, and vanilla velocity restitution therefore remain in the normal movement pipeline. No hard-collision path calls `setPos`. / 本地玩家的 `HARD` 物理通过客户端 Mixin 包装原版 `Entity.move -> collide(delta)` 的结果：原版先处理方块及普通实体形状，本库再执行仅平移的连续 OBB SAT，并把唯一的最终允许位移交回原版 `move` 后半段。因此位置、碰撞标志、落地状态、坠落处理和原版速度恢复仍由正常移动管线维护；硬碰撞路径不再调用 `setPos`。

Four serverbound payloads are used / 使用四个发往服务端的包：

- `ObbEntityPartsPayload`: model-derived OBB geometry, attributes, collision mode, and animation state / 模型 OBB 几何、属性、碰撞模式、动画状态。
- `ObbPartSelectionPayload`: current crosshair-selected bone for vanilla attack/interaction correlation / 当前准星骨骼，用于关联原版攻击与交互。
- `ObbContactReportPayload`: batched `ATTACK` and `COLLISION` contact transitions (`BEGIN`, `STAY`, `END`) / 批量攻击与碰撞接触阶段。
- `GeoKeyframeSkillPayload`: optional GeckoLib custom marker and controller clock; it is queued until the same tick's geometry packets have been sent / 可选 GeckoLib 自定义 marker 与控制器时钟；它会等待同 tick 几何包发出后再发送。

Worst-case event narrow phase is `O(P^2)` for `P` visible registered cubes. Local hard movement costs `O(I * L * H * 15)` per move call, where `L` is local-player OBB count, `H` is nearby hard OBB count, `15` is the OBB SAT axis bound, and `I` is capped at four slide iterations. Snapshot/contact memory is `O(P + C + L + H)`. / 接触事件窄相位最坏为 `O(P^2)`；本地硬碰撞每次移动为 `O(I * L * H * 15)`，其中 `L` 是本地玩家 OBB 数、`H` 是附近硬 OBB 数、`15` 是 OBB SAT 轴上限，滑动迭代 `I` 固定最多四次。快照与接触空间为 `O(P + C + L + H)`。

## Server Role / 服务端职责

EN: `ServerObbStore` stores recent client model snapshots and previous bone transforms. `ObbServerGeometry` can raycast and run SAT against those immutable snapshots, and the server validates finite, invertible affine transforms, registered keys, entity tracking, snapshot radius, report budget and contact attributes. It does not replay GeckoLib animation or prove the model pose is genuine. Player attacks, interactions, mob attacks, and projectile hits are correlated only after their vanilla path occurs; Kltyton Bedrock Runtime does not create a second damage call.

CN：`ServerObbStore` 保存最近的客户端模型快照和上一帧骨骼变换；`ObbServerGeometry` 可以在这些不可变快照上执行射线和 SAT，服务端校验有限且可逆的仿射矩阵、注册键、实体追踪、快照半径、上报预算与接触属性；仍不会重放 GeckoLib 动画，也不能证明模型姿态真实。玩家攻击、交互、Mob 攻击和投射物命中只在对应原版路径完成后关联部位；Kltyton Bedrock Runtime 不会再次调用伤害。

## Server Geometry Query / 服务端几何查询

`ObbServerGeometry.snapshot(entity)` exposes current/previous entity pose snapshots. Every `ObbPartGeometrySnapshot` contains its `ObbBoneState` and immutable `ObbGeometry`. The latter exposes local/world bounds, defensive matrix copies, center, normalized world axes, scaled half extents, eight local/world vertices, point/direction transforms, ray clipping, surface queries, SAT overlap, and translational sweep. / `ObbServerGeometry.snapshot(entity)` 暴露实体当前/上一帧姿态；每个 `ObbPartGeometrySnapshot` 包含 `ObbBoneState` 与不可变 `ObbGeometry`。后者提供本地/世界边界、防御性矩阵副本、中心、归一化世界主轴、缩放后半尺寸、8 个本地/世界顶点、点/方向变换、射线裁剪、表面查询、SAT 重叠与平移 sweep。

```java
ObbEntityGeometrySnapshot snapshot = ObbServerGeometry.snapshot(entity).orElseThrow();
for (ObbPartGeometrySnapshot part : snapshot.parts()) {
    Vec3 center = part.geometry().worldCenter();
    List<Vec3> vertices = part.worldVertices();
}

ObbServerGeometry.raycast(entity, from, to).ifPresent(hit ->
        handlePart(hit.part().bone(), hit.distanceSqr()));
```

This is a server query API over a client-produced pose. Without a rendering client or after the 40-tick freshness window, the optional snapshot is empty. / 这是“服务端查询客户端产出姿态”的 API；没有渲染客户端，或快照超过 40 tick 新鲜度窗口后，返回值为空。

EN: Reports require an entity tracked by the reporter and a registered bone. The server derives inverse matrices and broad-phase bounds itself, takes ATTACK and physical collision modes from server bone state, requires recent matching geometry for contacts, and applies a shared per-player tick budget. A short reporter lease prevents different clients mixing entity pose frames. Override `kltyton_bedrock_runtime$acceptObbReporter` to narrow reporters and `kltyton_bedrock_runtime$maxSnapshotRadius` for models exceeding the default 64-block radius. These constraints cannot prove a client-produced animation or contact is genuine.

CN：报告必须来自正在追踪该实体的玩家，并命中注册骨骼。服务端重算逆矩阵与粗筛范围，攻击属性、物理碰撞模式取服务端骨骼状态；接触需有新鲜且匹配的几何，并受每玩家每 tick 总预算限制。短期上报者租约避免不同客户端混合姿态帧。可重写 `kltyton_bedrock_runtime$acceptObbReporter` 缩小上报范围，大型模型可重写默认 64 格的 `kltyton_bedrock_runtime$maxSnapshotRadius`。这些限制仍不能证明客户端动画或接触真实。

## Duplicate Suppression / 重复抑制

EN: A collision key is built from dimension, sorted entity UUIDs, and both `ObbBoneKey`s. Reporter UUIDs are tracked per active contact. Therefore reports such as client A saying `A -> B` and client B saying `B -> A` produce one `BEGIN`, shared `STAY` state, and one `END` after all reporters leave or expire.

CN：碰撞键由维度、排序后的实体 UUID 和两个 `ObbBoneKey` 组成，并为每个连续接触记录上报玩家 UUID。因此 A 客户端上报 `A -> B`、B 客户端上报 `B -> A` 时，只会产生一次 `BEGIN`、共享的 `STAY` 状态，以及所有上报者结束或超时后的一次 `END`。

Each contact dispatches at most one `STAY` callback per server tick with a valid `STAY` report. Any current reporter can supply it; a reporter retained only by its timeout cannot block another client's callback. Reporter timeouts still determine when the final `END` occurs. / 每个接触在收到有效 `STAY` 的服务端 tick 最多派发一次 `STAY` 回调，任一当前上报者均可提供；仅因超时保留的旧上报者不会阻断其他客户端。所有上报者离开或超时后才结束接触。

Directional attack contacts keep attacker and target order. They both cache the active attacking bone for later vanilla correlation and immediately dispatch the separate OBB-contact attack/hurt lifecycle. / 有方向的攻击接触保留攻击者/目标顺序；它既缓存活动攻击骨骼以关联后续原版攻击，也会立即派发独立的 OBB 接触攻击/受击生命周期。

## Vanilla Events vs OBB-Contact Events / 原版事件与 OBB 接触事件

Original-path listeners fire only after the corresponding vanilla attack/hurt path / 原版链监听只在对应原版攻击/受击路径完成后触发：

- `registerPlayerPartAttack`, `registerPlayerPartHurt`
- `registerEntityAttackBoxAttack`, `registerEntityHurtBoxHurt`

OBB-only listeners fire from visual OBB overlap even when vanilla entity AABBs do not overlap and no vanilla damage occurs / 纯 OBB 监听由视觉 OBB 重叠触发，即使原版实体 AABB 未重叠且没有原版伤害：

- `registerPlayerObbCollisionAttack`, `registerPlayerObbCollisionHurt`
- `registerEntityObbCollisionAttack`, `registerEntityObbCollisionHurt`

Both use `ObbAttackContext`. Check `origin()` or `vanillaAttack()`/`obbContactAttack()`. Vanilla contexts expose `ObbDamageInfo`: `requestedDamage`, `healthDamage`, `absorptionDamage`, `successful`, and `source`. Pure contact contexts intentionally use `ObbDamageInfo.NONE`, because the library does not inflict damage on that path. / 两类事件都使用 `ObbAttackContext`，可通过 `origin()` 或辅助方法区分。原版上下文提供请求伤害、生命伤害、吸收伤害、成功状态和伤害源；纯接触路径不会由库造成伤害，因此使用 `ObbDamageInfo.NONE`。

## Persistent Bone Data / 骨骼持久数据

Minecraft 26.2 entity persistence uses `ValueInput` / `ValueOutput`. Vanilla data components are primarily the `ItemStack` data model and are not used as this cross-loader entity store. / Minecraft 26.2 的实体持久化使用 `ValueInput` / `ValueOutput`。原版数据组件主要是 `ItemStack` 数据模型，本库不把它作为跨 loader 的实体存储。

`ObbDataKey` is the global schema identifier and codec, not a shared value container. Every resolved `ObbBoneState` owns its own value for that key. Register custom keys before entity saves load. / `ObbDataKey` 是全局数据结构标识与编解码器，不是共享值容器；每个已解析的 `ObbBoneState` 都独立保存该键对应的值。自定义键必须在实体存档加载前注册：

```java
public static final ObbDataKey<PartHealth> PART_HEALTH = ObbDataRegistry.register(
        Identifier.fromNamespaceAndPath("examplemod", "part_health"),
        PartHealth.CODEC,
        () -> new PartHealth(20.0F, 20.0F));
```

Data binding belongs to the fluent registration it follows. `data(key, value)` applies to every OBB selected by the current `register(...)`. `dataFactory(subSelector, key, factory)` selects one or more OBBs inside that parent scope and creates an independent value for each match. There are no registrar-level data shortcuts. / 数据绑定属于它所在的注册链：`data(key, value)` 作用于当前 `register(...)` 选中的全部 OBB；`dataFactory(subSelector, key, factory)` 在父范围内进一步选择一块或多块 OBB，并为每个命中项独立创建值。注册器不再提供容易混淆的数据快捷方法：

```java
registrar.register(ObbBoneRegistrar.ALL)
        .collision(ObbCollisionMode.HARD)
        .dataFactory(
                ObbBoneRegistrar.BASE,
                ObbBuiltinDataKeys.PART_HEALTH,
                () -> new ObbPartHealth(20.0F, 20.0F))
        .dataFactory("head", MY_DATA_KEY, MyData::new)
        .dataFactory(List.of("left_arm", "right_arm"), ARM_DATA_KEY, ArmData::new)
        .data(ObbBuiltinDataKeys.CARRIES_ENTITIES, true);
```

`ObbBuiltinDataKeys.PART_HEALTH` is a built-in behavior: when a registered hurt bone receives vanilla damage, Kltyton Bedrock Runtime automatically subtracts `damageReceived()` once. Consumers can read the updated value from later hooks for destruction, UI, or skill logic / `ObbBuiltinDataKeys.PART_HEALTH` 是内置行为：已注册受击骨骼承受原版伤害时，Kltyton Bedrock Runtime 会自动扣除一次 `damageReceived()`；使用方可在后续 hook 中读取更新值，用于破坏、UI 或技能逻辑：

```java
context.hurtBone().getData(ObbBuiltinDataKeys.PART_HEALTH).ifPresent(health -> {
    if (health.health() <= 0.0F) {
        destroyPart(context.hurtBone());
    }
});
```

The library writes persistent bone attributes, collision mode, registered data values, and persistent named attribute groups below the entity's `KltytonBedrockRuntime` save child. Client snapshot attributes and temporary animation attack groups are runtime-only. Unknown or undecodable extension values are preserved for a later compatible load. Entity-owned runtime state is released with the entity. / 库会在实体的 `KltytonBedrockRuntime` 保存子节点中写入持久属性、碰撞模式、已注册扩展数据和永久命名属性组；未知或解码失败的扩展值保留到下一次兼容加载。客户端快照属性与动画临时攻击组仅存在于运行时；运行状态直接由实体持有，随实体释放。

Data packages are separated by responsibility: key infrastructure is under `api.data.key`, built-in keys under `api.data.builtin.key`, and `ObbPartHealth` under `api.data.builtin.health`. Its automatically registered runtime consumer is `server.combat.damage.ObbPartHealthHooks`; it is not example code. The rest of the project follows the same non-flat package policy; see [Package Layout and Migration](package_layout.md). / 数据包按职责拆分：键基础设施位于 `api.data.key`，内置键位于 `api.data.builtin.key`，`ObbPartHealth` 位于 `api.data.builtin.health`；自动注册的运行时消费逻辑为 `server.combat.damage.ObbPartHealthHooks`，不属于示例代码。项目其他部分同样采用非扁平包策略，详见 [包结构与迁移](package_layout.md)。

## GeckoLib Attack Windows / GeckoLib 攻击窗口

`GeoBoneHitboxEntity` does not import GeckoLib classes. It exposes generic permanent/temporary group methods plus attack/hurt/collision conveniences. All attributes in the `gecko_animation` scope automatically clear when the bound animation ends or changes. / `GeoBoneHitboxEntity` 不导入 GeckoLib 类。它提供通用永久/临时组方法，以及攻击、受击、碰撞便捷方法；`gecko_animation` 作用域中的全部属性会在绑定动画结束或切换时自动清除。

```java
kltyton_bedrock_runtime$geoSetTemporaryAttackGroups(List.of("right_arm"));
kltyton_bedrock_runtime$geoSetTemporaryHurtGroups(List.of("shield"));
kltyton_bedrock_runtime$geoSetTemporaryCollisionGroups(ObbCollisionMode.HARD, List.of("barrier"));
kltyton_bedrock_runtime$geoClearTemporaryGroups();
```

## GeckoLib Keyframe Skills / GeckoLib 关键帧技能

`GeoKeyframeSkillEntity` extends the Gecko-independent `GeoBoneHitboxEntity` contract and still imports no GeckoLib class. Register a point marker or a state window on the entity. A window emits `BEGIN`, one `TICK` per server tick, then `END`; entity removal, animation change, or timeout emits `CANCEL`. / `GeoKeyframeSkillEntity` 扩展不直接引用 GeckoLib 类的 `GeoBoneHitboxEntity` 契约，本身仍不导入 GeckoLib 类。实体可注册单点 marker 或状态窗口；窗口会派发 `BEGIN`、每服务端 tick 一次 `TICK`、最后 `END`，实体移除、动画切换或超时时派发 `CANCEL`。

In 26.2, on each client logic tick, the ticker extracts a state for every tracked `GeoBoneHitboxEntity` through vanilla's public `EntityRenderDispatcher.extractEntity` entrypoint. It uses partial tick `0.0` for ordinary entities and `1.0` only when `TickRateManager.isEntityFrozen(entity)` is true. GeckoLib advances its normal controller and marker timeline during state extraction; this path does not call `submit`, enqueue draw tasks, or produce an OBB snapshot. Earlier targets keep their previous animation update path. / 26.2 中，每个客户端逻辑 tick，ticker 都会通过原版公开的 `EntityRenderDispatcher.extractEntity` 为已追踪的 `GeoBoneHitboxEntity` 提取状态。普通实体使用 partial tick `0.0`，只有 `TickRateManager.isEntityFrozen(entity)` 为真时才使用 `1.0`。GeckoLib 在状态提取时推进常规控制器和 marker 时间轴；该路径不调用 `submit`、不加入绘制任务，也不生成新的 OBB 快照。更早目标保持原有的动画更新路径。

The entity must still be tracked by at least one client, and its custom instruction handler must still call `GeoKeyframeSkillClientBridge.forward(event)`. The logical tick does not invent a new bone pose snapshot: `context.parts()` remains the latest geometry snapshot captured by the OBB render/capture path. Multiple tracking clients may report the same marker, which the server deduplicates. / 实体仍须至少被一个客户端追踪，其自定义指令 handler 仍须调用 `GeoKeyframeSkillClientBridge.forward(event)`。逻辑 tick 不会虚构新的骨骼姿态快照，`context.parts()` 仍是 OBB 渲染/捕获路径最近一次生成的几何快照。多个追踪客户端可能上报同一 marker，服务端会进行去重。

```java
public final class SkillMob extends PathfinderMob
        implements GeoEntity, GeoKeyframeSkillEntity {
    @Override
    public void kltyton_bedrock_runtime$registerGeoKeyframeSkills(GeoKeyframeSkillRegistrar registrar) {
        registrar.point("fire", context -> fire(context.target().orElse(null)));
        registrar.window("sword_window", "attackStart", "attackEnd", context -> {
            if (context.phase() == GeoKeyframeSkillPhase.TICK) {
                for (ObbPartGeometrySnapshot part : context.parts()) {
                    testSkillCollision(part.geometry());
                }
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<SkillMob>("main", 5, this::animate)
                .setCustomInstructionKeyframeHandler(event -> {
                    handleTemporaryAttackGroups(event.keyframeData().getInstructions());
                    GeoKeyframeSkillClientBridge.forward(event);
                }));
    }
}
```

The server context includes reporter, subject, AI target/projectile owner helpers, skill id, raw marker, lifecycle phase, controller and animation names, animation/timeline/marker clocks, speed, trigger/transition/finished flags, client sequence, client/server game times, entity pose/velocity, and the latest `ObbEntityGeometrySnapshot`. Reports from multiple clients are deduplicated by entity, animation, controller, marker, and nearby observed/receive ticks. / 服务端上下文包含上报玩家、技能主体、AI 目标/投射物拥有者辅助方法、技能 id、原始 marker、生命周期阶段、控制器与动画名、动画/timeline/marker 时钟、速度、触发/过渡/结束状态、客户端序列、客户端/服务端游戏时间、实体位姿/速度，以及最新 `ObbEntityGeometrySnapshot`。多客户端上报按实体、动画、控制器、marker 与相邻观察/接收 tick 去重。

GeckoLib imports are isolated in client compatibility packages, including the marker bridge and controller snapshot extractor. The logical ticker references only vanilla rendering APIs and the Gecko-independent entity contract. All packet, server, context, registration, ticking, and geometry APIs remain free of GeckoLib types. / GeckoLib 导入隔离在客户端兼容包中，包括 marker 桥和控制器快照提取器；逻辑 ticker 只引用原版渲染 API 和不依赖 GeckoLib 的实体契约。网络包、服务端、上下文、注册、tick 与几何 API 均不暴露 GeckoLib 类型。

All controller states are sent, including stopped controllers and off-screen heartbeat updates. `ObbServerGeometry.animations(entity)` exposes the current list. Each skill window stays bound to its opening controller; stop, switch, loop rewind, finished animation, removal, dimension change and timeout cancel it. The separate `gecko_animation` attribute scope follows the same animation lifecycle; a custom skill handler must release any resources or custom scopes it owns in END/CANCEL. A default window is limited to 1,200 server ticks. / 上报包含全部控制器及视锥外心跳；可用 `ObbServerGeometry.animations(entity)` 查询。技能窗口绑定开启它的控制器，在停止、切换、循环回绕、结束、实体移除、换维度或超时时取消；独立的 `gecko_animation` 属性作用域按同样的动画生命周期清理，自定义技能处理器需在 END/CANCEL 中释放自己的资源或自建作用域。窗口默认最长 1,200 服务端 tick。

Override `kltyton_bedrock_runtime$acceptGeoKeyframeSkill(reporter, marker, animation, markerTimeSeconds)` to check the server-owned attack/skill state before accepting an opening marker. Registration and client tracking alone are not skill authorization. / 必须由接入方按需要重写此方法，将 marker 与服务端攻击/技能状态对应；仅注册 marker 或追踪实体不代表技能已获准执行。

## Independent Physics and Carrying / 独立物理与承载

Physical response does not insert an axis-aligned `VoxelShape` and does not require vanilla entity AABB overlap. `HARD` clips local-player displacement with exact OBB surfaces and continuous SAT. Server entity pairs aggregate all active bone contacts into one persistent manifold, retain a hysteretic normal across adjacent cubes, apply zero-restitution normal/friction impulses, and use only a capped Baumgarte correction through `Entity.move`. `SOFT` uses a mass-weighted penetration-slop spring/damper impulse. / 物理响应不会插入轴对齐 `VoxelShape`，也不要求原版实体 AABB 重叠。`HARD` 使用精确 OBB 表面与连续 SAT 裁剪本地玩家位移；服务端实体对会把全部活动骨骼接触聚合为一个持续接触流形，在相邻 cube 间保留带迟滞的稳定法线，施加零恢复系数法向/摩擦冲量，并仅通过 `Entity.move` 执行有上限的 Baumgarte 小幅纠偏。`SOFT` 使用质量加权、带穿透容差的弹簧/阻尼冲量。

Contact events still retain every bone pair. Local hard movement treats nearby hard bones as one compound collider and resolves at most four earliest contacts per move. Server reports only update contact state; responses are batched at the end of the server tick and each entity pair is solved once. Effective inverse mass is derived from entity volume; a physical top support is treated as the carrier for vertical correction, preventing the supported entity from pushing it into terrain. / 接触事件仍保留每个骨骼对；本地硬碰撞把附近硬骨骼视为一个复合碰撞体，每次移动最多求解四个最早接触。服务端数据包只更新接触状态，响应统一在 tick 末批处理，每个实体对只求解一次。有效逆质量按实体体积估算；竖直接触中的物理顶部支撑会被视为承载体，避免站立实体把它纠偏进地形。

For player-to-hard-entity pushing, the report carries the local player's movement. The server projects it onto a stable root-to-root direction and transfers only positive approach as a capped velocity impulse; it no longer moves the collider by the whole penetration vector. A stationary player or an animated bone that merely changes its MTV cannot add velocity. / 玩家推动硬碰撞实体时，报告会携带本地玩家移动；服务端将其投影到稳定的根节点连线方向，只把正向接近量作为有上限的速度冲量传递，不再按完整穿透向量移动碰撞实体。静止玩家或仅改变 MTV 的动画骨骼不会增加速度。

Mark a hard-collision bone as a moving support surface / 将硬碰撞骨骼标记为可移动承载表面：

```java
registrar.register(ObbBoneRegistrar.ALL)
        .collision(ObbCollisionMode.HARD)
        .data(ObbBuiltinDataKeys.CARRIES_ENTITIES, true);
```

When another registered OBB contacts a top surface near its feet, carrying transfers the carrier entity's root translation and yaw once per model snapshot. Skeletal idle/walk animation is excluded. The resulting displacement is submitted through `Entity.move(MoverType.SHULKER_BOX, delta)` and therefore respects the normal movement/collision pipeline. / 当其他已注册 OBB 在脚底附近接触承载顶面时，系统会按每个模型快照传递承载实体根节点的平移与偏航旋转，并排除待机/行走骨骼动画。最终位移通过 `Entity.move(MoverType.SHULKER_BOX, delta)` 提交，因此仍遵守正常移动和碰撞管线。

Support selection is shared by Fabric and NeoForge. Five foot samples cast vertical segments into every nearby hard OBB and use the exact first OBB face hit, including its real world normal; enclosing-AABB `maxY` is never used as a surface. One compound support is selected globally, while continuous movement clipping prevents gravity from entering the collider and being corrected after the tick. Crossing from one bone OBB to another therefore changes the allowed displacement, not player velocity, and cannot create an upward impulse. / Fabric 与 NeoForge 共用同一套支撑选择：五个脚底采样点向附近所有硬 OBB 发射竖直线段，使用最先命中的真实 OBB 面及其世界法线，绝不把外包 AABB 的 `maxY` 当作表面。系统全局只选择一个复合支撑，连续位移裁剪会在重力进入碰撞体前阻止它，而不是 tick 结束后再修正。跨越骨骼 OBB 接缝时改变的是允许位移，不是玩家速度，因此不会产生向上冲量。

For walkable slopes, the center-foot ray owns the support height. Corner samples are consulted only while the center is outside the finite face, and their contact plane is projected back to the player center before calculating height. The final sweep cannot move above that center-plane height. This prevents an uphill corner sample from acting as an artificial step or trampoline. / 对可行走斜面，支撑高度以脚底中心射线为准；只有中心离开有限面时才使用角点，并先把角点接触平面投影回玩家中心再计算高度。最终 sweep 也不能高于该中心平面，因此上坡侧角点不会再被当成人工台阶或蹦床。

## Operational Limits / 运行边界

- No rendering client means no fresh visual-model snapshots or client contact reports. / 没有客户端渲染时，不会产生新的视觉模型快照或接触报告。
- In 26.2, a culled `BoneHitboxEntity` within 32 blocks is sampled to include its model bounds in culling. The sample runs model/layer callbacks with a private `SubmitNodeStorage`; its deferred draw tasks are not enqueued into the active frame. Vanilla distance and frustum checks still decide regular rendering using the expanded bounds. Beyond that range, only normally rendered cubes participate, except the local-player and projectile fallbacks below. Other versions keep their previous behavior. / 26.2 中，32 格内被视锥剔除的 `BoneHitboxEntity` 会单独采样，以便把模型边界计入剔除范围。采样通过私有 `SubmitNodeStorage` 运行模型和渲染层回调；其中的延迟绘制任务不会加入当前帧。原版距离与视锥检查仍依据扩展后的边界决定常规绘制。超过该范围后，除下述本地玩家与投射物回退外，仅通常渲染的 cube 参与。其他版本保持原有行为。
- Contact events and rotating/animating collider updates remain snapshot-discrete. Local-player translational `HARD` movement now uses swept OBB SAT, but an OBB that rotates or teleports completely through another object between two model snapshots can still tunnel. / 接触事件以及旋转/动画碰撞体更新仍按快照离散执行；本地玩家平移 `HARD` 已使用 swept OBB SAT，但若某个 OBB 在两次模型快照间旋转或瞬移并完全穿过目标，仍可能发生穿透。
- First-person local-player physics uses six rotated OBBs with vanilla player-model proportions when no full player model was submitted. Third-person/remote players still use captured animated cubes. / 第一人称未提交完整玩家模型时，物理系统使用六个按原版玩家模型比例生成的旋转 OBB；第三人称与远程玩家仍使用捕获的动画 cube。
- Vanilla projectiles without traversable model cubes use their current AABB as a synthetic axis-aligned OBB attack source; target bones remain visual-model OBBs. / 无法遍历模型 cube 的原版投射物使用当前 AABB 作为合成轴对齐攻击 OBB，目标骨骼仍为视觉模型 OBB。

## Protocol and limits / 协议与限制

Entity snapshot payloads use `kltyton_bedrock_runtime:entity_parts_v4`; Forge and NeoForge transports use version `4`. Both sides must run a matching build. Snapshots carry their capture age, bounded to 40 ticks; repeated logical ticks retain the last rendered local pose and apply current entity-root motion without advancing its animation. Resource reloads discard captured geometry. Existing entity save keys remain unchanged. Server snapshot, contact, selection and skill tables clear on server shutdown. / 实体快照使用 `kltyton_bedrock_runtime:entity_parts_v4`，Forge 与 NeoForge 传输版本为 `4`，两端需使用匹配构建。快照携带最多 40 tick 的采样年龄；同一渲染帧内的多个逻辑 tick 保留已采样的局部姿势，并应用当前实体根变换，不推进动画。资源重载会丢弃已捕获几何。实体存档键不变，服务端关闭时清除快照、接触、选择和技能运行表。

Spatial broad-phase filtering and per-tick reuse reduce unnecessary pair checks; worst-case dense contacts remain quadratic. No worker thread reads mutable world state or render poses. / 空间粗筛和同 tick 复用减少无效比较，密集接触的最坏复杂度仍为平方级；没有把可变世界状态或渲染姿态交给工作线程。
