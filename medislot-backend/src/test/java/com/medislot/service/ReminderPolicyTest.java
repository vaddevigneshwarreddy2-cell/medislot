package com.medislot.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class ReminderPolicyTest {

    private final Instant slot = Instant.parse("2026-10-12T10:00:00Z");

    @Test
    void bookedDaysAheadGetsAReminder() {
        assertTrue(ReminderPolicy.bookedEarlyEnough(slot.minus(3, ChronoUnit.DAYS), slot, 24));
    }

    @Test
    void bookedExactlyAtTheWindowEdgeGetsAReminder() {
        assertTrue(ReminderPolicy.bookedEarlyEnough(slot.minus(24, ChronoUnit.HOURS), slot, 24));
    }

    @Test
    void bookedInsideTheWindowGetsNoReminder() {
        assertFalse(ReminderPolicy.bookedEarlyEnough(slot.minus(5, ChronoUnit.HOURS), slot, 24));
    }
}