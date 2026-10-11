package com.medislot.security;

import java.util.concurrent.ConcurrentHashMap;

/** Fixed-window request counter, one window per key. In memory, so it covers one backend instance. */
public final class RateLimiter {

    public record Result(boolean allowed, long retryAfterSeconds) {}

    private static final class Window {
        final long start;
        final long windowMs;
        int count;

        Window(long start, long windowMs) {
            this.start = start;
            this.windowMs = windowMs;
            this.count = 1;
        }
    }

    private static final int PURGE_THRESHOLD = 50_000;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public Result tryAcquire(String key, int limit, long windowMs, long nowMs) {
        if (windows.size() > PURGE_THRESHOLD) {
            windows.entrySet().removeIf(e -> nowMs - e.getValue().start >= e.getValue().windowMs);
        }
        Window w = windows.compute(key, (k, cur) -> {
            if (cur == null || nowMs - cur.start >= windowMs) return new Window(nowMs, windowMs);
            cur.count++;
            return cur;
        });
        if (w.count <= limit) return new Result(true, 0);
        long retryMs = Math.max(0, w.start + windowMs - nowMs);
        return new Result(false, Math.max(1, (retryMs + 999) / 1000));
    }
}