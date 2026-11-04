package com.kltyton.bedrockruntime.api.state.runtime;


import com.kltyton.bedrockruntime.api.entity.BoneHitboxEntity;
import com.kltyton.bedrockruntime.api.registration.registrar.ObbBoneRegistrar;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * CN: 将 OBB 状态附着到实现 BoneHitboxEntity 的实体实例；数据跟随实体生命周期，不使用全局实体表。
 * EN: Attaches OBB state to BoneHitboxEntity instances; state is owned by the entity, including registrations that capture their owner.
 */
public final class ObbEntityStates {
    public static final String SAVE_KEY = "KltytonBedrockRuntime";

    private ObbEntityStates() {
    }

    public static ObbEntityState get(Entity entity) {
        if (!(entity instanceof BoneHitboxEntity hitboxEntity)) {
            throw new IllegalArgumentException("Entity does not implement BoneHitboxEntity: " + entity.getClass().getName());
        }
        if (!(entity instanceof ObbEntityStateAccess attachment)) {
            throw new IllegalStateException("KltytonBedrockRuntime Entity state mixin is missing");
        }
        synchronized (entity) {
            ObbEntityState state = attachment.kltyton_bedrock_runtime$getAttachedState();
            if (state == null) {
                state = create(hitboxEntity);
                attachment.kltyton_bedrock_runtime$setAttachedState(state);
            }
            return state;
        }
    }

    public static ObbEntityState get(BoneHitboxEntity hitboxEntity) {
        if (!(hitboxEntity instanceof Entity entity)) {
            throw new IllegalStateException("BoneHitboxEntity must also be a Minecraft Entity");
        }
        return get(entity);
    }

    public static void save(Entity entity, ValueOutput output) {
        if (!(entity instanceof BoneHitboxEntity)) {
            return;
        }
        get(entity).save(output.child(SAVE_KEY));
    }

    public static void load(Entity entity, ValueInput input) {
        if (!(entity instanceof BoneHitboxEntity)) {
            return;
        }
        input.child(SAVE_KEY).ifPresent(get(entity)::load);
    }

    private static ObbEntityState create(BoneHitboxEntity entity) {
        ObbBoneRegistrar registrar = new ObbBoneRegistrar();
        entity.kltyton_bedrock_runtime$registerObbBones(registrar);
        var definitions = registrar.definitions();
        if (definitions.isEmpty()) {
            throw new IllegalStateException(entity.getClass().getName()
                    + " implements BoneHitboxEntity but registered no OBB bones");
        }
        return new ObbEntityState(definitions);
    }
}
