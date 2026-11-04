package com.kltyton.bedrockruntime.server.combat.damage;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.UUID;

import com.kltyton.bedrockruntime.api.context.damage.ObbDamageInfo;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

/**
 * CN: 短暂保存原版 LivingEntity 伤害结果，供紧随其后的攻击部位回调消费。
 * EN: Briefly stores vanilla LivingEntity damage results for the immediately following part-attack callback.
 */
public final class VanillaDamageCapture {
    private static final int MAX_CAPTURES = 256;
    private static final long MAX_AGE_TICKS = 2L;
    private static final Deque<Capture> CAPTURES = new ArrayDeque<>();

    private VanillaDamageCapture() {
    }

    public static synchronized void record(Entity target, DamageSource source, ObbDamageInfo damage) {
        long gameTime = target.level().getGameTime();
        cleanup(gameTime);
        CAPTURES.addLast(new Capture(
                target.getUUID(),
                uuid(source.getDirectEntity()),
                uuid(source.getEntity()),
                gameTime,
                damage));
        while (CAPTURES.size() > MAX_CAPTURES) {
            CAPTURES.removeFirst();
        }
    }

    public static synchronized ObbDamageInfo consume(Entity attacker, Entity target) {
        long gameTime = target.level().getGameTime();
        cleanup(gameTime);
        Iterator<Capture> iterator = CAPTURES.descendingIterator();
        DamageSource source = null;
        float requestedDamage = 0.0F;
        float healthDamage = 0.0F;
        float absorptionDamage = 0.0F;
        boolean successful = false;
        boolean matched = false;
        while (iterator.hasNext()) {
            Capture capture = iterator.next();
            if (capture.target().equals(target.getUUID())
                    && (attacker.getUUID().equals(capture.direct()) || attacker.getUUID().equals(capture.causing()))) {
                iterator.remove();
                ObbDamageInfo damage = capture.damage();
                if (source == null) {
                    source = damage.source().orElse(null);
                }
                requestedDamage += damage.requestedDamage();
                healthDamage += damage.healthDamage();
                absorptionDamage += damage.absorptionDamage();
                successful |= damage.successful();
                matched = true;
            }
        }
        return matched
                ? ObbDamageInfo.captured(source, requestedDamage, healthDamage, absorptionDamage, successful)
                : ObbDamageInfo.NONE;
    }

    private static void cleanup(long gameTime) {
        CAPTURES.removeIf(capture -> gameTime - capture.gameTime() > MAX_AGE_TICKS);
    }

    private static UUID uuid(Entity entity) {
        return entity == null ? null : entity.getUUID();
    }

    private record Capture(UUID target, UUID direct, UUID causing, long gameTime, ObbDamageInfo damage) {
    }
}
