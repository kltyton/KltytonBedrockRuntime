package com.kltyton.bedrockruntime.client.particle.platform;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;

/** Version-isolated access to the active camera for 26.2. */
public final class ParticleCameraAccess {
    private ParticleCameraAccess() {
    }

    public static Camera camera(Minecraft minecraft) {
        return minecraft.gameRenderer.mainCamera();
    }
}
