# Kltyton Bedrock Runtime

**以 GeckoLib 为基础，扩展基岩版模型、分层动画、粒子和模型部位交互。**

Kltyton Bedrock Runtime，简称 KBR，是供其他模组调用的运行库。它保留 GeckoLib 的模型资源、渲染器和动画生态，扩展动画图、多边形几何、基岩版粒子，以及由模型生成的部位选择和碰撞能力。

## 玩家与整合包作者

当使用的模组将 KBR 列为前置时，安装与它匹配的版本即可。生物、装备及具体特效由接入本库的模组提供。

1. 选择与你的 Minecraft 版本、加载器一致的文件。
2. 安装对应版本的 **GeckoLib**。Fabric 还需要 **Fabric API** 和 **Forge Config API Port**。
3. 将普通 KBR JAR 安装到对应实例。`-sources.jar` 和 `-javadoc.jar` 是开发资料，不是可安装模组。

不需要额外安装 SimpleBedrockModel、ParticleStorm 或 ThreeDimensionParticle。

## 开发者可以实现什么

- **连续动画混合**：通过 1D BlendSpace 衔接待机、行走和奔跑，或通过 2D BlendSpace 驱动方向移动与瞄准。
- **分层姿势**：按骨骼或整棵子树设置遮罩，组合普通混合与增量姿势，并按需使用显式起终速度驱动的 Hermite 过渡。
- **Tree 与 Baked 模型**：保留逻辑骨骼和 Locator，组织几何，并显式声明可合并的静态结构。
- **基岩版 `poly_mesh`**：在同一模型中混合 cubes 与多边形几何，随骨骼运动，并使用真实网格三角形进行选择和射线查询。
- **基岩版粒子与 MoLang**：从 JSON 加载效果、表达式、曲线和事件；支持 Billboard 粒子，以及使用普通 cube、GeckoLib 或增强多边形模型的模型粒子。
- **模型部位选择与碰撞**：注册参与处理的骨骼，选择和高亮部位，处理攻击与接触事件，保存部位数据，并在服务端查询近期几何。实体运动碰撞使用有向包围盒（OBB）。
- **模型方块**：通过对应版本的方块 API，从模型生成 AABB/OBB 形状并构建自动多格结构。

增强动画和模型能力由接入模组显式启用。已有 GeckoLib 动画文件继续作为动作来源，动画 MoLang 和关键帧回调继续由 GeckoLib 处理；粒子 MoLang 则由 KBR 的粒子运行时求值。

## 构建版本

| Minecraft | 加载器 | Java | GeckoLib 开发基线 |
| --- | --- | --- | --- |
| 1.20.1 | Fabric、Forge | 17 | 4.8.4 |
| 1.21.1 | Fabric、NeoForge | 21 | 4.9.2 |
| 26.1.2 | Fabric、NeoForge | 25 | 5.5.2 |
| 26.2 | Fabric、NeoForge | 25 | 5.5.3 |

请选择对应文件及其声明的依赖。版本专属 API 在 Wiki 中分别说明。

## 文档与开发接入

[中文 Wiki](https://github.com/kltyton/KltytonBedrockRuntime/wiki/Home.zh-CN) 提供[安装说明](https://github.com/kltyton/KltytonBedrockRuntime/wiki/Installation.zh-CN)、[开发入门](https://github.com/kltyton/KltytonBedrockRuntime/wiki/Getting-Started.zh-CN)及各功能的资源与 API 示例。

源代码与问题反馈位于[项目仓库](https://github.com/kltyton/KltytonBedrockRuntime)。Maven 组为 `com.kltyton`，Java 包为 `com.kltyton.bedrockruntime`。

## 许可与鸣谢

KBR 使用 **LGPL-3.0-only** 许可证。GeckoLib 是必需的外部依赖；模型、动画和粒子能力参考或复用了 SimpleBedrockModel、MAE、ParticleStorm、ThreeDimensionParticle 与 mochafloats 的相关工作。源码及发行包保留原有许可与归属说明。
