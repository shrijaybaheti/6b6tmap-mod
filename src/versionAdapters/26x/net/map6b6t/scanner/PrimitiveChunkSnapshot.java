package net.map6b6t.scanner;

public class PrimitiveChunkSnapshot {
    public final long[] blocks;
    public final int size;

    private static final java.util.concurrent.ConcurrentLinkedQueue<long[]> POOL = new java.util.concurrent.ConcurrentLinkedQueue<>();

    public static long[] borrowArray() {
        long[] arr = POOL.poll();
        return arr != null ? arr : new long[8192];
    }

    public static void releaseArray(long[] arr) {
        if (arr != null && arr.length <= 32768 && POOL.size() < 16) {
            POOL.offer(arr);
        }
    }

    public PrimitiveChunkSnapshot(long[] blocks, int size) {
        this.blocks = blocks;
        this.size = size;
    }

    public void release() {
        releaseArray(this.blocks);
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (int i = 0; i < size; i++) {
            long element = blocks[i];
            int elementHash = (int)(element ^ (element >>> 32));
            result = 31 * result + elementHash;
        }
        return result;
    }
}
