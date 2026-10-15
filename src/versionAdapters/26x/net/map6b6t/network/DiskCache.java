package net.map6b6t.network;

import net.fabricmc.loader.api.FabricLoader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class DiskCache {
    private static final File CACHE_DIR = FabricLoader.getInstance().getConfigDir().resolve("map6b6t_queue").toFile();

    static {
        if (!CACHE_DIR.exists()) {
            CACHE_DIR.mkdirs();
        }
    }

    public static File getFile(String dimension, int chunkX, int chunkZ) {
        String dimName = dimension.replace(":", "_").replace("/", "_");
        return new File(CACHE_DIR, dimName + "_" + chunkX + "_" + chunkZ + ".gz");
    }

    public static void save(String dimension, int chunkX, int chunkZ, byte[] gzipBytes) {
        try {
            if (!CACHE_DIR.exists()) CACHE_DIR.mkdirs();
            Files.write(getFile(dimension, chunkX, chunkZ).toPath(), gzipBytes);
        } catch (IOException ignored) {}
    }

    public static byte[] load(String dimension, int chunkX, int chunkZ) {
        try {
            File f = getFile(dimension, chunkX, chunkZ);
            if (f.exists()) {
                return Files.readAllBytes(f.toPath());
            }
        } catch (IOException ignored) {}
        return null;
    }

    public static void delete(String dimension, int chunkX, int chunkZ) {
        getFile(dimension, chunkX, chunkZ).delete();
    }
}
