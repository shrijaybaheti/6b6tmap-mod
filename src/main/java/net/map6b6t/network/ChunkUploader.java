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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.zip.GZIPOutputStream;

final class ChunkUploader {
    private static final int GZIP_AFTER_BYTES = 256;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final HttpClient httpClient;

    ChunkUploader(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    UploadResult sendSingle(ChunkSubmission job) {
        String url = ModConfig.sanitizeServerUrl(ConfigManager.get().serverUrl);
        byte[] gzipped = job.preEncodedGzip;
        if (gzipped == null) gzipped = DiskCache.load(job.dimension, job.chunkX, job.chunkZ);
        if (gzipped != null) {
            return sendGzipDirect(url, gzipped);
        }
        return send(url, encodeSingle(job));
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
                    gzip = gzip(encodeSingle(job));
                }
                dos.writeInt(gzip.length);
                dos.write(gzip);
            }
            dos.flush();
            byte[] payload = baos.toByteArray();
            
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/octet-stream")
                    .header(Protocol.HEADER, String.valueOf(Protocol.VERSION))
                    .header("X-Player-Name", jobs.isEmpty() ? "binary" : jobs.get(0).playerName)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(payload));
            String token = ConfigManager.get().submitToken;
            if (token != null && !token.isBlank()) {
                builder.header(Protocol.TOKEN_HEADER, token.trim());
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                List<UploadResult.Item> items = parseBatchItems(response.body());
                if (items != null) return UploadResult.mixed(payload.length, items);
                return UploadResult.ok(payload.length);
            }
            return UploadResult.http(status, payload.length, "HTTP " + status, null);
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
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("Content-Encoding", "gzip")
                    .header(Protocol.HEADER, String.valueOf(Protocol.VERSION))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(gzipBytes));
            String token = ConfigManager.get().submitToken;
            if (token != null && !token.isBlank()) {
                builder.header(Protocol.TOKEN_HEADER, token.trim());
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                List<UploadResult.Item> items = parseBatchItems(response.body());
                if (items != null) {
                    return UploadResult.mixed(gzipBytes.length, items);
                }
                return UploadResult.ok(gzipBytes.length);
            }
            String err = "HTTP " + status;
            if (response.body() != null && !response.body().isBlank()) {
                err = err + " " + response.body();
            }
            return UploadResult.http(status, gzipBytes.length, err, RetryPolicy.parseRetryAfter(response.headers().firstValue("Retry-After").orElse(null)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return UploadResult.network(e);
        } catch (Exception e) {
            return UploadResult.network(e);
        }
    }

    private UploadResult send(String url, byte[] jsonBytes) {
        try {
            byte[] body = jsonBytes;
            boolean gzip = jsonBytes.length >= GZIP_AFTER_BYTES;
            if (gzip) {
                body = gzip(jsonBytes);
            }

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header(Protocol.HEADER, String.valueOf(Protocol.VERSION))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body));
            String token = ConfigManager.get().submitToken;
            if (token != null && !token.isBlank()) {
                builder.header(Protocol.TOKEN_HEADER, token.trim());
            }
            if (gzip) {
                builder.header("Content-Encoding", "gzip");
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                List<UploadResult.Item> items = parseBatchItems(response.body());
                if (items != null) {
                    return UploadResult.mixed(body.length, items);
                }
                return UploadResult.ok(body.length);
            }
            String err = "HTTP " + status;
            if (response.body() != null && !response.body().isBlank()) {
                err = err + " " + response.body();
            }
            return UploadResult.http(status, body.length, err, RetryPolicy.parseRetryAfter(response.headers().firstValue("Retry-After").orElse(null)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return UploadResult.network(e);
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

            static byte[] encodeSingle(ChunkSubmission job) {
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
                sb.append(new String(encodeSingle(job), StandardCharsets.UTF_8));
            }
        }
        sb.append("]}");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    static byte[] gzip(byte[] input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, input.length / 4));
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(input);
        }
        return out.toByteArray();
    }
}
