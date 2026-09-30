package net.map6b6t;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class EnvBridge {

    public static long asLong(int x, int z) {
        try {
            Method m = ChunkPos.class.getMethod("asLong", int.class, int.class);
            return (Long) m.invoke(null, x, z);
        } catch (Exception e) {
            try {
                Method m = ChunkPos.class.getMethod("toLong", int.class, int.class);
                return (Long) m.invoke(null, x, z);
            } catch (Exception e2) {
                return 0L; // Fallback
            }
        }
    }

    public static int getChunkX(ChunkPos pos) {
        try {
            try {
                Field xField = ChunkPos.class.getDeclaredField("x");
                xField.setAccessible(true);
                return xField.getInt(pos);
            } catch (Exception e) {
                Method xMethod = ChunkPos.class.getDeclaredMethod("x");
                xMethod.setAccessible(true);
                return (Integer) xMethod.invoke(pos);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static int getChunkZ(ChunkPos pos) {
        try {
            try {
                Field zField = ChunkPos.class.getDeclaredField("z");
                zField.setAccessible(true);
                return zField.getInt(pos);
            } catch (Exception e) {
                Method zMethod = ChunkPos.class.getDeclaredMethod("z");
                zMethod.setAccessible(true);
                return (Integer) zMethod.invoke(pos);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static int getMinBuildHeight(LevelChunk chunk) {
        try {
            try {
                Method m = LevelChunk.class.getMethod("getMinBuildHeight");
                return (Integer) m.invoke(chunk);
            } catch (Exception e) {
                Method m = LevelChunk.class.getMethod("getBottomY");
                return (Integer) m.invoke(chunk);
            }
        } catch (Exception e) {
            return -64; // fallback
        }
    }

    public static String getServerVersion(Minecraft client) {
        try {
            Object currentServer = null;
            try {
                Method m = Minecraft.class.getMethod("getCurrentServer");
                currentServer = m.invoke(client);
            } catch (Exception e) {
                Method m = Minecraft.class.getMethod("getCurrentServerEntry");
                currentServer = m.invoke(client);
            }
            if (currentServer == null) return null;
            
            try {
                Field vField = currentServer.getClass().getDeclaredField("version");
                Object versionObj = vField.get(currentServer);
                Method getString = versionObj.getClass().getMethod("getString");
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
                Method m = Minecraft.class.getMethod("getCurrentServer");
                currentServer = m.invoke(client);
            } catch (Exception e) {
                Method m = Minecraft.class.getMethod("getCurrentServerEntry");
                currentServer = m.invoke(client);
            }
            if (currentServer == null) return null;
            Field aField = currentServer.getClass().getDeclaredField("address");
            return (String) aField.get(currentServer);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getServerBrand(Minecraft client) {
        try {
            Object conn = null;
            try {
                Method m = Minecraft.class.getMethod("getConnection");
                conn = m.invoke(client);
            } catch (Exception e) {
                Method m = Minecraft.class.getMethod("getNetworkHandler");
                conn = m.invoke(client);
            }
            if (conn == null) return null;
            Method m = conn.getClass().getMethod("serverBrand");
            return (String) m.invoke(conn);
        } catch (Exception e) {
            return null;
        }
    }
}
