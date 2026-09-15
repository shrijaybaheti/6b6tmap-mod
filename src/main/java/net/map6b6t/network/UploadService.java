package net.map6b6t.network;

import net.map6b6t.scanner.ScannedBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.time.Duration;
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
    private static final int WORKERS = 6;
    private static final long STATS_EVERY_MS = 15_000L;

    private final UploadQueue queue = new UploadQueue();
    private final NetworkStats stats = new NetworkStats();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean batchEnabled = new AtomicBoolean(true);

    private ExecutorService workers;
    private Thread statsThread;
    private HttpClient httpClient;
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
        workers = Executors.newFixedThreadPool(WORKERS, factory);
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        ChunkUploader uploader = new ChunkUploader(httpClient);
        for (int i = 0; i < WORKERS; i++) {
            workers.execute(new UploadWorker(queue, uploader, stats, this));
        }
        statsThread = new Thread(this::statsLoop, "6b6tmap-net-stats");
        statsThread.setDaemon(true);
        statsThread.start();
        LOGGER.info("Upload service started (workers={}, queueCap={})", WORKERS, UploadQueue.CAPACITY);
    }

    public synchronized void shutdown() {
        if (!running.compareAndSet(true, false)) {
            return;
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

    public UploadQueue.OfferResult submit(
            String dimension,
            int chunkX,
            int chunkZ,
            String playerName,
            String serverVersion,
            List<ScannedBlock> blocks
    ) {
        if (!running.get() || blocks == null || blocks.isEmpty()) {
            return UploadQueue.OfferResult.REJECTED;
        }
        ChunkSubmission job = new ChunkSubmission(
                dimension,
                chunkX,
                chunkZ,
                playerName,
                serverVersion,
                blocks,
                ChunkSubmission.contentHash(blocks)
        );
        UploadQueue.OfferResult result = queue.offer(job);
        switch (result) {
            case DEDUPLICATED -> stats.markDeduplicated();
            case REPLACED -> stats.markReplaced();
            default -> {
            }
        }
        stats.setQueueSize(queue.size());
        stats.setUploading(queue.uploadingCount());
        return result;
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
            return t;
        }
    }
}
