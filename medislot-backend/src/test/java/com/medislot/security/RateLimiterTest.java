package com.medislot.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {

    private final RateLimiter limiter = new RateLimiter();

    @Test
    void allowsUpToTheLimitThenBlocks() {
        for (int i = 0; i < 3; i++) assertTrue(limiter.tryAcquire("k", 3, 60_000, 1_000).allowed());
        RateLimiter.Result blocked = limiter.tryAcquire("k", 3, 60_000, 2_000);
        assertFalse(blocked.allowed());
        assertEquals(59, blocked.retryAfterSeconds());
    }

    @Test
    void windowResetsAfterItExpires() {
        for (int i = 0; i < 4; i++) limiter.tryAcquire("k", 3, 60_000, 1_000);
        assertTrue(limiter.tryAcquire("k", 3, 60_000, 61_001).allowed());
    }

    @Test
    void keysAreIndependent() {
        for (int i = 0; i < 4; i++) limiter.tryAcquire("a", 3, 60_000, 1_000);
        assertTrue(limiter.tryAcquire("b", 3, 60_000, 1_000).allowed());
    }
}