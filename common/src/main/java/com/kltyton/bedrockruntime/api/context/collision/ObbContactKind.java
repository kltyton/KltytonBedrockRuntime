package com.kltyton.bedrockruntime.api.context.collision;

/**
 * CN: 客户端 OBB 接触汇总的语义种类；ATTACK 是有方向的属性事件，COLLISION 是所有盒的通用接触。
 * EN: Client contact kind; ATTACK is directional attribute semantics while COLLISION is generic contact for all boxes.
 */
public enum ObbContactKind {
    ATTACK,
    COLLISION
}
