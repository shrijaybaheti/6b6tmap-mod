package net.map6b6t.network;

@FunctionalInterface
public interface UploadListener {
    void onUploaded(int chunkX, int chunkZ, int contentHash);
}
