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
        if (job.preEncodedGzip != null) {
            return sendGzipDirect(url, job.preEncodedGzip);
        }
        return send(url, encodeSingle(job));
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
        StringBuilder sb = new StringBuilder(1024 * 16);
        appendEnvelopeStart(sb, job);
        appendBlocks(sb, job.blocks);
        sb.append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
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
            if (job.preEncodedGzip != null) {
                sb.append(unzipToString(job.preEncodedGzip));
            } else {
                sb.append('{');
                appendEnvelopeFields(sb, job);
                appendBlocks(sb, job.blocks);
                sb.append('}');
            }
        }
        sb.append("]}");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void appendEnvelopeStart(StringBuilder sb, ChunkSubmission job) {
        sb.append('{');
        appendEnvelopeFields(sb, job);
    }

    private static void appendEnvelopeFields(StringBuilder sb, ChunkSubmission job) {
        sb.append("\"protocolVersion\":").append(Protocol.VERSION)
          .append(",\"player\":\"").append(escape(job.playerName))
          .append("\",\"chunkX\":").append(job.chunkX)
          .append(",\"chunkZ\":").append(job.chunkZ)
          .append(",\"dimension\":\"").append(escape(job.dimension))
          .append("\",\"serverVersion\":\"").append(escape(job.serverVersion))
          .append("\",");
    }

    private static void appendBlocks(StringBuilder sb, net.map6b6t.scanner.PrimitiveChunkSnapshot snapshot) {
        sb.append("\"blocks\":[");
        if (snapshot != null) {
            long[] blocks = snapshot.blocks;
            int size = snapshot.size;
            for (int i = 0; i < size; i++) {
                if (i > 0) sb.append(',');
                long val = blocks[i];
                int x = (int) (val & 0xF);
                int y = (int) ((val >>> 4) & 0x1FFF);
                int z = (int) ((val >>> 17) & 0xF);
                int rawId = (int) (val >>> 21);
                String blockName = net.map6b6t.scanner.ChunkScanner.getBlockIdStringFromRaw(rawId);
                sb.append("{\"x\":").append(x)
                  .append(",\"y\":").append(y)
                  .append(",\"z\":").append(z)
                  .append(",\"block\":\"").append(escape(blockName)).append("\"}");
            }
        }
        sb.append(']');
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    static byte[] gzip(byte[] input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, input.length / 4));
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(input);
        }
        return out.toByteArray();
    }
}
