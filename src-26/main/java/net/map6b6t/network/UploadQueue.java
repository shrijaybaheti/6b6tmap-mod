package net.map6b6t.network;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class UploadQueue {
    public static final int CAPACITY = 256;
    public static final int PAUSE_SCAN_AT = 224;

    public enum OfferResult {
        ACCEPTED,
        DEDUPLICATED,
        REPLACED,
        FULL,
        REJECTED
    }

    private final ArrayDeque<ChunkSubmission> ready = new ArrayDeque<>();
    private final Map<ChunkSubmission.Key, ChunkSubmission> queuedByChunk = new HashMap<>();
    private final Map<ChunkSubmission.Key, Integer> uploadingHash = new HashMap<>();
    private final Set<ChunkSubmission.Key> uploading = new HashSet<>();
    private final Object lock = new Object();

    public OfferResult offer(ChunkSubmission job) {
        if (job == null || job.blocks == null || job.blocks.isEmpty()) {
            return OfferResult.REJECTED;
        }
        synchronized (lock) {
            ChunkSubmission queued = queuedByChunk.get(job.key);
            if (queued != null) {
                if (queued.contentHash == job.contentHash) {
                    return OfferResult.DEDUPLICATED;
                }
                replaceQueued(queued, job);
                lock.notifyAll();
                return OfferResult.REPLACED;
            }
            Integer inflightHash = uploadingHash.get(job.key);
            if (inflightHash != null && inflightHash == job.contentHash) {
                return OfferResult.DEDUPLICATED;
            }
            if (occupancy() >= CAPACITY) {
                return OfferResult.FULL;
            }
            queuedByChunk.put(job.key, job);
            ready.addLast(job);
            lock.notifyAll();
            return OfferResult.ACCEPTED;
        }
    }

    public ChunkSubmission take(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + Math.max(1L, timeoutMs);
        synchronized (lock) {
            while (true) {
                long now = System.currentTimeMillis();
                ChunkSubmission due = pollDue(now);
                if (due != null) {
                    queuedByChunk.remove(due.key, due);
                    uploading.add(due.key);
                    uploadingHash.put(due.key, due.contentHash);
                    return due;
                }
                if (now >= deadline) {
                    return null;
                }
                long wait = Math.min(deadline - now, nextWaitMs(now));
                lock.wait(Math.max(1L, wait));
            }
        }
    }

    public ChunkSubmission tryTakeDue() {
        synchronized (lock) {
            ChunkSubmission due = pollDue(System.currentTimeMillis());
            if (due == null) {
                return null;
            }
            queuedByChunk.remove(due.key, due);
            uploading.add(due.key);
            uploadingHash.put(due.key, due.contentHash);
            return due;
        }
    }

    public void complete(ChunkSubmission job) {
        synchronized (lock) {
            uploading.remove(job.key);
            uploadingHash.remove(job.key);
            lock.notifyAll();
        }
    }

    public void returnToQueue(ChunkSubmission job) {
        synchronized (lock) {
            uploading.remove(job.key);
            uploadingHash.remove(job.key);
            if (!queuedByChunk.containsKey(job.key)) {
                queuedByChunk.put(job.key, job);
                ready.addFirst(job);
            }
            lock.notifyAll();
        }
    }

    public void requeue(ChunkSubmission job) {
        synchronized (lock) {
            uploading.remove(job.key);
            uploadingHash.remove(job.key);
            ChunkSubmission newer = queuedByChunk.get(job.key);
            if (newer != null && newer.contentHash != job.contentHash) {
                lock.notifyAll();
                return;
            }
            if (newer == job) {
                lock.notifyAll();
                return;
            }
            queuedByChunk.put(job.key, job);
            ready.addLast(job);
            lock.notifyAll();
        }
    }

    public int size() {
        synchronized (lock) {
            return occupancy();
        }
    }

    public int uploadingCount() {
        synchronized (lock) {
            return uploading.size();
        }
    }

    public boolean shouldPauseScanning() {
        return size() >= PAUSE_SCAN_AT;
    }

    public void clear() {
        synchronized (lock) {
            ready.clear();
            queuedByChunk.clear();
            uploading.clear();
            uploadingHash.clear();
            lock.notifyAll();
        }
    }

    private void replaceQueued(ChunkSubmission oldJob, ChunkSubmission newer) {
        queuedByChunk.put(newer.key, newer);
        Iterator<ChunkSubmission> it = ready.iterator();
        while (it.hasNext()) {
            if (it.next() == oldJob) {
                it.remove();
                break;
            }
        }
        ready.addLast(newer);
    }

    private ChunkSubmission pollDue(long nowMs) {
        int n = ready.size();
        for (int i = 0; i < n; i++) {
            ChunkSubmission job = ready.pollFirst();
            if (job == null) {
                return null;
            }
            if (job.due(nowMs)) {
                return job;
            }
            ready.addLast(job);
        }
        return null;
    }

    private long nextWaitMs(long nowMs) {
        long soonest = 250L;
        for (ChunkSubmission job : ready) {
            long wait = job.nextAttemptAtMs() - nowMs;
            if (wait <= 0) {
                return 1L;
            }
            soonest = Math.min(soonest, wait);
        }
        return soonest;
    }

    private int occupancy() {
        return queuedByChunk.size() + uploading.size();
    }
}
