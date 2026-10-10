package com.medislot.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class BookingPolicyTest {

    private final Instant now = Instant.parse("2026-10-12T04:00:00Z");

    @Test
    void changeAllowedWellBeforeTheCutoff() {
        assertTrue(BookingPolicy.withinChangeWindow(now.plus(3, ChronoUnit.HOURS), now, 2));
    }

    @Test
    void changeAllowedExactlyAtTheCutoff() {
        assertTrue(BookingPolicy.withinChangeWindow(now.plus(2, ChronoUnit.HOURS), now, 2));
    }

    @Test
    void changeRejectedInsideTheCutoff() {
        assertFalse(BookingPolicy.withinChangeWindow(now.plus(1, ChronoUnit.HOURS), now, 2));
    }
}