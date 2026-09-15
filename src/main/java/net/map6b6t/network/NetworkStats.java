package net.map6b6t.network;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class NetworkStats {
    private final AtomicInteger queueSize = new AtomicInteger();
    private final AtomicInteger uploading = new AtomicInteger();
    private final AtomicInteger uploaded = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private final AtomicInteger retried = new AtomicInteger();
    private final AtomicInteger deduplicated = new AtomicInteger();
    private final AtomicInteger replaced = new AtomicInteger();
    private final AtomicLong bytesUploaded = new AtomicLong();
    private final AtomicReference<String> lastError = new AtomicReference<>("");

    public int queueSize() { return queueSize.get(); }
    public int uploading() { return uploading.get(); }
    public int uploaded() { return uploaded.get(); }
    public int failed() { return failed.get(); }
    public int retried() { return retried.get(); }
    public int deduplicated() { return deduplicated.get(); }
    public int replaced() { return replaced.get(); }
    public long bytesUploaded() { return bytesUploaded.get(); }
    public String lastError() { return lastError.get(); }

    void setQueueSize(int size) { queueSize.set(size); }
    void setUploading(int n) { uploading.set(n); }
    void markUploaded(long bytes) {
        uploaded.incrementAndGet();
        if (bytes > 0) bytesUploaded.addAndGet(bytes);
    }
    void markFailed(String error) {
        failed.incrementAndGet();
        setLastError(error);
    }
    void markRetried() { retried.incrementAndGet(); }
    void markDeduplicated() { deduplicated.incrementAndGet(); }
    void markReplaced() { replaced.incrementAndGet(); }
    public void setLastError(String error) {
        if (error != null && !error.isBlank()) {
            lastError.set(truncate(error));
        }
    }

    public String snapshot() {
        return "queue=" + queueSize()
                + " uploading=" + uploading()
                + " uploaded=" + uploaded()
                + " retries=" + retried()
                + " failed=" + failed()
                + " dedup=" + deduplicated();
    }

    private static String truncate(String error) {
        String oneLine = error.replace('\n', ' ').trim();
        return oneLine.length() > 180 ? oneLine.substring(0, 180) : oneLine;
    }
}
