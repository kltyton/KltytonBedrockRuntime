package com.kltyton.bedrockruntime.examples.mixin;

import com.kltyton.bedrockruntime.api.bone.collision.ObbCollisionMode;
import com.kltyton.bedrockruntime.api.entity.BoneHitboxEntity;
import com.kltyton.bedrockruntime.api.registration.registrar.ObbBoneRegistrar;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;

/** Enables OBB registration only for armor stands spawned with the development-scene tag. */
@Mixin(ArmorStand.class)
public abstract class ArmorStandDemoMixin implements BoneHitboxEntity {
    private static final String DEMO_TAG = "kltyton_bedrock_runtime_demo";

    @Override
    public void kltyton_bedrock_runtime$registerObbBones(ObbBoneRegistrar registrar) {
        ArmorStand stand = (ArmorStand) (Object) this;
        if (stand.entityTags().contains(DEMO_TAG)) {
            registrar.register(ObbBoneRegistrar.ALL)
                    .hurt()
                    .collision(ObbCollisionMode.SOFT);
        } else {
            registrar.register(key -> false);
        }
    }
}
