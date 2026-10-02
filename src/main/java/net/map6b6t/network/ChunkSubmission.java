package net.map6b6t.network;

import java.util.Objects;

public final class ChunkSubmission {
    public final String dimension;
    public final int chunkX;
    public final int chunkZ;
    public final String playerName;
    public final String serverVersion;
    public final byte[] rawBytes;
    public final int contentHash;
    public final byte[] preEncodedGzip;
    public final Object proof = null;
    public final Key key;

    private int attempts;
    private long nextAttemptAtMs;

    public ChunkSubmission(
            String dimension,
            int chunkX,
            int chunkZ,
            String playerName,
            String serverVersion,
            byte[] rawBytes,
            int contentHash
    ) {
        this(dimension, chunkX, chunkZ, playerName, serverVersion, rawBytes, contentHash, null);
    }

    public ChunkSubmission(
            String dimension,
            int chunkX,
            int chunkZ,
            String playerName,
            String serverVersion,
            byte[] rawBytes,
            int contentHash,
            byte[] preEncodedGzip
    ) {
        this.dimension = dimension == null || dimension.isBlank() ? "minecraft:overworld" : dimension;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.playerName = playerName;
        this.serverVersion = serverVersion;
        this.rawBytes = rawBytes;
        this.contentHash = contentHash;
        this.preEncodedGzip = preEncodedGzip;
        this.key = new Key(this.dimension, this.chunkX, this.chunkZ);
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

    public static int contentHash(byte[] rawBytes) {
        return java.util.Arrays.hashCode(rawBytes);
    }

    public static final class Key {
        public final String dimension;
        public final int chunkX;
        public final int chunkZ;
        private final int hash;

        public Key(String dimension, int chunkX, int chunkZ) {
            this.dimension = dimension;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.hash = Objects.hash(dimension, chunkX, chunkZ);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return chunkX == key.chunkX && chunkZ == key.chunkZ && dimension.equals(key.dimension);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
