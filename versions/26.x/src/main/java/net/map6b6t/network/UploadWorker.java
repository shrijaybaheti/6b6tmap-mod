package net.map6b6t.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class UploadWorker implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger("6b6tMap");
    private static final int MAX_BATCH = 4;

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
                ChunkSubmission first = queue.take(250);
                if (first == null) {
                    continue;
                }
                List<ChunkSubmission> batch = collectBatch(first);
                stats.setUploading(queue.uploadingCount());
                stats.setQueueSize(queue.size());

                UploadResult result = batch.size() == 1 || !service.isBatchEnabled()
                        ? uploader.sendSingle(first)
                        : uploader.sendBatch(batch);

                if (result.batchNotSupported && batch.size() > 1) {
                    service.disableBatching();
                    result = sendEach(batch);
                    continue;
                }

                handleResult(batch, result);
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
                    stats.markUploaded(per);
                    service.notifyUploaded(job);
                    continue;
                }
                if (item.retryable) {
                    job.markRetry(now + RetryPolicy.delayMs(job.attempts() + 1, result.retryAfter));
                    queue.requeue(job);
                    stats.markRetried();
                    stats.setLastError(item.error);
                } else {
                    queue.complete(job);
                    stats.markFailed(item.error);
                    LOGGER.warn("Dropping chunk {},{} after non-retryable failure: {}", job.chunkX, job.chunkZ, item.error);
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
            if (result.retryable) {
                long delay = RetryPolicy.delayMs(job.attempts() + 1, result.retryAfter);
                job.markRetry(now + delay);
                queue.requeue(job);
                stats.markRetried();
            } else {
                queue.complete(job);
                stats.markFailed(result.error);
                LOGGER.warn("Dropping chunk {},{} after non-retryable failure: {}", job.chunkX, job.chunkZ, result.error);
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
