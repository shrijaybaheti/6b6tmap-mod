package net.map6b6t;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class EnvBridge {
    private static Field getField(Class<?> clazz, String... names) throws Exception {
        for (String name : names) {
            try { return clazz.getDeclaredField(name); } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException("None of " + java.util.Arrays.toString(names) + " found in " + clazz);
    }

    private static Method getMethod(Class<?> clazz, Class<?>[] params, String... names) throws Exception {
        for (String name : names) {
            try { return clazz.getDeclaredMethod(name, params); } catch (NoSuchMethodException ignored) {}
        }
        throw new NoSuchMethodException("None of " + java.util.Arrays.toString(names) + " found in " + clazz);
    }

    public static long asLong(int x, int z) {
        try {
            Method m = getMethod(ChunkPos.class, new Class[]{int.class, int.class}, "toLong", "method_24022", "asLong", "method_8324");
            m.setAccessible(true);
            return (Long) m.invoke(null, x, z);
        } catch (Exception e) {
            return 0L;
        }
    }

    public static int getChunkX(ChunkPos pos) {
        try {
            try {
                Field f = getField(ChunkPos.class, "x", "field_9181");
                f.setAccessible(true);
                return f.getInt(pos);
            } catch (Exception e) {
                Method m = getMethod(ChunkPos.class, new Class[]{}, "x", "method_33942");
                m.setAccessible(true);
                return (Integer) m.invoke(pos);
            }
        } catch (Exception e) {
            return 0;
        }
    }

    public static int getChunkZ(ChunkPos pos) {
        try {
            try {
                Field f = getField(ChunkPos.class, "z", "field_9182");
                f.setAccessible(true);
                return f.getInt(pos);
            } catch (Exception e) {
                Method m = getMethod(ChunkPos.class, new Class[]{}, "z", "method_33943");
                m.setAccessible(true);
                return (Integer) m.invoke(pos);
            }
        } catch (Exception e) {
            return 0;
        }
    }

    public static int getMinBuildHeight(LevelChunk chunk) {
        try {
            Method m = getMethod(chunk.getClass(), new Class[]{}, "getBottomY", "method_31719", "getMinBuildHeight", "method_31607");
            m.setAccessible(true);
            return (Integer) m.invoke(chunk);
        } catch (Exception e) {
            return -64;
        }
    }

    public static String getDimension(ClientLevel world) {
        try {
            Object dimKey = null;
            for (String mName : new String[]{"dimension", "method_27983", "getDimension"}) {
                try {
                    Method m = world.getClass().getMethod(mName);
                    dimKey = m.invoke(world);
                    break;
                } catch (Exception ignored) {}
            }
            if (dimKey == null) return "minecraft:overworld";

            for (String mName : new String[]{"identifier", "location", "method_10485", "registry", "key", "getKey"}) {
                try {
                    Method m = dimKey.getClass().getMethod(mName);
                    Object result = m.invoke(dimKey);
                    if (result != null) return result.toString();
                } catch (Exception ignored) {}
            }

            return dimKey.toString();
        } catch (Exception e) {
            return "minecraft:overworld";
        }
    }

    public static String getServerVersion(Minecraft client) {
        try {
            Object currentServer = getServerEntry(client);
            if (currentServer == null) return null;
            try {
                Field vField = getField(currentServer.getClass(), "version", "field_3760");
                vField.setAccessible(true);
                Object versionObj = vField.get(currentServer);
                if (versionObj == null) return null;
                Method getString = getMethod(versionObj.getClass(), new Class[]{}, "getString", "method_10851");
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
            Object currentServer = getServerEntry(client);
            if (currentServer == null) return null;
            Field aField = getField(currentServer.getClass(), "ip", "address", "field_3761");
            aField.setAccessible(true);
            return (String) aField.get(currentServer);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getServerBrand(Minecraft client) {
        try {
            Object conn = null;
            for (String mName : new String[]{"getConnection", "getNetworkHandler", "method_1562"}) {
                try {
                    Method m = Minecraft.class.getMethod(mName);
                    conn = m.invoke(client);
                    if (conn != null) break;
                } catch (Exception ignored) {}
            }
            if (conn == null) return null;
            Method m = getMethod(conn.getClass(), new Class[]{}, "serverBrand", "method_46949", "getBrand");
            return (String) m.invoke(conn);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object getServerEntry(Minecraft client) {
        for (String mName : new String[]{"getCurrentServer", "getCurrentServerEntry", "method_1555", "method_1558"}) {
            try {
                Method m = Minecraft.class.getMethod(mName);
                Object result = m.invoke(client);
                if (result != null) return result;
            } catch (Exception ignored) {}
        }
        return null;
    }
}
