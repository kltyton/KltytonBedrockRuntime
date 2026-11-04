package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleEngine;

public final class CameraRotationQuery extends MathFunction {
    private final MathValue axis;

    public CameraRotationQuery(MathValue... values) {
        super(values);
        axis = values[0];
    }

    @Override
    public double compute(MolangInstance instance) {
        double selected = axis.get(instance);
        if (selected != 0 && selected != 1) throw new IllegalArgumentException("Camera rotation axis must be 0 or 1");
        Camera camera = camera(instance);
        if (camera == null) return 0.0;
        return selected == 0 ? camera.getXRot() : camera.getYRot();
    }

    public static Camera camera(MolangInstance instance) {
        var manager = MolangParticleEngine.managerFor(instance);
        return manager != null && manager.isIndependentParticles()
                ? manager.previewCamera() : Minecraft.getInstance().gameRenderer.getMainCamera();
    }

    @Override public String getName() { return "query.camera_rotation"; }
    @Override public int getMinArgs() { return 1; }
    @Override public void validate(MathValue... values) {
        if (values.length != 1) throw new IllegalArgumentException("Camera rotation requires one axis");
    }
    @Override public MathValue[] getArgs() { return new MathValue[]{axis}; }
    @Override public boolean isMutable(MathValue... values) { return true; }
}
