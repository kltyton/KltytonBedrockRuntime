package com.kltyton.bedrockruntime.network.payload.entity;

import java.util.List;
import java.util.Set;

import com.kltyton.bedrockruntime.Constants;
import com.kltyton.bedrockruntime.api.bone.attribute.ObbBoneAttribute;
import com.kltyton.bedrockruntime.api.bone.collision.ObbCollisionMode;
import com.kltyton.bedrockruntime.api.geckolib.state.GeoObbAnimationState;
import com.kltyton.bedrockruntime.network.payload.ObbPayloadIO;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

import org.joml.Matrix4f;

/**
 * CN: 客户端从真实模型渲染管线提取并同步给服务端的实体 OBB 状态。
 * EN: Entity OBB state extracted from the real client model pipeline and synchronized to the server.
 */
public record ObbEntityPartsPayload(int entityId, GeoObbAnimationState animationState, List<Part> parts,
        List<GeoObbAnimationState> controllerStates) {
    public static final int MAX_PARTS = 384;
    public static final int MAX_CONTROLLERS = 64;

    public static final ResourceLocation TYPE = Constants.id("entity_parts_v3");

    public ObbEntityPartsPayload(int entityId, List<Part> parts) {
        this(entityId, GeoObbAnimationState.NONE, parts);
    }

    public ObbEntityPartsPayload(int entityId, GeoObbAnimationState animationState, List<Part> parts) {
        this(entityId, animationState, parts, animationState != null && animationState.active() ? List.of(animationState) : List.of());
    }

    public ObbEntityPartsPayload {
        animationState = animationState == null ? GeoObbAnimationState.NONE : animationState;
        controllerStates = List.copyOf(controllerStates);
        if (controllerStates.size() > MAX_CONTROLLERS) { throw new IllegalArgumentException("Too many animation controllers"); }
        parts = List.copyOf(parts.size() > MAX_PARTS ? parts.subList(0, MAX_PARTS) : parts);
    }

    public void write(FriendlyByteBuf buffer) {
        ObbPayloadIO.writeEntityParts(buffer, this);
    }

    public static ObbEntityPartsPayload read(FriendlyByteBuf buffer) {
        return ObbPayloadIO.readEntityParts(buffer);
    }

    /**
     * CN: 单个 cube/bone group 的服务端 OBB 输入数据。
     * EN: Server OBB input data for one cube or bone group.
     */
    public record Part(String source, String partName, int cubeIndex, AABB localBounds, AABB worldBounds,
            Matrix4f localToWorld, Matrix4f worldToLocal, Set<ObbBoneAttribute> attributes,
            ObbCollisionMode collisionMode) {
        public Part(String source, String partName, int cubeIndex, AABB localBounds, AABB worldBounds,
                Matrix4f localToWorld, Matrix4f worldToLocal) {
            this(source, partName, cubeIndex, localBounds, worldBounds, localToWorld, worldToLocal, Set.of(), ObbCollisionMode.NONE);
        }

        public Part {
            localToWorld = new Matrix4f(localToWorld);
            worldToLocal = new Matrix4f(worldToLocal);
            attributes = attributes == null ? Set.of() : Set.copyOf(attributes);
            collisionMode = collisionMode == null ? ObbCollisionMode.NONE : collisionMode;
        }

        @Override public Matrix4f localToWorld() { return new Matrix4f(localToWorld); }
        @Override public Matrix4f worldToLocal() { return new Matrix4f(worldToLocal); }
    }
}
