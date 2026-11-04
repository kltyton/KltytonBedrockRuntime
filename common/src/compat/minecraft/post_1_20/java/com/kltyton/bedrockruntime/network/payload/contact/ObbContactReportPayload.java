package com.kltyton.bedrockruntime.network.payload.contact;

import java.util.List;

import com.kltyton.bedrockruntime.Constants;
import com.kltyton.bedrockruntime.api.bone.key.ObbBoneKey;
import com.kltyton.bedrockruntime.api.bone.collision.ObbCollisionMode;
import com.kltyton.bedrockruntime.api.context.collision.ObbCollisionPhase;
import com.kltyton.bedrockruntime.api.context.collision.ObbContactKind;
import com.kltyton.bedrockruntime.network.payload.ObbPayloadIO;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

/**
 * CN: 客户端完成 SAT 后发送的批量接触摘要；服务端负责解析、去重、事件派发及可选 OBB 物理响应。
 * EN: Batched contact summary sent after client SAT; the server resolves, deduplicates, dispatches events, and applies optional OBB physics.
 */
public record ObbContactReportPayload(long observedGameTime, Vec3 reporterMovement, List<Contact> contacts)
        implements CustomPacketPayload {
    public static final int MAX_CONTACTS = 512;

    public static final Type<ObbContactReportPayload> TYPE = new Type<>(Constants.id("contact_report"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ObbContactReportPayload> CODEC =
            StreamCodec.ofMember(ObbContactReportPayload::write, ObbContactReportPayload::read);

    public ObbContactReportPayload {
        reporterMovement = finiteMovement(reporterMovement);
        contacts = List.copyOf(contacts.size() > MAX_CONTACTS ? contacts.subList(0, MAX_CONTACTS) : contacts);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        ObbPayloadIO.writeContactReport(buffer, this);
    }

    private static ObbContactReportPayload read(RegistryFriendlyByteBuf buffer) {
        return ObbPayloadIO.readContactReport(buffer);
    }

    private static Vec3 finiteMovement(Vec3 movement) {
        if (movement == null
                || !Double.isFinite(movement.x())
                || !Double.isFinite(movement.y())
                || !Double.isFinite(movement.z())) {
            return Vec3.ZERO;
        }
        double length = movement.length();
        return length > 4.0 ? movement.scale(4.0 / length) : movement;
    }

    public record Contact(
            ObbContactKind kind,
            ObbCollisionPhase phase,
            int firstEntityId,
            ObbBoneKey firstBone,
            int secondEntityId,
            ObbBoneKey secondBone,
            ObbCollisionMode collisionMode,
            Vec3 correction) {
    }
}
