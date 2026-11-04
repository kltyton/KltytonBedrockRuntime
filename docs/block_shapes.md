# 模型方块形状与自动多格方块

适用 Minecraft 26.2、Fabric/NeoForge。形状读取已打包的 Java 方块模型 `elements`，沿用 Eden Realm 的旋转、凸多面体、按单元格裁切、四方向旋转、联动放置/拆除和交互转发逻辑。运行时不依赖 GeckoLib 或客户端渲染。

## 选择 AABB 或 OBB

`BlockShapeMode.OBB` 保留元素旋转后的凸几何，使用真实表面做射线、形状相交、实体移动和台阶支撑处理。`BlockShapeMode.AABB` 将几何转换为原版轴对齐包围盒；旋转元素会包含斜面外侧的空白区域。两种模式都由同一份模型资源产生，调用方不需要手写碰撞尺寸。

```java
Identifier model = Identifier.fromNamespaceAndPath("examplemod", "block/slanted_prop");
ModelShapeProvider.prepare(model); // 在注册/初始化阶段预热四个朝向
VoxelShape shape = ModelShapeProvider.shape(model, Direction.NORTH, BlockShapeMode.OBB);
VoxelShape nativeShape = ModelShapeProvider.shape(model, BlockShapeMode.AABB);
```

普通自定义方块可在 `getShape` / `getCollisionShape` 返回这些预备形状。`ModelShapeProvider.union` 支持多个模型合并；`selectEqualWeight` 提供按种子确定的等权模型选择。缓存不会跟随客户端资源包重载改变服务端物理。

## 自动多格方块接入

使用所属 Loader 的正常方块注册流程，给属性设置正确的注册键，再构造一个方块和对应 `BlockItem`：

```java
AutoModelBlock block = new AutoModelBlock(blockId, properties, BlockShapeMode.OBB);
// 整体选择轮廓使用：
// new AutoWholeModelBlock(blockId, properties, BlockShapeMode.AABB);
```

- `AutoModelBlock`：准星选中所在单元格的局部轮廓。
- `AutoWholeModelBlock`：准星选中同一结构的整体轮廓。
- 需要自定义交互、掉落、方块实体或放置逻辑时，继承 `AutoPartShapeBlock` / `AutoWholeShapeBlock`，实现自己的 codec 和受保护的 origin hooks。
- 模型约定为 `assets/<namespace>/models/block/<block path>.json`，北向为模型原始朝向；玩家放置方向决定四向旋转。
- 仅注册一个方块 ID；`origin_x/y/z` 方块状态保存各占位格相对原点的偏移。仅原点渲染模型，链接格不可见，交互和掉落转交原点。占位格不需要逐个注册方块。
- 放置前检查全部占位格的区块、世界高度、边界、可替换状态及实体阻挡；破坏链接格会联动拆除结构，加载后的孤立链接格会自清理。
- 模型范围继承 Eden Realm 的约束：每轴相对原点最多偏移 4 格，解析坐标裁剪至 `[-4, 5]` 格。超出范围的模型应先调整，不会自动扩展无限结构。

数据生成时将所有已注册自动方块交给：

```java
generator.addProvider(includeClient, new AutoShapeDataProvider(output, automaticBlocks));
```

这里的 `generator`、`includeClient`、`output` 与 `automaticBlocks` 由接入方已有 datagen 入口提供。生成器输出四朝向、仅原点可见的 multipart blockstate；存在方块物品时输出 26.2 的 `assets/<namespace>/items/<item>.json`。原始模型、纹理、语言和掉落表仍由方块所属 Mod 提供。没有引入另一套跨 Loader 注册器。

## 全局模型几何配置

服务端配置文件为 `kltyton_bedrock_runtime-server.toml`，由 Loader 同步到客户端。当前 Fabric 和 NeoForge 开发运行目录生成在 `config/`；1.20.1 Forge 生成在 `saves/<世界>/serverconfig/`。部署时编辑所属 Loader 实际生成的文件，保留其他配置项。

```toml
vanillaSlantedBlockObb = true
forceAllBlockObb = false
blockModelWhitelist = []
blockModelBlacklist = []
```

- `vanillaSlantedBlockObb`：默认开启，自动使用原版斜面模型几何。
- `forceAllBlockObb`：默认关闭。开启后对允许的所有原版和模组方块启用 OBB 碰撞，优先使用已打包模型；模型缺失、特殊渲染器或无可解析元素时，将方块自身返回的碰撞分片转换为 OBB，保留原有范围。轴对齐几何也保留 OBB 表示。原本的空碰撞始终为空。
- `blockModelWhitelist`：空列表不限制；非空时只有匹配方块可使用全局模型替换或强制 OBB。
- `blockModelBlacklist`：优先于白名单，也约束强制模式。条目支持 `namespace:id`、`namespace:*` 和 `*`，例如 `minecraft:stone`、`minecraft:*`。不支持省略命名空间、方块标签或任意位置通配符。

例如，只为原版方块强制启用，但排除箱子：

```toml
forceAllBlockObb = true
blockModelWhitelist = ["minecraft:*"]
blockModelBlacklist = ["minecraft:chest", "minecraft:trapped_chest"]
```

白名单只限制范围，不会自行开启任何模式。黑白名单同时约束本库 API 提供的模型 OBB：被排除的模型 OBB 回退为模型 AABB，其他原生形状保持原样。自动多格方块保留各占位格裁切和放置范围；名单允许时，强制模式使用原始模型 OBB，否则采用注册时指定的 AABB/OBB 模式。关闭两个开关并清空名单可恢复方块原有形状及接入方指定模式。

目标任务 `generateVanillaBlockShapes` 从已解析的 Minecraft 资源提取全部原版 blockstate 与模型元素，生成 `common/build/<loader>-<minecraft>/generated/resources/vanilla-models/data/kltyton_bedrock_runtime/block_shapes/vanilla.json`，并由对应目标的 common 资源源集加载。模组模型在注册完成后预处理，沿用模型父级、旋转、rescale、加权 variants、multipart 和位置种子。查询只读取预编译形状与当前配置，不在碰撞或渲染时读取模型文件。

客户端资源包、运行时特殊渲染器和 Gecko 动画不改变这份服务端物理几何。动态或特殊方块、第三方形状覆盖、配置热重载和多人同步效果不由此处的模型几何定义。

## 26.2 公共元素解析入口

`ModelShapeCache.buildForElements(JsonArray, Direction)` 返回公开 `ShapeSet`，供下游几何工具或已获得模型元素的调用方复用同一算法。`Cell`、`ShapeSet.localShape(Cell)` 同时公开；`localShapes` 与 `occupiedCells` 在结果构造时复制为不可修改集合。该元素解析入口只在 26.2 目标公开；其他版本保留内部缓存实现。
