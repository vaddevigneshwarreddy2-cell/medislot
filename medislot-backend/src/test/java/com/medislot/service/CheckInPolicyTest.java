package com.medislot.service;

import com.medislot.entity.AppointmentStatus;
import com.medislot.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class CheckInPolicyTest {

    private final LocalDate today = LocalDate.of(2026, 10, 12);
    private final Instant now = Instant.parse("2026-10-12T04:00:00Z");

    private String code(AppointmentStatus s, LocalDate date, Instant slot, boolean enforceEarly) {
        return assertThrows(ApiException.class,
                () -> CheckInPolicy.validate(s, date, today, slot, now, 60, enforceEarly)).getCode();
    }

    @Test
    void confirmedToday_withinWindow_isAllowed() {
        assertDoesNotThrow(() -> CheckInPolicy.validate(AppointmentStatus.CONFIRMED, today,
                today, now.plus(30, ChronoUnit.MINUTES), now, 60, true));
    }

    @Test
    void alreadyCheckedInIsRejected() {
        assertEquals("ALREADY_CHECKED_IN", code(AppointmentStatus.WAITING, today, now, true));
        assertEquals("ALREADY_CHECKED_IN", code(AppointmentStatus.COMPLETED, today, now, true));
    }

    @Test
    void cancelledRescheduledAndNoShowAreRejected() {
        assertEquals("APPOINTMENT_CANCELLED", code(AppointmentStatus.CANCELLED, today, now, true));
        assertEquals("APPOINTMENT_RESCHEDULED", code(AppointmentStatus.RESCHEDULED, today, now, true));
        assertEquals("APPOINTMENT_NO_SHOW", code(AppointmentStatus.NO_SHOW, today, now, true));
    }

    @Test
    void wrongDateIsRejected() {
        assertEquals("WRONG_DATE", code(AppointmentStatus.CONFIRMED, today.plusDays(1), now, true));
    }

    @Test
    void tooEarlyIsRejectedOnlyWhenEnforced() {
        Instant slot = now.plus(3, ChronoUnit.HOURS);
        assertEquals("TOO_EARLY", code(AppointmentStatus.CONFIRMED, today, slot, true));
        assertDoesNotThrow(() -> CheckInPolicy.validate(AppointmentStatus.CONFIRMED, today, today, slot, now, 60, false));
    }
}