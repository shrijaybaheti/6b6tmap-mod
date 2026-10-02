package net.map6b6t.scanner;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class RawChunkExtractor {
    public static byte[] extractSectionsData(Object chunkDataS2CPacket) {
        try {
            // Get ChunkData object
            Object chunkData = null;
            try {
                Method getChunkData = chunkDataS2CPacket.getClass().getMethod("getChunkData");
                chunkData = getChunkData.invoke(chunkDataS2CPacket);
            } catch (Exception e) {
                // Obfuscated / Yarn fallback
                for (Field f : chunkDataS2CPacket.getClass().getDeclaredFields()) {
                    f.setAccessible(true);
                    Object val = f.get(chunkDataS2CPacket);
                    if (val != null && val.getClass().getSimpleName().contains("ChunkData")) {
                        chunkData = val;
                        break;
                    }
                }
            }
            
            if (chunkData == null) return null;

            // Get sectionsData byte[]
            for (Field f : chunkData.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(chunkData);
                if (val instanceof byte[]) {
                    return (byte[]) val;
                } else if (val != null && val.getClass().getSimpleName().contains("PacketByteBuf")) {
                    try {
                        Method arrayMethod = val.getClass().getMethod("array");
                        return (byte[]) arrayMethod.invoke(val);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
