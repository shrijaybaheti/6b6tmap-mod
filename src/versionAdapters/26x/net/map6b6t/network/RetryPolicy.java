package net.map6b6t.network;

import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public final class RetryPolicy {
    public static final long MAX_DELAY_MS = 45_000L;
    public static final long BASE_DELAY_MS = 1_000L;

    private RetryPolicy() {}

    public static boolean isRetryableStatus(int statusCode) {
        return statusCode == 408 || statusCode == 429 || statusCode >= 500;
    }

    public static boolean isFatalStatus(int statusCode) {
        return statusCode == 400 || statusCode == 401 || statusCode == 403
                || statusCode == 404 || statusCode == 413 || statusCode == 415
                || statusCode == 422;
    }

    public static boolean isRetryableFailure(Throwable error, int statusCode) {
        if (statusCode > 0) {
            return isRetryableStatus(statusCode);
        }
        if (error == null) {
            return true;
        }
        Throwable cause = error;
        while (cause != null) {
            if (cause instanceof HttpTimeoutException) {
                return true;
            }
            String name = cause.getClass().getName();
            String msg = cause.getMessage() == null ? "" : cause.getMessage().toLowerCase();
            if (name.contains("Connect") || name.contains("Socket") || name.contains("ClosedChannel")
                    || msg.contains("connection") || msg.contains("timed out") || msg.contains("timeout")
                    || msg.contains("refused") || msg.contains("reset") || msg.contains("unreachable")) {
                return true;
            }
            cause = cause.getCause();
        }
        return true;
    }

    public static long delayMs(int attemptsAfterFailure, Duration retryAfter) {
        if (retryAfter != null && !retryAfter.isNegative() && !retryAfter.isZero()) {
            return Math.min(retryAfter.toMillis(), MAX_DELAY_MS);
        }
        int exp = Math.max(0, Math.min(attemptsAfterFailure - 1, 5));
        long base = Math.min(MAX_DELAY_MS, BASE_DELAY_MS << exp);
        long jitter = ThreadLocalRandom.current().nextLong(0, Math.max(1L, base / 5L + 1L));
        return Math.min(MAX_DELAY_MS, base + jitter);
    }

    public static Duration parseRetryAfter(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        try {
            double seconds = Double.parseDouble(header.trim());
            if (seconds < 0) {
                return null;
            }
            return Duration.ofMillis((long) (seconds * 1000.0));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
