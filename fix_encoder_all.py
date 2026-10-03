
import os
import re

encoder = """    static byte[] encodeSingle(ChunkSubmission job) {
        if (job.blocks == null) return new byte[0];
        
        java.util.Map<String, Integer> paletteMap = new java.util.HashMap<>();
        java.util.List<String> paletteList = new java.util.ArrayList<>();
        
        int[] encodedBlocks;
        int validCount = 0;
        
        if (job.blocks instanceof net.map6b6t.scanner.PrimitiveChunkSnapshot) {
            net.map6b6t.scanner.PrimitiveChunkSnapshot snap = (net.map6b6t.scanner.PrimitiveChunkSnapshot) job.blocks;
            long[] blocks = snap.blocks;
            int size = snap.size;
            encodedBlocks = new int[size];
            for (int i = 0; i < size; i++) {
                long val = blocks[i];
                int x = (int) (val & 0xF);
                int y = (int) ((val >>> 4) & 0x1FFF);
                int z = (int) ((val >>> 17) & 0xF);
                int rawId = (int) ((val >>> 21) & 0xFFFFFFFFL);
                String bName = net.map6b6t.scanner.ChunkScanner.getBlockIdStringFromRaw(rawId);
                
                Integer pIdx = paletteMap.get(bName);
                if (pIdx == null) {
                    pIdx = paletteList.size();
                    paletteMap.put(bName, pIdx);
                    paletteList.add(bName);
                }
                
                encodedBlocks[validCount++] = (x << 28) | (z << 24) | (((y + 64) & 0x3FF) << 14) | (pIdx & 0x3FFF);
            }
        } else if (job.blocks instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) job.blocks;
            encodedBlocks = new int[list.size()];
            for (Object obj : list) {
                try {
                    int x = obj.getClass().getField("x").getInt(obj);
                    int y = obj.getClass().getField("y").getInt(obj);
                    int z = obj.getClass().getField("z").getInt(obj);
                    String bName = (String) obj.getClass().getField("block").get(obj);
                    
                    Integer pIdx = paletteMap.get(bName);
                    if (pIdx == null) {
                        pIdx = paletteList.size();
                        paletteMap.put(bName, pIdx);
                        paletteList.add(bName);
                    }
                    
                    encodedBlocks[validCount++] = (x << 28) | (z << 24) | (((y + 64) & 0x3FF) << 14) | (pIdx & 0x3FFF);
                } catch (Exception e) {}
            }
        } else {
            return new byte[0];
        }
        
        int paletteBytesSize = 0;
        byte[][] pBytesArray = new byte[paletteList.size()][];
        for (int i = 0; i < paletteList.size(); i++) {
            byte[] pBytes = paletteList.get(i).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            pBytesArray[i] = pBytes;
            paletteBytesSize += 1 + pBytes.length;
        }
        
        int capacity = 1 + 4 + 4 + 2 + paletteBytesSize + 4 + (validCount * 4);
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate(capacity).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        
        buf.put((byte) 2);
        buf.putInt(job.chunkX);
        buf.putInt(job.chunkZ);
        buf.putShort((short) paletteList.size());
        
        for (byte[] pBytes : pBytesArray) {
            buf.put((byte) pBytes.length);
            buf.put(pBytes);
        }
        
        buf.putInt(validCount);
        for (int i = 0; i < validCount; i++) {
            buf.putInt(encodedBlocks[i]);
        }
        
        return buf.array();
    }"""

f = "src/main/java/net/map6b6t/network/ChunkUploader.java"
with open(f, "r") as file:
    content = file.read()

content = re.sub(r"static byte\[\] encodeSingle\(ChunkSubmission job\).*?return buf\.array\(\);\s*\}", encoder, content, flags=re.DOTALL)

with open(f, "w") as file:
    file.write(content)
print("Fixed ChunkUploader with Reflection list support")

