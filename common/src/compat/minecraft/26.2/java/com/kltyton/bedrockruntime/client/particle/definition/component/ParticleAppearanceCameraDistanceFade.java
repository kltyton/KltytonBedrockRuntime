package com.kltyton.bedrockruntime.client.particle.definition.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import com.kltyton.bedrockruntime.client.particle.PSClientConfigs;
import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.api.IParticleComponent;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.molang.RenderFrameContext;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleEngine;

import java.util.List;

/** Applies the fixed near-camera alpha ramp used by the effect. */
public enum ParticleAppearanceCameraDistanceFade implements IParticleComponent {
    INSTANCE;

    public static final Identifier ID = Identifier.withDefaultNamespace("particle_appearance_camera_distance_fade");
    public static final Codec<ParticleAppearanceCameraDistanceFade> CODEC = MapCodec.of(Encoder.empty(), Decoder.unit(INSTANCE)).codec();

    @Override
    public Codec<ParticleAppearanceCameraDistanceFade> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of();
    }

    public static boolean appliesTo(String identifier) {
        return switch (identifier) {
            case "minecraft:geyser_base", "minecraft:geyser_poof", "minecraft:geyser_plume",
                    "minecraft:noxious_gas_particle" -> true;
            default -> false;
        };
    }

    @Override
    public void apply(IMolangParticleInstance instance) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && !PSClientConfigs.cameraDistanceFade) return;
        var manager = MolangParticleEngine.managerFor(instance);
        Camera camera = manager != null && manager.isIndependentParticles()
                ? manager.previewCamera() : minecraft.gameRenderer.mainCamera();
        if (camera == null) return;
        Vector3f position = instance.getRenderWorldPosition(new Vector3f(), RenderFrameContext.framePartialTick());
        Vec3 cameraPosition = camera.position();
        float fade = (float) Math.min(1, Math.sqrt(cameraPosition.distanceToSqr(position.x, position.y, position.z)));
        Float red = instance.getColorComponent(0);
        Float green = instance.getColorComponent(1);
        Float blue = instance.getColorComponent(2);
        Float tintAlpha = instance.getColorComponent(3);
        instance.setColor(red == null ? 1.0F : red, green == null ? 1.0F : green,
                blue == null ? 1.0F : blue, (tintAlpha == null ? 1.0F : tintAlpha) * fade);
    }
}
