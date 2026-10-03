package net.map6b6t.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.map6b6t.config.ConfigManager;
import net.map6b6t.config.ModConfig;
import net.map6b6t.scanner.ScannedBlock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.GZIPOutputStream;

final class ChunkUploader { private static final ThreadLocal<java.util.Map<String, Integer>> PALETTE_MAP = ThreadLocal.withInitial(java.util.HashMap::new); private static final ThreadLocal<java.util.List<String>> PALETTE_LIST = ThreadLocal.withInitial(java.util.ArrayList::new); private static final ThreadLocal<int[]> ENCODE_BUFFER = ThreadLocal.withInitial(() -> new int[131072]); private static final ThreadLocal<java.nio.ByteBuffer> BYTE_BUFFER = ThreadLocal.withInitial(() -> java.nio.ByteBuffer.allocate(1048576).order(java.nio.ByteOrder.LITTLE_ENDIAN));
    private static final int GZIP_AFTER_BYTES = 256;
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 60000;

    ChunkUploader() {
    }

    private static UploadResult httpPost(String url, byte[] body, String contentType, String contentEncoding, String playerName, String dimension) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setFixedLengthStreamingMode(body.length);
            conn.setRequestProperty("Content-Type", contentType);
            conn.setRequestProperty(Protocol.HEADER, String.valueOf(Protocol.VERSION));
            conn.setRequestProperty(Protocol.MOD_VERSION_HEADER, Protocol.MOD_VERSION);
            if (contentEncoding != null) conn.setRequestProperty("Content-Encoding", contentEncoding);
            if (playerName != null) conn.setRequestProperty("X-Player-Name", playerName);
            if (dimension != null) conn.setRequestProperty("X-Dimension", dimension);
            String token = ConfigManager.get().submitToken;
            if (token != null && !token.isBlank()) conn.setRequestProperty(Protocol.TOKEN_HEADER, token.trim());

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }

            int status = conn.getResponseCode();
            String retryAfter = conn.getHeaderField("Retry-After");
            InputStream is = status < 400 ? conn.getInputStream() : conn.getErrorStream();
            String responseBody = "";
            if (is != null) {
                try { responseBody = new String(is.readAllBytes(), StandardCharsets.UTF_8); } finally { is.close(); }
            }

            if (status >= 200 && status < 300) {
                List<UploadResult.Item> items = parseBatchItems(responseBody);
                if (items != null) return UploadResult.mixed(body.length, items);
                return UploadResult.ok(body.length);
            }
            String err = responseBody != null && !responseBody.isBlank() ? "HTTP " + status + " " + responseBody : "HTTP " + status;
            return UploadResult.http(status, body.length, err, RetryPolicy.parseRetryAfter(retryAfter));
        } catch (Exception e) {
            return UploadResult.network(e);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    UploadResult sendSingle(ChunkSubmission job) {
        return sendBatchRaw(java.util.Collections.singletonList(job));
    }

    private static String batchRawUrl(String base) {
        if (base.endsWith("/api/chunks/submit")) {
            return base.substring(0, base.length() - 6) + "batch_raw";
        }
        if (base.endsWith("/")) {
            return base + "api/chunks/batch_raw";
        }
        return base + "/api/chunks/batch_raw";
    }

    UploadResult sendBatchRaw(List<ChunkSubmission> jobs) {
        String url = batchRawUrl(ModConfig.sanitizeServerUrl(ConfigManager.get().serverUrl));
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream(jobs.size() * 1500);
            java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
            dos.writeInt(jobs.size());
            for (ChunkSubmission job : jobs) {
                byte[] gzip = job.preEncodedGzip;
                if (gzip == null) gzip = DiskCache.load(job.dimension, job.chunkX, job.chunkZ);
                if (gzip == null) {
                    gzip = encodeBlocks(job.blocks, job.chunkX, job.chunkZ);
                }
                dos.writeInt(gzip.length);
                dos.write(gzip);
            }
            dos.flush();
            byte[] payload = baos.toByteArray();
            String playerName = jobs.isEmpty() ? "binary" : jobs.get(0).playerName;
            String dimension = jobs.isEmpty() ? "minecraft:overworld" : jobs.get(0).dimension;
            return httpPost(url, payload, "application/octet-stream", null, playerName, dimension);
        } catch (Exception e) {
            return UploadResult.network(e);
        }
    }

    UploadResult sendBatch(List<ChunkSubmission> jobs) {
        String url = batchUrl(ModConfig.sanitizeServerUrl(ConfigManager.get().serverUrl));
        try {
            byte[] rawJson = encodeBatch(jobs);
            byte[] gzipped = gzip(rawJson);
            return sendGzipDirect(url, gzipped);
        } catch (Exception e) {
            return UploadResult.network(e);
        }
    }

    private UploadResult sendGzipDirect(String url, byte[] gzipBytes) {
        return httpPost(url, gzipBytes, "application/json", "gzip", null, null);
    }

    private UploadResult send(String url, byte[] jsonBytes) {
        try {
            byte[] body = jsonBytes;
            boolean gzip = jsonBytes.length >= GZIP_AFTER_BYTES;
            if (gzip) {
                body = gzip(jsonBytes);
            }
            return httpPost(url, body, "application/json", gzip ? "gzip" : null, null, null);
        } catch (Exception e) {
            return UploadResult.network(e);
        }
    }

    private static List<UploadResult.Item> parseBatchItems(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonElement rootEl = JsonParser.parseString(body);
            if (!rootEl.isJsonObject()) {
                return null;
            }
            JsonObject root = rootEl.getAsJsonObject();
            if (!root.has("results") || !root.get("results").isJsonArray()) {
                return null;
            }
            JsonArray results = root.getAsJsonArray("results");
            List<UploadResult.Item> items = new java.util.ArrayList<>(results.size());
            for (int i = 0; i < results.size(); i++) {
                if (!results.get(i).isJsonObject()) {
                    continue;
                }
                JsonObject row = results.get(i).getAsJsonObject();
                int chunkX = row.has("chunkX") ? row.get("chunkX").getAsInt() : Integer.MIN_VALUE;
                int chunkZ = row.has("chunkZ") ? row.get("chunkZ").getAsInt() : Integer.MIN_VALUE;
                boolean ok = row.has("success") && row.get("success").getAsBoolean();
                int itemStatus = row.has("status") ? row.get("status").getAsInt() : (ok ? 200 : 400);
                String error = row.has("error") ? row.get("error").getAsString() : ("HTTP " + itemStatus);
                items.add(new UploadResult.Item(chunkX, chunkZ, ok, !ok && RetryPolicy.isRetryableStatus(itemStatus), error));
            }
            return items.isEmpty() ? null : items;
        } catch (Exception ignored) {
            return null;
        }
    }

    static String batchUrl(String submitUrl) {
        if (submitUrl == null || submitUrl.isBlank()) {
            return "http://localhost:3000/api/chunks/batch";
        }
        if (submitUrl.endsWith("/api/chunks/submit")) {
            return submitUrl.substring(0, submitUrl.length() - "submit".length()) + "batch";
        }
        if (submitUrl.endsWith("/submit")) {
            return submitUrl.substring(0, submitUrl.length() - "submit".length()) + "batch";
        }
        return submitUrl;
    }

            public static byte[] encodeBlocks(Object blocksObj, int chunkX, int chunkZ) {
        if (blocksObj == null) return new byte[0];
        
        java.util.Map<String, Integer> paletteMap = PALETTE_MAP.get(); paletteMap.clear();
        java.util.List<String> paletteList = PALETTE_LIST.get(); paletteList.clear();
        
        int[] encodedBlocks = ENCODE_BUFFER.get();
        int validCount = 0;
        
        if (blocksObj instanceof net.map6b6t.scanner.PrimitiveChunkSnapshot) {
            net.map6b6t.scanner.PrimitiveChunkSnapshot snap = (net.map6b6t.scanner.PrimitiveChunkSnapshot) blocksObj;
            long[] blocks = snap.blocks;
            int size = snap.size; if (size > encodedBlocks.length) { encodedBlocks = new int[size]; ENCODE_BUFFER.set(encodedBlocks); }
            
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
        } else if (blocksObj instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) blocksObj; if (list.size() > encodedBlocks.length) { encodedBlocks = new int[list.size()]; ENCODE_BUFFER.set(encodedBlocks); }
            
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
        java.nio.ByteBuffer buf = BYTE_BUFFER.get(); if (capacity > buf.capacity()) { buf = java.nio.ByteBuffer.allocate(capacity).order(java.nio.ByteOrder.LITTLE_ENDIAN); BYTE_BUFFER.set(buf); } buf.clear(); buf.limit(capacity);
        
        buf.put((byte) 2);
        buf.putInt(chunkX);
        buf.putInt(chunkZ);
        buf.putShort((short) paletteList.size());
        
        for (byte[] pBytes : pBytesArray) {
            buf.put((byte) pBytes.length);
            buf.put(pBytes);
        }
        
        buf.putInt(validCount);
        for (int i = 0; i < validCount; i++) {
            buf.putInt(encodedBlocks[i]);
        }
        
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream(2048); try (java.util.zip.GZIPOutputStream gos = new java.util.zip.GZIPOutputStream(bos)) { gos.write(buf.array(), 0, capacity); } catch (java.io.IOException e) { return new byte[0]; } return bos.toByteArray();
    }

    private static String unzipToString(byte[] gzipBytes) throws IOException {
        try (java.util.zip.GZIPInputStream gis = new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(gzipBytes))) {
            return new String(gis.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static byte[] encodeBatch(List<ChunkSubmission> jobs) throws IOException {
        StringBuilder sb = new StringBuilder(1024 * 1024);
        sb.append("{\"protocolVersion\":").append(Protocol.VERSION).append(",\"chunks\":[");
        for (int i = 0; i < jobs.size(); i++) {
            if (i > 0) sb.append(',');
            ChunkSubmission job = jobs.get(i);
            byte[] gzip = job.preEncodedGzip;
            if (gzip == null) gzip = DiskCache.load(job.dimension, job.chunkX, job.chunkZ);
            if (gzip != null) {
                sb.append(unzipToString(gzip));
            } else {
                sb.append(new String(encodeBlocks(job.blocks, job.chunkX, job.chunkZ), StandardCharsets.UTF_8));
            }
        }
        sb.append("]}");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] gzip(byte[] input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, input.length / 4));
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(input);
        }
        return out.toByteArray();
    }
}
