package net.map6b6t.scanner;

public class PrimitiveChunkSnapshot {
    public final long[] blocks;
    public final int size;

    public PrimitiveChunkSnapshot(long[] blocks, int size) {
        this.blocks = blocks;
        this.size = size;
    }

    @Override
    public int hashCode() {
        int result = java.util.Arrays.hashCode(blocks);
        result = 31 * result + size;
        return result;
    }
}
