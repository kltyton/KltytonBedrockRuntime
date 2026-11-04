# 常见问题与排查

[English](https://github.com/kltyton/KltytonBedrockRuntime/wiki/FAQ) | [简体中文](https://github.com/kltyton/KltytonBedrockRuntime/wiki/FAQ.zh-CN) · [首页](https://github.com/kltyton/KltytonBedrockRuntime/wiki/Home.zh-CN)

## 依赖与功能范围

**GeckoLib 是必需前置。** KBR 扩展其资源与渲染生态，需安装 Minecraft/加载器目标对应的版本，不能随意混用 GeckoLib 大版本。

KBR 提供运行 API。新生物、动画分层和制作好的粒子效果来自接入模组及其资源，不会替换所有已安装模组的模型，也不是把任意基岩版附加包整体转换为 Java 玩法内容的工具。

不包含可视化粒子编辑器，也不需要为了提供已整合的运行能力而安装参考项目的旧版 Mod JAR。

## 资源与动画

| 表现 | 检查内容 |
| --- | --- |
| 找不到模型 | 完整资源 ID、命名空间，以及 GeckoLib 4 的 `geo/` 与 GeckoLib 5 的 `geckolib/models/` 路径 |
| 找不到动作 | 节点的 `animation` 应对应 GeckoLib 动画文件中的动作键 |
| 找不到内联图 | 模型注册、`kltyton_bedrock_runtime.graph` 与当前烘焙资源 |
| 图引用报错 | 节点名、输出、循环依赖，以及 `velocity_input` 只能用作 Hermite 速度引用 |
| Mask 骨骼被拒绝 | 真实骨骼名与当前骨架代次 |
| 几何只从一侧可见 | 制作时的绕序、法线及模型显式 `double_sided` 选择 |
| Baked 没有合并分组 | `static_bones`、被保留/消费的骨骼及至少两根可合并几何骨 |
| 找不到粒子 | `particle_definitions/<id>.json` 和 `description.identifier` |
| 模型粒子不显示 | 描述文件 ID、`model_id`、贴图 sprite，以及对应类型已注册/烘焙的几何 |

客户端资源使用 F3+T 重载。在延迟绘制之前捕获游戏参数，不在重复的骨骼绘制回调中读取可变世界状态。

## 事件与碰撞

图采样只产生姿势。声音、粒子与自定义指令事件仍放在原生 GeckoLib handler，使用技能时追加 KBR 关键帧桥。不要按采样次数或 render pass 派发玩法动作。

查询部位时，实现 `BoneHitboxEntity` 并注册匹配选择器。选择显示需要启用，并非游戏中的每个实体都实现了该接口。

网格射线和运动碰撞是两个职责：增强 `poly_mesh` 可以提供真实三角形命中，运动仍使用 OBB。需要服务端精确网格查询时，应注册模型数据和实体绑定。

服务端几何来自近期客户端姿态；空快照表示当前没有可用上报。技能和伤害仍需服务端自身状态授权。

## 版本与迁移

KBR 使用 `kltyton_bedrock_runtime`、`com.kltyton.bedrockruntime` 和以 `kltyton_bedrock_runtime-` 开头的 Maven 产物，不提供旧 BoneHitboxLib 包或坐标的兼容别名。使用方需要更新导入、依赖和资源声明。

Minecraft 26.x 使用 `Identifier`，1.21.1 使用 `ResourceLocation.fromNamespaceAndPath`，1.20.1 使用 `ResourceLocation` 构造方法。GeckoLib 4 的包为 `software.bernie.geckolib`，GeckoLib 5 的包为 `com.geckolib`；版本专属 API 在对应页面中说明。

## 反馈问题

使用[问题反馈](https://github.com/kltyton/KltytonBedrockRuntime/issues)，提供 Minecraft、加载器、Java、KBR 和 GeckoLib 版本、相关资源 ID、简短复现步骤，以及对应日志或资源错误。移除凭据及无关私人数据。

## 许可与来源

KBR 使用 LGPL-3.0-only。GeckoLib 保持为外部依赖；ParticleStorm 与 ThreeDimensionParticle 提供粒子运行相关参考，mochafloats 提供经粒子路径适配的 MIT 许可词法、语法和解释器源码；模型概念及动画工作也保留对 SimpleBedrockModel 和 MAE 的鸣谢。

详见[来源与许可证说明](https://github.com/kltyton/KltytonBedrockRuntime/blob/main/docs/dependencies/animation-models.md)和[项目许可证](https://github.com/kltyton/KltytonBedrockRuntime/blob/main/LICENSE)。
