package net.map6b6t.network;

import net.map6b6t.scanner.ScannedBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class UploadService {
    private static final Logger LOGGER = LoggerFactory.getLogger("6b6tMap");
    private static final UploadService INSTANCE = new UploadService();
    private static final int WORKERS = 4;
    private static final long STATS_EVERY_MS = 15_000L;

    private final UploadQueue queue = new UploadQueue();
    private final NetworkStats stats = new NetworkStats();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean batchEnabled = new AtomicBoolean(true);

    private ExecutorService workers;
    private ExecutorService scanExecutor;
    private Thread statsThread;
    private volatile UploadListener uploadListener;

    public static UploadService get() {
        return INSTANCE;
    }

    public synchronized void start() {
        if (running.get()) {
            return;
        }
        running.set(true);
        batchEnabled.set(true);
        ThreadFactory factory = new WorkerFactory();
        int scanWorkers = Math.min(3, Math.max(2, Runtime.getRuntime().availableProcessors() - 2));
        workers = Executors.newFixedThreadPool(WORKERS, factory);
        scanExecutor = new java.util.concurrent.ThreadPoolExecutor(
                scanWorkers, scanWorkers,
                0L, TimeUnit.MILLISECONDS,
                new java.util.concurrent.ArrayBlockingQueue<>(128),
                new ScanWorkerFactory(),
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
        );
        
                
                
                
        ChunkUploader uploader = new ChunkUploader();
        for (int i = 0; i < WORKERS; i++) {
            workers.execute(new UploadWorker(queue, uploader, stats, this));
        }
        statsThread = new Thread(this::statsLoop, "6b6tmap-net-stats");
        statsThread.setDaemon(true);
        statsThread.start();
        LOGGER.info("Upload service started (workers={}, scanWorkers={}, queueCap={})", WORKERS, scanWorkers, UploadQueue.CAPACITY);
    }

    public synchronized void shutdown() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        if (scanExecutor != null) {
            scanExecutor.shutdownNow();
            try {
                scanExecutor.awaitTermination(1500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (workers != null) {
            workers.shutdownNow();
            try {
                workers.awaitTermination(1500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (statsThread != null) {
            statsThread.interrupt();
        }
        LOGGER.info("Upload service stopped ({})", stats.snapshot());
    }

    public void submitAsyncScan(
            String dimension,
            int chunkX,
            int chunkZ,
            String playerName,
            String serverVersion,
            net.map6b6t.scanner.PrimitiveChunkSnapshot snapshot,
            java.util.function.BiConsumer<Long, Integer> onHashComputed
    ) {
        if (!running.get() || scanExecutor == null || scanExecutor.isShutdown()) { if (snapshot != null) snapshot.release();
            return;
        }
        scanExecutor.execute(() -> {
            try {
                int contentHash = snapshot.hashCode();
                long chunkKey = (((long) chunkX) & 0xFFFFFFFFL) | ((((long) chunkZ) & 0xFFFFFFFFL) << 32);
                if (onHashComputed != null) {
                    onHashComputed.accept(chunkKey, contentHash);
                }

                byte[] encodedGzip = null;
                try {
                    
                    encodedGzip = ChunkUploader.encodeBlocks(snapshot, chunkX, chunkZ);
                } catch (Exception e) {
                    LOGGER.error("Failed to gzip chunk", e); } finally { snapshot.release();
                }

                ChunkSubmission job = new ChunkSubmission(
                        dimension,
                        chunkX,
                        chunkZ,
                        playerName,
                        serverVersion,
                        null,
                        contentHash,
                        encodedGzip
                );
                
                queue.offer(job);
                
            } catch (Exception e) {
                LOGGER.error("Failed async chunk scan", e);
            }
        });
    }

    public void submitRawPacketAsync(
            String dimension,
            int chunkX,
            int chunkZ,
            String playerName,
            String serverVersion,
            byte[] rawBytes,
            java.util.function.BiConsumer<Long, Integer> onHashComputed
    ) {
        if (!running.get() || scanExecutor == null || scanExecutor.isShutdown()) {
            return;
        }
        scanExecutor.execute(() -> {
            try {
                int payloadHash = java.util.Arrays.hashCode(rawBytes);
                long chunkKey = (((long) chunkX) & 0xFFFFFFFFL) | ((((long) chunkZ) & 0xFFFFFFFFL) << 32);
                if (onHashComputed != null) {
                    onHashComputed.accept(chunkKey, payloadHash);
                }

                byte[] encodedGzip = null;
                try {
                    String safePlayer = (playerName != null && !playerName.trim().isEmpty()) ? playerName.trim() : "Player";
                    String safeVer = (serverVersion != null && !serverVersion.trim().isEmpty()) ? serverVersion.trim() : "1.21.1";
                    StringBuilder sb = new StringBuilder(rawBytes.length * 4 + 400);
                    sb.append("{\"chunkX\":").append(chunkX)
                      .append(",\"chunkZ\":").append(chunkZ)
                      .append(",\"player\":\"").append(safePlayer).append("\"")
                      .append(",\"serverVersion\":\"").append(safeVer).append("\"")
                      .append(",\"gameVersion\":\"").append(safeVer).append("\"")
                      .append(",\"minY\":").append(-64)
                      .append(",\"worldHeight\":").append(384)
                      .append(",\"rawPacketBuffer\":[");
                    for (int i = 0; i < rawBytes.length; i++) {
                        if (i > 0) sb.append(',');
                        sb.append(rawBytes[i] & 0xFF);
                    }
                    sb.append("]}");

                    byte[] jsonBytes = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream(jsonBytes.length / 2);
                    try (java.util.zip.GZIPOutputStream gos = new java.util.zip.GZIPOutputStream(bos)) {
                        gos.write(jsonBytes);
                    }
                    encodedGzip = bos.toByteArray();
                    if (encodedGzip != null) {
                        DiskCache.save(dimension, chunkX, chunkZ, encodedGzip);
                    }
                } catch (Exception ignored) {
                }

                ChunkSubmission finalizedJob = new ChunkSubmission(dimension, chunkX, chunkZ, playerName, serverVersion, null, payloadHash, encodedGzip);
                UploadQueue.OfferResult result = queue.offer(finalizedJob);
                switch (result) {
                    case DEDUPLICATED -> stats.markDeduplicated();
                    case REPLACED -> stats.markReplaced();
                    default -> {}
                }
                stats.setQueueSize(queue.size());
                stats.setUploading(queue.uploadingCount());
            } catch (Exception e) {
                LOGGER.warn("Async raw chunk scan failed for {},{}: {}", chunkX, chunkZ, e.toString());
            }
        });
    }

    

    

    public boolean shouldPauseScanning() {
        return !running.get() || queue.shouldPauseScanning();
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isBatchEnabled() {
        return batchEnabled.get();
    }

    void disableBatching() {
        if (batchEnabled.compareAndSet(true, false)) {
            LOGGER.info("Batch upload endpoint unavailable; falling back to single-chunk submits");
        }
    }

    public void setUploadListener(UploadListener listener) {
        this.uploadListener = listener;
    }

    void notifyUploaded(ChunkSubmission job) {
        UploadListener listener = uploadListener;
        if (listener != null && job != null) {
            listener.onUploaded(job.chunkX, job.chunkZ, job.contentHash);
        }
    }

    public NetworkStats stats() {
        return stats;
    }

    public int queueSize() {
        return queue.size();
    }

    private void statsLoop() {
        int lastUploaded = -1;
        int lastQueue = -1;
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(STATS_EVERY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            stats.setQueueSize(queue.size());
            stats.setUploading(queue.uploadingCount());
            if (stats.uploaded() != lastUploaded || stats.queueSize() != lastQueue || stats.retried() > 0) {
                LOGGER.info("[6b6tMap] {}", stats.snapshot());
                lastUploaded = stats.uploaded();
                lastQueue = stats.queueSize();
            }
        }
    }

    private static final class WorkerFactory implements ThreadFactory {
        private final AtomicInteger n = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "6b6tmap-upload-" + n.incrementAndGet());
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        }
    }

    private static final class ScanWorkerFactory implements ThreadFactory {
        private final AtomicInteger n = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "6b6tmap-scan-" + n.incrementAndGet());
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        }
    }
}





