package com.kltyton.bedrockruntime.client.particle.api;


import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.definition.molang.VariableTable;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;
import java.util.Map;

public interface MolangInstance {
    /** The table object is owned by this instance; explicit parents and binding views may share values. */
    VariableTable getVars();

    default Map<String, ?> getMolangContext() {
        return Map.of();
    }

    default @Nullable ItemStack getReferencedItem() {
        return null;
    }

    default @Nullable Boolean getFirstPersonPerspective() {
        return null;
    }

    Level getLevel();

    float tickAge();

    float tickLifetime();

    double getRandom1();

    double getRandom2();

    double getRandom3();

    double getRandom4();

    ResourceLocation getIdentity();

    Vec3 getPosition();

    @Nullable Entity getAttachedEntity();

    default @Nullable ParticleEmitterAttachable getAttached() {
        ParticleEmitter emitter = getEmitter();
        return emitter == null ? null : emitter.getAttached();
    }

    float getInvTickRate();

    @Nullable ParticleEmitter getEmitter();
}
