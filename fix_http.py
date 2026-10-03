import re

with open('src/main/java/net/map6b6t/network/ChunkUploader.java', 'r', encoding='utf-8') as f:
    code = f.read()

helper = """
    private UploadResult sendHttp(String url, byte[] payload, String contentType, String encoding, String playerName, String dimension) {
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", contentType);
            conn.setRequestProperty(Protocol.HEADER, String.valueOf(Protocol.VERSION));
            conn.setRequestProperty(Protocol.MOD_VERSION_HEADER, Protocol.MOD_VERSION);
            if (encoding != null) conn.setRequestProperty("Content-Encoding", encoding);
            if (playerName != null) conn.setRequestProperty("X-Player-Name", playerName);
            if (dimension != null) conn.setRequestProperty("X-Dimension", dimension);
            
            String token = ConfigManager.get().submitToken;
            if (token != null && !token.isBlank()) {
                conn.setRequestProperty(Protocol.TOKEN_HEADER, token.trim());
            }
            
            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(payload);
                os.flush();
            }
            
            int status = conn.getResponseCode();
            String responseBody = "";
            java.io.InputStream is = status < 400 ? conn.getInputStream() : conn.getErrorStream();
            if (is != null) {
                try (java.util.Scanner s = new java.util.Scanner(is, java.nio.charset.StandardCharsets.UTF_8).useDelimiter("\\\\A")) {
                    responseBody = s.hasNext() ? s.next() : "";
                }
            }
            
            if (status >= 200 && status < 300) {
                List<UploadResult.Item> items = parseBatchItems(responseBody);
                if (items != null) return UploadResult.mixed(payload.length, items);
                return UploadResult.ok(payload.length);
            }
            return UploadResult.http(status, payload.length, "HTTP " + status, responseBody);
        } catch (Exception e) {
            return UploadResult.network(e);
        }
    }
"""

# Replace sendBatchRaw body
code = re.sub(
    r'HttpRequest\.Builder builder = HttpRequest\.newBuilder\(\).*?return UploadResult\.network\(e\);\s*\}',
    r'String playerName = jobs.isEmpty() ? "binary" : jobs.get(0).playerName; String dimension = jobs.isEmpty() ? "minecraft:overworld" : jobs.get(0).dimension; return sendHttp(url, payload, "application/octet-stream", null, playerName, dimension);',
    code,
    flags=re.DOTALL
)

# Replace sendGzipDirect body
code = re.sub(
    r'HttpRequest\.Builder builder = HttpRequest\.newBuilder\(\).*?return UploadResult\.network\(e\);\s*\}',
    r'return sendHttp(url, gzipBytes, "application/json", "gzip", null, null);',
    code,
    flags=re.DOTALL
)

# Replace send body
code = re.sub(
    r'HttpRequest\.Builder builder = HttpRequest\.newBuilder\(\).*?return UploadResult\.network\(e\);\s*\}',
    r'return sendHttp(url, body, "application/json", gzip ? "gzip" : null, null, null);',
    code,
    flags=re.DOTALL
)

# Insert the helper method
code = re.sub(r'public UploadResult sendBatchRaw', helper + '\n    public UploadResult sendBatchRaw', code)

with open('src/main/java/net/map6b6t/network/ChunkUploader.java', 'w', encoding='utf-8') as f:
    f.write(code)
