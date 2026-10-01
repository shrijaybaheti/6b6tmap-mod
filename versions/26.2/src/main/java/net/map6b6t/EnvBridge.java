package net.map6b6t;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class EnvBridge {
    private static Field chunkPosX;
    private static Field chunkPosZ;
    private static Method chunkPosXMethod;
    private static Method chunkPosZMethod;
    private static Method chunkPosToLong;
    private static Method levelChunkBottomY;
    private static Field serverEntryIp;
    private static Field serverEntryVersion;
    private static Method serverEntryVersionGetString;
    private static Method getServerEntry;
    private static Method getConnection;
    private static Method serverBrand;
    private static boolean reflectionReady = false;

    private static volatile String cachedServerAddress = null;
    private static volatile Object cachedServerEntry = null;

    private static void initReflection() {
        if (reflectionReady) return;
        synchronized (EnvBridge.class) {
            if (reflectionReady) return;
            try {
                for (String name : new String[]{"toLong", "method_24022", "asLong", "method_8324"}) {
                    try {
                        Method m = ChunkPos.class.getDeclaredMethod(name, int.class, int.class);
                        m.setAccessible(true);
                        chunkPosToLong = m;
                        break;
                    } catch (NoSuchMethodException ignored) {}
                }

                boolean xField = false;
                for (String name : new String[]{"x", "field_9181"}) {
                    try {
                        Field f = ChunkPos.class.getDeclaredField(name);
                        f.setAccessible(true);
                        chunkPosX = f;
                        xField = true;
                        break;
                    } catch (NoSuchFieldException ignored) {}
                }
                if (!xField) {
                    for (String name : new String[]{"x", "method_33942"}) {
                        try {
                            Method m = ChunkPos.class.getDeclaredMethod(name);
                            m.setAccessible(true);
                            chunkPosXMethod = m;
                            break;
                        } catch (NoSuchMethodException ignored) {}
                    }
                }

                boolean zField = false;
                for (String name : new String[]{"z", "field_9182"}) {
                    try {
                        Field f = ChunkPos.class.getDeclaredField(name);
                        f.setAccessible(true);
                        chunkPosZ = f;
                        zField = true;
                        break;
                    } catch (NoSuchFieldException ignored) {}
                }
                if (!zField) {
                    for (String name : new String[]{"z", "method_33943"}) {
                        try {
                            Method m = ChunkPos.class.getDeclaredMethod(name);
                            m.setAccessible(true);
                            chunkPosZMethod = m;
                            break;
                        } catch (NoSuchMethodException ignored) {}
                    }
                }

                for (String name : new String[]{"getBottomY", "method_31719", "getMinBuildHeight", "method_31607"}) {
                    try {
                        Method m = LevelChunk.class.getMethod(name);
                        m.setAccessible(true);
                        levelChunkBottomY = m;
                        break;
                    } catch (NoSuchMethodException ignored) {}
                }

                for (String name : new String[]{"getCurrentServer", "getCurrentServerEntry", "method_1555", "method_1558"}) {
                    try {
                        Method m = Minecraft.class.getMethod(name);
                        getServerEntry = m;
                        break;
                    } catch (NoSuchMethodException ignored) {}
                }

                if (getServerEntry != null) {
                    try {
                        Object testEntry = getServerEntry.invoke(Minecraft.getInstance());
                        if (testEntry != null) {
                            for (String name : new String[]{"ip", "address", "field_3761"}) {
                                try {
                                    Field f = testEntry.getClass().getDeclaredField(name);
                                    f.setAccessible(true);
                                    serverEntryIp = f;
                                    break;
                                } catch (NoSuchFieldException ignored) {}
                            }
                            for (String name : new String[]{"version", "field_3760"}) {
                                try {
                                    Field f = testEntry.getClass().getDeclaredField(name);
                                    f.setAccessible(true);
                                    serverEntryVersion = f;
                                    if (serverEntryVersion != null) {
                                        Object ver = f.get(testEntry);
                                        if (ver != null) {
                                            for (String mn : new String[]{"getString", "method_10851"}) {
                                                try {
                                                    Method m = ver.getClass().getMethod(mn);
                                                    serverEntryVersionGetString = m;
                                                    break;
                                                } catch (NoSuchMethodException ignored) {}
                                            }
                                        }
                                    }
                                    break;
                                } catch (NoSuchFieldException ignored) {}
                            }
                        }
                    } catch (Exception ignored) {}
                }

                for (String name : new String[]{"getConnection", "getNetworkHandler", "method_1562"}) {
                    try {
                        Method m = Minecraft.class.getMethod(name);
                        getConnection = m;
                        break;
                    } catch (NoSuchMethodException ignored) {}
                }

                if (getConnection != null) {
                    try {
                        Object conn = getConnection.invoke(Minecraft.getInstance());
                        if (conn != null) {
                            for (String name : new String[]{"serverBrand", "method_46949", "getBrand"}) {
                                try {
                                    Method m = conn.getClass().getMethod(name);
                                    serverBrand = m;
                                    break;
                                } catch (NoSuchMethodException ignored) {}
                            }
                        }
                    } catch (Exception ignored) {}
                }

                reflectionReady = true;
            } catch (Exception e) {
                reflectionReady = true;
            }
        }
    }

    public static void clearSessionCache() {
        cachedServerAddress = null;
        cachedServerEntry = null;
    }

    public static long asLong(int x, int z) {
        initReflection();
        try {
            if (chunkPosToLong != null) return (Long) chunkPosToLong.invoke(null, x, z);
        } catch (Exception ignored) {}
        return 0L;
    }

    public static int getChunkX(ChunkPos pos) {
        initReflection();
        try {
            if (chunkPosX != null) return chunkPosX.getInt(pos);
            if (chunkPosXMethod != null) return (Integer) chunkPosXMethod.invoke(pos);
        } catch (Exception ignored) {}
        return 0;
    }

    public static int getChunkZ(ChunkPos pos) {
        initReflection();
        try {
            if (chunkPosZ != null) return chunkPosZ.getInt(pos);
            if (chunkPosZMethod != null) return (Integer) chunkPosZMethod.invoke(pos);
        } catch (Exception ignored) {}
        return 0;
    }

    public static int getMinBuildHeight(LevelChunk chunk) {
        initReflection();
        try {
            if (levelChunkBottomY != null) return (Integer) levelChunkBottomY.invoke(chunk);
        } catch (Exception ignored) {}
        return -64;
    }

    public static String getServerAddress(Minecraft client) {
        if (cachedServerAddress != null) return cachedServerAddress;
        initReflection();
        try {
            Object entry = getServerEntry != null ? getServerEntry.invoke(client) : null;
            if (entry == null) return null;
            if (serverEntryIp == null) {
                for (String name : new String[]{"ip", "address", "field_3761"}) {
                    try {
                        Field f = entry.getClass().getDeclaredField(name);
                        f.setAccessible(true);
                        serverEntryIp = f;
                        break;
                    } catch (NoSuchFieldException ignored) {}
                }
            }
            if (serverEntryIp != null) {
                String addr = (String) serverEntryIp.get(entry);
                cachedServerAddress = addr;
                return addr;
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static String getServerVersion(Minecraft client) {
        initReflection();
        try {
            Object entry = getServerEntry != null ? getServerEntry.invoke(client) : null;
            if (entry == null) return null;
            if (serverEntryVersion != null) {
                Object ver = serverEntryVersion.get(entry);
                if (ver != null && serverEntryVersionGetString != null) {
                    return (String) serverEntryVersionGetString.invoke(ver);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static String getServerBrand(Minecraft client) {
        initReflection();
        try {
            if (getConnection == null) return null;
            Object conn = getConnection.invoke(client);
            if (conn == null) return null;
            if (serverBrand == null) {
                for (String name : new String[]{"serverBrand", "method_46949", "getBrand"}) {
                    try {
                        Method m = conn.getClass().getMethod(name);
                        serverBrand = m;
                        break;
                    } catch (NoSuchMethodException ignored) {}
                }
            }
            if (serverBrand != null) return (String) serverBrand.invoke(conn);
        } catch (Exception ignored) {}
        return null;
    }

    public static String getDimension(ClientLevel world) {
        try {
            for (String mName : new String[]{"dimension", "method_27983", "getDimension"}) {
                try {
                    Method m = world.getClass().getMethod(mName);
                    Object dimKey = m.invoke(world);
                    if (dimKey == null) continue;
                    for (String rName : new String[]{"identifier", "location", "method_10485", "key", "getKey"}) {
                        try {
                            Method r = dimKey.getClass().getMethod(rName);
                            Object result = r.invoke(dimKey);
                            if (result != null) return result.toString();
                        } catch (Exception ignored) {}
                    }
                    return dimKey.toString();
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return "minecraft:overworld";
    }
}
