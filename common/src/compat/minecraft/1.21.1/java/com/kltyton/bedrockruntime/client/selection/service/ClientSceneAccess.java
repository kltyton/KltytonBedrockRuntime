package com.kltyton.bedrockruntime.client.selection.service;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

final class ClientSceneAccess {
    private ClientSceneAccess() { }
    static Camera camera(Minecraft minecraft) { return minecraft.gameRenderer.getMainCamera(); }
    static Vec3 position(Camera camera) { return camera.getPosition(); }
    static Vector3fc forward(Camera camera) { return camera.getLookVector(); }
    static void ground(Player player, Vec3 movement) { player.setOnGroundWithMovement(true, movement); }
}
