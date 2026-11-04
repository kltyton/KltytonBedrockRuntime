package com.kltyton.bedrockruntime.client.particle.molang.query;

import com.sun.management.OperatingSystemMXBean;
import com.kltyton.bedrockruntime.particle.BedrockParticles;

import java.lang.management.ManagementFactory;

/** Classifies the client device memory using Bedrock's memory tiers. */
public final class ClientMemoryTier {
    private static final long GIB = 1L << 30;
    private static final int VALUE = detect();

    private ClientMemoryTier() {}

    public static int value() {
        return VALUE;
    }

    private static int detect() {
        var bean = ManagementFactory.getOperatingSystemMXBean();
        if (!(bean instanceof OperatingSystemMXBean operatingSystem)) {
            BedrockParticles.LOGGER.warn("Cannot determine client memory tier: total memory is unavailable");
            return 0;
        }
        long total = operatingSystem.getTotalMemorySize();
        if (total <= 0) {
            BedrockParticles.LOGGER.warn("Cannot determine client memory tier: total memory is unavailable");
            return 0;
        }
        if (total <= GIB + GIB / 2) return 0;
        if (total <= 2 * GIB) return 1;
        if (total <= 4 * GIB) return 2;
        if (total <= 8 * GIB) return 3;
        return 4;
    }
}
