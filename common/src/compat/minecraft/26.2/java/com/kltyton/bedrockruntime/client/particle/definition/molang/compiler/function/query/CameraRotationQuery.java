package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleEngine;

public final class CameraRotationQuery extends MathFunction {
    private static final java.util.concurrent.atomic.AtomicBoolean REPORTED_ARGUMENTS = new java.util.concurrent.atomic.AtomicBoolean();
    private final MathValue[] arguments;

    public CameraRotationQuery(MathValue... values) {
        super(values);
        arguments = values.clone();
    }

    @Override
    public double compute(MolangInstance instance) {
        if (arguments.length != 1) return invalidAxis();
        float selected = (float) arguments[0].get(instance);
        if (!(selected > -1.0F && selected < 2.0F)) return invalidAxis();
        if (instance.getReferencedItem() != null) return 0.0;
        Camera camera = camera(instance);
        if (camera == null) return 0.0;
        return (int) selected == 0 ? camera.xRot() : camera.yRot();
    }

    static @Nullable Camera camera(MolangInstance instance) {
        var manager = MolangParticleEngine.managerFor(instance);
        return manager != null && manager.isIndependentParticles()
                ? manager.previewCamera() : Minecraft.getInstance().gameRenderer.mainCamera();
    }

    private double invalidAxis() {
        if (REPORTED_ARGUMENTS.compareAndSet(false, true)) {
            BedrockParticles.LOGGER.warn("query.camera_rotation requires one axis in the range 0 to 1");
        }
        return 0.0;
    }

    @Override public String getName() { return "query.camera_rotation"; }
    @Override public int getMinArgs() { return 0; }
    @Override public MathValue[] getArgs() { return arguments.clone(); }
    @Override public boolean isMutable(MathValue... values) { return true; }
}
