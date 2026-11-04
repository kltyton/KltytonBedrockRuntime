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

    static boolean appliesTo(String identifier) {
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
        var emitter = instance.getEmitter();
        Vector3f position = instance.toSimulationSpace(instance.getX(), instance.getY(), instance.getZ(), new Vector3f());
        Vec3 origin = emitter.getEmissionOrigin();
        Vec3 cameraPosition = camera.position();
        float dx = (float) cameraPosition.x - (position.x + (float) origin.x);
        float dy = (float) cameraPosition.y - (position.y + (float) origin.y);
        float dz = (float) cameraPosition.z - (position.z + (float) origin.z);
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float alpha = distance <= 0.0F ? 0.0F : distance >= 1.0F ? 1.0F : distance;
        Float red = instance.getColorComponent(0);
        Float green = instance.getColorComponent(1);
        Float blue = instance.getColorComponent(2);
        instance.setColor(red == null ? 1.0F : red, green == null ? 1.0F : green,
                blue == null ? 1.0F : blue, alpha);
    }
}
