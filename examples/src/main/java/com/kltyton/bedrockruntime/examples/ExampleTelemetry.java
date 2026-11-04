package com.kltyton.bedrockruntime.examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class ExampleTelemetry {
    private static final Logger LOGGER = LoggerFactory.getLogger("kltyton_bedrock_runtime-examples");
    private static final AtomicLong CAPTURES = new AtomicLong();
    private static final AtomicLong TICKS = new AtomicLong();
    private static final Map<String, AtomicLong> SCENES = new ConcurrentHashMap<>();

    private ExampleTelemetry() {
    }

    public static void capture(String scene) {
        CAPTURES.incrementAndGet();
        SCENES.computeIfAbsent(scene, key -> new AtomicLong()).incrementAndGet();
    }

    public static void tick() {
        long ticks = TICKS.incrementAndGet();
        if (ticks % 200 != 0) {
            return;
        }
        long captures = CAPTURES.get();
        if (captures == 0) {
            return;
        }
        StringBuilder counts = new StringBuilder();
        SCENES.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> counts.append(entry.getKey()).append('=').append(entry.getValue().get()).append(' '));
        LOGGER.info("Development example graph captures {} ({})", captures, counts.toString().trim());
    }
}
