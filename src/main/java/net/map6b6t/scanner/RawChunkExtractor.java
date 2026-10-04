package net.map6b6t.scanner;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class RawChunkExtractor {
    public static byte[] extractSectionsData(Object chunkDataS2CPacket) {
        try {
            try {
                Method getChunkData = chunkDataS2CPacket.getClass().getMethod("getChunkData");
                Object chunkData = getChunkData.invoke(chunkDataS2CPacket);
                if (chunkData != null) {
                    byte[] b = searchForByteArray(chunkData);
                    if (b != null) return b;
                }
            } catch (Exception ignored) {}

            try {
                Method method = chunkDataS2CPacket.getClass().getMethod("method_38525"); // getChunkData
                Object chunkData = method.invoke(chunkDataS2CPacket);
                if (chunkData != null) {
                    byte[] b = searchForByteArray(chunkData);
                    if (b != null) return b;
                }
            } catch (Exception ignored) {}

            for (Field f : chunkDataS2CPacket.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(chunkDataS2CPacket);
                if (val != null && !val.getClass().isPrimitive() && !(val instanceof String) && !(val instanceof Number) && !(val instanceof Boolean)) {
                    byte[] b = searchForByteArray(val);
                    if (b != null && b.length > 10) {
                        return b;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static byte[] searchForByteArray(Object obj) throws IllegalAccessException {
        for (Field f : obj.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            Object val = f.get(obj);
            if (val instanceof byte[]) {
                return (byte[]) val;
            }
            if (val != null && (val.getClass().getSimpleName().contains("PacketByteBuf") || val.getClass().getName().contains("class_2540"))) {
                try {
                    Method arrayMethod = val.getClass().getMethod("array");
                    return (byte[]) arrayMethod.invoke(val);
                } catch (Exception e) {}
            }
        }
        return null;
    }
}
