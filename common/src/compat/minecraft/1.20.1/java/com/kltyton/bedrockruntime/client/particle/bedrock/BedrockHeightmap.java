package com.kltyton.bedrockruntime.client.particle.bedrock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLongArray;

/** Caches only the block columns requested by particle height expressions. */
public final class BedrockHeightmap {
    private static final Map<LevelChunk, Columns> COLUMNS = new WeakHashMap<>();
    private static final int UNKNOWN = Integer.MIN_VALUE;
    private static final long UNKNOWN_BITS = Integer.toUnsignedLong(UNKNOWN);

    private BedrockHeightmap() {
    }

    public static int height(Level level, int x, int z) {
        if (!level.hasChunk(x >> 4, z >> 4)) return level.getMinBuildHeight();
        LevelChunk chunk = level.getChunk(x >> 4, z >> 4);
        Columns columns;
        synchronized (COLUMNS) {
            columns = COLUMNS.computeIfAbsent(chunk, ignored -> new Columns());
        }
        return columns.height(chunk, x, z);
    }

    public static void blockChanged(LevelChunk chunk, BlockPos position) {
        Columns columns;
        synchronized (COLUMNS) {
            columns = COLUMNS.get(chunk);
        }
        if (columns != null) columns.invalidate(index(position.getX(), position.getZ()));
    }

    public static void chunkReplaced(LevelChunk chunk) {
        synchronized (COLUMNS) {
            COLUMNS.remove(chunk);
        }
    }

    private static int index(int x, int z) {
        return (x & 15) | (z & 15) << 4;
    }

    private static int scan(LevelChunk chunk, int x, int z) {
        int top = chunk.getMaxBuildHeight() - 1;
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos(x, top, z);
        while (position.getY() >= chunk.getMinBuildHeight()) {
            if (BedrockBlockLight.stopsHeightmap(chunk.getBlockState(position))) return position.getY() + 1;
            position.move(Direction.DOWN);
        }
        return chunk.getMinBuildHeight();
    }

    private static final class Columns {
        // The upper word tracks invalidation so an old scan cannot republish a stale height.
        private final AtomicLongArray values = new AtomicLongArray(256);

        private Columns() {
            for (int i = 0; i < values.length(); i++) values.set(i, UNKNOWN_BITS);
        }

        private int height(LevelChunk chunk, int x, int z) {
            int index = index(x, z);
            while (true) {
                long current = values.get(index);
                if ((int) current != UNKNOWN) return (int) current;
                int height = scan(chunk, x, z);
                long next = current & 0xFFFF_FFFF_0000_0000L | Integer.toUnsignedLong(height);
                if (values.compareAndSet(index, current, next)) return height;
            }
        }

        private void invalidate(int index) {
            values.getAndUpdate(index, value -> (value + 0x1_0000_0000L & 0xFFFF_FFFF_0000_0000L) | UNKNOWN_BITS);
        }
    }
}
