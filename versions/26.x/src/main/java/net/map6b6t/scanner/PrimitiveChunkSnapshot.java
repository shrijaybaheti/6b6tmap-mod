package net.map6b6t.scanner;

public class PrimitiveChunkSnapshot {
    public final long[] blocks;
    public final int size;

    public PrimitiveChunkSnapshot(long[] blocks, int size) {
        this.blocks = blocks;
        this.size = size;
    }
}
