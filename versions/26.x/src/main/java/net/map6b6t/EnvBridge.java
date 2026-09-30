package net.map6b6t;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class EnvBridge {
    private static Field getField(Class<?> clazz, String devName, String prodName) throws Exception {
        try {
            return clazz.getDeclaredField(devName);
        } catch (NoSuchFieldException e) {
            return clazz.getDeclaredField(prodName);
        }
    }

    private static Method getMethod(Class<?> clazz, String devName, String prodName, Class<?>... parameterTypes) throws Exception {
        try {
            return clazz.getDeclaredMethod(devName, parameterTypes);
        } catch (NoSuchMethodException e) {
            return clazz.getDeclaredMethod(prodName, parameterTypes);
        }
    }

    public static long asLong(int x, int z) {
        try {
            Method m = getMethod(ChunkPos.class, "toLong", "method_24022", int.class, int.class);
            return (Long) m.invoke(null, x, z);
        } catch (Exception e) {
            try {
                Method m = getMethod(ChunkPos.class, "asLong", "method_8324", int.class, int.class);
                return (Long) m.invoke(null, x, z);
            } catch (Exception e2) {
                return 0L;
            }
        }
    }

    public static int getChunkX(ChunkPos pos) {
        try {
            try {
                Field f = getField(ChunkPos.class, "x", "field_9181");
                f.setAccessible(true);
                return f.getInt(pos);
            } catch (Exception e) {
                Method m = getMethod(ChunkPos.class, "x", "method_33942");
                m.setAccessible(true);
                return (Integer) m.invoke(pos);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static int getChunkZ(ChunkPos pos) {
        try {
            try {
                Field f = getField(ChunkPos.class, "z", "field_9182");
                f.setAccessible(true);
                return f.getInt(pos);
            } catch (Exception e) {
                Method m = getMethod(ChunkPos.class, "z", "method_33943");
                m.setAccessible(true);
                return (Integer) m.invoke(pos);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static int getMinBuildHeight(LevelChunk chunk) {
        try {
            try {
                Method m = getMethod(LevelChunk.class, "getBottomY", "method_31719");
                return (Integer) m.invoke(chunk);
            } catch (Exception e) {
                Method m = getMethod(LevelChunk.class, "getMinBuildHeight", "method_31607");
                return (Integer) m.invoke(chunk);
            }
        } catch (Exception e) {
            return -64;
        }
    }

    public static String getServerVersion(Minecraft client) {
        try {
            Object currentServer = null;
            try {
                Method m = getMethod(Minecraft.class, "getCurrentServerEntry", "method_1558");
                currentServer = m.invoke(client);
            } catch (Exception e) {
                Method m = getMethod(Minecraft.class, "getCurrentServer", "method_1558"); // Actually method_1558 in both usually, or changed mapping
                currentServer = m.invoke(client);
            }
            if (currentServer == null) return null;
            
            try {
                Field vField = getField(currentServer.getClass(), "version", "field_3760");
                Object versionObj = vField.get(currentServer);
                Method getString = getMethod(versionObj.getClass(), "getString", "method_10851");
                return (String) getString.invoke(versionObj);
            } catch (Exception e) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static String getServerAddress(Minecraft client) {
        try {
            Object currentServer = null;
            try {
                Method m = getMethod(Minecraft.class, "getCurrentServerEntry", "method_1558");
                currentServer = m.invoke(client);
            } catch (Exception e) {
                Method m = getMethod(Minecraft.class, "getCurrentServer", "method_1558");
                currentServer = m.invoke(client);
            }
            if (currentServer == null) return null;
            Field aField = getField(currentServer.getClass(), "address", "field_3761");
            return (String) aField.get(currentServer);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getServerBrand(Minecraft client) {
        try {
            Object conn = null;
            try {
                Method m = getMethod(Minecraft.class, "getNetworkHandler", "method_1562");
                conn = m.invoke(client);
            } catch (Exception e) {
                Method m = getMethod(Minecraft.class, "getConnection", "method_1562");
                conn = m.invoke(client);
            }
            if (conn == null) return null;
            Method m = getMethod(conn.getClass(), "serverBrand", "method_46949");
            return (String) m.invoke(conn);
        } catch (Exception e) {
            return null;
        }
    }
}
