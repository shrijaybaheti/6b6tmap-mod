package net.map6b6t.scanner;

public class ScannedBlock {
    public final int x;
    public final int y;
    public final int z;
    public final String block;

    public ScannedBlock(int x, int y, int z, String block) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.block = block;
    }
}
