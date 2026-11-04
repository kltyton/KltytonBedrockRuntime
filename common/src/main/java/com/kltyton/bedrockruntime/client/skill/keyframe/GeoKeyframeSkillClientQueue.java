package com.kltyton.bedrockruntime.client.skill.keyframe;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import com.kltyton.bedrockruntime.network.payload.skill.GeoKeyframeSkillPayload;
import com.kltyton.bedrockruntime.network.protocol.BoneHitboxNetworking;

/**
 * CN: 将 render 中产生的 marker 延迟到客户端 tick，在几何快照包之后发送。
 * EN: Defers render-time markers to the client tick so they are sent after geometry snapshot packets.
 */
public final class GeoKeyframeSkillClientQueue {
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private static final Queue<GeoKeyframeSkillPayload> PENDING = new ConcurrentLinkedQueue<>();

    private GeoKeyframeSkillClientQueue() {
    }

    public static long nextSequence() {
        return SEQUENCE.incrementAndGet();
    }

    public static void enqueue(GeoKeyframeSkillPayload payload) {
        PENDING.add(payload);
    }

    public static void flush() {
        GeoKeyframeSkillPayload payload;
        while ((payload = PENDING.poll()) != null) {
            BoneHitboxNetworking.sendClientGeoKeyframeSkill(payload);
        }
    }

    public static void clear() {
        PENDING.clear();
    }
}
