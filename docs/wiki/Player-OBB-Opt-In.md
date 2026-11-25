# 玩家部位 OBB / Player Part OBBs

## 简体中文

玩家默认使用原版命中判定和实体推挤。需要识别玩家部位的模组，可以在自己的通用源码中加入下方的 `PlayerBoneHitboxMixin.java`。

1. 将下方代码保存为 `PlayerBoneHitboxMixin.java`，放入你模组的通用源码目录。把 Java 的 `package` 改成你现有 Mixin 配置的 `package`，并在模组中依赖对应 Minecraft 版本和加载器的 KBR。
2. 在该配置的 `mixins` 数组中加入 `"PlayerBoneHitboxMixin"`。如果类放在配置包的 `player` 子包中，就填写 `"player.PlayerBoneHitboxMixin"`。多人联机时，客户端和服务端都安装包含该 Mixin 的模组及对应版本的 KBR。
3. 保留示例中的 `HUMAN_BASE` 可注册头、躯干、四肢和左右手持物。只需要身体部位时改为 `BASE`，或调用 `registrar.register("head")` 等方法逐个选取。

准星射线根据客户端的模型姿态识别部位。原版 `Player.attack` 完成后，KBR 将命中部位和实际伤害交给回调。可通过 `BoneHitboxEvents.registerPlayerPartHurt` 订阅玩家受击部位事件，用 `context.hurtBone()` 读取部位，`context.damageReceived()` 读取损失的生命值和吸收值之和。攻击方监听入口为 `BoneHitboxEvents.registerPlayerPartAttack`。F3+B 和 `client.showPlayerDebugObbs` 只控制调试显示。

早期版本写入存档的玩家 OBB 属性会随玩家数据保留。接入模组注册相应部位后，KBR 读取这些属性。

## English

Players use vanilla hit detection and entity pushing by default. A mod that needs player part selection can copy the `PlayerBoneHitboxMixin.java` example below into its common sources.

1. Save the example below as `PlayerBoneHitboxMixin.java` in your mod's common sources. Match the Java package declaration to the `package` in your existing Mixin configuration and declare a dependency on KBR for your Minecraft version and loader.
2. Add `"PlayerBoneHitboxMixin"` to that configuration's `mixins` array. If you place the class in a `player` subpackage, use `"player.PlayerBoneHitboxMixin"`. For multiplayer, install the mod containing this Mixin and the matching KBR version on both the client and server.
3. Use `HUMAN_BASE` for the head, torso, limbs, and both held items. Use `BASE` for body parts alone, or choose individual parts with calls such as `registrar.register("head")`.

The crosshair ray resolves a part using the client model pose. Once the vanilla `Player.attack` finishes, KBR passes the selected part and actual damage to its callbacks. Subscribe with `BoneHitboxEvents.registerPlayerPartHurt` to read the hit part from `context.hurtBone()` and the total health and absorption lost from `context.damageReceived()`. Use `BoneHitboxEvents.registerPlayerPartAttack` for the attacker-side callback. F3+B and `client.showPlayerDebugObbs` control debug display.

Player OBB attributes saved by earlier releases stay with player data. KBR loads the attributes when a mod registers the corresponding parts.

## 示例 / Example

```java
package com.kltyton.bedrockruntime.example.playerobb.mixin;

import com.kltyton.bedrockruntime.api.entity.BoneHitboxEntity;
import com.kltyton.bedrockruntime.api.registration.registrar.ObbBoneRegistrar;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Player.class)
public abstract class PlayerBoneHitboxMixin implements BoneHitboxEntity {
    @Override
    public void kltyton_bedrock_runtime$registerObbBones(ObbBoneRegistrar registrar) {
        registrar.register(ObbBoneRegistrar.HUMAN_BASE);
    }
}
```
