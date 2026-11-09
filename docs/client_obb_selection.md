# Client OBB Selection / 客户端 OBB 部位选择

## Capture / 捕获

- Vanilla `ModelPart` cubes are captured after animation setup. / 原版 `ModelPart` cube 在动画设置完成后捕获。
- GeckoLib cubes are captured through GeckoLib render events; GeckoLib is a required dependency. / GeckoLib cube 通过 GeckoLib 渲染事件捕获；GeckoLib 是必需依赖。
- A cube is recorded only when its entity implements `BoneHitboxEntity` and its `ObbBoneKey` matches the entity's mandatory registration method. / 只有实体实现 `BoneHitboxEntity` 且 `ObbBoneKey` 命中强制注册方法时才会记录 cube。
- `ALL`, `BASE`, and `HUMAN_BASE` are selectors, not pre-authored hitbox dimensions. `HUMAN_BASE` adds both held items to the six `BASE` body parts and is registered by the built-in player mixin; other entities opt in explicitly. / `ALL`、`BASE` 与 `HUMAN_BASE` 都是选择器，不是手写碰撞尺寸；`HUMAN_BASE` 在六个 `BASE` 身体部位外加入左右手持物，内置玩家 Mixin 已注册该预设，其他实体需显式接入。

Each snapshot contains local bounds, an OBB-derived world broad-phase bound, local/world matrices, composable attributes, collision mode, and optional Geo animation state. Vanilla entity AABBs are not used to decide OBB-to-OBB contact. / 每个快照包含本地边界、由 OBB 推导的世界粗筛范围、本地/世界矩阵、可组合属性、碰撞模式及可选 Geo 动画状态；OBB 接触判定不使用原版实体 AABB。

Affine box contact and translation sweeps use transformed face normals and edge cross products, including shear from nested nonuniform scale and rotation. Render captures retain the interpolated entity root used for that draw; tick reuse advances that root without advancing skeletal animation. / 仿射盒接触与平移扫掠使用变换后的面法线和边叉积，支持嵌套非均匀缩放与旋转形成的剪切。渲染捕获保留本次绘制的插值实体根姿势；tick 复用只补偿根运动，不推进骨骼动画。

## Culled model sampling / 被剔除模型的采样

In 26.2, within 32 blocks, a culled `BoneHitboxEntity` is sampled to extend its culling bounds to its model bounds. The sample runs the real model and layer callbacks with a private `SubmitNodeStorage`; deferred draw tasks stay out of the active frame. Vanilla distance and frustum checks still control ordinary drawing using the extended bounds. Other versions keep their previous capture behavior. / 26.2 中，32 格内被视锥剔除的 `BoneHitboxEntity` 会被采样，并把剔除边界扩展到真实模型边界。采样使用私有 `SubmitNodeStorage` 运行模型和渲染层回调，延迟绘制任务不会进入当前帧。常规绘制仍由原版距离与视锥检查依据扩展后的边界决定。其他版本保持原有捕获行为。

## Held items / 手持物

`held_item:left_item` and `held_item:right_item` use actual `ItemInHandLayer` transforms and item-layer extents. Each render layer produces one enclosing OBB; internally rotated pieces within a layer are enclosed together. Flat item geometry receives a small nonzero thickness. Empty hands produce no item OBB. / 左右手持物使用实际手臂变换、物品 display 变换和渲染层范围；每层生成一个包围 OBB，层内独立旋转的小部件会合并包围，平面物品保留极薄厚度，空手不生成物品框。

The first-person player reuses the entity renderer's third-person body/arm pose without drawing it. Its collision represents the world-held item, not the first-person screen overlay. / 第一人称玩家会复用实体渲染器的第三人称身体与手臂姿态，但不绘制它；碰撞表示世界中的持物位置，而非屏幕第一人称覆盖层。

## Selection and Highlight / 选择与高亮

The camera ray is transformed into every candidate OBB's local space. The nearest exact local-box hit becomes the selected bone. The outline uses the same pose and local bounds, so selection and rendering share one transform path. / 相机射线会变换到每个候选 OBB 的本地空间，最近的精确本地盒命中成为选中骨骼。轮廓使用同一姿态和本地边界，因此选择与渲染共用一条变换路径。

- Crosshair OBB display is off by default. / 准星 OBB 默认关闭。
- Enable it through `config/kltyton_bedrock_runtime-client.toml`, the hold key, or F3+B. / 可通过客户端配置、按住键位或 F3+B 开启。
- Player part OBBs are hidden in F3+B by default. Set `client.showPlayerDebugObbs = true` in `config/kltyton_bedrock_runtime-client.toml` to show them. / 玩家部位 OBB 默认不随 F3+B 显示；在客户端配置中将 `client.showPlayerDebugObbs` 设为 `true` 可开启。
- Fabric and NeoForge both register the default-unbound hold key in Controls. / Fabric 与 NeoForge 都会把默认未绑定按住键注册到控制设置。
- Ordinary entities that do not implement `BoneHitboxEntity` never display library OBBs. / 未实现接口的普通实体不会显示库 OBB。

When `kltyton_bedrock_runtime$onlyHitPartTurnsRed()` is true, a client attack suppresses the full-body hurt overlay and refreshes the selected cube's red overlay. GeckoLib suppression is registered during client bootstrap. / 当该方法返回 true 时，客户端攻击会抑制全身受击红色并刷新选中 cube 的红色 overlay；GeckoLib 抑制逻辑在客户端引导阶段注册。

## Package Layout / 包分类

Packages are no longer broad class buckets. Bone, context, registration, state, client render/selection, payload, server hook, and mixin code are split into responsibility-specific leaf packages. See [Package Layout and Migration](package_layout.md) for the complete tree and breaking import migration. / 各目录不再作为扁平类容器；骨骼、上下文、注册、状态、客户端选择/渲染、数据包、服务端 hook、Mixin 均拆入职责明确的叶子包。完整目录与破坏性 import 迁移见 [包结构与迁移](package_layout.md)。
