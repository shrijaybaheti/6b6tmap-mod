package net.map6b6t.network;

import net.map6b6t.scanner.ScannedBlock;

import java.util.List;
import java.util.Objects;

public final class ChunkSubmission {
    public final String dimension;
    public final int chunkX;
    public final int chunkZ;
    public final String playerName;
    public final String serverVersion;
    public final List<ScannedBlock> blocks;
    public final int contentHash;
    public final byte[] preEncodedGzip;
    public final Object proof;
    public final Key key;

    private int attempts;
    private long nextAttemptAtMs;

    public ChunkSubmission(
            String dimension,
            int chunkX,
            int chunkZ,
            String playerName,
            String serverVersion,
            List<ScannedBlock> blocks,
            int contentHash
    ) {
        this(dimension, chunkX, chunkZ, playerName, serverVersion, blocks, contentHash, null);
    }

    public ChunkSubmission(
            String dimension,
            int chunkX,
            int chunkZ,
            String playerName,
            String serverVersion,
            List<ScannedBlock> blocks,
            int contentHash,
            byte[] preEncodedGzip
    ) {
        this.dimension = dimension == null || dimension.isBlank() ? "minecraft:overworld" : dimension;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.playerName = playerName;
        this.serverVersion = serverVersion;
        this.blocks = blocks;
        this.contentHash = contentHash;
        this.preEncodedGzip = preEncodedGzip;
        this.proof = null;
        this.key = new Key(this.dimension, chunkX, chunkZ);
        this.nextAttemptAtMs = 0L;
    }

    public static int contentHash(List<ScannedBlock> blocks) {
        int h = blocks.size();
        for (int i = 0; i < blocks.size(); i++) {
            ScannedBlock b = blocks.get(i);
            h = 31 * h + b.x;
            h = 31 * h + b.y;
            h = 31 * h + b.z;
            h = 31 * h + (b.block == null ? 0 : b.block.hashCode());
        }
        return h;
    }

    public int attempts() {
        return attempts;
    }

    public void markRetry(long nextAttemptAtMs) {
        this.attempts++;
        this.nextAttemptAtMs = nextAttemptAtMs;
    }

    public long nextAttemptAtMs() {
        return nextAttemptAtMs;
    }

    public boolean due(long nowMs) {
        return nowMs >= nextAttemptAtMs;
    }

    public static final class Key {
        public final String dimension;
        public final int chunkX;
        public final int chunkZ;

        public Key(String dimension, int chunkX, int chunkZ) {
            this.dimension = dimension;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return chunkX == key.chunkX && chunkZ == key.chunkZ && Objects.equals(dimension, key.dimension);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dimension, chunkX, chunkZ);
        }
    }
}
