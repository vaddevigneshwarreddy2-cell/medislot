package com.medislot.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Pure booking rules, kept separate so they are easy to test. */
public final class BookingPolicy {

    private BookingPolicy() {}

    /** True while the change (cancel/reschedule) is still allowed: up to cutoffHours before the slot. */
    public static boolean withinChangeWindow(Instant slotStart, Instant now, int cutoffHours) {
        return !now.isAfter(slotStart.minus(cutoffHours, ChronoUnit.HOURS));
    }
}