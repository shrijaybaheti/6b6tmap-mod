package net.map6b6t.network;

import java.time.Duration;
import java.util.List;

public final class UploadResult {
    public final boolean success;
    public final boolean retryable;
    public final int statusCode;
    public final long bytesSent;
    public final String error;
    public final Duration retryAfter;
    public final boolean batchNotSupported;
    public final List<Item> items;

    public static final class Item {
        public final int chunkX;
        public final int chunkZ;
        public final boolean success;
        public final boolean retryable;
        public final String error;

        public Item(int chunkX, int chunkZ, boolean success, boolean retryable, String error) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.success = success;
            this.retryable = retryable;
            this.error = error == null ? "" : error;
        }
    }

    private UploadResult(
            boolean success,
            boolean retryable,
            int statusCode,
            long bytesSent,
            String error,
            Duration retryAfter,
            boolean batchNotSupported,
            List<Item> items
    ) {
        this.success = success;
        this.retryable = retryable;
        this.statusCode = statusCode;
        this.bytesSent = bytesSent;
        this.error = error;
        this.retryAfter = retryAfter;
        this.batchNotSupported = batchNotSupported;
        this.items = items;
    }

    public static UploadResult ok(long bytesSent) {
        return new UploadResult(true, false, 200, bytesSent, "", null, false, null);
    }

    public static UploadResult mixed(long bytesSent, List<Item> items) {
        return new UploadResult(false, false, 200, bytesSent, "", null, false, items);
    }

    public static UploadResult http(int status, long bytesSent, String error, Duration retryAfter) {
        boolean retryable = RetryPolicy.isRetryableStatus(status);
        return new UploadResult(false, retryable, status, bytesSent, error, retryAfter, status == 404, null);
    }

    public static UploadResult network(Throwable error) {
        String msg = error == null ? "network error" : error.toString();
        return new UploadResult(false, RetryPolicy.isRetryableFailure(error, 0), 0, 0, msg, null, false, null);
    }
}
