package com.kltyton.bedrockruntime.client.particle.api.geckolib;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public interface WithCurrentEntity {
    @Nullable Entity getCurrentEntity();

    void setCurrentEntity(@Nullable Entity entity);
}
