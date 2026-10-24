package net.map6b6t.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

final class UploadWorker implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger("6b6tMap");
    private static final int MAX_BATCH = 64;
    private static final int MAX_CONCURRENT_TRANSMISSIONS = 4;
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_WAIT_MS = 15_000L;

    private static final ExecutorService TRANSMISSION_POOL = Executors.newFixedThreadPool(MAX_CONCURRENT_TRANSMISSIONS, r -> {
        Thread t = new Thread(r, "6b6tmap-async-uploader");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    private static final Semaphore IN_FLIGHT_SEMAPHORE = new Semaphore(MAX_CONCURRENT_TRANSMISSIONS);

    private final UploadQueue queue;
    private final ChunkUploader uploader;
    private final NetworkStats stats;
    private final UploadService service;

    UploadWorker(UploadQueue queue, ChunkUploader uploader, NetworkStats stats, UploadService service) {
        this.queue = queue;
        this.uploader = uploader;
        this.stats = stats;
        this.service = service;
    }

    @Override
    public void run() {
        while (service.isRunning() && !Thread.currentThread().isInterrupted()) {
            try {
                if (!IN_FLIGHT_SEMAPHORE.tryAcquire(250, TimeUnit.MILLISECONDS)) {
                    continue;
                }

                ChunkSubmission first = queue.take(250);
                if (first == null) {
                    IN_FLIGHT_SEMAPHORE.release();
                    continue;
                }

                List<ChunkSubmission> batch = collectBatch(first);
                stats.setUploading(queue.uploadingCount());
                stats.setQueueSize(queue.size());

                TRANSMISSION_POOL.execute(() -> {
                    try {
                        UploadResult result = batch.size() == 1 || !service.isBatchEnabled()
                                ? uploader.sendSingle(first)
                                : uploader.sendBatchRaw(batch);

                        if (result.batchNotSupported && batch.size() > 1) {
                            service.disableBatching();
                            sendEach(batch);
                            return;
                        }

                        handleResult(batch, result);
                    } catch (Throwable t) {
                        LOGGER.warn("Async transmission error: {}", t.toString());
                        handleResult(batch, UploadResult.network(t));
                    } finally {
                        IN_FLIGHT_SEMAPHORE.release();
                    }
                });

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LOGGER.warn("Upload worker error: {}", e.toString());
            }
        }
    }

    private List<ChunkSubmission> collectBatch(ChunkSubmission first) {
        List<ChunkSubmission> batch = new ArrayList<>(MAX_BATCH);
        batch.add(first);
        if (!service.isBatchEnabled()) {
            return batch;
        }
        Set<ChunkSubmission.Key> seen = new HashSet<>();
        seen.add(first.key);
        while (batch.size() < MAX_BATCH) {
            ChunkSubmission extra = queue.tryTakeDue();
            if (extra == null) {
                break;
            }
            if (!seen.add(extra.key)) {
                queue.returnToQueue(extra);
                break;
            }
            batch.add(extra);
        }
        return batch;
    }

    private UploadResult sendEach(List<ChunkSubmission> jobs) {
        UploadResult last = UploadResult.ok(0);
        for (ChunkSubmission job : jobs) {
            last = uploader.sendSingle(job);
            handleResult(List.of(job), last);
        }
        return last;
    }

    private void handleResult(List<ChunkSubmission> jobs, UploadResult result) {
        if (result.items != null && !result.items.isEmpty()) {
            long per = jobs.isEmpty() ? 0 : result.bytesSent / jobs.size();
            long now = System.currentTimeMillis();
            for (ChunkSubmission job : jobs) {
                UploadResult.Item item = findItem(result.items, job);
                if (item == null || item.success) {
                    queue.complete(job);
                    DiskCache.delete(job.dimension, job.chunkX, job.chunkZ);
                    stats.markUploaded(per);
                    service.notifyUploaded(job);
                    continue;
                }
                if (item.retryable && job.attempts() < MAX_ATTEMPTS) {
                    job.markRetry(now + RETRY_WAIT_MS);
                    queue.requeue(job);
                    stats.markRetried();
                    stats.setLastError(item.error);
                } else {
                    queue.complete(job);
                    DiskCache.delete(job.dimension, job.chunkX, job.chunkZ);
                    stats.markFailed(item.error);
                    LOGGER.warn("Dropping chunk {},{} after failure: {}", job.chunkX, job.chunkZ, item.error);
                }
            }
            stats.setUploading(queue.uploadingCount());
            stats.setQueueSize(queue.size());
            return;
        }

        if (result.success) {
            long per = jobs.isEmpty() ? 0 : result.bytesSent / jobs.size();
            for (ChunkSubmission job : jobs) {
                queue.complete(job);
                DiskCache.delete(job.dimension, job.chunkX, job.chunkZ);
                stats.markUploaded(per);
                service.notifyUploaded(job);
            }
            stats.setUploading(queue.uploadingCount());
            stats.setQueueSize(queue.size());
            return;
        }

        stats.setLastError(result.error);
        long now = System.currentTimeMillis();
        for (ChunkSubmission job : jobs) {
            if (result.retryable && job.attempts() < MAX_ATTEMPTS) {
                job.markRetry(now + RETRY_WAIT_MS);
                queue.requeue(job);
                stats.markRetried();
            } else {
                queue.complete(job);
                DiskCache.delete(job.dimension, job.chunkX, job.chunkZ);
                stats.markFailed(result.error);
                LOGGER.warn("Dropping chunk {},{} after failure: {}", job.chunkX, job.chunkZ, result.error);
            }
        }
        stats.setUploading(queue.uploadingCount());
        stats.setQueueSize(queue.size());
    }

    private static UploadResult.Item findItem(List<UploadResult.Item> items, ChunkSubmission job) {
        for (int i = 0; i < items.size(); i++) {
            UploadResult.Item item = items.get(i);
            if (item.chunkX == job.chunkX && item.chunkZ == job.chunkZ) {
                return item;
            }
        }
        return items.size() == 1 ? items.get(0) : null;
    }
}
