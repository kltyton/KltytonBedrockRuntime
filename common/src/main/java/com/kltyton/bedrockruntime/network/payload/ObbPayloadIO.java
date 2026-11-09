package com.kltyton.bedrockruntime.network.payload;

import java.util.ArrayList;
import java.util.List;

import com.kltyton.bedrockruntime.api.bone.attribute.ObbBoneAttribute;
import com.kltyton.bedrockruntime.api.bone.collision.ObbCollisionMode;
import com.kltyton.bedrockruntime.api.bone.key.ObbBoneKey;
import com.kltyton.bedrockruntime.api.context.collision.ObbCollisionPhase;
import com.kltyton.bedrockruntime.api.context.collision.ObbContactKind;
import com.kltyton.bedrockruntime.api.geckolib.state.GeoObbAnimationState;
import com.kltyton.bedrockruntime.network.payload.contact.ObbContactReportPayload;
import com.kltyton.bedrockruntime.network.payload.entity.ObbEntityPartsPayload;
import com.kltyton.bedrockruntime.network.payload.selection.ObbPartSelectionPayload;
import com.kltyton.bedrockruntime.network.payload.skill.GeoKeyframeSkillPayload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;

/**
 * CN: 与原版网络类型无关的共享 payload 编解码核心。
 * EN: Shared payload codec core independent of the native packet-wrapper types.
 */
public final class ObbPayloadIO {
    private static final int MAX_FIELD_LENGTH = 128;

    private ObbPayloadIO() {
    }

    public static void writeContactReport(FriendlyByteBuf buffer, ObbContactReportPayload payload) {
        buffer.writeVarLong(payload.observedGameTime());
        buffer.writeFloat((float) payload.reporterMovement().x());
        buffer.writeFloat((float) payload.reporterMovement().y());
        buffer.writeFloat((float) payload.reporterMovement().z());
        buffer.writeVarInt(payload.contacts().size());
        payload.contacts().forEach(contact -> writeContact(buffer, contact));
    }

    public static ObbContactReportPayload readContactReport(FriendlyByteBuf buffer) {
        long observedGameTime = buffer.readVarLong();
        Vec3 reporterMovement = new Vec3(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
        int declaredCount = buffer.readVarInt();
        if (declaredCount < 0 || declaredCount > ObbContactReportPayload.MAX_CONTACTS) {
            throw new IllegalArgumentException("Invalid OBB contact count: " + declaredCount);
        }
        List<ObbContactReportPayload.Contact> contacts = new ArrayList<>(declaredCount);
        for (int i = 0; i < declaredCount; i++) {
            contacts.add(readContact(buffer));
        }
        return new ObbContactReportPayload(observedGameTime, reporterMovement, contacts);
    }

    private static void writeContact(FriendlyByteBuf buffer, ObbContactReportPayload.Contact contact) {
        buffer.writeVarInt(contact.kind().ordinal());
        buffer.writeVarInt(contact.phase().ordinal());
        buffer.writeVarInt(contact.firstEntityId());
        writeBone(buffer, contact.firstBone());
        buffer.writeVarInt(contact.secondEntityId());
        writeBone(buffer, contact.secondBone());
        buffer.writeVarInt(contact.collisionMode().ordinal());
        buffer.writeFloat((float) contact.correction().x());
        buffer.writeFloat((float) contact.correction().y());
        buffer.writeFloat((float) contact.correction().z());
    }

    private static ObbContactReportPayload.Contact readContact(FriendlyByteBuf buffer) {
        ObbContactKind kind = enumValue(ObbContactKind.values(), buffer.readVarInt(), ObbContactKind.COLLISION);
        ObbCollisionPhase phase = enumValue(ObbCollisionPhase.values(), buffer.readVarInt(), ObbCollisionPhase.BEGIN);
        int firstEntityId = buffer.readVarInt();
        ObbBoneKey firstBone = readBone(buffer);
        int secondEntityId = buffer.readVarInt();
        ObbBoneKey secondBone = readBone(buffer);
        ObbCollisionMode collisionMode = enumValue(ObbCollisionMode.values(), buffer.readVarInt(), ObbCollisionMode.NONE);
        Vec3 correction = new Vec3(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
        return new ObbContactReportPayload.Contact(
                kind, phase, firstEntityId, firstBone, secondEntityId, secondBone, collisionMode, correction);
    }

    public static void writeEntityParts(FriendlyByteBuf buffer, ObbEntityPartsPayload payload) {
        buffer.writeVarInt(payload.entityId());
        buffer.writeVarInt(payload.captureAgeTicks());
        writeAnimationState(buffer, payload.animationState());
        buffer.writeVarInt(payload.controllerStates().size());
        for (GeoObbAnimationState state : payload.controllerStates()) {
            writeAnimationState(buffer, state);
        }
        buffer.writeVarInt(payload.parts().size());
        for (ObbEntityPartsPayload.Part part : payload.parts()) {
            writePart(buffer, part);
        }
    }

    public static ObbEntityPartsPayload readEntityParts(FriendlyByteBuf buffer) {
        int entityId = buffer.readVarInt();
        int captureAgeTicks = buffer.readVarInt();
        if (captureAgeTicks < 0 || captureAgeTicks > ObbEntityPartsPayload.MAX_CAPTURE_AGE_TICKS) {
            throw new IllegalArgumentException("Invalid OBB capture age: " + captureAgeTicks);
        }
        GeoObbAnimationState animationState = readAnimationState(buffer);
        int controllerCount = buffer.readVarInt();
        if (controllerCount < 0 || controllerCount > ObbEntityPartsPayload.MAX_CONTROLLERS) {
            throw new IllegalArgumentException("Invalid controller count");
        }
        List<GeoObbAnimationState> controllers = new ArrayList<>(controllerCount);
        for (int i = 0; i < controllerCount; i++) {
            controllers.add(readAnimationState(buffer));
        }
        int declaredCount = buffer.readVarInt();
        if (declaredCount < 0 || declaredCount > ObbEntityPartsPayload.MAX_PARTS) {
            throw new IllegalArgumentException("Invalid OBB part count: " + declaredCount);
        }
        if (declaredCount == 0) {
            return new ObbEntityPartsPayload(entityId, animationState, List.of(), controllers, captureAgeTicks);
        }
        List<ObbEntityPartsPayload.Part> parts = new ArrayList<>(declaredCount);
        for (int i = 0; i < declaredCount; i++) {
            parts.add(readPart(buffer));
        }
        return new ObbEntityPartsPayload(entityId, animationState, parts, controllers, captureAgeTicks);
    }

    public static void writePart(FriendlyByteBuf buffer, ObbEntityPartsPayload.Part part) {
        buffer.writeUtf(part.source(), MAX_FIELD_LENGTH);
        buffer.writeUtf(part.partName(), MAX_FIELD_LENGTH);
        buffer.writeVarInt(part.cubeIndex());
        writeAabb(buffer, part.localBounds());
        writeAabb(buffer, part.worldBounds());
        writeMatrix(buffer, part.localToWorld());
        writeMatrix(buffer, part.worldToLocal());
        buffer.writeVarInt(ObbBoneAttribute.toMask(part.attributes()));
        buffer.writeVarInt(part.collisionMode().ordinal());
    }

    public static ObbEntityPartsPayload.Part readPart(FriendlyByteBuf buffer) {
        return new ObbEntityPartsPayload.Part(
                buffer.readUtf(MAX_FIELD_LENGTH),
                buffer.readUtf(MAX_FIELD_LENGTH),
                buffer.readVarInt(),
                readAabb(buffer),
                readAabb(buffer),
                readMatrix(buffer),
                readMatrix(buffer),
                ObbBoneAttribute.fromMask(buffer.readVarInt()),
                collisionMode(buffer.readVarInt()));
    }

    public static void writeSelection(FriendlyByteBuf buffer, ObbPartSelectionPayload payload) {
        buffer.writeVarInt(payload.entityId());
        buffer.writeUtf(payload.source(), MAX_FIELD_LENGTH);
        buffer.writeUtf(payload.partName(), MAX_FIELD_LENGTH);
        buffer.writeVarInt(payload.cubeIndex());
    }

    public static ObbPartSelectionPayload readSelection(FriendlyByteBuf buffer) {
        return new ObbPartSelectionPayload(
                buffer.readVarInt(),
                buffer.readUtf(MAX_FIELD_LENGTH),
                buffer.readUtf(MAX_FIELD_LENGTH),
                buffer.readVarInt());
    }

    public static void writeSkill(FriendlyByteBuf buffer, GeoKeyframeSkillPayload payload) {
        buffer.writeVarInt(payload.entityId());
        buffer.writeLong(payload.clientSequence());
        buffer.writeLong(payload.observedGameTime());
        buffer.writeUtf(payload.marker(), MAX_FIELD_LENGTH);
        buffer.writeUtf(payload.animationState().controllerName(), MAX_FIELD_LENGTH);
        buffer.writeUtf(payload.animationState().animationName(), MAX_FIELD_LENGTH);
        buffer.writeDouble(payload.animationState().animationTimeSeconds());
        buffer.writeDouble(payload.animationState().timelineTimeSeconds());
        buffer.writeBoolean(payload.animationState().triggered());
        buffer.writeBoolean(payload.animationState().transitioning());
        buffer.writeBoolean(payload.animationState().finished());
        buffer.writeDouble(payload.markerTimeSeconds());
        buffer.writeDouble(payload.animationSpeed());
    }

    public static GeoKeyframeSkillPayload readSkill(FriendlyByteBuf buffer) {
        int entityId = buffer.readVarInt();
        long sequence = buffer.readLong();
        long observedGameTime = buffer.readLong();
        String marker = buffer.readUtf(MAX_FIELD_LENGTH);
        GeoObbAnimationState animationState = new GeoObbAnimationState(
                buffer.readUtf(MAX_FIELD_LENGTH),
                buffer.readUtf(MAX_FIELD_LENGTH),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean());
        return new GeoKeyframeSkillPayload(
                entityId,
                sequence,
                observedGameTime,
                marker,
                animationState,
                buffer.readDouble(),
                buffer.readDouble());
    }

    private static void writeAnimationState(FriendlyByteBuf buffer, GeoObbAnimationState state) {
        buffer.writeUtf(state.controllerName(), MAX_FIELD_LENGTH);
        buffer.writeUtf(state.animationName(), MAX_FIELD_LENGTH);
        buffer.writeDouble(state.animationTimeSeconds());
        buffer.writeDouble(state.timelineTimeSeconds());
        buffer.writeBoolean(state.triggered());
        buffer.writeBoolean(state.transitioning());
        buffer.writeBoolean(state.finished());
    }

    private static GeoObbAnimationState readAnimationState(FriendlyByteBuf buffer) {
        return new GeoObbAnimationState(
                buffer.readUtf(MAX_FIELD_LENGTH),
                buffer.readUtf(MAX_FIELD_LENGTH),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean());
    }

    private static void writeBone(FriendlyByteBuf buffer, ObbBoneKey key) {
        buffer.writeUtf(key.source(), MAX_FIELD_LENGTH);
        buffer.writeUtf(key.name(), MAX_FIELD_LENGTH);
        buffer.writeVarInt(key.cubeIndex());
    }

    private static ObbBoneKey readBone(FriendlyByteBuf buffer) {
        return new ObbBoneKey(
                buffer.readUtf(MAX_FIELD_LENGTH),
                buffer.readUtf(MAX_FIELD_LENGTH),
                Math.max(0, buffer.readVarInt()));
    }

    private static ObbCollisionMode collisionMode(int ordinal) {
        return ordinal >= 0 && ordinal < ObbCollisionMode.values().length
                ? ObbCollisionMode.values()[ordinal]
                : ObbCollisionMode.NONE;
    }

    private static <T> T enumValue(T[] values, int ordinal, T fallback) {
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : fallback;
    }

    private static void writeAabb(FriendlyByteBuf buffer, AABB box) {
        buffer.writeDouble(box.minX);
        buffer.writeDouble(box.minY);
        buffer.writeDouble(box.minZ);
        buffer.writeDouble(box.maxX);
        buffer.writeDouble(box.maxY);
        buffer.writeDouble(box.maxZ);
    }

    private static AABB readAabb(FriendlyByteBuf buffer) {
        return new AABB(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble());
    }

    private static void writeMatrix(FriendlyByteBuf buffer, Matrix4f matrix) {
        buffer.writeFloat(matrix.m00());
        buffer.writeFloat(matrix.m01());
        buffer.writeFloat(matrix.m02());
        buffer.writeFloat(matrix.m03());
        buffer.writeFloat(matrix.m10());
        buffer.writeFloat(matrix.m11());
        buffer.writeFloat(matrix.m12());
        buffer.writeFloat(matrix.m13());
        buffer.writeFloat(matrix.m20());
        buffer.writeFloat(matrix.m21());
        buffer.writeFloat(matrix.m22());
        buffer.writeFloat(matrix.m23());
        buffer.writeFloat(matrix.m30());
        buffer.writeFloat(matrix.m31());
        buffer.writeFloat(matrix.m32());
        buffer.writeFloat(matrix.m33());
    }

    private static Matrix4f readMatrix(FriendlyByteBuf buffer) {
        return new Matrix4f(
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
    }
}
