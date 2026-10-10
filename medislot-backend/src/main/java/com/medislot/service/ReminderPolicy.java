package com.medislot.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Pure reminder rules, kept separate so they are easy to test. */
public final class ReminderPolicy {

    private ReminderPolicy() {}

    /** A reminder makes sense only if the booking was made at least windowHours before the visit. */
    public static boolean bookedEarlyEnough(Instant createdAt, Instant slotStart, int windowHours) {
        return !createdAt.isAfter(slotStart.minus(windowHours, ChronoUnit.HOURS));
    }
}